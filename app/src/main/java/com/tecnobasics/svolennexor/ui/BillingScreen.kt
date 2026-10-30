package com.tecnobasics.svolennexor.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tecnobasics.svolennexor.core.AppData
import com.tecnobasics.svolennexor.core.Billing
import com.tecnobasics.svolennexor.core.Fmt
import com.tecnobasics.svolennexor.core.Money
import com.tecnobasics.svolennexor.core.Payment
import java.time.LocalDate

private enum class BFilter(val label: String) {
    ALL("Todo"),
    OVERDUE("Vencido"),
    MONTH("Este mes"),
    LATER("Más adelante"),
}

@Composable
fun BillingScreen(data: AppData, today: Long, nav: Nav) {
    val ctx = LocalContext.current
    var filter by rememberSaveable { mutableStateOf(BFilter.ALL) }
    var paying by remember { mutableStateOf<Billing.Charge?>(null) }

    val summary = remember(data, today) { Billing.summary(data, today) }
    val charges = remember(data) { Billing.pendingCharges(data) }
    val monthEnd = LocalDate.ofEpochDay(today).let { it.withDayOfMonth(it.lengthOfMonth()).toEpochDay() }
    val monthStart = LocalDate.ofEpochDay(today).withDayOfMonth(1).toEpochDay()
    val visible = charges.filter {
        when (filter) {
            BFilter.ALL -> true
            BFilter.OVERDUE -> it.dueDay < today
            BFilter.MONTH -> it.dueDay in today..monthEnd
            BFilter.LATER -> it.dueDay > monthEnd
        }
    }
    val collected = data.payments.filter { it.day in monthStart..monthEnd }.sortedByDescending { it.day }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "title") {
            Text("Cobranza", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
        }
        item(key = "stats") {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Vencido", summary.overdue, if (summary.overdue > 0) RedU else Muted, Modifier.weight(1f))
                StatTile("Pendiente total", summary.pendingTotal, AmberU, Modifier.weight(1f))
            }
        }
        item(key = "filters") {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BFilter.entries.forEach { f ->
                    FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f.label) }, colors = chipColors())
                }
            }
        }
        if (visible.isEmpty()) {
            item(key = "empty") { EmptyState("Nada por cobrar aquí", "Cuando registres parcialidades o servicios, aparecerán en esta lista.") }
        } else {
            item(key = "sum") {
                Text(
                    "${visible.size} cobros · ${Money.format(visible.sumOf { it.amount })}",
                    color = Muted,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            items(visible, key = { it.key }) { c ->
                ChargeRow(
                    c = c,
                    client = data.client(c.clientId),
                    today = today,
                    onOpen = { nav.openCharge(c) },
                    onPaid = { paying = c },
                    onWhatsApp = { ChargeOps.whatsApp(ctx, data, listOf(c), today) },
                )
            }
        }

        item(key = "collected-title") {
            SectionTitle("Cobrado en ${Fmt.month(today)} · ${Money.format(collected.sumOf { it.amount })}", Modifier.padding(top = 12.dp))
        }
        if (collected.isEmpty()) {
            item(key = "collected-empty") { Text("Todavía no hay pagos este mes.", color = Muted) }
        } else {
            items(collected, key = { "p-" + it.id }) { p -> PaymentRow(data, p, today) { openPayment(nav, p) } }
        }
    }

    paying?.let { c ->
        PaymentDialog(
            title = "Registrar cobro",
            subtitle = "${c.concept} · ${c.source}",
            suggested = c.amount,
            onSave = { amount, day, note ->
                ChargeOps.markPaid(ctx, c, amount, day, note)
                paying = null
            },
            onDismiss = { paying = null },
        )
    }
}

private fun openPayment(nav: Nav, p: Payment) {
    val pid = p.projectId
    val sid = p.serviceId
    if (pid != null) nav.push(Screen.ProjectDetail(pid)) else if (sid != null) nav.push(Screen.ServiceDetail(sid))
}

/** Un pago recibido: de quién, de qué y cuánto. */
@Composable
fun PaymentRow(data: AppData, p: Payment, today: Long, onClick: (() -> Unit)?) {
    val project = data.project(p.projectId)
    val service = data.service(p.serviceId)
    val client = data.client(project?.clientId ?: service?.clientId)
    ColorRow(color = client?.let { clientColor(it.color) } ?: Muted, onClick = onClick) {
        Column(Modifier.weight(1f)) {
            Text(
                project?.title ?: service?.let { "${it.kind.label} · ${it.name}" } ?: "Pago",
                color = Ink,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                listOfNotNull(client?.display, Fmt.date(p.day, today), p.note.ifBlank { null }).joinToString(" · "),
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(Money.format(p.amount), color = GreenU, fontWeight = FontWeight.Bold)
    }
}
