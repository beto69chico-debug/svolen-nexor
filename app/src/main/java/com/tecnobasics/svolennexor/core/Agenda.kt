package com.tecnobasics.svolennexor.core

import java.time.LocalDate

enum class EventKind(val label: String) {
    DELIVERY("Entrega"),
    CHARGE("Cobro"),
    SERVICE("Servicio"),
    RENEW("Renovar dominio"),
}

/** Algo con fecha que hay que atender: una entrega, un cobro, un servicio por vencer. */
data class Event(
    val key: String,
    val kind: EventKind,
    val clientId: String,
    val projectId: String?,
    val serviceId: String?,
    val title: String,
    val detail: String,
    val dueDay: Long,
    val amount: Long?,
    val armedMs: Long,
    val offsets: List<Int>,
)

/** En qué parte del timeline cae cada cosa. */
enum class Bucket(val title: String) {
    OVERDUE("Vencido"),
    TODAY("Hoy"),
    WEEK("Próximos 7 días"),
    MONTH("Próximos 30 días"),
    LATER("Más adelante"),
}

/** Qué tan encima está; decide el color. */
enum class Urgency { OVERDUE, HIGH, MID, LOW }

object Agenda {

    /** Todo lo pendiente, con sus avisos. */
    fun events(d: AppData): List<Event> {
        val s = d.settings
        val out = mutableListOf<Event>()
        d.projects.forEach { p ->
            if (p.status.open) {
                p.milestones.filter { !it.done }.forEach { m ->
                    out += Event(
                        key = "m:${m.id}",
                        kind = EventKind.DELIVERY,
                        clientId = p.clientId,
                        projectId = p.id,
                        serviceId = null,
                        title = m.title.ifBlank { "Entrega" },
                        detail = p.title,
                        dueDay = m.dueDay,
                        amount = null,
                        armedMs = m.armedMs,
                        offsets = p.deliveryReminders ?: s.deliveryReminders,
                    )
                }
            }
            if (p.status != ProjectStatus.CANCELLED) {
                val paid = Billing.projectPayments(p.id, d.payments).sumOf { it.amount }
                Billing.allocate(p.installments, paid).filter { !it.settled }.forEach { st ->
                    val i = st.installment
                    out += Event(
                        key = "i:${i.id}",
                        kind = EventKind.CHARGE,
                        clientId = p.clientId,
                        projectId = p.id,
                        serviceId = null,
                        title = i.label.ifBlank { "Parcialidad" },
                        detail = p.title,
                        dueDay = i.dueDay,
                        amount = st.remaining,
                        armedMs = i.armedMs,
                        offsets = p.chargeReminders ?: s.chargeReminders,
                    )
                }
            }
        }
        d.services.filter { it.active }.forEach { sv ->
            val offsets = sv.reminders ?: s.serviceReminders
            if (!Billing.servicePaid(sv, d.payments)) {
                out += Event(
                    key = "s:${sv.id}",
                    kind = EventKind.SERVICE,
                    clientId = sv.clientId,
                    projectId = null,
                    serviceId = sv.id,
                    title = "Cobrar ${sv.kind.label.lowercase()}",
                    detail = sv.name,
                    dueDay = sv.dueDay,
                    amount = sv.amount,
                    armedMs = sv.armedMs,
                    offsets = offsets,
                )
            }
            if (sv.needsRenewal && !sv.renewed) {
                out += Event(
                    key = "r:${sv.id}",
                    kind = EventKind.RENEW,
                    clientId = sv.clientId,
                    projectId = null,
                    serviceId = sv.id,
                    title = "Renovar dominio",
                    detail = sv.name,
                    dueDay = sv.dueDay,
                    amount = null,
                    armedMs = sv.armedMs,
                    offsets = offsets,
                )
            }
        }
        return out.sortedWith(compareBy({ it.dueDay }, { it.kind.ordinal }))
    }

    fun bucket(dueDay: Long, today: Long): Bucket = when {
        dueDay < today -> Bucket.OVERDUE
        dueDay == today -> Bucket.TODAY
        dueDay <= today + 7 -> Bucket.WEEK
        dueDay <= today + 30 -> Bucket.MONTH
        else -> Bucket.LATER
    }

    fun urgency(dueDay: Long, today: Long): Urgency {
        val left = dueDay - today
        return when {
            left < 0 -> Urgency.OVERDUE
            left <= 3 -> Urgency.HIGH
            left <= 15 -> Urgency.MID
            else -> Urgency.LOW
        }
    }

    /** "Hoy", "Mañana", "En 5 días", "Venció ayer", "Venció hace 3 días". */
    fun countdown(dueDay: Long, today: Long): String {
        val diff = dueDay - today
        return when {
            diff == 0L -> "Hoy"
            diff == 1L -> "Mañana"
            diff > 1 -> "En $diff días"
            diff == -1L -> "Venció ayer"
            else -> "Venció hace ${-diff} días"
        }
    }

    fun group(events: List<Event>, today: Long): List<Pair<Bucket, List<Event>>> =
        events.sortedBy { it.dueDay }
            .groupBy { bucket(it.dueDay, today) }
            .toList()
            .sortedBy { it.first.ordinal }
}

/** Fechas en español, a mano, para no depender de cómo cada teléfono traduce. */
object Fmt {
    private val DAYS = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")
    private val DAYS_LONG = listOf("lunes", "martes", "miércoles", "jueves", "viernes", "sábado", "domingo")
    private val MONTHS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
    private val MONTHS_LONG = listOf(
        "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre",
    )

    /** "vie 31 oct" (y el año si no es el actual). */
    fun date(day: Long, today: Long? = null): String {
        val d = LocalDate.ofEpochDay(day)
        val base = "${DAYS[d.dayOfWeek.value - 1]} ${d.dayOfMonth} ${MONTHS[d.monthValue - 1]}"
        val year = today?.let { LocalDate.ofEpochDay(it).year }
        return if (year != null && year != d.year) "$base ${d.year}" else base
    }

    /** "31 de octubre de 2026" */
    fun long(day: Long): String {
        val d = LocalDate.ofEpochDay(day)
        return "${d.dayOfMonth} de ${MONTHS_LONG[d.monthValue - 1]} de ${d.year}"
    }

    /** "octubre 2026" */
    fun month(day: Long): String {
        val d = LocalDate.ofEpochDay(day)
        return "${MONTHS_LONG[d.monthValue - 1]} ${d.year}"
    }

    /** "2026-10-31" para el CSV. */
    fun iso(day: Long): String = LocalDate.ofEpochDay(day).toString()

    /** "hoy", "mañana", "el viernes" o "el vie 7 nov" */
    fun dayLabel(day: Long, today: Long): String = when {
        day == today -> "hoy"
        day == today + 1 -> "mañana"
        day == today - 1 -> "ayer"
        day > today && day <= today + 6 -> "el ${DAYS_LONG[LocalDate.ofEpochDay(day).dayOfWeek.value - 1]}"
        else -> "el ${date(day, today)}"
    }

    fun hour(h: Int, h24: Boolean): String {
        if (h24) return "${h.toString().padStart(2, '0')}:00"
        val hh = if (h % 12 == 0) 12 else h % 12
        return "$hh:00 ${if (h < 12) "am" else "pm"}"
    }

    fun period(months: Int): String = when (months) {
        1 -> "Mensual"
        2 -> "Bimestral"
        3 -> "Trimestral"
        6 -> "Semestral"
        12 -> "Anual"
        24 -> "Cada 2 años"
        else -> "Cada $months meses"
    }
}
