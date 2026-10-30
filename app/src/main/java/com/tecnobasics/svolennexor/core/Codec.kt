package com.tecnobasics.svolennexor.core

import org.json.JSONArray
import org.json.JSONObject

/** Convierte los datos a JSON y de regreso. Tolera campos faltantes para no perder nada al actualizar. */
object Codec {
    private const val VERSION = 1
    const val APP_TAG = "svolen-nexor"

    fun encode(d: AppData): String = JSONObject()
        .put("app", APP_TAG)
        .put("v", VERSION)
        .put("clients", JSONArray().apply {
            d.clients.forEach {
                put(
                    JSONObject().put("id", it.id).put("name", it.name).put("company", it.company)
                        .put("contact", it.contact).put("phone", it.phone).put("email", it.email)
                        .put("notes", it.notes).put("color", it.color),
                )
            }
        })
        .put("projects", JSONArray().apply {
            d.projects.forEach { p ->
                put(
                    JSONObject()
                        .put("id", p.id)
                        .put("clientId", p.clientId)
                        .put("title", p.title)
                        .put("notes", p.notes)
                        .put("total", p.total)
                        .put("status", p.status.name)
                        .put("milestones", JSONArray().apply {
                            p.milestones.forEach { m ->
                                put(
                                    JSONObject().put("id", m.id).put("title", m.title).put("dueDay", m.dueDay)
                                        .put("doneMs", m.doneMs ?: JSONObject.NULL).put("armedMs", m.armedMs),
                                )
                            }
                        })
                        .put("installments", JSONArray().apply {
                            p.installments.forEach { i ->
                                put(
                                    JSONObject().put("id", i.id).put("label", i.label).put("amount", i.amount)
                                        .put("dueDay", i.dueDay).put("armedMs", i.armedMs),
                                )
                            }
                        })
                        .put("deliveryReminders", p.deliveryReminders?.let { ints(it) } ?: JSONObject.NULL)
                        .put("chargeReminders", p.chargeReminders?.let { ints(it) } ?: JSONObject.NULL)
                        .put("createdMs", p.createdMs),
                )
            }
        })
        .put("services", JSONArray().apply {
            d.services.forEach { s ->
                put(
                    JSONObject()
                        .put("id", s.id)
                        .put("clientId", s.clientId)
                        .put("kind", s.kind.name)
                        .put("name", s.name)
                        .put("amount", s.amount)
                        .put("periodMonths", s.periodMonths)
                        .put("dueDay", s.dueDay)
                        .put("renewedMs", s.renewedMs ?: JSONObject.NULL)
                        .put("active", s.active)
                        .put("reminders", s.reminders?.let { ints(it) } ?: JSONObject.NULL)
                        .put("notes", s.notes)
                        .put("armedMs", s.armedMs)
                        .put("createdMs", s.createdMs),
                )
            }
        })
        .put("payments", JSONArray().apply {
            d.payments.forEach { p ->
                put(
                    JSONObject()
                        .put("id", p.id)
                        .put("projectId", p.projectId ?: JSONObject.NULL)
                        .put("serviceId", p.serviceId ?: JSONObject.NULL)
                        .put("periodDay", p.periodDay ?: JSONObject.NULL)
                        .put("amount", p.amount)
                        .put("day", p.day)
                        .put("note", p.note)
                        .put("createdMs", p.createdMs),
                )
            }
        })
        .put("settings", JSONObject()
            .put("deliveryReminders", ints(d.settings.deliveryReminders))
            .put("chargeReminders", ints(d.settings.chargeReminders))
            .put("serviceReminders", ints(d.settings.serviceReminders))
            .put("notifyHour", d.settings.notifyHour)
            .put("signature", d.settings.signature))
        .put("lastCheckMs", d.lastCheckMs)
        .toString()

    fun decode(text: String): AppData {
        val o = JSONObject(text)
        val clients = o.optJSONArray("clients").objects().map {
            Client(
                id = it.getString("id"),
                name = it.optString("name"),
                company = it.optString("company"),
                contact = it.optString("contact"),
                phone = it.optString("phone"),
                email = it.optString("email"),
                notes = it.optString("notes"),
                color = it.optInt("color", 0),
            )
        }
        val projects = o.optJSONArray("projects").objects().map { p ->
            val created = p.optLong("createdMs", 0L)
            Project(
                id = p.getString("id"),
                clientId = p.optString("clientId"),
                title = p.optString("title"),
                notes = p.optString("notes"),
                total = p.optLong("total", 0L),
                status = enumOr(p.optString("status"), ProjectStatus.ACTIVE),
                milestones = p.optJSONArray("milestones").objects().map { m ->
                    Milestone(
                        id = m.getString("id"),
                        title = m.optString("title"),
                        dueDay = m.getLong("dueDay"),
                        doneMs = optLongOrNull(m, "doneMs"),
                        armedMs = m.optLong("armedMs", created),
                    )
                },
                installments = p.optJSONArray("installments").objects().map { i ->
                    Installment(
                        id = i.getString("id"),
                        label = i.optString("label"),
                        amount = i.optLong("amount", 0L),
                        dueDay = i.getLong("dueDay"),
                        armedMs = i.optLong("armedMs", created),
                    )
                },
                deliveryReminders = if (p.isNull("deliveryReminders")) null else p.optJSONArray("deliveryReminders").ints(),
                chargeReminders = if (p.isNull("chargeReminders")) null else p.optJSONArray("chargeReminders").ints(),
                createdMs = created,
            )
        }
        val services = o.optJSONArray("services").objects().map { s ->
            val kind = enumOr(s.optString("kind"), ServiceKind.OTHER)
            val created = s.optLong("createdMs", 0L)
            Service(
                id = s.getString("id"),
                clientId = s.optString("clientId"),
                kind = kind,
                name = s.optString("name"),
                amount = s.optLong("amount", 0L),
                periodMonths = s.optInt("periodMonths", kind.defaultMonths).coerceAtLeast(1),
                dueDay = s.getLong("dueDay"),
                renewedMs = optLongOrNull(s, "renewedMs"),
                active = s.optBoolean("active", true),
                reminders = if (s.isNull("reminders")) null else s.optJSONArray("reminders").ints(),
                notes = s.optString("notes"),
                armedMs = s.optLong("armedMs", created),
                createdMs = created,
            )
        }
        val payments = o.optJSONArray("payments").objects().map { p ->
            Payment(
                id = p.getString("id"),
                projectId = optStringOrNull(p, "projectId"),
                serviceId = optStringOrNull(p, "serviceId"),
                periodDay = optLongOrNull(p, "periodDay"),
                amount = p.optLong("amount", 0L),
                day = p.getLong("day"),
                note = p.optString("note"),
                createdMs = p.optLong("createdMs", 0L),
            )
        }
        val st = o.optJSONObject("settings")
        val settings = if (st == null) Settings() else Settings(
            deliveryReminders = if (st.has("deliveryReminders")) st.optJSONArray("deliveryReminders").ints() else Reminders.DEFAULT_DELIVERY,
            chargeReminders = if (st.has("chargeReminders")) st.optJSONArray("chargeReminders").ints() else Reminders.DEFAULT_CHARGE,
            serviceReminders = if (st.has("serviceReminders")) st.optJSONArray("serviceReminders").ints() else Reminders.DEFAULT_SERVICE,
            notifyHour = st.optInt("notifyHour", 9).coerceIn(0, 23),
            signature = st.optString("signature"),
        )
        return AppData(
            clients = clients,
            projects = projects,
            services = services,
            payments = payments,
            settings = settings,
            lastCheckMs = o.optLong("lastCheckMs", 0L),
        )
    }

    /** Un respaldo válido de esta app (para no importar cualquier JSON). */
    fun looksLikeBackup(text: String): Boolean = try {
        JSONObject(text).optString("app") == APP_TAG
    } catch (_: Exception) {
        false
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String, fallback: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: fallback

    private fun optLongOrNull(o: JSONObject, key: String): Long? = if (!o.has(key) || o.isNull(key)) null else o.optLong(key)

    private fun optStringOrNull(o: JSONObject, key: String): String? =
        if (!o.has(key) || o.isNull(key)) null else o.optString(key).ifEmpty { null }

    private fun ints(list: List<Int>) = JSONArray().apply { list.forEach { put(it) } }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

    private fun JSONArray?.ints(): List<Int> =
        if (this == null) emptyList() else (0 until length()).map { optInt(it, -1) }.filter { it >= 0 }
}
