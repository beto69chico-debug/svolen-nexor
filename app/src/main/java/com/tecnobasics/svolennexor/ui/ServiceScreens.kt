package com.tecnobasics.svolennexor.ui

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tecnobasics.svolennexor.core.Agenda
import com.tecnobasics.svolennexor.core.AppData
import com.tecnobasics.svolennexor.core.Billing
import com.tecnobasics.svolennexor.core.Fmt
import com.tecnobasics.svolennexor.core.Money
import com.tecnobasics.svolennexor.core.Payment
import com.tecnobasics.svolennexor.core.Reminders
import com.tecnobasics.svolennexor.core.Service
import com.tecnobasics.svolennexor.core.ServiceKind
import com.tecnobasics.svolennexor.data.Repo

private val PERIODS = listOf(1, 3, 6, 12, 24)

private fun placeholderFor(k: ServiceKind) = when (k) {
    ServiceKind.DOMAIN -> "midominio.com.mx"
    ServiceKind.HOSTING -> "Hosting del sitio"
    ServiceKind.POLICY -> "Póliza de mantenimiento web"
    ServiceKind.OTHER -> "Ej. Licencia, correo, SEO mensual"
}

/** Renglón de un servicio en la ficha del cliente. */
@Composable
fun ServiceRow(data: AppData, s: Service, today: Long, onClick: () -> Unit) {
    val client = data.client(s.clientId)
    val paid = Billing.servicePaid(s, data.payments)
    ColorRow(client?.let { clientColor(it.color) } ?: Muted, onClick) {
        Column(Modifier.weight(1f)) {
            Text(s.kind.label.uppercase(), color = AmberU, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(s.name, color = if (s.active) Ink else Muted, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${Fmt.period(s.periodMonths)} · ${Money.format(s.amount)} · vence ${Fmt.date(s.dueDay, today)}",
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        when {
            !s.active -> Pill("Inactivo", Muted)
            paid && s.needsRenewal && !s.renewed -> Pill("Falta renovar", Orange)
            else -> Pill(Agenda.countdown(s.dueDay, today), urgencyColor(Agenda.urgency(s.dueDay, today)))
        }
    }
}

@Composable
fun ServiceDetailScreen(data: AppData, id: String, today: Long, nav: Nav) {
    val ctx = LocalContext.current
    val s = data.service(id)
    if (s == null) {
        LaunchedEffect(Unit) { nav.pop() }
        return
    }
    val client = data.client(s.clientId)
    val paid = Billing.servicePaid(s, data.payments)
    val collectedNow = data.payments.filter { it.serviceId == s.id && it.periodDay == s.dueDay }.sumOf { it.amount }
    val history = data.payments.filter { it.serviceId == s.id }.sortedByDescending { it.day }
    val charge = Billing.pendingCharges(data).firstOrNull { it.serviceId == s.id }
    var paying by remember { mutableStateOf(false) }
    var confirmSkip by remember { mutableStateOf(false) }
    var deletingPayment by remember { mutableStateOf<Payment?>(null) }
    val nextDue = Billing.nextDue(s.dueDay, s.periodMonths)

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopBar(s.name.ifBlank { s.kind.label }, onBack = { nav.pop() }) {
                IconButton(onClick = { nav.push(Screen.EditService(s.id, null)) }) {
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
            item(key = "info") {
                Block {
                    if (client != null) {
                        Row(
                            Modifier.clickable { nav.push(Screen.ClientDetail(client.id)) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Dot(clientColor(client.color), 12)
                            Spacer(Modifier.width(8.dp))
                            Text(client.display, color = Ink, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    InfoLine("Tipo", s.kind.label)
                    InfoLine("Monto", "${Money.format(s.amount)} · ${Fmt.period(s.periodMonths).lowercase()}")
                    InfoLine("Vence", Fmt.long(s.dueDay))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (s.active) Pill(Agenda.countdown(s.dueDay, today), urgencyColor(Agenda.urgency(s.dueDay, today)))
                        else Pill("Inactivo: sin avisos", Muted)
                    }
                }
            }

            item(key = "period") {
                Block {
                    SectionTitle("Este periodo")
                    // Cobro al cliente
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Cobro al cliente", color = Ink, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (paid) "Cobrado: ${Money.format(collectedNow)}" else "Pendiente: ${Money.format(s.amount)}",
                                color = if (paid) GreenU else AmberU,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        if (paid) Icon(Icons.Default.CheckCircle, contentDescription = "Cobrado", tint = GreenU)
                    }
                    if (!paid) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { paying = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Orange),
                                modifier = Modifier.weight(1f),
                            ) { Text("Registrar cobro", maxLines = 1) }
                            if (charge != null) {
                                OutlinedButton(
                                    onClick = { ChargeOps.whatsApp(ctx, data, listOf(charge), today) },
                                    modifier = Modifier.weight(1f),
                                ) { Text("Cobrar por WhatsApp", color = GreenU, maxLines = 1) }
                            }
                        }
                    }
                    // Renovación (solo dominios)
                    if (s.needsRenewal) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Renovación con tu proveedor", color = Ink, fontWeight = FontWeight.SemiBold)
                                Text(
                                    if (s.renewed) "Ya lo renovaste" else "Pendiente de renovar antes del ${Fmt.date(s.dueDay, today)}",
                                    color = if (s.renewed) GreenU else Orange,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            if (s.renewed) {
                                TextButton(onClick = { Repo.setServiceRenewed(ctx, s.id, false) }) { Text("Deshacer", color = Muted) }
                            } else {
                                Button(
                                    onClick = { Repo.setServiceRenewed(ctx, s.id, true) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Orange),
                                ) { Text("Ya lo renové") }
                            }
                        }
                    }
                    Text(
                        if (s.needsRenewal) "Cuando esté cobrado y renovado, pasa solo al siguiente periodo (${Fmt.date(nextDue, today)})."
                        else "Al cobrarlo, pasa solo al siguiente periodo (${Fmt.date(nextDue, today)}).",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    TextButton(onClick = { confirmSkip = true }) { Text("Saltar este periodo sin cobrar", color = Muted) }
                }
            }

            item(key = "active") {
                Block {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Servicio activo", color = Ink, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (s.active) "Avisos: " + Reminders.summary(s.reminders ?: data.settings.serviceReminders).lowercase()
                                else "Apagado: no genera cobros ni avisos.",
                                color = Muted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Switch(
                            checked = s.active,
                            onCheckedChange = { Repo.saveService(ctx, s.copy(active = it)) },
                            colors = SwitchDefaults.colors(checkedTrackColor = Orange),
                        )
                    }
                }
            }

            item(key = "hist-title") { SectionTitle("Historial de cobros (${history.size})", Modifier.padding(top = 6.dp)) }
            if (history.isEmpty()) item(key = "hist-empty") { Text("Todavía no hay cobros registrados.", color = Muted) }
            items(history, key = { "h-" + it.id }) { pay ->
                Surface(color = CardBg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(start = 12.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(Money.format(pay.amount), color = GreenU, fontWeight = FontWeight.Bold)
                            val period = pay.periodDay?.let { "periodo que vencía ${Fmt.date(it, today)}" }
                            Text(
                                listOfNotNull("Cobrado ${Fmt.date(pay.day, today)}", period, pay.note.ifBlank { null }).joinToString(" · "),
                                color = Muted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        IconButton(onClick = { deletingPayment = pay }) {
                            Icon(Icons.Default.Delete, contentDescription = "Borrar cobro", tint = Muted)
                        }
                    }
                }
            }

            if (s.notes.isNotBlank()) {
                item(key = "notes") {
                    Block {
                        SectionTitle("Notas")
                        Text(s.notes, color = Ink)
                    }
                }
            }
        }
    }

    if (paying) {
        PaymentDialog(
            title = "Registrar cobro",
            subtitle = "${s.kind.label} · ${s.name} (vence ${Fmt.date(s.dueDay, today)})",
            suggested = s.amount,
            onSave = { amount, day, note ->
                Repo.chargeService(ctx, s.id, amount, day, note)
                paying = false
            },
            onDismiss = { paying = false },
        )
    }
    if (confirmSkip) {
        ConfirmDialog(
            title = "¿Saltar este periodo?",
            text = "El vencimiento pasa al ${Fmt.long(nextDue)} sin registrar cobro.",
            confirm = "Saltar",
            destructive = false,
            onConfirm = {
                confirmSkip = false
                Repo.skipServicePeriod(ctx, s.id)
            },
            onDismiss = { confirmSkip = false },
        )
    }
    deletingPayment?.let { pay ->
        ConfirmDialog(
            title = "¿Borrar este cobro?",
            text = "${Money.format(pay.amount)} del ${Fmt.long(pay.day)}. El vencimiento no regresa solo: si hace falta, corrígelo en Editar.",
            confirm = "Borrar",
            onConfirm = {
                Repo.deletePayment(ctx, pay.id)
                deletingPayment = null
            },
            onDismiss = { deletingPayment = null },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditServiceScreen(data: AppData, id: String?, clientId: String?, today: Long, nav: Nav) {
    val ctx = LocalContext.current
    val original = remember(id) { id?.let { data.service(it) } }
    var clientSel by remember { mutableStateOf(original?.clientId ?: clientId ?: data.clients.singleOrNull()?.id) }
    var kind by remember { mutableStateOf(original?.kind ?: ServiceKind.DOMAIN) }
    var name by remember { mutableStateOf(original?.name ?: "") }
    var amount by remember { mutableStateOf(original?.amount?.let { Money.plain(it) } ?: "") }
    var months by remember { mutableIntStateOf(original?.periodMonths ?: ServiceKind.DOMAIN.defaultMonths) }
    var dueDay by remember { mutableLongStateOf(original?.dueDay ?: (today + 30)) }
    var reminders by remember { mutableStateOf(original?.reminders) }
    var notes by remember { mutableStateOf(original?.notes ?: "") }
    var active by remember { mutableStateOf(original?.active ?: true) }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    val amountCents = Money.parse(amount)

    fun save() {
        val cid = clientSel?.takeIf { c -> data.clients.any { it.id == c } }
        val a = if (amount.isBlank()) 0L else amountCents
        if (cid == null) {
            error = "Escoge el cliente."
            return
        }
        if (name.isBlank()) {
            error = if (kind == ServiceKind.DOMAIN) "Escribe el dominio." else "Ponle nombre al servicio."
            return
        }
        if (a == null) {
            error = "El monto no es válido."
            return
        }
        val newId = original?.id ?: Repo.newId()
        Repo.saveService(
            ctx,
            Service(
                id = newId,
                clientId = cid,
                kind = kind,
                name = name.trim(),
                amount = a,
                periodMonths = months,
                dueDay = dueDay,
                // Si se cambia el vencimiento a otro periodo, la renovación vuelve a quedar pendiente.
                renewedMs = if (original != null && original.dueDay == dueDay && kind == ServiceKind.DOMAIN) original.renewedMs else null,
                active = active,
                reminders = reminders,
                notes = notes.trim(),
                armedMs = original?.armedMs ?: 0L,
                createdMs = original?.createdMs ?: System.currentTimeMillis(),
            ),
        )
        if (original == null) nav.replace(Screen.ServiceDetail(newId)) else nav.pop()
    }

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopBar(if (original == null) "Nuevo servicio" else "Editar servicio", onBack = { nav.pop() }) {
                if (original != null) {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Borrar servicio", tint = RedU)
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
                ) { Text("Guardar servicio", fontWeight = FontWeight.SemiBold) }
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

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Tipo")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ServiceKind.entries.forEach { k ->
                        FilterChip(
                            selected = kind == k,
                            onClick = {
                                if (original == null && months == kind.defaultMonths) months = k.defaultMonths
                                kind = k
                            },
                            label = { Text(k.label) },
                            colors = chipColors(),
                        )
                    }
                }
                if (kind == ServiceKind.DOMAIN) {
                    Text(
                        "Te aviso para renovarlo con tu proveedor y para cobrarle la renovación al cliente.",
                        color = Muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    error = null
                },
                label = { Text(if (kind == ServiceKind.DOMAIN) "Dominio" else "Nombre") },
                placeholder = { Text(placeholderFor(kind)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = if (kind == ServiceKind.DOMAIN) KeyboardCapitalization.None else KeyboardCapitalization.Sentences,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            AmountField(
                value = amount,
                onValueChange = {
                    amount = it
                    error = null
                },
                label = "Monto que le cobras (sin IVA)",
                isError = amount.isNotBlank() && amountCents == null,
                modifier = Modifier.fillMaxWidth(),
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle("Cada cuándo")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PERIODS.forEach { m ->
                        FilterChip(selected = months == m, onClick = { months = m }, label = { Text(Fmt.period(m)) }, colors = chipColors())
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionTitle(if (kind == ServiceKind.DOMAIN) "Fecha de vencimiento del dominio" else "Próximo cobro")
                DateButton(dueDay, { dueDay = it }, Modifier.fillMaxWidth())
                Text(
                    Agenda.countdown(dueDay, today),
                    color = urgencyColor(Agenda.urgency(dueDay, today)),
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            ReminderEditor("Avisos", reminders, data.settings.serviceReminders) { reminders = it }

            if (original != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Servicio activo", color = Ink, modifier = Modifier.weight(1f))
                    Switch(checked = active, onCheckedChange = { active = it }, colors = SwitchDefaults.colors(checkedTrackColor = Orange))
                }
            }

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas (opcional)") },
                placeholder = { Text("Proveedor, cuenta, lo que incluye…") },
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
            title = "¿Borrar este servicio?",
            text = "\"${original.name}\" se borra con su historial de cobros. No se puede deshacer.",
            confirm = "Borrar",
            onConfirm = {
                confirmDelete = false
                Repo.deleteService(ctx, original.id)
                nav.pop()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
