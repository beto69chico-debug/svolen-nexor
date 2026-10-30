package com.tecnobasics.svolennexor.core

/*
 * Todo lo que guarda Svölen-Nexor, en el propio teléfono.
 *
 * Convenciones:
 *  - Dinero en centavos (Long), siempre MXN y sin IVA.
 *  - Fechas como "día" (LocalDate.toEpochDay): un hito, una parcialidad o un vencimiento
 *    caen en un día, no a una hora. Los avisos suenan a la hora fijada en Ajustes.
 *  - Avisos en días antes (0 = el mismo día). null en un registro = usar los de Ajustes.
 *  - armedMs: última vez que cambió la fecha o los avisos de algo. Los avisos que caían
 *    antes de ese momento ya no se mandan (si no, al crear un cobro para mañana sonaría
 *    de golpe el aviso de "faltan 30 días").
 */

/** Un cliente: con quién tratar y el color con el que se distinguen sus cosas. */
data class Client(
    val id: String,
    val name: String,
    val company: String,
    val contact: String,
    val phone: String,
    val email: String,
    val notes: String,
    val color: Int, // índice en Palette.COLORS
) {
    val display: String get() = name.ifBlank { company }
}

enum class ProjectStatus(val label: String) {
    PENDING("Pendiente"),
    ACTIVE("En curso"),
    REVIEW("En revisión"),
    DELIVERED("Entregado"),
    CANCELLED("Cancelado"),
    ;

    /** Sigue habiendo entregas por hacer. */
    val open: Boolean get() = this != DELIVERED && this != CANCELLED
}

/** Un hito de entrega dentro de un proyecto (diseño, desarrollo, lanzamiento…). */
data class Milestone(
    val id: String,
    val title: String,
    val dueDay: Long,
    val doneMs: Long?,
    val armedMs: Long,
) {
    val done: Boolean get() = doneMs != null
}

/** Una parcialidad del plan de pagos: cuánto y cuándo se espera cobrar. */
data class Installment(
    val id: String,
    val label: String,
    val amount: Long,
    val dueDay: Long,
    val armedMs: Long,
)

data class Project(
    val id: String,
    val clientId: String,
    val title: String,
    val notes: String,
    val total: Long,
    val status: ProjectStatus,
    val milestones: List<Milestone>,
    val installments: List<Installment>,
    /** Días antes de cada hito en que se avisa. */
    val deliveryReminders: List<Int>?,
    /** Días antes de cada parcialidad en que se avisa. */
    val chargeReminders: List<Int>?,
    val createdMs: Long,
)

enum class ServiceKind(val label: String, val defaultMonths: Int) {
    DOMAIN("Dominio", 12),
    HOSTING("Hosting", 12),
    POLICY("Póliza de mantenimiento", 12),
    OTHER("Otro servicio", 12),
}

/**
 * Algo que se cobra cada cierto tiempo: dominio, hosting, póliza…
 *
 * [dueDay] es el vencimiento del periodo actual. El periodo se cierra cuando ya se cobró
 * (un Payment con periodDay == dueDay) y, si es dominio, cuando además ya se renovó
 * ([renewedMs]). Al cerrarse, [dueDay] avanza [periodMonths] meses.
 */
data class Service(
    val id: String,
    val clientId: String,
    val kind: ServiceKind,
    val name: String,
    val amount: Long,
    val periodMonths: Int,
    val dueDay: Long,
    val renewedMs: Long?,
    val active: Boolean,
    val reminders: List<Int>?,
    val notes: String,
    val armedMs: Long,
    val createdMs: Long,
) {
    val renewed: Boolean get() = renewedMs != null
    val needsRenewal: Boolean get() = kind == ServiceKind.DOMAIN
}

/** Un pago que de verdad llegó. Es de un proyecto o de un periodo de un servicio. */
data class Payment(
    val id: String,
    val projectId: String?,
    val serviceId: String?,
    /** Para servicios: el vencimiento del periodo que se cobró. */
    val periodDay: Long?,
    val amount: Long,
    val day: Long,
    val note: String,
    val createdMs: Long,
)

data class Settings(
    val deliveryReminders: List<Int> = Reminders.DEFAULT_DELIVERY,
    val chargeReminders: List<Int> = Reminders.DEFAULT_CHARGE,
    val serviceReminders: List<Int> = Reminders.DEFAULT_SERVICE,
    /** Hora del día (0–23) a la que suenan los avisos. */
    val notifyHour: Int = 9,
    /** Cómo firmas los mensajes de cobro por WhatsApp. */
    val signature: String = "",
)

data class AppData(
    val clients: List<Client> = emptyList(),
    val projects: List<Project> = emptyList(),
    val services: List<Service> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val settings: Settings = Settings(),
    /** Hasta qué momento ya se revisaron y mandaron los avisos. */
    val lastCheckMs: Long = 0L,
) {
    fun client(id: String?): Client? = clients.firstOrNull { it.id == id }
    fun project(id: String?): Project? = projects.firstOrNull { it.id == id }
    fun service(id: String?): Service? = services.firstOrNull { it.id == id }
}

/** Colores para los clientes (ARGB). */
object Palette {
    val COLORS: List<Long> = listOf(
        0xFFFD491C, // naranja Svölen
        0xFF3B82F6, // azul
        0xFF22C55E, // verde
        0xFFA855F7, // morado
        0xFFEC4899, // rosa
        0xFFEAB308, // mostaza
        0xFF14B8A6, // turquesa
        0xFFEF4444, // rojo
        0xFF94A3B8, // gris pizarra
        0xFFB45309, // café
        0xFF6366F1, // índigo
        0xFF84CC16, // lima
    )

    fun argb(index: Int): Long = COLORS[Math.floorMod(index, COLORS.size)]

    /** El siguiente color que todavía nadie usa, para que cada cliente nuevo se distinga. */
    fun nextFree(used: Collection<Int>): Int =
        COLORS.indices.firstOrNull { it !in used } ?: (used.size % COLORS.size)
}
