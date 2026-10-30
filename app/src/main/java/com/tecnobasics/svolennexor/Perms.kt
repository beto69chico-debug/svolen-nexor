package com.tecnobasics.svolennexor

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import com.tecnobasics.svolennexor.reminders.Notifs
import com.tecnobasics.svolennexor.reminders.ReminderController

enum class Perm(val title: String, val desc: String, val essential: Boolean) {
    NOTIFICATIONS("Notificaciones", "Sin esto no te llegan los avisos de entregas y cobros.", true),
    EXACT_ALARMS("Avisos a tiempo", "Para que el aviso llegue a la hora exacta.", true),
    BATTERY(
        "Sin restricción de batería",
        "Evita que el teléfono cierre la app. En Xiaomi, Huawei u Oppo activa también \"Inicio automático\".",
        false,
    ),
}

object Perms {

    fun isGranted(ctx: Context, p: Perm): Boolean = when (p) {
        Perm.NOTIFICATIONS -> Notifs.canPost(ctx)
        Perm.EXACT_ALARMS -> ReminderController.canExact(ctx)
        Perm.BATTERY -> ctx.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(ctx.packageName)
    }

    fun missingEssential(ctx: Context): List<Perm> = Perm.entries.filter { it.essential && !isGranted(ctx, it) }

    @SuppressLint("BatteryLife", "InlinedApi")
    private fun settingsIntent(ctx: Context, p: Perm): Intent {
        val pkg = Uri.parse("package:${ctx.packageName}")
        return when (p) {
            Perm.NOTIFICATIONS -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName)
            Perm.EXACT_ALARMS -> Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg)
            Perm.BATTERY -> Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, pkg)
        }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    fun open(ctx: Context, p: Perm) {
        try {
            ctx.startActivity(settingsIntent(ctx, p))
        } catch (_: Exception) {
            // Algunas marcas no tienen esa pantalla: se abre la ficha de la app
            ctx.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${ctx.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}
