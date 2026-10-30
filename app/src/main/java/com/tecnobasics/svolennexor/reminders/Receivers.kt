package com.tecnobasics.svolennexor.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Llega a la hora del siguiente aviso. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        ReminderController.check(ctx)
    }
}

/** Android borra las alarmas al apagar el teléfono o cambiar la hora: aquí se vuelven a poner. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        ReminderController.check(ctx)
    }
}
