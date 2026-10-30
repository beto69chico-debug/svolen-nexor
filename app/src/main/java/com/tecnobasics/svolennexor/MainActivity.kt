package com.tecnobasics.svolennexor

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableIntStateOf
import com.tecnobasics.svolennexor.data.Repo
import com.tecnobasics.svolennexor.reminders.Notifs
import com.tecnobasics.svolennexor.reminders.ReminderController
import com.tecnobasics.svolennexor.ui.SvolenApp
import com.tecnobasics.svolennexor.ui.SvolenTheme

class MainActivity : ComponentActivity() {

    private val resumeTick = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // La app siempre es oscura: íconos claros en las barras aunque el teléfono esté en modo claro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        Notifs.ensureChannel(this)
        Repo.get(this)
        ReminderController.check(this) // re-sincroniza la alarma por si el sistema la borró
        setContent {
            SvolenTheme {
                SvolenApp(resumeTick = resumeTick.intValue)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        resumeTick.intValue++
    }
}
