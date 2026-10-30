package com.tecnobasics.svolennexor.core

/** Pesos mexicanos en centavos, escritos y leídos a mano para no depender del teléfono. */
object Money {

    /** 1250050 -> "$12,500.50"; 1250000 -> "$12,500". */
    fun format(cents: Long, alwaysDecimals: Boolean = false): String {
        val neg = cents < 0
        val abs = kotlin.math.abs(cents)
        val pesos = abs / 100
        val cts = abs % 100
        val grouped = pesos.toString().reversed().chunked(3).joinToString(",").reversed()
        val dec = if (cts != 0L || alwaysDecimals) "." + cts.toString().padStart(2, '0') else ""
        return (if (neg) "-$" else "$") + grouped + dec
    }

    /** Para el campo de texto al editar: 1250050 -> "12500.50"; 1250000 -> "12500". */
    fun plain(cents: Long): String {
        if (cents == 0L) return ""
        val pesos = cents / 100
        val cts = cents % 100
        return if (cts == 0L) pesos.toString() else "$pesos." + cts.toString().padStart(2, '0')
    }

    /**
     * Lee lo que se escribió: "12500", "12,500.50", "$ 12 500", "12500.5".
     * Regresa null si no es un monto válido. Las comas se toman como separador de miles.
     */
    fun parse(text: String): Long? {
        val clean = text.trim().replace("$", "").replace(",", "").replace(" ", "").replace("MXN", "", ignoreCase = true)
        if (clean.isEmpty()) return null
        if (!Regex("""\d+(\.\d{0,2})?|\.\d{1,2}""").matches(clean)) return null
        val parts = clean.split(".")
        val pesos = parts[0].ifEmpty { "0" }
        if (pesos.length > 13) return null
        val cts = if (parts.size > 1) parts[1].padEnd(2, '0') else "00"
        return pesos.toLong() * 100 + cts.toLong()
    }
}
