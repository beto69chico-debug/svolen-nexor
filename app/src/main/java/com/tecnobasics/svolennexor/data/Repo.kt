package com.tecnobasics.svolennexor.data

import android.content.Context
import android.util.AtomicFile
import com.tecnobasics.svolennexor.core.AppData
import com.tecnobasics.svolennexor.core.Billing
import com.tecnobasics.svolennexor.core.Client
import com.tecnobasics.svolennexor.core.Codec
import com.tecnobasics.svolennexor.core.Payment
import com.tecnobasics.svolennexor.core.Project
import com.tecnobasics.svolennexor.core.ProjectStatus
import com.tecnobasics.svolennexor.core.Service
import com.tecnobasics.svolennexor.core.Settings
import com.tecnobasics.svolennexor.reminders.ReminderController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.time.LocalDate
import java.util.UUID

/** Todo vive en un archivo dentro del teléfono: sin internet y sin cuentas. */
object Repo {
    private val lock = Any()
    private val state = MutableStateFlow<AppData?>(null)
    val data: StateFlow<AppData?> = state

    private fun file(ctx: Context) = AtomicFile(File(ctx.applicationContext.filesDir, "svolen.json"))

    fun get(ctx: Context): AppData = synchronized(lock) {
        state.value ?: load(ctx).also { state.value = it }
    }

    private fun load(ctx: Context): AppData {
        val f = file(ctx)
        val loaded = try {
            if (f.baseFile.exists()) Codec.decode(String(f.readFully(), Charsets.UTF_8)) else null
        } catch (_: Exception) {
            null
        }
        // Primera vez: se empieza a contar desde ahora para no mandar avisos viejos.
        return loaded ?: AppData(lastCheckMs = System.currentTimeMillis())
    }

    private fun save(ctx: Context, d: AppData) {
        val f = file(ctx)
        val out = f.startWrite()
        try {
            out.write(Codec.encode(d).toByteArray(Charsets.UTF_8))
            f.finishWrite(out)
        } catch (e: Exception) {
            f.failWrite(out)
            throw e
        }
    }

    /** Cambia los datos, los guarda y vuelve a acomodar los avisos. */
    fun update(ctx: Context, change: (AppData) -> AppData) {
        synchronized(lock) {
            val next = change(get(ctx))
            save(ctx, next)
            state.value = next
        }
        ReminderController.check(ctx)
    }

    /** Solo para el revisor de avisos: no vuelve a disparar la revisión. */
    internal fun setLastCheck(ctx: Context, ms: Long) = synchronized(lock) {
        val next = get(ctx).copy(lastCheckMs = ms)
        save(ctx, next)
        state.value = next
    }

    fun newId(): String = UUID.randomUUID().toString()

    fun today(): Long = LocalDate.now().toEpochDay()

    private fun now() = System.currentTimeMillis()

    // ---- Clientes ----

    fun saveClient(ctx: Context, c: Client) = update(ctx) { d ->
        val exists = d.clients.any { it.id == c.id }
        d.copy(clients = if (exists) d.clients.map { if (it.id == c.id) c else it } else d.clients + c)
    }

    /** Borra al cliente con sus proyectos, servicios y pagos. */
    fun deleteClient(ctx: Context, id: String) = update(ctx) { d ->
        val pIds = d.projects.filter { it.clientId == id }.map { it.id }.toSet()
        val sIds = d.services.filter { it.clientId == id }.map { it.id }.toSet()
        d.copy(
            clients = d.clients.filterNot { it.id == id },
            projects = d.projects.filterNot { it.id in pIds },
            services = d.services.filterNot { it.id in sIds },
            payments = d.payments.filterNot { it.projectId in pIds || it.serviceId in sIds },
        )
    }

    // ---- Proyectos ----

    /**
     * Guarda el proyecto. Los hitos y parcialidades nuevos, o cuya fecha o avisos cambiaron,
     * se re-arman para que no suenen de golpe avisos atrasados.
     */
    fun saveProject(ctx: Context, p: Project) = update(ctx) { d ->
        val old = d.projects.firstOrNull { it.id == p.id }
        val t = now()
        val deliveryChanged = old == null || old.deliveryReminders != p.deliveryReminders
        val chargeChanged = old == null || old.chargeReminders != p.chargeReminders
        val fixed = p.copy(
            milestones = p.milestones.map { m ->
                val o = old?.milestones?.firstOrNull { it.id == m.id }
                if (o == null || o.dueDay != m.dueDay || deliveryChanged) m.copy(armedMs = t) else m.copy(armedMs = o.armedMs)
            },
            installments = p.installments.map { i ->
                val o = old?.installments?.firstOrNull { it.id == i.id }
                if (o == null || o.dueDay != i.dueDay || o.amount != i.amount || chargeChanged) i.copy(armedMs = t) else i.copy(armedMs = o.armedMs)
            },
        )
        d.copy(projects = if (old != null) d.projects.map { if (it.id == p.id) fixed else it } else d.projects + fixed)
    }

    fun deleteProject(ctx: Context, id: String) = update(ctx) { d ->
        d.copy(projects = d.projects.filterNot { it.id == id }, payments = d.payments.filterNot { it.projectId == id })
    }

    fun setProjectStatus(ctx: Context, id: String, status: ProjectStatus) = update(ctx) { d ->
        val t = now()
        d.copy(projects = d.projects.map { p ->
            if (p.id != id) p
            // Al reabrir un proyecto, lo que pasó mientras estaba cerrado no suena de golpe.
            else if (!p.status.open && status.open) p.copy(status = status, milestones = p.milestones.map { it.copy(armedMs = t) })
            else p.copy(status = status)
        })
    }

    fun setMilestoneDone(ctx: Context, projectId: String, milestoneId: String, done: Boolean) = update(ctx) { d ->
        val t = now()
        d.copy(projects = d.projects.map { p ->
            if (p.id != projectId) p else p.copy(milestones = p.milestones.map { m ->
                when {
                    m.id != milestoneId -> m
                    done -> m.copy(doneMs = t)
                    else -> m.copy(doneMs = null, armedMs = t)
                }
            })
        })
    }

    // ---- Pagos ----

    fun addPayment(ctx: Context, p: Payment) = update(ctx) { d ->
        val withPay = d.copy(payments = d.payments + p)
        closeService(withPay, p.serviceId)
    }

    fun deletePayment(ctx: Context, id: String) = update(ctx) { d ->
        d.copy(payments = d.payments.filterNot { it.id == id })
    }

    // ---- Servicios ----

    fun saveService(ctx: Context, s: Service) = update(ctx) { d ->
        val old = d.services.firstOrNull { it.id == s.id }
        val rearm = old == null || old.dueDay != s.dueDay || old.reminders != s.reminders || (!old.active && s.active)
        val fixed = if (rearm) s.copy(armedMs = now()) else s
        d.copy(services = if (old != null) d.services.map { if (it.id == s.id) fixed else it } else d.services + fixed)
    }

    fun deleteService(ctx: Context, id: String) = update(ctx) { d ->
        d.copy(services = d.services.filterNot { it.id == id }, payments = d.payments.filterNot { it.serviceId == id })
    }

    /** Cobro del periodo actual de un servicio. Si ya está completo, pasa al siguiente periodo. */
    fun chargeService(ctx: Context, serviceId: String, amount: Long, day: Long, note: String) = update(ctx) { d ->
        val s = d.service(serviceId)
        if (s == null) d
        else closeService(d.copy(payments = d.payments + Payment(newId(), null, s.id, s.dueDay, amount, day, note, now())), s.id)
    }

    /** "Ya renové el dominio" (o deshacerlo). */
    fun setServiceRenewed(ctx: Context, serviceId: String, renewed: Boolean) = update(ctx) { d ->
        val withFlag = d.copy(services = d.services.map {
            if (it.id == serviceId) it.copy(renewedMs = if (renewed) now() else null) else it
        })
        closeService(withFlag, serviceId)
    }

    /** Pasa al siguiente periodo sin cobrar (por ejemplo, ese año no se cobró). */
    fun skipServicePeriod(ctx: Context, serviceId: String) = update(ctx) { d ->
        d.copy(services = d.services.map {
            if (it.id == serviceId) it.copy(dueDay = Billing.nextDue(it.dueDay, it.periodMonths), renewedMs = null, armedMs = now()) else it
        })
    }

    private fun closeService(d: AppData, serviceId: String?): AppData {
        if (serviceId == null) return d
        val t = now()
        return d.copy(services = d.services.map { if (it.id == serviceId) Billing.closeIfComplete(it, d.payments, t) else it })
    }

    // ---- Ajustes ----

    /** Cambiar avisos u hora no debe disparar avisos atrasados de golpe: se re-arma todo. */
    fun saveSettings(ctx: Context, s: Settings) = update(ctx) { d ->
        val old = d.settings
        val rearm = old.deliveryReminders != s.deliveryReminders || old.chargeReminders != s.chargeReminders ||
            old.serviceReminders != s.serviceReminders || old.notifyHour != s.notifyHour
        val base = d.copy(settings = s)
        if (rearm) rearmAll(base, now()) else base
    }

    /** Reemplaza todo con un respaldo. Solo cuenta lo que pase de aquí en adelante. */
    fun replaceAll(ctx: Context, imported: AppData) = update(ctx) { _ ->
        val t = now()
        rearmAll(imported, t).copy(lastCheckMs = t)
    }

    private fun rearmAll(d: AppData, t: Long): AppData = d.copy(
        projects = d.projects.map { p ->
            p.copy(
                milestones = p.milestones.map { it.copy(armedMs = maxOf(it.armedMs, t)) },
                installments = p.installments.map { it.copy(armedMs = maxOf(it.armedMs, t)) },
            )
        },
        services = d.services.map { it.copy(armedMs = maxOf(it.armedMs, t)) },
    )

    fun exportJson(ctx: Context): String = Codec.encode(get(ctx))
}
