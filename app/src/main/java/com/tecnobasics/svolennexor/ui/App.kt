package com.tecnobasics.svolennexor.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.tecnobasics.svolennexor.core.AppData
import com.tecnobasics.svolennexor.core.Billing
import com.tecnobasics.svolennexor.core.Event
import com.tecnobasics.svolennexor.core.Messages
import com.tecnobasics.svolennexor.core.Payment
import com.tecnobasics.svolennexor.data.Repo
import kotlinx.coroutines.delay

enum class Tab(val label: String) {
    HOME("Inicio"),
    TIMELINE("Timeline"),
    CLIENTS("Clientes"),
    BILLING("Cobranza"),
    SETTINGS("Ajustes"),
}

sealed interface Screen {
    data class ClientDetail(val id: String) : Screen
    data class ProjectDetail(val id: String) : Screen
    data class EditProject(val id: String?, val clientId: String?) : Screen
    data class ServiceDetail(val id: String) : Screen
    data class EditService(val id: String?, val clientId: String?) : Screen
}

/** Navegación sencilla: pestañas abajo y una pila de pantallas encima. */
class Nav(private val stack: SnapshotStateList<Screen>, private val setTab: (Tab) -> Unit) {
    fun push(s: Screen) {
        stack.add(s)
    }

    fun pop() {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
    }

    /** Cambia la pantalla de arriba por otra (al crear algo, ir directo a su ficha). */
    fun replace(s: Screen) {
        pop()
        push(s)
    }

    fun tab(t: Tab) {
        stack.clear()
        setTab(t)
    }

    fun openEvent(e: Event) {
        val p = e.projectId
        val s = e.serviceId
        if (p != null) push(Screen.ProjectDetail(p)) else if (s != null) push(Screen.ServiceDetail(s))
    }

    fun openCharge(c: Billing.Charge) {
        val p = c.projectId
        val s = c.serviceId
        if (p != null) push(Screen.ProjectDetail(p)) else if (s != null) push(Screen.ServiceDetail(s))
    }
}

/** Lo que se hace con un cobro pendiente, desde cualquier pantalla. */
object ChargeOps {
    fun markPaid(ctx: Context, c: Billing.Charge, amount: Long, day: Long, note: String) {
        val s = c.serviceId
        if (s != null) {
            Repo.chargeService(ctx, s, amount, day, note)
        } else {
            Repo.addPayment(ctx, Payment(Repo.newId(), c.projectId, null, null, amount, day, note, System.currentTimeMillis()))
        }
    }

    /** Abre WhatsApp con el recordatorio de cobro (todos los cobros deben ser del mismo cliente). */
    fun whatsApp(ctx: Context, data: AppData, charges: List<Billing.Charge>, today: Long) {
        if (charges.isEmpty()) return
        val client = data.client(charges.first().clientId)
        val text = Messages.chargeText(client, charges, today, data.settings.signature)
        Actions.whatsApp(ctx, client?.phone ?: "", text)
    }
}

@Composable
fun SvolenApp(resumeTick: Int) {
    val data = Repo.data.collectAsState().value ?: return
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    val stack = remember { mutableStateListOf<Screen>() }
    val nav = remember { Nav(stack) { tab = it } }

    // "Hoy" se refresca solo, por si la app se queda abierta después de medianoche.
    var today by remember { mutableLongStateOf(Repo.today()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            today = Repo.today()
        }
    }
    LaunchedEffect(resumeTick) { today = Repo.today() }

    BackHandler(enabled = stack.isNotEmpty()) { nav.pop() }
    BackHandler(enabled = stack.isEmpty() && tab != Tab.HOME) { tab = Tab.HOME }

    val top = stack.lastOrNull()
    if (top != null) {
        when (top) {
            is Screen.ClientDetail -> ClientDetailScreen(data, top.id, today, nav)
            is Screen.ProjectDetail -> ProjectDetailScreen(data, top.id, today, nav)
            is Screen.EditProject -> EditProjectScreen(data, top.id, top.clientId, today, nav)
            is Screen.ServiceDetail -> ServiceDetailScreen(data, top.id, today, nav)
            is Screen.EditService -> EditServiceScreen(data, top.id, top.clientId, today, nav)
        }
        return
    }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(containerColor = CardBg) {
                Tab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { TabIcon(t) },
                        label = { Text(t.label, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Orange,
                            selectedTextColor = Orange,
                            indicatorColor = OrangeSoft,
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted,
                        ),
                    )
                }
            }
        },
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (tab) {
                Tab.HOME -> HomeScreen(data, today, resumeTick, nav)
                Tab.TIMELINE -> TimelineScreen(data, today, nav)
                Tab.CLIENTS -> ClientsScreen(data, today, nav)
                Tab.BILLING -> BillingScreen(data, today, nav)
                Tab.SETTINGS -> SettingsScreen(data, resumeTick)
            }
        }
    }
}

@Composable
private fun TabIcon(t: Tab) {
    when (t) {
        Tab.HOME -> Icon(Icons.Default.Home, contentDescription = null)
        Tab.TIMELINE -> Icon(Icons.Default.DateRange, contentDescription = null)
        Tab.CLIENTS -> Icon(Icons.Default.Person, contentDescription = null)
        Tab.BILLING -> Text("$", fontWeight = FontWeight.Black)
        Tab.SETTINGS -> Icon(Icons.Default.Settings, contentDescription = null)
    }
}
