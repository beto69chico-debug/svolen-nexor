package com.tecnobasics.svolennexor.ui

import android.app.TimePickerDialog
import android.net.Uri
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.tecnobasics.svolennexor.Perm
import com.tecnobasics.svolennexor.Perms
import com.tecnobasics.svolennexor.R
import com.tecnobasics.svolennexor.core.AppData
import com.tecnobasics.svolennexor.core.Codec
import com.tecnobasics.svolennexor.core.Csv
import com.tecnobasics.svolennexor.core.Fmt
import com.tecnobasics.svolennexor.data.Repo
import com.tecnobasics.svolennexor.reminders.Notifs
import java.time.LocalDate

@Composable
fun SettingsScreen(data: AppData, resumeTick: Int) {
    val ctx = LocalContext.current
    val s = data.settings
    var permTick by remember { mutableIntStateOf(0) }
    val granted = remember(resumeTick, permTick) { Perm.entries.associateWith { Perms.isGranted(ctx, it) } }
    val askNotifs = rememberAskNotifications(settingsIfDenied = true) { permTick++ }
    var signature by remember(s.signature) { mutableStateOf(s.signature) }
    var pendingImport by remember { mutableStateOf<String?>(null) }
    val h24 = DateFormat.is24HourFormat(ctx)
    val stamp = LocalDate.now().toString()
    val version = remember {
        try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
        } catch (_: Exception) {
            ""
        }
    }

    fun write(uri: Uri?, text: String, ok: String) {
        if (uri == null) return
        try {
            ctx.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            Actions.toast(ctx, ok)
        } catch (_: Exception) {
            Actions.toast(ctx, "No se pudo guardar el archivo.")
        }
    }

    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        write(uri, Repo.exportJson(ctx), "Respaldo guardado.")
    }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        write(uri, Csv.export(Repo.get(ctx), Repo.today()), "Cobranza exportada.")
    }
    val importJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = try {
                ctx.contentResolver.openInputStream(uri)?.use { String(it.readBytes(), Charsets.UTF_8) }
            } catch (_: Exception) {
                null
            }
            if (text != null && Codec.looksLikeBackup(text)) pendingImport = text
            else Actions.toast(ctx, "Ese archivo no es un respaldo de Svölen-Nexor.")
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Ajustes", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)

        Block {
            SectionTitle("Avisos de siempre")
            Text(
                "Se usan en todo, a menos que a un proyecto o servicio le pongas avisos propios.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            Text("Entregas", color = Ink, fontWeight = FontWeight.SemiBold)
            ReminderChips(s.deliveryReminders) { Repo.saveSettings(ctx, s.copy(deliveryReminders = it)) }
            Text("Cobros de proyectos", color = Ink, fontWeight = FontWeight.SemiBold)
            ReminderChips(s.chargeReminders) { Repo.saveSettings(ctx, s.copy(chargeReminders = it)) }
            Text("Dominios, hosting y pólizas", color = Ink, fontWeight = FontWeight.SemiBold)
            ReminderChips(s.serviceReminders) { Repo.saveSettings(ctx, s.copy(serviceReminders = it)) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hora de los avisos", color = Ink, fontWeight = FontWeight.SemiBold)
                    Text("A esta hora te llegan los recordatorios del día.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = {
                    TimePickerDialog(ctx, { _, h, _ -> Repo.saveSettings(ctx, s.copy(notifyHour = h)) }, s.notifyHour, 0, h24).show()
                }) { Text(Fmt.hour(s.notifyHour, h24)) }
            }
        }

        Block {
            SectionTitle("Mensajes de cobro")
            Text(
                "Firma que va al final del mensaje de WhatsApp. Tú lo revisas antes de enviarlo.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            OutlinedTextField(
                value = signature,
                onValueChange = { signature = it },
                label = { Text("Firma") },
                placeholder = { Text("Ej. Alberto · Tecnobasics") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
            )
            if (signature != s.signature) {
                Button(
                    onClick = { Repo.saveSettings(ctx, s.copy(signature = signature.trim())) },
                    colors = ButtonDefaults.buttonColors(containerColor = Orange),
                ) { Text("Guardar firma") }
            }
        }

        Block {
            SectionTitle("Para que los avisos sí lleguen")
            Perm.entries.forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(if (p.essential) p.title else "${p.title} (recomendado)", fontWeight = FontWeight.SemiBold, color = Ink)
                        Text(p.desc, style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                    if (granted[p] == true) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Listo", tint = GreenU)
                    } else {
                        Button(
                            onClick = { if (p == Perm.NOTIFICATIONS) askNotifs() else Perms.open(ctx, p) },
                            colors = ButtonDefaults.buttonColors(containerColor = Orange),
                        ) { Text("Activar") }
                    }
                }
            }
            OutlinedButton(
                onClick = { if (!Notifs.test(ctx)) Actions.toast(ctx, "Primero activa las notificaciones") },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Probar un aviso") }
        }

        Block {
            SectionTitle("Respaldo")
            Text(
                "Todo se guarda solo en este teléfono. Haz un respaldo de vez en cuando y guárdalo en Drive o mándatelo por correo.",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            Button(
                onClick = { exportJson.launch("svolen-respaldo-$stamp.json") },
                colors = ButtonDefaults.buttonColors(containerColor = Orange),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Guardar respaldo (JSON)") }
            OutlinedButton(
                onClick = { importJson.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Restaurar un respaldo") }
            OutlinedButton(
                onClick = { exportCsv.launch("svolen-cobranza-$stamp.csv") },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Exportar cobranza a Excel (CSV)") }
        }

        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(R.drawable.icono_sn), contentDescription = null, modifier = Modifier.size(44.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Svölen-Nexor $version", fontWeight = FontWeight.SemiBold, color = Ink)
                Text(
                    "${data.clients.size} clientes · ${data.projects.size} proyectos · ${data.services.size} servicios. Hecha por Tecnobasics.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Muted,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    pendingImport?.let { text ->
        ConfirmDialog(
            title = "¿Restaurar este respaldo?",
            text = "Se reemplaza todo lo que hay ahora en la app (clientes, proyectos, servicios y pagos) por lo del respaldo.",
            confirm = "Restaurar",
            onConfirm = {
                pendingImport = null
                try {
                    Repo.replaceAll(ctx, Codec.decode(text))
                    Actions.toast(ctx, "Respaldo restaurado.")
                } catch (_: Exception) {
                    Actions.toast(ctx, "El respaldo está dañado; no se cambió nada.")
                }
            },
            onDismiss = { pendingImport = null },
        )
    }
}
