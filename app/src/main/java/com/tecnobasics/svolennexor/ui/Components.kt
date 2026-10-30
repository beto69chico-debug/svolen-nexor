package com.tecnobasics.svolennexor.ui

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableChipColors
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.tecnobasics.svolennexor.Perm
import com.tecnobasics.svolennexor.Perms
import com.tecnobasics.svolennexor.core.Agenda
import com.tecnobasics.svolennexor.core.Billing
import com.tecnobasics.svolennexor.core.Client
import com.tecnobasics.svolennexor.core.Event
import com.tecnobasics.svolennexor.core.Fmt
import com.tecnobasics.svolennexor.core.Money
import com.tecnobasics.svolennexor.core.Palette
import com.tecnobasics.svolennexor.core.Reminders
import com.tecnobasics.svolennexor.data.Repo
import java.time.LocalDate

/** Pastilla de color con texto: cuenta regresiva, estado, etc. */
@Composable
fun Pill(text: String, color: Color) {
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.16f)) {
        Text(
            text,
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
        )
    }
}

@Composable
fun Dot(color: Color, size: Int = 10) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(color))
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = Orange,
        modifier = modifier,
    )
}

/** Tarjeta de fondo con borde fino. */
@Composable
fun Block(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color = CardBg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Line),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}

/** Cifra grande con su etiqueta, para el resumen. */
@Composable
fun StatTile(label: String, amount: Long, color: Color, modifier: Modifier = Modifier) {
    Surface(
        color = CardBg,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Line),
        modifier = modifier,
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Muted, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(
                Money.format(amount),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun MoneyLine(label: String, amount: Long, color: Color = Ink, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Muted, modifier = Modifier.weight(1f))
        Text(Money.format(amount), color = color, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold)
    }
}

@Composable
fun EmptyState(title: String, text: String) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, fontWeight = FontWeight.Bold, color = Ink, style = MaterialTheme.typography.titleMedium)
        Text(text, color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg, titleContentColor = Ink),
    )
}

@Composable
fun chipColors(): SelectableChipColors = FilterChipDefaults.filterChipColors(
    selectedContainerColor = OrangeSoft,
    selectedLabelColor = Ink,
    labelColor = Muted,
)

/** Campo de monto en pesos. */
@Composable
fun AmountField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { t -> onValueChange(t.filter { it.isDigit() || it == '.' || it == ',' }) },
        label = { Text(label) },
        prefix = { Text("$ ") },
        singleLine = true,
        isError = isError,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier,
    )
}

/** Botón que muestra una fecha y abre el calendario. */
@Composable
fun DateButton(day: Long, onPick: (Long) -> Unit, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    OutlinedButton(
        onClick = {
            val d = LocalDate.ofEpochDay(day)
            DatePickerDialog(
                ctx,
                { _, y, m, dd -> onPick(LocalDate.of(y, m + 1, dd).toEpochDay()) },
                d.year,
                d.monthValue - 1,
                d.dayOfMonth,
            ).show()
        },
        modifier = modifier,
    ) {
        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(Fmt.date(day, Repo.today()), maxLines = 1)
    }
}

/** Chips de "cuántos días antes". */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReminderChips(selected: List<Int>, onChange: (List<Int>) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Reminders.OPTIONS.forEach { d ->
            val on = d in selected
            FilterChip(
                selected = on,
                onClick = { onChange(Reminders.normalize(if (on) selected - d else selected + d)) },
                label = { Text(Reminders.chip(d)) },
                colors = chipColors(),
            )
        }
    }
}

/**
 * Avisos de un proyecto o servicio: los de Ajustes (null) o propios.
 */
@Composable
fun ReminderEditor(title: String, custom: List<Int>?, defaults: List<Int>, onChange: (List<Int>?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle(title)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Usar los de Ajustes", color = Ink)
                Text(Reminders.summary(defaults), style = MaterialTheme.typography.bodySmall, color = Muted)
            }
            Switch(
                checked = custom == null,
                onCheckedChange = { onChange(if (it) null else defaults) },
                colors = SwitchDefaults.colors(checkedTrackColor = Orange),
            )
        }
        if (custom != null) {
            ReminderChips(custom) { onChange(it) }
            Text(Reminders.summary(custom), style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPicker(selected: Int, onPick: (Int) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Palette.COLORS.indices.forEach { i ->
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(clientColor(i))
                    .clickable { onPick(i) },
                contentAlignment = Alignment.Center,
            ) {
                if (i == selected) Icon(Icons.Default.Check, contentDescription = "Elegido", tint = Color.White)
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirm, color = if (destructive) RedU else Orange) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** Registrar un pago: monto (sugerido), fecha y nota. */
@Composable
fun PaymentDialog(
    title: String,
    subtitle: String?,
    suggested: Long,
    onSave: (amount: Long, day: Long, note: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var amount by remember { mutableStateOf(Money.plain(suggested)) }
    var day by remember { mutableLongStateOf(Repo.today()) }
    var note by remember { mutableStateOf("") }
    val parsed = Money.parse(amount)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBg,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (subtitle != null) Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
                AmountField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Monto recibido",
                    isError = amount.isNotBlank() && (parsed == null || parsed <= 0),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Fecha del pago", style = MaterialTheme.typography.labelLarge, color = Muted)
                DateButton(day, { day = it }, Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Nota (opcional)") },
                    placeholder = { Text("Transferencia, efectivo…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = parsed != null && parsed > 0,
                onClick = { if (parsed != null) onSave(parsed, day, note.trim()) },
            ) { Text("Guardar pago") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/**
 * Regresa una función que pide el permiso de notificaciones.
 * Si Android ya no muestra la ventanita (la negaron antes), abre los ajustes de la app.
 */
@Composable
fun rememberAskNotifications(settingsIfDenied: Boolean, onResult: () -> Unit): () -> Unit {
    val ctx = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted && settingsIfDenied) Perms.open(ctx, Perm.NOTIFICATIONS)
        onResult()
    }
    return {
        val needsRuntime = Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (needsRuntime) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (settingsIfDenied) {
            Perms.open(ctx, Perm.NOTIFICATIONS)
            onResult()
        }
    }
}

/** Renglón con una barrita del color del cliente a la izquierda. */
@Composable
fun ColorRow(color: Color, onClick: (() -> Unit)?, content: @Composable RowScope.() -> Unit) {
    Surface(
        color = CardBg,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Line),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Row(Modifier.height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(5.dp).fillMaxHeight().background(color))
            Row(
                Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

/** Una cosa del timeline: entrega, cobro, servicio o renovación. */
@Composable
fun EventRow(e: Event, client: Client?, today: Long, onClick: () -> Unit) {
    ColorRow(color = client?.let { clientColor(it.color) } ?: Muted, onClick = onClick) {
        Column(Modifier.weight(1f)) {
            Text(
                e.kind.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = kindColor(e.kind),
                fontWeight = FontWeight.Bold,
            )
            Text(e.title, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(e.detail.ifBlank { null }, client?.display).joinToString(" · "),
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Pill(Agenda.countdown(e.dueDay, today), urgencyColor(Agenda.urgency(e.dueDay, today)))
            val amount = e.amount
            if (amount != null) Text(Money.format(amount), color = Ink, fontWeight = FontWeight.Bold)
            else Text(Fmt.date(e.dueDay, today), color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Un cobro pendiente con sus botones. */
@Composable
fun ChargeRow(
    c: Billing.Charge,
    client: Client?,
    today: Long,
    onOpen: () -> Unit,
    onPaid: () -> Unit,
    onWhatsApp: () -> Unit,
) {
    ColorRow(color = client?.let { clientColor(it.color) } ?: Muted, onClick = onOpen) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(c.concept, color = Ink, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(c.source.ifBlank { null }, client?.display).joinToString(" · "),
                color = Muted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Pill(Agenda.countdown(c.dueDay, today), urgencyColor(Agenda.urgency(c.dueDay, today)))
                Spacer(Modifier.width(6.dp))
                Text(Fmt.date(c.dueDay, today), color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(Money.format(c.amount), color = Ink, fontWeight = FontWeight.Bold)
            Row {
                TextButton(onClick = onWhatsApp) { Text("WhatsApp", color = GreenU) }
                TextButton(onClick = onPaid) { Text("Cobrado", color = Orange) }
            }
        }
    }
}
