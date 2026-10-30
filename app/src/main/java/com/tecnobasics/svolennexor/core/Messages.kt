package com.tecnobasics.svolennexor.core

/** Textos que se mandan al cliente por WhatsApp. Siempre los revisa quien los envía. */
object Messages {

    /** Número para wa.me: solo dígitos y con lada de país (52 si son 10 dígitos de México). */
    fun waPhone(phone: String): String? {
        var digits = phone.filter { it.isDigit() }
        if (digits.startsWith("00")) digits = digits.drop(2)
        return when {
            digits.length == 10 -> "52$digits"
            digits.length == 13 && digits.startsWith("521") -> "52" + digits.drop(3) // formato viejo con 1
            digits.length in 11..15 -> digits
            else -> null
        }
    }

    /** Número para marcar: tal como lo escribiste, sin espacios raros. */
    fun dialPhone(phone: String): String = phone.filter { it.isDigit() || it == '+' }

    /** Mensaje para recordar uno o varios cobros pendientes del mismo cliente. */
    fun chargeText(client: Client?, charges: List<Billing.Charge>, today: Long, signature: String): String {
        val who = client?.contact?.trim()?.ifBlank { null } ?: client?.name?.trim()?.ifBlank { null }
        val greeting = if (who != null) "Hola $who, ¿cómo estás?" else "Hola, ¿cómo estás?"
        val sb = StringBuilder(greeting).append("\n\n")
        if (charges.size == 1) {
            val c = charges[0]
            sb.append("Te escribo para recordarte el pago de ").append(Money.format(c.amount))
                .append(" por ").append(describe(c)).append(", ").append(whenText(c.dueDay, today)).append('.')
        } else {
            sb.append("Te escribo para recordarte estos pagos pendientes:\n")
            charges.forEach { c ->
                sb.append("• ").append(describe(c)).append(": ").append(Money.format(c.amount))
                    .append(" (").append(whenText(c.dueDay, today)).append(")\n")
            }
            sb.append("\nTotal: ").append(Money.format(charges.sumOf { it.amount }))
        }
        sb.append("\n\nQuedo atento a tu confirmación. ¡Gracias!")
        if (signature.isNotBlank()) sb.append("\n").append(signature.trim())
        return sb.toString()
    }

    private fun describe(c: Billing.Charge): String =
        if (c.serviceId != null) "${c.concept.lowercase()} ${c.source}".trim() else "${c.concept} de ${c.source}"

    private fun whenText(dueDay: Long, today: Long): String = when {
        dueDay < today -> "con vencimiento el ${Fmt.long(dueDay)}"
        dueDay == today -> "que vence hoy"
        else -> "que vence el ${Fmt.long(dueDay)}"
    }
}
