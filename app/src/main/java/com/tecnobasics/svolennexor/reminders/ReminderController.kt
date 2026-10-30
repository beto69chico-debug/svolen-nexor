package com.tecnobasics.svolennexor.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tecnobasics.svolennexor.core.Agenda
import com.tecnobasics.svolennexor.core.Reminders
import com.tecnobasics.svolennexor.data.Repo
import java.time.LocalDate
import java.time.ZoneId

/**
 * Una sola alarma programada a la vez: la del siguiente aviso.
 * Cuando suena, se mandan los avisos que tocan y se programa el siguiente.
 */
object ReminderController {
    /** Margen para que una alarma que llega unos segundos antes no se pierda. */
    private const val GRACE_MS = 30_000L
    private val lock = Any()

    fun canExact(ctx: Context): Boolean =
        Build.VERSION.SDK_INT < 31 || ctx.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun check(ctx: Context) = synchronized(lock) {
        val d = Repo.get(ctx)
        val zone = ZoneId.systemDefault()
        val upTo = System.currentTimeMillis() + GRACE_MS
        val today = LocalDate.now(zone).toEpochDay()
        val events = Agenda.events(d)
        val hour = d.settings.notifyHour
        Reminders.due(events, hour, zone, d.lastCheckMs, upTo).forEach { hit ->
            Notifs.reminder(ctx, hit, d.client(hit.event.clientId), today)
        }
        Repo.setLastCheck(ctx, upTo)
        schedule(ctx, Reminders.next(events, hour, zone, upTo))
    }

    private fun pending(ctx: Context): PendingIntent = PendingIntent.getBroadcast(
        ctx,
        1,
        Intent(ctx, ReminderReceiver::class.java).setAction("com.tecnobasics.svolennexor.CHECK"),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun schedule(ctx: Context, atMs: Long?) {
        val am = ctx.getSystemService(AlarmManager::class.java)
        val op = pending(ctx)
        am.cancel(op)
        if (atMs == null) return
        try {
            if (canExact(ctx)) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, op)
            else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, op)
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, atMs, op)
        }
    }
}
