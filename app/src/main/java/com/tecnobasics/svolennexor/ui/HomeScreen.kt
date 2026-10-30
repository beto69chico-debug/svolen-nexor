package com.tecnobasics.svolennexor.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecnobasics.svolennexor.Perm
import com.tecnobasics.svolennexor.Perms
import com.tecnobasics.svolennexor.R
import com.tecnobasics.svolennexor.core.Agenda
import com.tecnobasics.svolennexor.core.AppData
import com.tecnobasics.svolennexor.core.Billing
import com.tecnobasics.svolennexor.core.Fmt

@Composable
fun HomeScreen(data: AppData, today: Long, resumeTick: Int, nav: Nav) {
    val ctx = LocalContext.current
    var permTick by remember { mutableIntStateOf(0) }
    val missing = remember(resumeTick, permTick) { Perms.missingEssential(ctx) }
    val askNotifs = rememberAskNotifications(settingsIfDenied = true) { permTick++ }
    val askOnce = rememberAskNotifications(settingsIfDenied = false) { permTick++ }
    // La primera vez que se abre la app se piden las notificaciones directo.
    LaunchedEffect(Unit) { if (!Perms.isGranted(ctx, Perm.NOTIFICATIONS)) askOnce() }

    val summary = remember(data, today) { Billing.summary(data, today) }
    val events = remember(data) { Agenda.events(data) }
    val overdue = events.filter { it.dueDay < today }
    val upcoming = events.filter { it.dueDay in today..(today + 14) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "header") {
            Column(Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Image(
                    painter = painterResource(R.drawable.logo_sn),
                    contentDescription = "Svölen-Nexor",
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.height(40.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    Fmt.long(today).replaceFirstChar { it.uppercase() },
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (missing.isNotEmpty()) {
            item(key = "perm") {
                Block {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = Orange)
                        Spacer(Modifier.width(8.dp))
                        Text("Activa los avisos", fontWeight = FontWeight.Bold, color = Ink)
                    }
                    Text(
                        "Sin esto no te llegan los recordatorios de entregas, cobros y dominios.",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    missing.forEach { p ->
                        Button(
                            onClick = { if (p == Perm.NOTIFICATIONS) askNotifs() else Perms.open(ctx, p) },
                            colors = ButtonDefaults.buttonColors(containerColor = Orange),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Activar: ${p.title}") }
                    }
                }
            }
        }

        item(key = "stats") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Por cobrar este mes", summary.dueThisMonth, Ink, Modifier.weight(1f))
                    StatTile("Vencido", summary.overdue, if (summary.overdue > 0) RedU else Muted, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile("Cobrado este mes", summary.collectedThisMonth, GreenU, Modifier.weight(1f))
                    StatTile("Pendiente total", summary.pendingTotal, AmberU, Modifier.weight(1f))
                }
            }
        }

        if (data.clients.isEmpty()) {
            item(key = "welcome") {
                Block {
                    Text("Empieza por tus clientes", fontWeight = FontWeight.Bold, color = Ink)
                    Text(
                        "Agrega un cliente y luego sus proyectos (con hitos y plan de pagos) o sus servicios: dominio, hosting o póliza.",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        onClick = { nav.tab(Tab.CLIENTS) },
                        colors = ButtonDefaults.buttonColors(containerColor = Orange),
                    ) { Text("Ir a Clientes") }
                }
            }
        }

        if (overdue.isNotEmpty()) {
            item(key = "overdue-title") { SectionTitle("Vencido (${overdue.size})", Modifier.padding(top = 8.dp)) }
            items(overdue.take(8), key = { "o-" + it.key }) { e ->
                EventRow(e, data.client(e.clientId), today) { nav.openEvent(e) }
            }
            if (overdue.size > 8) {
                item(key = "overdue-more") {
                    TextButton(onClick = { nav.tab(Tab.TIMELINE) }) { Text("Ver los ${overdue.size} en el timeline") }
                }
            }
        }

        item(key = "upcoming-title") {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Próximos 14 días", Modifier.weight(1f))
                TextButton(onClick = { nav.tab(Tab.TIMELINE) }) { Text("Ver timeline", color = Orange) }
            }
        }
        if (upcoming.isEmpty()) {
            item(key = "upcoming-empty") {
                Text("Nada en los próximos 14 días.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
        } else {
            items(upcoming.take(12), key = { "u-" + it.key }) { e ->
                EventRow(e, data.client(e.clientId), today) { nav.openEvent(e) }
            }
        }
    }
}
