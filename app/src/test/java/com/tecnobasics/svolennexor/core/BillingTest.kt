package com.tecnobasics.svolennexor.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class BillingTest {

    private fun day(s: String) = LocalDate.parse(s).toEpochDay()
    private val today = day("2026-10-30")

    private fun project(installments: List<Installment>, status: ProjectStatus = ProjectStatus.ACTIVE) = Project(
        id = "p1", clientId = "c1", title = "Sitio web", notes = "", total = 3_000_000,
        status = status, milestones = emptyList(), installments = installments,
        deliveryReminders = null, chargeReminders = null, createdMs = 0,
    )

    private fun inst(id: String, amount: Long, due: String) = Installment(id, "Pago $id", amount, day(due), 0)
    private fun pay(amount: Long, on: String, projectId: String? = "p1", serviceId: String? = null, period: Long? = null) =
        Payment("x$amount$on", projectId, serviceId, period, amount, day(on), "", 0)

    @Test
    fun reparte_los_pagos_en_orden() {
        val p = project(listOf(inst("b", 1_500_000, "2026-12-01"), inst("a", 1_500_000, "2026-10-01")))
        val m = Billing.money(p, listOf(pay(2_000_000, "2026-10-02")), today)
        assertEquals(2_000_000L, m.paid)
        assertEquals(1_000_000L, m.pending)
        // La de octubre se cubre completa; la de diciembre queda a medias.
        assertEquals("a", m.states[0].installment.id)
        assertTrue(m.states[0].settled)
        assertTrue(m.states[1].partial)
        assertEquals(1_000_000L, m.states[1].remaining)
        assertEquals(0L, m.overdue)
    }

    @Test
    fun lo_vencido_es_lo_que_no_se_cobro_a_tiempo() {
        val p = project(listOf(inst("a", 1_500_000, "2026-10-01"), inst("b", 1_500_000, "2026-12-01")))
        val m = Billing.money(p, listOf(pay(500_000, "2026-10-02")), today)
        assertEquals(1_000_000L, m.overdue)
        assertEquals(0L, m.unplanned)
    }

    @Test
    fun cancelado_no_genera_cobros() {
        val p = project(listOf(inst("a", 1_500_000, "2026-10-01")), ProjectStatus.CANCELLED)
        val d = AppData(projects = listOf(p))
        assertTrue(Billing.pendingCharges(d).isEmpty())
        assertEquals(0L, Billing.money(p, emptyList(), today).overdue)
    }

    private fun domain(due: String, renewedMs: Long? = null) = Service(
        id = "s1", clientId = "c1", kind = ServiceKind.DOMAIN, name = "ejemplo.com.mx", amount = 60_000,
        periodMonths = 12, dueDay = day(due), renewedMs = renewedMs, active = true, reminders = null,
        notes = "", armedMs = 0, createdMs = 0,
    )

    @Test
    fun dominio_cobrado_sin_renovar_no_avanza() {
        val s = domain("2026-11-15")
        val pays = listOf(pay(60_000, "2026-11-01", projectId = null, serviceId = "s1", period = s.dueDay))
        assertTrue(Billing.servicePaid(s, pays))
        val same = Billing.closeIfComplete(s, pays, 10)
        assertEquals(s.dueDay, same.dueDay)
        // Ya no hay cobro pendiente, pero sigue el aviso de renovar.
        val events = Agenda.events(AppData(services = listOf(s), payments = pays))
        assertEquals(listOf(EventKind.RENEW), events.map { it.kind })
    }

    @Test
    fun dominio_cobrado_y_renovado_pasa_al_siguiente_anio() {
        val s = domain("2026-11-15", renewedMs = 5)
        val pays = listOf(pay(60_000, "2026-11-01", projectId = null, serviceId = "s1", period = s.dueDay))
        val next = Billing.closeIfComplete(s, pays, 99)
        assertEquals(day("2027-11-15"), next.dueDay)
        assertFalse(next.renewed)
        assertEquals(99L, next.armedMs)
        // El nuevo periodo vuelve a estar pendiente de cobro.
        assertFalse(Billing.servicePaid(next, pays))
    }

    @Test
    fun hosting_se_cierra_solo_con_cobrar() {
        val s = domain("2026-11-15").copy(kind = ServiceKind.HOSTING, periodMonths = 1)
        val pays = listOf(pay(60_000, "2026-11-01", projectId = null, serviceId = "s1", period = s.dueDay))
        assertEquals(day("2026-12-15"), Billing.closeIfComplete(s, pays, 1).dueDay)
    }

    @Test
    fun mes_corto_no_se_desborda() {
        assertEquals(day("2027-02-28"), Billing.nextDue(day("2027-01-31"), 1))
    }

    @Test
    fun resumen_del_mes() {
        val p = project(listOf(inst("a", 1_000_000, "2026-10-01"), inst("b", 1_000_000, "2026-10-31"), inst("c", 1_000_000, "2026-11-20")))
        val s = domain("2026-10-30")
        val d = AppData(
            projects = listOf(p),
            services = listOf(s),
            payments = listOf(pay(400_000, "2026-10-05")),
        )
        val sum = Billing.summary(d, today)
        assertEquals(600_000L, sum.overdue) // resto de la parcialidad a
        assertEquals(1_060_000L, sum.dueThisMonth) // parcialidad b + dominio que vence hoy
        assertEquals(400_000L, sum.collectedThisMonth)
        assertEquals(2_660_000L, sum.pendingTotal)
    }
}
