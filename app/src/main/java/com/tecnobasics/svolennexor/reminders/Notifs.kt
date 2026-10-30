package com.tecnobasics.svolennexor.reminders

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tecnobasics.svolennexor.MainActivity
import com.tecnobasics.svolennexor.R
import com.tecnobasics.svolennexor.core.Agenda
import com.tecnobasics.svolennexor.core.Client
import com.tecnobasics.svolennexor.core.EventKind
import com.tecnobasics.svolennexor.core.Fmt
import com.tecnobasics.svolennexor.core.Money
import com.tecnobasics.svolennexor.core.Palette
import com.tecnobasics.svolennexor.core.Reminders

object Notifs {
    const val CHANNEL = "sn_avisos_v1"
    private const val ID_TEST = 9001

    fun ensureChannel(ctx: Context) {
        val ch = NotificationChannel(CHANNEL, "Entregas y cobros", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Avisos antes de entregas, cobros y vencimientos de servicios"
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
    }

    fun canPost(ctx: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    private fun openApp(ctx: Context): PendingIntent = PendingIntent.getActivity(
        ctx, 0,
        Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    @SuppressLint("MissingPermission")
    fun reminder(ctx: Context, hit: Reminders.Hit, client: Client?, today: Long) {
        if (!canPost(ctx)) return
        ensureChannel(ctx)
        val e = hit.event
        val whenText = Agenda.countdown(e.dueDay, today).replaceFirstChar { it.lowercase() }
        val who = client?.display ?: "Cliente"
        val (title, body) = when (e.kind) {
            EventKind.DELIVERY ->
                "Entrega $whenText: ${e.title}" to "${e.detail} · $who\nSe entrega ${Fmt.dayLabel(e.dueDay, today)}."
            EventKind.CHARGE ->
                "Cobro $whenText: ${Money.format(e.amount ?: 0)}" to "${e.title} de ${e.detail} · $who\nVence ${Fmt.dayLabel(e.dueDay, today)}."
            EventKind.SERVICE ->
                "Vence $whenText: ${e.detail}" to "${e.title.replaceFirstChar { it.uppercase() }} a $who: ${Money.format(e.amount ?: 0)}\nVence ${Fmt.dayLabel(e.dueDay, today)}."
            EventKind.RENEW ->
                "Renueva ${e.detail}" to "El dominio de $who vence ${Fmt.dayLabel(e.dueDay, today)}. Renuévalo con tu proveedor."
        }
        val color = (client?.let { Palette.argb(it.color) } ?: Palette.argb(0)).toInt()
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_sn)
            .setColor(color)
            .setContentTitle(title)
            .setContentText(body.substringBefore('\n'))
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx))
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(e.key.hashCode(), n)
        } catch (_: SecurityException) {
        }
    }

    @SuppressLint("MissingPermission")
    fun test(ctx: Context): Boolean {
        if (!canPost(ctx)) return false
        ensureChannel(ctx)
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_sn)
            .setColor(Palette.argb(0).toInt())
            .setContentTitle("Así te van a llegar los avisos")
            .setContentText("Svölen-Nexor te avisa antes de cada entrega y cada cobro.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openApp(ctx))
            .build()
        return try {
            NotificationManagerCompat.from(ctx).notify(ID_TEST, n)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
