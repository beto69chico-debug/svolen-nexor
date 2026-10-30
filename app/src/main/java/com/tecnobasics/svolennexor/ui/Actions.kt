package com.tecnobasics.svolennexor.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.tecnobasics.svolennexor.core.Messages

/** Abrir WhatsApp, el marcador o el correo. Si algo no está instalado, se avisa sin tronar. */
object Actions {

    /** Abre WhatsApp con el mensaje escrito; quien lo manda lo revisa antes de enviar. */
    fun whatsApp(ctx: Context, phone: String, text: String) {
        val num = Messages.waPhone(phone)
        val url = if (num != null) "https://wa.me/$num?text=${Uri.encode(text)}" else "https://wa.me/?text=${Uri.encode(text)}"
        start(ctx, Intent(Intent.ACTION_VIEW, Uri.parse(url)), "No encontré WhatsApp en este teléfono.")
    }

    fun call(ctx: Context, phone: String) {
        start(ctx, Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Messages.dialPhone(phone)}")), "No se pudo abrir el marcador.")
    }

    fun mail(ctx: Context, email: String, subject: String = "") {
        val uri = Uri.parse("mailto:${email.trim()}" + if (subject.isNotBlank()) "?subject=${Uri.encode(subject)}" else "")
        start(ctx, Intent(Intent.ACTION_SENDTO, uri), "No hay app de correo configurada.")
    }

    private fun start(ctx: Context, intent: Intent, error: String) {
        try {
            ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            Toast.makeText(ctx, error, Toast.LENGTH_SHORT).show()
        }
    }

    fun toast(ctx: Context, text: String) = Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show()
}
