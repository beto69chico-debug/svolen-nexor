package com.tecnobasics.svolennexor.core

import java.time.LocalDate
import java.time.ZoneId

/** Cuándo avisar. Todo en días antes del vencimiento; 0 = el mismo día. */
object Reminders {
    val DEFAULT_DELIVERY: List<Int> = listOf(7, 3, 1)
    val DEFAULT_CHARGE: List<Int> = listOf(30, 15, 3)
    val DEFAULT_SERVICE: List<Int> = listOf(30, 15, 3)

    /** Opciones que se pueden escoger. */
    val OPTIONS: List<Int> = listOf(60, 30, 15, 7, 5, 3, 2, 1, 0)

    fun short(days: Int): String = when (days) {
        0 -> "el mismo día"
        1 -> "1 día"
        else -> "$days días"
    }

    fun chip(days: Int): String = if (days == 0) "Mismo día" else short(days)

    /** "30, 15 y 3 días antes" */
    fun summary(list: List<Int>): String {
        val n = normalize(list)
        if (n.isEmpty()) return "Sin avisos"
        val sameDay = 0 in n
        val before = n.filter { it > 0 }
        val parts = mutableListOf<String>()
        if (before.isNotEmpty()) {
            val nums = before.map { it.toString() }
            val joined = if (nums.size == 1) nums[0] else nums.dropLast(1).joinToString(", ") + " y " + nums.last()
            parts += joined + (if (before.size == 1 && before[0] == 1) " día antes" else " días antes")
        }
        if (sameDay) parts += "el mismo día"
        return parts.joinToString(" y ").replaceFirstChar { it.uppercase() }
    }

    /** Sin repetidos, del más lejano al más cercano. */
    fun normalize(list: List<Int>): List<Int> = list.filter { it in 0..365 }.distinct().sortedDescending()

    /** A qué hora exacta suena el aviso de [daysBefore] días antes de [dueDay]. */
    fun atMs(dueDay: Long, daysBefore: Int, hour: Int, zone: ZoneId): Long =
        LocalDate.ofEpochDay(dueDay - daysBefore).atTime(hour.coerceIn(0, 23), 0).atZone(zone).toInstant().toEpochMilli()

    /** Fin del día del vencimiento: después de eso ya no se avisa (ya venció). */
    fun endOfDay(dueDay: Long, zone: ZoneId): Long =
        LocalDate.ofEpochDay(dueDay + 1).atStartOfDay(zone).toInstant().toEpochMilli()

    /** Un aviso concreto: de qué, cuántos días antes y a qué hora suena. */
    data class Hit(val event: Event, val daysBefore: Int, val atMs: Long)

    fun all(events: List<Event>, hour: Int, zone: ZoneId): List<Hit> =
        events.asSequence()
            .flatMap { e -> normalize(e.offsets).asSequence().map { Hit(e, it, atMs(e.dueDay, it, hour, zone)) } }
            .filter { it.atMs > it.event.armedMs }
            .toList()

    /**
     * Los avisos que tocan en (afterMs, upToMs].
     * Si a una misma cosa le tocaban varios (el teléfono estuvo apagado), solo se manda el
     * más reciente, y nunca de cosas cuyo día ya terminó.
     */
    fun due(events: List<Event>, hour: Int, zone: ZoneId, afterMs: Long, upToMs: Long): List<Hit> =
        all(events, hour, zone)
            .filter { it.atMs > afterMs && it.atMs <= upToMs && endOfDay(it.event.dueDay, zone) > upToMs }
            .groupBy { it.event.key }
            .map { (_, hits) -> hits.maxBy { it.atMs } }
            .sortedBy { it.event.dueDay }

    /** El siguiente momento en que hay que avisar de algo, o null si no hay nada. */
    fun next(events: List<Event>, hour: Int, zone: ZoneId, afterMs: Long): Long? =
        all(events, hour, zone).asSequence().map { it.atMs }.filter { it > afterMs }.minOrNull()
}
