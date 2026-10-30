package com.tecnobasics.svolennexor.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tecnobasics.svolennexor.core.Agenda
import com.tecnobasics.svolennexor.core.AppData
import com.tecnobasics.svolennexor.core.Bucket
import com.tecnobasics.svolennexor.core.EventKind

private enum class TFilter(val label: String) {
    ALL("Todo"),
    DELIVERY("Entregas"),
    CHARGE("Cobros"),
    SERVICE("Servicios y dominios"),
}

@Composable
fun TimelineScreen(data: AppData, today: Long, nav: Nav) {
    var filter by rememberSaveable { mutableStateOf(TFilter.ALL) }
    val all = remember(data) { Agenda.events(data) }
    val visible = all.filter {
        when (filter) {
            TFilter.ALL -> true
            TFilter.DELIVERY -> it.kind == EventKind.DELIVERY
            TFilter.CHARGE -> it.kind == EventKind.CHARGE
            TFilter.SERVICE -> it.kind == EventKind.SERVICE || it.kind == EventKind.RENEW
        }
    }
    val groups = Agenda.group(visible, today)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "title") {
            Text("Timeline", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
        }
        item(key = "filters") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TFilter.entries.forEach { f ->
                    FilterChip(
                        selected = filter == f,
                        onClick = { filter = f },
                        label = { Text(f.label) },
                        colors = chipColors(),
                    )
                }
            }
        }
        if (groups.isEmpty()) {
            item(key = "empty") {
                EmptyState(
                    "Todo al día",
                    if (data.clients.isEmpty()) "Agrega clientes, proyectos y servicios para verlos aquí por fecha."
                    else "No hay pendientes con fecha en esta vista.",
                )
            }
        }
        groups.forEach { (bucket, events) ->
            item(key = "b-${bucket.name}") {
                Text(
                    "${bucket.title} (${events.size})",
                    color = if (bucket == Bucket.OVERDUE) RedU else Orange,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(events, key = { it.key }) { e ->
                EventRow(e, data.client(e.clientId), today) { nav.openEvent(e) }
            }
        }
    }
}
