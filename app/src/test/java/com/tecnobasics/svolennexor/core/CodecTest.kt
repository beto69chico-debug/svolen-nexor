package com.tecnobasics.svolennexor.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CodecTest {

    private val sample = AppData(
        clients = listOf(Client("c1", "Cliente Demo", "Demo SA", "Ana", "55 1234 5678", "ana@demo.mx", "nota, con coma", 3)),
        projects = listOf(
            Project(
                id = "p1", clientId = "c1", title = "Sitio web", notes = "", total = 3_000_000,
                status = ProjectStatus.REVIEW,
                milestones = listOf(Milestone("m1", "Diseño", 20_400, null, 7), Milestone("m2", "Lanzamiento", 20_430, 99, 7)),
                installments = listOf(Installment("i1", "Anticipo", 1_500_000, 20_390, 7)),
                deliveryReminders = listOf(7, 0), chargeReminders = null, createdMs = 7,
            ),
        ),
        services = listOf(
            Service("s1", "c1", ServiceKind.DOMAIN, "demo.mx", 60_000, 12, 20_500, null, true, listOf(30, 15, 3), "", 7, 7),
        ),
        payments = listOf(
            Payment("pay1", "p1", null, null, 1_000_000, 20_391, "Transferencia", 8),
            Payment("pay2", null, "s1", 20_500, 60_000, 20_480, "", 9),
        ),
        settings = Settings(notifyHour = 10, signature = "Alberto · Tecnobasics"),
        lastCheckMs = 123,
    )

    @Test
    fun ida_y_vuelta_sin_perder_nada() {
        val text = Codec.encode(sample)
        assertEquals(sample, Codec.decode(text))
        assertTrue(Codec.looksLikeBackup(text))
    }

    @Test
    fun json_ajeno_no_es_respaldo() {
        assertFalse(Codec.looksLikeBackup("{\"tasks\":[]}"))
        assertFalse(Codec.looksLikeBackup("no es json"))
    }

    @Test
    fun tolera_campos_faltantes() {
        val d = Codec.decode("{\"clients\":[{\"id\":\"c1\",\"name\":\"X\"}],\"projects\":[{\"id\":\"p1\",\"clientId\":\"c1\"}]}")
        assertEquals("X", d.clients[0].name)
        assertEquals(ProjectStatus.ACTIVE, d.projects[0].status)
        assertEquals(Reminders.DEFAULT_CHARGE, d.settings.chargeReminders)
        assertEquals(9, d.settings.notifyHour)
    }

    @Test
    fun csv_para_excel() {
        val csv = Csv.export(sample, 20_395)
        val lines = csv.removePrefix("﻿").trim().split("\r\n")
        assertEquals("Tipo,Cliente,Proyecto o servicio,Concepto,Fecha,Monto,Estado", lines[0])
        assertTrue(lines.any { it.startsWith("Pago recibido,Cliente Demo,Sitio web,Transferencia,") && it.endsWith(",10000,Cobrado") })
        // Anticipo de 15,000 con 10,000 pagados: quedan 5,000 vencidos.
        assertTrue(lines.any { it.startsWith("Por cobrar,Cliente Demo,Sitio web,Anticipo,") && it.endsWith(",5000,Vencido") })
    }

    @Test
    fun mensaje_de_cobro() {
        val c = Billing.pendingCharges(sample).first { it.projectId == "p1" }
        val text = Messages.chargeText(sample.clients[0], listOf(c), 20_395, "Alberto")
        assertTrue(text.startsWith("Hola Ana, ¿cómo estás?"))
        assertTrue(text.contains("\$5,000"))
        assertTrue(text.contains("Anticipo de Sitio web"))
        assertTrue(text.trimEnd().endsWith("Alberto"))
    }

    @Test
    fun telefono_para_whatsapp() {
        assertEquals("525512345678", Messages.waPhone("55 1234 5678"))
        assertEquals("525512345678", Messages.waPhone("+52 1 55 1234 5678"))
        assertEquals("16195550100", Messages.waPhone("+1 (619) 555-0100"))
        assertEquals(null, Messages.waPhone("123"))
    }
}
