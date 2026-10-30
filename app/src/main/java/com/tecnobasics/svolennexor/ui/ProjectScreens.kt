package com.tecnobasics.svolennexor.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tecnobasics.svolennexor.core.Agenda
import com.tecnobasics.svolennexor.core.AppData
import com.tecnobasics.svolennexor.core.Billing
import com.tecnobasics.svolennexor.core.Fmt
import com.tecnobasics.svolennexor.core.Installment
import com.tecnobasics.svolennexor.core.Milestone
import com.tecnobasics.svolennexor.core.Money
import com.tecnobasics.svolennexor.core.Payment
import com.tecnobasics.svolennexor.core.Project
import com.tecnobasics.svolennexor.core.ProjectStatus
import com.tecnobasics.svolennexor.core.Reminders
import com.tecnobasics.svolennexor.data.Repo

fun statusColor(s: ProjectStatus): Color = when (s) {
    ProjectStatus.PENDING -> Muted
    ProjectStatus.ACTIVE -> BlueU
    ProjectStatus.REVIEW -> AmberU
    ProjectStatus.DELIVERED -> GreenU
    ProjectStatus.CANCELLED -> RedU
}

/** Renglón de un proyecto en la ficha del cliente. */
@Composable
fun ProjectRow(data: AppData, p: Project, today: Long, onClick: () -> Unit) {
    val m = Billing.money(p, data.payments, today)
    val next = p.milestones.filter { !it.done }.minByOrNull { it.dueDay }
    val client = data.client(p.clientId)
    ColorRow(client?.let { clientColor(it.color) } ?: Muted, onClick) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(p.title, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (next != null && p.status.open) {
                Text(
                    "${next.title} · ${Fmt.date(next.dueDay, today)}",
                    color = urgencyColor(Agenda.urgency(next.dueDay, today)),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (p.total > 0) {
                LinearProgressIndicator(
                    progress = { m.progress },
                    color = GreenU,
                    trackColor = Line,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                )
                Text(
                    "${Money.format(m.paid)} de ${Money.format(m.total)}",
                    color = Muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Pill(p.status.label, statusColor(p.status))
            if (m.overdue > 0) Pill("Vencido", RedU)
        }
    }
}

@Composable
fun ProjectDetailScreen(data: AppData, id: String, today: Long, nav: Nav) {
    val ctx = LocalContext.current
    val p = data.project(id)
    if (p == null) {
        LaunchedEffect(Unit) { nav.pop() }
        return
    }
    val client = data.client(p.clientId)
    val money = Billing.money(p, data.payments, today)
    val payments = Billing.projectPayments(p.id, data.payments).reversed()
    val charges = Billing.pendingCharges(data).filter { it.projectId == p.id }
    var paying by remember { mutableStateOf(false) }
    var deletingPayment by remember { mutableStateOf<Payment?>(null) }

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopBar(p.title, onBack = { nav.pop() }) {
                IconButton(onClick = { nav.push(Screen.EditProject(p.id, null)) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar")
                }
            }
        },
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (client != null) {
                item(key = "client") {
                    Row(
                        Modifier.clickable { nav.push(Screen.ClientDetail(client.id)) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Dot(clientColor(client.color), 12)
                        Spacer(Modifier.width(8.dp))
                        Text(client.display, color = Ink, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            item(key = "status") {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProjectStatus.entries.forEach { s ->
                        FilterChip(
                            selected = p.status == s,
                            onClick = { if (p.status != s) Repo.setProjectStatus(ctx, p.id, s) },
                            label = { Text(s.label) },
                            colors = chipColors(),
                        )
                    }
                }
            }

            item(key = "money") {
                Block {
                    SectionTitle("Cobranza")
                    MoneyLine("Total (sin IVA)", money.total, Ink, bold = true)
                    MoneyLine("Cobrado", money.paid, GreenU)
                    MoneyLine("Pendiente", money.pending, AmberU)
                    if (money.total > 0) {
                        LinearProgressIndicator(
                            progress = { money.progress },
                            color = GreenU,
                            trackColor = Line,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (money.overdue > 0) {
                        Text("Vencido: ${Money.format(money.overdue)}", color = RedU, fontWeight = FontWeight.Bold)
                    }
                    if (p.installments.isNotEmpty() && money.unplanned != 0L) {
                        Text(
                            if (money.unplanned > 0) "Faltan ${Money.format(money.unplanned)} por meter al plan de pagos."
                            else "El plan de pagos suma ${Money.format(-money.unplanned)} más que el total.",
                            color = AmberU,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { paying = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Orange),
                            modifier = Modifier.weight(1f),
                        ) { Text("Registrar pago", maxLines = 1) }
                        if (charges.isNotEmpty()) {
                            OutlinedButton(
                                onClick = { ChargeOps.whatsApp(ctx, data, charges.filter { it.dueDay <= today + 30 }.ifEmpty { charges.take(1) }, today) },
                                modifier = Modifier.weight(1f),
                            ) { Text("Cobrar por WhatsApp", color = GreenU, maxLines = 1) }
                        }
                    }
                }
            }

            item(key = "ms-title") { SectionTitle("Hitos de entrega", Modifier.padding(top = 6.dp)) }
            if (p.milestones.isEmpty()) {
                item(key = "ms-empty") { Text("Sin hitos. Agrégalos con el lápiz de arriba.", color = Muted) }
            }
            items(p.milestones.sortedBy { it.dueDay }, key = { "m-" + it.id }) { m ->
                MilestoneRow(m, today, p.status.open) { done -> Repo.setMilestoneDone(ctx, p.id, m.id, done) }
            }

            item(key = "plan-title") { SectionTitle("Plan de pagos", Modifier.padding(top = 6.dp)) }
            if (money.states.isEmpty()) {
                item(key = "plan-empty") { Text("Sin parcialidades. Agrégalas con el lápiz de arriba.", color = Muted) }
            }
            items(money.states, key = { "i-" + it.installment.id }) { st ->
                Surface(color = CardBg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(st.installment.label.ifBlank { "Parcialidad" }, color = Ink, fontWeight = FontWeight.SemiBold)
                            Text(Fmt.date(st.installment.dueDay, today), color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(Money.format(st.installment.amount), color = Ink, fontWeight = FontWeight.Bold)
                            when {
                                st.settled -> Pill("Pagada", GreenU)
                                st.partial -> Pill("Faltan ${Money.format(st.remaining)}", AmberU)
                                p.status == ProjectStatus.CANCELLED -> Pill("Cancelada", Muted)
                                else -> Pill(
                                    Agenda.countdown(st.installment.dueDay, today),
                                    urgencyColor(Agenda.urgency(st.installment.dueDay, today)),
                                )
                            }
                        }
                    }
                }
            }

            item(key = "pays-title") { SectionTitle("Pagos recibidos (${payments.size})", Modifier.padding(top = 6.dp)) }
            if (payments.isEmpty()) item(key = "pays-empty") { Text("Todavía no hay pagos.", color = Muted) }
            items(payments, key = { "pay-" + it.id }) { pay ->
                Surface(color = CardBg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(Money.format(pay.amount), color = GreenU, fontWeight = FontWeight.Bold)
                            Text(
                                listOfNotNull(Fmt.date(pay.day, today), pay.note.ifBlank { null }).joinToString(" · "),
                                color = Muted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        IconButton(onClick = { deletingPayment = pay }) {
                            Icon(Icons.Default.Delete, contentDescription = "Borrar pago", tint = Muted)
                        }
                    }
                }
            }

            item(key = "reminders") {
                Block {
                    SectionTitle("Avisos")
                    Text(
                        "Entregas: " + Reminders.summary(p.deliveryReminders ?: data.settings.deliveryReminders).lowercase(),
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        "Cobros: " + Reminders.summary(p.chargeReminders ?: data.settings.chargeReminders).lowercase(),
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            if (p.notes.isNotBlank()) {
                item(key = "notes") {
                    Block {
                        SectionTitle("Notas")
                        Text(p.notes, color = Ink)
                    }
                }
            }
        }
    }

    if (paying) {
        PaymentDialog(
            title = "Registrar pago",
            subtitle = p.title,
            suggested = charges.firstOrNull()?.amount ?: money.pending,
            onSave = { amount, day, note ->
                Repo.addPayment(ctx, Payment(Repo.newId(), p.id, null, null, amount, day, note, System.currentTimeMillis()))
                paying = false
            },
            onDismiss = { paying = false },
        )
    }
    deletingPayment?.let { pay ->
        ConfirmDialog(
            title = "¿Borrar este pago?",
            text = "${Money.format(pay.amount)} del ${Fmt.long(pay.day)}. Volverá a contar como pendiente.",
            confirm = "Borrar",
            onConfirm = {
                Repo.deletePayment(ctx, pay.id)
                deletingPayment = null
            },
            onDismiss = { deletingPayment = null },
        )
    }
}

@Composable
private fun MilestoneRow(m: Milestone, today: Long, open: Boolean, onToggle: (Boolean) -> Unit) {
    Surface(color = CardBg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(end = 12.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = m.done,
                onCheckedChange = onToggle,
                colors = CheckboxDefaults.colors(checkedColor = Orange, uncheckedColor = Muted),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    m.title,
                    color = if (m.done) Muted else Ink,
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = if (m.done) TextDecoration.LineThrough else null,
                )
                Text(Fmt.date(m.dueDay, today), color = Muted, style = MaterialTheme.typography.bodySmall)
            }
            when {
                m.done -> Pill("Entregado", GreenU)
                open -> Pill(Agenda.countdown(m.dueDay, today), urgencyColor(Agenda.urgency(m.dueDay, today)))
            }
        }
    }
}

private data class MDraft(val id: String, val title: String, val dueDay: Long, val doneMs: Long?)
private data class IDraft(val id: String, val label: String, val amount: String, val dueDay: Long)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditProjectScreen(data: AppData, id: String?, clientId: String?, today: Long, nav: Nav) {
    val ctx = LocalContext.current
    val original = remember(id) { id?.let { data.project(it) } }
    var clientSel by remember { mutableStateOf(original?.clientId ?: clientId ?: data.clients.singleOrNull()?.id) }
    var title by remember { mutableStateOf(original?.title ?: "") }
    var total by remember { mutableStateOf(original?.total?.let { Money.plain(it) } ?: "") }
    var status by remember { mutableStateOf(original?.status ?: ProjectStatus.ACTIVE) }
    var notes by remember { mutableStateOf(original?.notes ?: "") }
    var deliveryRem by remember { mutableStateOf(original?.deliveryReminders) }
    var chargeRem by remember { mutableStateOf(original?.chargeReminders) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val milestones = remember {
        mutableStateListOf<MDraft>().apply {
            if (original != null) {
                original.milestones.sortedBy { it.dueDay }.forEach { add(MDraft(it.id, it.title, it.dueDay, it.doneMs)) }
            } else {
                add(MDraft(Repo.newId(), "Entrega final", today + 30, null))
            }
        }
    }
    val installments = remember {
        mutableStateListOf<IDraft>().apply {
            original?.installments?.sortedBy { it.dueDay }?.forEach { add(IDraft(it.id, it.label, Money.plain(it.amount), it.dueDay)) }
        }
    }

    val totalCents = Money.parse(total)
    val plannedCents = installments.sumOf { Money.parse(it.amount) ?: 0L }

    fun lastDelivery(): Long = milestones.maxOfOrNull { it.dueDay } ?: (today + 30)

    fun splitPlan(parts: List<Pair<String, Long>>) {
        val t = totalCents
        if (t == null) {
            error = "Primero escribe el monto total."
            return
        }
        installments.clear()
        var assigned = 0L
        parts.forEachIndexed { i, part ->
            val amount = if (i == parts.lastIndex) t - assigned else t / parts.size
            assigned += amount
            installments.add(IDraft(Repo.newId(), part.first, Money.plain(amount), part.second))
        }
        error = null
    }

    fun save() {
        val cid = clientSel?.takeIf { c -> data.clients.any { it.id == c } }
        val t = totalCents
        val badInstallment = installments.any { (Money.parse(it.amount) ?: 0L) <= 0L }
        if (cid == null) {
            error = "Escoge el cliente."
            return
        }
        if (title.isBlank()) {
            error = "Ponle nombre al proyecto."
            return
        }
        if (t == null) {
            error = "Escribe el monto total (sin IVA)."
            return
        }
        if (milestones.any { it.title.isBlank() }) {
            error = "Ponle nombre a cada hito."
            return
        }
        if (badInstallment) {
            error = "Cada parcialidad necesita un monto."
            return
        }
        val newId = original?.id ?: Repo.newId()
        Repo.saveProject(
            ctx,
            Project(
                id = newId,
                clientId = cid,
                title = title.trim(),
                notes = notes.trim(),
                total = t,
                status = status,
                milestones = milestones.map { m ->
                    Milestone(m.id, m.title.trim(), m.dueDay, m.doneMs, original?.milestones?.firstOrNull { it.id == m.id }?.armedMs ?: 0L)
                },
                installments = installments.mapIndexed { i, d ->
                    Installment(
                        d.id,
                        d.label.trim().ifBlank { "Pago ${i + 1}" },
                        Money.parse(d.amount) ?: 0L,
                        d.dueDay,
                        original?.installments?.firstOrNull { it.id == d.id }?.armedMs ?: 0L,
                    )
                },
                deliveryReminders = deliveryRem,
                chargeReminders = chargeRem,
                createdMs = original?.createdMs ?: System.currentTimeMillis(),
            ),
        )
        if (original == null) nav.replace(Screen.ProjectDetail(newId)) else nav.pop()
    }

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopBar(if (original == null) "Nuevo proyecto" else "Editar proyecto", onBack = { nav.pop() }) {
                if (original != null) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Borrar proyecto", tint = RedU)
                    }
                }
            }
        },
        bottomBar = {
            Surface(color = Bg) {
                Button(
                    onClick = { save() },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Orange),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(16.dp)
                        .height(52.dp),
                ) { Text("Guardar proyecto", fontWeight = FontWeight.SemiBold) }
            }
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            if (original == null && clientId == null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Cliente")
                    if (data.clients.isEmpty()) Text("Primero agrega un cliente en la pestaña Clientes.", color = Muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        data.clients.sortedBy { it.display.lowercase() }.forEach { c ->
                            FilterChip(
                                selected = clientSel == c.id,
                                onClick = { clientSel = c.id },
                                label = { Text(c.display) },
                                leadingIcon = { Dot(clientColor(c.color)) },
                                colors = chipColors(),
                            )
                        }
                    }
                }
            } else {
                val c = data.client(clientSel)
                if (c != null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(clientColor(c.color), 12)
                        Spacer(Modifier.width(8.dp))
                        Text(c.display, color = Ink, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    error = null
                },
                label = { Text("Proyecto") },
                placeholder = { Text("Ej. Rediseño del sitio web") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            AmountField(
                value = total,
                onValueChange = {
                    total = it
                    error = null
                },
                label = "Monto total (sin IVA)",
                isError = total.isNotBlank() && totalCents == null,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Estado")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ProjectStatus.entries.forEach { s ->
                        FilterChip(selected = status == s, onClick = { status = s }, label = { Text(s.label) }, colors = chipColors())
                    }
                }
            }

            // ---- Hitos ----
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Hitos de entrega")
                milestones.forEachIndexed { i, m ->
                    Block {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = m.title,
                                onValueChange = { milestones[i] = m.copy(title = it) },
                                label = { Text("Hito ${i + 1}") },
                                placeholder = { Text("Diseño, desarrollo, lanzamiento…") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { milestones.removeAt(i) }) {
                                Icon(Icons.Default.Close, contentDescription = "Quitar hito", tint = Muted)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DateButton(m.dueDay, { milestones[i] = m.copy(dueDay = it) }, Modifier.weight(1f))
                            if (m.doneMs != null) {
                                Spacer(Modifier.width(8.dp))
                                Pill("Entregado", GreenU)
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        val base = milestones.maxOfOrNull { it.dueDay } ?: today
                        milestones.add(MDraft(Repo.newId(), "", maxOf(base, today) + 7, null))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("+ Agregar hito") }
            }

            // ---- Plan de pagos ----
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Plan de pagos")
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        splitPlan(listOf("Anticipo 50%" to today, "Liquidación 50%" to lastDelivery()))
                    }) { Text("50% anticipo + 50% entrega") }
                    OutlinedButton(onClick = {
                        splitPlan(listOf("Pago 1" to today, "Pago 2" to today + 30, "Pago 3" to today + 60))
                    }) { Text("3 pagos mensuales") }
                    OutlinedButton(onClick = { splitPlan(listOf("Pago único" to lastDelivery())) }) { Text("Un solo pago") }
                }
                installments.forEachIndexed { i, d ->
                    Block {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = d.label,
                                onValueChange = { installments[i] = d.copy(label = it) },
                                label = { Text("Concepto") },
                                placeholder = { Text("Pago ${i + 1}") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { installments.removeAt(i) }) {
                                Icon(Icons.Default.Close, contentDescription = "Quitar parcialidad", tint = Muted)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            AmountField(
                                value = d.amount,
                                onValueChange = { installments[i] = d.copy(amount = it) },
                                label = "Monto",
                                isError = d.amount.isNotBlank() && Money.parse(d.amount) == null,
                                modifier = Modifier.weight(1f),
                            )
                            DateButton(d.dueDay, { installments[i] = d.copy(dueDay = it) }, Modifier.weight(1f))
                        }
                    }
                }
                OutlinedButton(
                    onClick = {
                        val left = (totalCents ?: 0L) - plannedCents
                        val base = installments.maxOfOrNull { it.dueDay } ?: (today - 30)
                        installments.add(
                            IDraft(Repo.newId(), "Pago ${installments.size + 1}", if (left > 0) Money.plain(left) else "", maxOf(base + 30, today)),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("+ Agregar parcialidad") }
                if (installments.isNotEmpty() && totalCents != null) {
                    val diff = totalCents - plannedCents
                    Text(
                        when {
                            diff == 0L -> "El plan suma ${Money.format(plannedCents)}: cuadra con el total."
                            diff > 0 -> "El plan suma ${Money.format(plannedCents)}; faltan ${Money.format(diff)}."
                            else -> "El plan suma ${Money.format(plannedCents)}; se pasa por ${Money.format(-diff)}."
                        },
                        color = if (diff == 0L) GreenU else AmberU,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            ReminderEditor("Avisos de entregas", deliveryRem, data.settings.deliveryReminders) { deliveryRem = it }
            ReminderEditor("Avisos de cobro", chargeRem, data.settings.chargeReminders) { chargeRem = it }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas (opcional)") },
                minLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )

            error?.let { Text(it, color = RedU, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (confirmDelete && original != null) {
        ConfirmDialog(
            title = "¿Borrar este proyecto?",
            text = "\"${original.title}\" se borra con sus hitos, su plan y sus pagos registrados. No se puede deshacer.",
            confirm = "Borrar",
            onConfirm = {
                confirmDelete = false
                Repo.deleteProject(ctx, original.id)
                nav.pop()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
