package com.tecnobasics.svolennexor.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tecnobasics.svolennexor.core.EventKind
import com.tecnobasics.svolennexor.core.Palette
import com.tecnobasics.svolennexor.core.Urgency

// Oscuro con el naranja del gallo: el logo es blanco y así se luce.
val Orange = Color(0xFFFD491C)
val OrangeSoft = Color(0x33FD491C)
val Bg = Color(0xFF121214)
val CardBg = Color(0xFF1C1C21)
val Card2 = Color(0xFF26262D)
val Ink = Color(0xFFF4F4F5)
val Muted = Color(0xFFA1A1AA)
val Line = Color(0xFF2E2E36)
val RedU = Color(0xFFF87171)
val AmberU = Color(0xFFFBBF24)
val GreenU = Color(0xFF4ADE80)
val BlueU = Color(0xFF60A5FA)

fun urgencyColor(u: Urgency): Color = when (u) {
    Urgency.OVERDUE -> RedU
    Urgency.HIGH -> Orange
    Urgency.MID -> AmberU
    Urgency.LOW -> GreenU
}

fun clientColor(index: Int): Color = Color(Palette.argb(index))

fun kindColor(k: EventKind): Color = when (k) {
    EventKind.DELIVERY -> BlueU
    EventKind.CHARGE -> GreenU
    EventKind.SERVICE -> AmberU
    EventKind.RENEW -> Orange
}

@Composable
fun SvolenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Orange,
            onPrimary = Color.White,
            primaryContainer = OrangeSoft,
            onPrimaryContainer = Ink,
            secondary = Orange,
            onSecondary = Color.White,
            secondaryContainer = OrangeSoft,
            onSecondaryContainer = Ink,
            background = Bg,
            onBackground = Ink,
            surface = CardBg,
            onSurface = Ink,
            surfaceVariant = Card2,
            onSurfaceVariant = Muted,
            surfaceContainer = CardBg,
            surfaceContainerHigh = Card2,
            surfaceContainerHighest = Card2,
            surfaceContainerLow = CardBg,
            surfaceContainerLowest = Bg,
            outline = Line,
            outlineVariant = Line,
            error = RedU,
            onError = Color.Black,
        ),
        content = content,
    )
}
