package com.tecnobasics.svolennexor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tecnobasics.svolennexor.core.AppData
import com.tecnobasics.svolennexor.core.Billing
import com.tecnobasics.svolennexor.core.Client
import com.tecnobasics.svolennexor.core.Money
import com.tecnobasics.svolennexor.core.Palette
import com.tecnobasics.svolennexor.data.Repo

@Composable
fun ClientsScreen(data: AppData, today: Long, nav: Nav) {
    val ctx = LocalContext.current
    var creating by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val list = data.clients
        .filter { c ->
            query.isBlank() || listOf(c.name, c.company, c.contact).any { it.contains(query.trim(), ignoreCase = true) }
        }
        .sortedBy { it.display.lowercase() }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "title") {
                Text("Clientes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
            }
            if (data.clients.size > 5) {
                item(key = "search") {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        placeholder = { Text("Buscar cliente") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            if (data.clients.isEmpty()) {
                item(key = "empty") {
                    EmptyState("Sin clientes todavía", "Toca \"Cliente\" para agregar el primero, con su color y contacto.")
                }
            }
            items(list, key = { it.id }) { c ->
                val m = Billing.clientMoney(data, c.id, today)
                val nProjects = data.projects.count { it.clientId == c.id && it.status.open }
                val nServices = data.services.count { it.clientId == c.id && it.active }
                ColorRow(clientColor(c.color), onClick = { nav.push(Screen.ClientDetail(c.id)) }) {
                    Column(Modifier.weight(1f)) {
                        Text(c.display, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        val sub = listOf(c.company, c.contact).filter { it.isNotBlank() && it != c.display }.joinToString(" · ")
                        if (sub.isNotBlank()) Text(sub, color = Muted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                        Text(
                            "$nProjects proyectos activos · $nServices servicios",
                            color = Muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    when {
                        m.overdue > 0 -> Pill("Vencido ${Money.format(m.overdue)}", RedU)
                        m.pending > 0 -> Text(Money.format(m.pending), color = AmberU, fontWeight = FontWeight.Bold)
                        else -> Pill("Al corriente", GreenU)
                    }
                }
            }
        }
        ExtendedFloatingActionButton(
            onClick = { creating = true },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Cliente", fontWeight = FontWeight.SemiBold) },
            containerColor = Orange,
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    if (creating) {
        ClientDialog(
            initial = null,
            usedColors = data.clients.map { it.color },
            onSave = { c ->
                Repo.saveClient(ctx, c)
                creating = false
                nav.push(Screen.ClientDetail(c.id))
            },
            onDismiss = { creating = false },
        )
    }
}

/** Crear o editar un cliente. */
@Composable
fun ClientDialog(initial: Client?, usedColors: List<Int>, onSave: (Client) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var company by remember { mutableStateOf(initial?.company ?: "") }
    var contact by remember { mutableStateOf(initial?.contact ?: "") }
    var phone by remember { mutableStateOf(initial?.phone ?: "") }
    var email by remember { mutableStateOf(initial?.email ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var color by remember { mutableIntStateOf(initial?.color ?: Palette.nextFree(usedColors)) }
    val valid = name.isNotBlank() || company.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = { Text(if (initial == null) "Nuevo cliente" else "Editar cliente") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Field(name, { name = it }, "Nombre o marca", "Ej. La Parrilla Vikinga", KeyboardCapitalization.Words)
                Field(company, { company = it }, "Empresa o razón social (opcional)", "", KeyboardCapitalization.Words)
                Field(contact, { contact = it }, "Persona de contacto", "Con quién hablas", KeyboardCapitalization.Words)
                Field(phone, { phone = it }, "Teléfono / WhatsApp", "55 1234 5678", KeyboardCapitalization.None, KeyboardType.Phone)
                Field(email, { email = it }, "Correo", "", KeyboardCapitalization.None, KeyboardType.Email)
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas") },
                    minLines = 2,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Color", style = MaterialTheme.typography.labelLarge, color = Muted)
                ColorPicker(color) { color = it }
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = {
                    onSave(
                        Client(
                            id = initial?.id ?: Repo.newId(),
                            name = name.trim(),
                            company = company.trim(),
                            contact = contact.trim(),
                            phone = phone.trim(),
                            email = email.trim(),
                            notes = notes.trim(),
                            color = color,
                        ),
                    )
                },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    placeholder: String,
    cap: KeyboardCapitalization,
    type: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = cap, keyboardType = type),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun ClientDetailScreen(data: AppData, id: String, today: Long, nav: Nav) {
    val ctx = LocalContext.current
    val c = data.client(id)
    if (c == null) {
        LaunchedEffect(Unit) { nav.pop() }
        return
    }
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val projects = data.projects.filter { it.clientId == id }.sortedWith(compareBy({ !it.status.open }, { -it.createdMs }))
    val services = data.services.filter { it.clientId == id }.sortedWith(compareBy({ !it.active }, { it.dueDay }))
    val money = Billing.clientMoney(data, id, today)
    val charges = Billing.pendingCharges(data).filter { it.clientId == id }

    Scaffold(
        containerColor = Bg,
        topBar = {
            TopBar(c.display, onBack = { nav.pop() }) {
                IconButton(onClick = { editing = true }) { Icon(Icons.Default.Edit, contentDescription = "Editar") }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = RedU) }
            }
        },
    ) { pad ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "contact") {
                Block {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(clientColor(c.color), 14)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(c.display, color = Ink, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            if (c.company.isNotBlank() && c.company != c.display) Text(c.company, color = Muted)
                        }
                    }
                    if (c.contact.isNotBlank()) InfoLine("Contacto", c.contact)
                    if (c.phone.isNotBlank()) InfoLine("Teléfono", c.phone)
                    if (c.email.isNotBlank()) InfoLine("Correo", c.email)
                    if (c.phone.isNotBlank() || c.email.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (c.phone.isNotBlank()) {
                                OutlinedButton(onClick = { Actions.call(ctx, c.phone) }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Llamar", maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = { Actions.whatsApp(ctx, c.phone, "Hola ${c.contact.ifBlank { c.display }}, ") },
                                    modifier = Modifier.weight(1f),
                                ) { Text("WhatsApp", color = GreenU, maxLines = 1) }
                            }
                            if (c.email.isNotBlank()) {
                                OutlinedButton(onClick = { Actions.mail(ctx, c.email) }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Correo", maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            item(key = "money") {
                Block {
                    SectionTitle("Cobranza")
                    MoneyLine("Cobrado", money.collected, GreenU)
                    MoneyLine("Pendiente", money.pending, AmberU)
                    if (money.overdue > 0) MoneyLine("Vencido", money.overdue, RedU, bold = true)
                    if (charges.isNotEmpty()) {
                        Button(
                            onClick = { ChargeOps.whatsApp(ctx, data, charges.filter { it.dueDay <= today + 30 }.ifEmpty { charges }, today) },
                            colors = ButtonDefaults.buttonColors(containerColor = Orange),
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("Recordar cobro por WhatsApp") }
                    }
                }
            }

            item(key = "projects-title") {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Proyectos", Modifier.weight(1f))
                    TextButton(onClick = { nav.push(Screen.EditProject(null, id)) }) { Text("+ Nuevo proyecto", color = Orange) }
                }
            }
            if (projects.isEmpty()) item(key = "projects-empty") { Text("Sin proyectos.", color = Muted) }
            items(projects, key = { "p-" + it.id }) { p ->
                ProjectRow(data, p, today) { nav.push(Screen.ProjectDetail(p.id)) }
            }

            item(key = "services-title") {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("Servicios", Modifier.weight(1f))
                    TextButton(onClick = { nav.push(Screen.EditService(null, id)) }) { Text("+ Nuevo servicio", color = Orange) }
                }
            }
            if (services.isEmpty()) item(key = "services-empty") { Text("Sin dominios, hosting ni pólizas.", color = Muted) }
            items(services, key = { "s-" + it.id }) { s ->
                ServiceRow(data, s, today) { nav.push(Screen.ServiceDetail(s.id)) }
            }

            if (c.notes.isNotBlank()) {
                item(key = "notes") {
                    Block {
                        SectionTitle("Notas")
                        Text(c.notes, color = Ink)
                    }
                }
            }
        }
    }

    if (editing) {
        ClientDialog(
            initial = c,
            usedColors = data.clients.filter { it.id != c.id }.map { it.color },
            onSave = {
                Repo.saveClient(ctx, it)
                editing = false
            },
            onDismiss = { editing = false },
        )
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "¿Borrar a ${c.display}?",
            text = "También se borran sus ${projects.size} proyectos, ${services.size} servicios y todos sus pagos. No se puede deshacer.",
            confirm = "Borrar",
            onConfirm = {
                confirmDelete = false
                Repo.deleteClient(ctx, c.id)
                nav.pop()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
fun InfoLine(label: String, value: String) {
    Row {
        Text(label, color = Muted, modifier = Modifier.width(90.dp), style = MaterialTheme.typography.bodyMedium)
        Text(value, color = Ink, style = MaterialTheme.typography.bodyMedium)
    }
}
