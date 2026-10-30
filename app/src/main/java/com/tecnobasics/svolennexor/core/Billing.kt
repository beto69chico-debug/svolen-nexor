package com.tecnobasics.svolennexor.core

import java.time.LocalDate

/** Cómo va una parcialidad con lo que ya se cobró. */
data class InstallmentState(val installment: Installment, val paid: Long) {
    val remaining: Long get() = (installment.amount - paid).coerceAtLeast(0)
    val settled: Boolean get() = remaining == 0L
    val partial: Boolean get() = paid > 0 && !settled
}

/** Las cuentas de un proyecto. */
data class ProjectMoney(
    val total: Long,
    val paid: Long,
    /** Suma de las parcialidades del plan. */
    val planned: Long,
    /** Lo de parcialidades con fecha pasada que no se ha cobrado. */
    val overdue: Long,
    val states: List<InstallmentState>,
) {
    val pending: Long get() = (total - paid).coerceAtLeast(0)

    /** Lo que falta por meter al plan de pagos (positivo) o lo que sobra (negativo). */
    val unplanned: Long get() = total - planned

    /** 0..1 para la barra de avance. */
    val progress: Float get() = if (total <= 0) 0f else (paid.toFloat() / total).coerceIn(0f, 1f)
}

object Billing {

    fun projectPayments(projectId: String, payments: List<Payment>): List<Payment> =
        payments.filter { it.projectId == projectId }.sortedWith(compareBy({ it.day }, { it.createdMs }))

    /**
     * Reparte lo cobrado entre las parcialidades, de la más antigua a la más nueva.
     * Así no hace falta decir "este pago es de la parcialidad 2": se cubren en orden.
     */
    fun allocate(installments: List<Installment>, paidTotal: Long): List<InstallmentState> {
        var left = paidTotal
        return installments.sortedBy { it.dueDay }.map { inst ->
            val take = minOf(left, inst.amount).coerceAtLeast(0)
            left -= take
            InstallmentState(inst, take)
        }
    }

    fun money(p: Project, payments: List<Payment>, today: Long): ProjectMoney {
        val paid = projectPayments(p.id, payments).sumOf { it.amount }
        val states = allocate(p.installments, paid)
        val overdue = if (p.status == ProjectStatus.CANCELLED) 0L
        else states.filter { it.installment.dueDay < today }.sumOf { it.remaining }
        return ProjectMoney(
            total = p.total,
            paid = paid,
            planned = p.installments.sumOf { it.amount },
            overdue = overdue,
            states = states,
        )
    }

    // ---- Servicios ----

    /** Ya se cobró el periodo actual del servicio. */
    fun servicePaid(s: Service, payments: List<Payment>): Boolean =
        payments.any { it.serviceId == s.id && it.periodDay == s.dueDay }

    fun nextDue(dueDay: Long, months: Int): Long =
        LocalDate.ofEpochDay(dueDay).plusMonths(months.coerceAtLeast(1).toLong()).toEpochDay()

    /**
     * Si el periodo actual ya está completo (cobrado y, si es dominio, renovado),
     * pasa al siguiente periodo y re-arma sus avisos.
     */
    fun closeIfComplete(s: Service, payments: List<Payment>, nowMs: Long): Service {
        val complete = servicePaid(s, payments) && (!s.needsRenewal || s.renewed)
        if (!complete) return s
        return s.copy(dueDay = nextDue(s.dueDay, s.periodMonths), renewedMs = null, armedMs = nowMs)
    }

    // ---- Lo que hay por cobrar ----

    /** Un cobro pendiente: el resto de una parcialidad o el periodo de un servicio. */
    data class Charge(
        val key: String,
        val clientId: String,
        val projectId: String?,
        val serviceId: String?,
        val concept: String,
        val source: String,
        val amount: Long,
        val dueDay: Long,
    )

    fun pendingCharges(d: AppData): List<Charge> {
        val out = mutableListOf<Charge>()
        d.projects.filter { it.status != ProjectStatus.CANCELLED }.forEach { p ->
            val paid = projectPayments(p.id, d.payments).sumOf { it.amount }
            allocate(p.installments, paid).filter { !it.settled }.forEach { st ->
                out += Charge(
                    key = "i:${st.installment.id}",
                    clientId = p.clientId,
                    projectId = p.id,
                    serviceId = null,
                    concept = st.installment.label.ifBlank { "Parcialidad" },
                    source = p.title,
                    amount = st.remaining,
                    dueDay = st.installment.dueDay,
                )
            }
        }
        d.services.filter { it.active && !servicePaid(it, d.payments) }.forEach { s ->
            out += Charge(
                key = "s:${s.id}",
                clientId = s.clientId,
                projectId = null,
                serviceId = s.id,
                concept = s.kind.label,
                source = s.name,
                amount = s.amount,
                dueDay = s.dueDay,
            )
        }
        return out.sortedBy { it.dueDay }
    }

    /** Las cifras de la pantalla de inicio. */
    data class Summary(
        val dueThisMonth: Long,
        val overdue: Long,
        val collectedThisMonth: Long,
        val pendingTotal: Long,
    )

    fun summary(d: AppData, today: Long): Summary {
        val t = LocalDate.ofEpochDay(today)
        val monthStart = t.withDayOfMonth(1).toEpochDay()
        val monthEnd = t.withDayOfMonth(t.lengthOfMonth()).toEpochDay()
        val charges = pendingCharges(d)
        return Summary(
            dueThisMonth = charges.filter { it.dueDay in today..monthEnd }.sumOf { it.amount },
            overdue = charges.filter { it.dueDay < today }.sumOf { it.amount },
            collectedThisMonth = d.payments.filter { it.day in monthStart..monthEnd }.sumOf { it.amount },
            pendingTotal = charges.sumOf { it.amount },
        )
    }

    /** Totales por cliente para su ficha. */
    data class ClientMoney(val collected: Long, val pending: Long, val overdue: Long)

    fun clientMoney(d: AppData, clientId: String, today: Long): ClientMoney {
        val projectIds = d.projects.filter { it.clientId == clientId }.map { it.id }.toSet()
        val serviceIds = d.services.filter { it.clientId == clientId }.map { it.id }.toSet()
        val collected = d.payments.filter { it.projectId in projectIds || it.serviceId in serviceIds }.sumOf { it.amount }
        val charges = pendingCharges(d).filter { it.clientId == clientId }
        return ClientMoney(collected, charges.sumOf { it.amount }, charges.filter { it.dueDay < today }.sumOf { it.amount })
    }
}
