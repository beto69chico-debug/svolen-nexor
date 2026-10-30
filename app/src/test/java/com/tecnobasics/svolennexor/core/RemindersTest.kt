package com.tecnobasics.svolennexor.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class RemindersTest {

    private val zone: ZoneId = ZoneId.of("America/Mexico_City")
    private fun day(s: String) = LocalDate.parse(s).toEpochDay()
    private fun at(s: String) = java.time.LocalDateTime.parse(s).atZone(zone).toInstant().toEpochMilli()

    private fun event(due: String, offsets: List<Int>, armed: String = "2026-09-01T00:00", key: String = "e1") = Event(
        key = key, kind = EventKind.CHARGE, clientId = "c1", projectId = "p1", serviceId = null,
        title = "Anticipo", detail = "Sitio", dueDay = day(due), amount = 100, armedMs = at(armed), offsets = offsets,
    )

    @Test
    fun avisa_30_15_y_3_dias_antes_a_la_hora_fijada() {
        val e = event("2026-11-30", listOf(30, 15, 3))
        val hits = Reminders.all(listOf(e), 9, zone).map { it.atMs }.sorted()
        assertEquals(listOf(at("2026-10-31T09:00"), at("2026-11-15T09:00"), at("2026-11-27T09:00")), hits)
    }

    @Test
    fun manda_el_que_toca_en_el_intervalo() {
        val e = event("2026-11-30", listOf(30, 15, 3))
        val due = Reminders.due(listOf(e), 9, zone, at("2026-10-31T00:00"), at("2026-10-31T09:00"))
        assertEquals(1, due.size)
        assertEquals(30, due[0].daysBefore)
    }

    @Test
    fun no_avisa_lo_que_caia_antes_de_crearlo() {
        // Creado a 10 días: el de 30 y el de 15 ya no suenan.
        val e = event("2026-11-30", listOf(30, 15, 3), armed = "2026-11-20T12:00")
        val hits = Reminders.all(listOf(e), 9, zone)
        assertEquals(listOf(3), hits.map { it.daysBefore })
    }

    @Test
    fun telefono_apagado_solo_manda_el_mas_reciente() {
        val e = event("2026-11-30", listOf(30, 15, 3))
        val due = Reminders.due(listOf(e), 9, zone, at("2026-10-01T00:00"), at("2026-11-28T00:00"))
        assertEquals(1, due.size)
        assertEquals(3, due[0].daysBefore)
    }

    @Test
    fun el_mismo_dia_cuenta_y_despues_ya_no() {
        val e = event("2026-11-30", listOf(0))
        assertEquals(1, Reminders.due(listOf(e), 9, zone, at("2026-11-30T00:00"), at("2026-11-30T09:00")).size)
        assertTrue(Reminders.due(listOf(e), 9, zone, at("2026-11-30T00:00"), at("2026-12-01T00:30")).isEmpty())
    }

    @Test
    fun el_siguiente_aviso() {
        val a = event("2026-11-30", listOf(3), key = "a")
        val b = event("2026-11-10", listOf(1), key = "b")
        assertEquals(at("2026-11-09T09:00"), Reminders.next(listOf(a, b), 9, zone, at("2026-11-01T00:00")))
        assertNull(Reminders.next(listOf(a, b), 9, zone, at("2026-12-01T00:00")))
    }

    @Test
    fun textos_de_los_avisos() {
        assertEquals("30, 15 y 3 días antes", Reminders.summary(listOf(3, 30, 15)))
        assertEquals("1 día antes", Reminders.summary(listOf(1)))
        assertEquals("3 días antes y el mismo día", Reminders.summary(listOf(0, 3)))
        assertEquals("El mismo día", Reminders.summary(listOf(0)))
        assertEquals("Sin avisos", Reminders.summary(emptyList()))
    }

    @Test
    fun timeline_por_cubetas() {
        val today = day("2026-10-30")
        assertEquals(Bucket.OVERDUE, Agenda.bucket(today - 1, today))
        assertEquals(Bucket.TODAY, Agenda.bucket(today, today))
        assertEquals(Bucket.WEEK, Agenda.bucket(today + 7, today))
        assertEquals(Bucket.MONTH, Agenda.bucket(today + 8, today))
        assertEquals(Bucket.LATER, Agenda.bucket(today + 31, today))
        assertEquals("Venció hace 3 días", Agenda.countdown(today - 3, today))
        assertEquals("Mañana", Agenda.countdown(today + 1, today))
    }

    @Test
    fun hitos_entregados_y_proyectos_cerrados_no_aparecen() {
        val m1 = Milestone("m1", "Diseño", day("2026-11-05"), null, 0)
        val m2 = Milestone("m2", "Desarrollo", day("2026-11-20"), 123, 0)
        val p = Project("p1", "c1", "Sitio", "", 0, ProjectStatus.ACTIVE, listOf(m1, m2), emptyList(), null, null, 0)
        assertEquals(listOf("m:m1"), Agenda.events(AppData(projects = listOf(p))).map { it.key })
        val delivered = p.copy(status = ProjectStatus.DELIVERED)
        assertTrue(Agenda.events(AppData(projects = listOf(delivered))).isEmpty())
    }
}
