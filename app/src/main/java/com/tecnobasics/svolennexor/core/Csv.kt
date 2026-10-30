package com.tecnobasics.svolennexor.core

/** Exporta la cobranza para abrirla en Excel: lo cobrado y lo que falta por cobrar. */
object Csv {
    private const val HEADER = "Tipo,Cliente,Proyecto o servicio,Concepto,Fecha,Monto,Estado"

    fun export(d: AppData, today: Long): String {
        val rows = mutableListOf<List<String>>()
        d.payments.sortedBy { it.day }.forEach { p ->
            val project = d.project(p.projectId)
            val service = d.service(p.serviceId)
            val client = d.client(project?.clientId ?: service?.clientId)
            rows += listOf(
                "Pago recibido",
                client?.display ?: "",
                project?.title ?: service?.name ?: "",
                p.note.ifBlank { service?.kind?.label ?: "Pago" },
                Fmt.iso(p.day),
                Money.plain(p.amount).ifEmpty { "0" },
                "Cobrado",
            )
        }
        Billing.pendingCharges(d).forEach { c ->
            rows += listOf(
                "Por cobrar",
                d.client(c.clientId)?.display ?: "",
                c.source,
                c.concept,
                Fmt.iso(c.dueDay),
                Money.plain(c.amount).ifEmpty { "0" },
                if (c.dueDay < today) "Vencido" else "Pendiente",
            )
        }
        // BOM para que Excel respete los acentos.
        return "﻿" + (listOf(HEADER) + rows.map { r -> r.joinToString(",") { cell(it) } }).joinToString("\r\n") + "\r\n"
    }

    private fun cell(s: String): String =
        if (s.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + s.replace("\"", "\"\"") + "\"" else s
}
