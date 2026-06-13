package com.safelink.app.data

import android.content.Context
import com.safelink.app.model.AnalysisResult
import com.safelink.app.model.RiskLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

class HistoryRepository(context: Context) {
    private val prefs = context.getSharedPreferences("safelink_private", Context.MODE_PRIVATE)
    private val _items = MutableStateFlow(load())
    val items: StateFlow<List<AnalysisResult>> = _items
    private val _terminalEntries = MutableStateFlow(loadTerminalEntries())
    val terminalEntries: StateFlow<List<TerminalEntry>> = _terminalEntries

    fun save(result: AnalysisResult) {
        val normalizedUrl = result.url.normalizeUrl()
        val existing = _items.value.firstOrNull { it.url.normalizeUrl() == normalizedUrl }
        val merged = if (existing == null) {
            val events = result.eventTimes.normalizedEvents(result.checkedAt, result.occurrences)
            result.copy(occurrences = events.size.coerceAtLeast(1), eventTimes = events)
        } else {
            val events = (existing.eventTimes.normalizedEvents(existing.checkedAt, existing.occurrences) +
                result.eventTimes.normalizedEvents(result.checkedAt, result.occurrences))
                .takeLast(MAX_EVENT_TIMES)
            result.copy(
                occurrences = events.size.coerceAtLeast(1),
                eventTimes = events,
            )
        }
        val next = (listOf(merged) + _items.value.filterNot { it.url.normalizeUrl() == normalizedUrl }).take(30)
        prefs.edit().putString("history", encode(next)).apply()
        _items.value = next
    }

    fun delete(url: String) {
        val normalizedUrl = url.normalizeUrl()
        val next = _items.value.filterNot { it.url.normalizeUrl() == normalizedUrl }
        prefs.edit().putString("history", encode(next)).apply()
        _items.value = next
    }

    fun updateDecision(url: String, decision: String) {
        val normalizedUrl = url.normalizeUrl()
        val next = _items.value.map {
            if (it.url.normalizeUrl() == normalizedUrl) it.copy(decision = decision) else it
        }
        prefs.edit().putString("history", encode(next)).apply()
        _items.value = next
    }

    fun addTerminalDomain(domain: String, status: String) {
        val normalized = domain.normalizeDomain()
        if (normalized.isBlank()) return
        val entry = TerminalEntry(
            type = TerminalEntry.TYPE_DOMAIN,
            value = normalized,
            status = status,
            checkedAt = System.currentTimeMillis(),
        )
        saveTerminalEntry(entry)
    }

    fun addTerminalAnalysis(result: AnalysisResult) {
        val entry = TerminalEntry(
            type = TerminalEntry.TYPE_ANALYSIS,
            value = result.url,
            status = result.level.label,
            host = result.host,
            score = result.score,
            reasons = result.reasons,
            checkedAt = result.checkedAt,
        )
        saveTerminalEntry(entry)
    }

    fun updateTerminalDomain(oldValue: String, newValue: String, status: String) {
        val oldNormalized = oldValue.normalizeDomain()
        val newNormalized = newValue.normalizeDomain()
        val next = _terminalEntries.value.map {
            if (it.type == TerminalEntry.TYPE_DOMAIN && it.value == oldNormalized && it.status == status) {
                it.copy(value = newNormalized, checkedAt = System.currentTimeMillis())
            } else {
                it
            }
        }
        persistTerminalEntries(next)
    }

    fun deleteTerminalEntry(value: String, status: String) {
        val normalized = value.normalizeDomain()
        val next = _terminalEntries.value.filterNot {
            it.type == TerminalEntry.TYPE_DOMAIN && it.value == normalized && it.status == status
        }
        persistTerminalEntries(next)
    }

    fun exportText(): String {
        if (_items.value.isEmpty()) return "SafeLink: nenhum link analisado."
        return buildString {
            appendLine("Historico SafeLink")
            _items.value.forEach { item ->
                appendLine("${item.level.label} | risco ${item.score}/100 | ${item.decision} | ${item.host} | ${item.url}")
                item.reasons.take(3).forEach { appendLine("- $it") }
                appendLine()
            }
        }
    }

    fun setProtectionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("protection_enabled", enabled).apply()
    }

    fun isProtectionEnabled(): Boolean = prefs.getBoolean("protection_enabled", false)

    fun setLayerEnabled(key: String, enabled: Boolean) {
        prefs.edit().putBoolean(key, enabled).apply()
    }

    fun isLayerEnabled(key: String): Boolean = prefs.getBoolean(key, false)

    fun setBoolean(key: String, enabled: Boolean) {
        prefs.edit().putBoolean(key, enabled).apply()
    }

    fun getBoolean(key: String): Boolean = prefs.getBoolean(key, false)

    fun setString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun getString(key: String): String = prefs.getString(key, "").orEmpty()

    fun trustDomain(domain: String) {
        updateSet(TRUSTED_DOMAINS_KEY) { it + domain.normalizeDomain() }
        updateSet(BLOCKED_DOMAINS_KEY) { it - domain.normalizeDomain() }
        clearTimedDomain(domain)
    }

    fun blockDomain(domain: String) {
        updateSet(BLOCKED_DOMAINS_KEY) { it + domain.normalizeDomain() }
        updateSet(TRUSTED_DOMAINS_KEY) { it - domain.normalizeDomain() }
        clearTimedDomain(domain)
    }

    fun trustDomainUntil(domain: String, expiresAt: Long) {
        val normalized = domain.normalizeDomain()
        if (normalized.isBlank() || normalized == UNKNOWN_DOMAIN) return
        prefs.edit()
            .putLong("$TEMP_TRUST_PREFIX$normalized", expiresAt)
            .remove("$TEMP_BLOCK_PREFIX$normalized")
            .apply()
        updateSet(BLOCKED_DOMAINS_KEY) { it - normalized }
    }

    fun blockDomainUntil(domain: String, expiresAt: Long) {
        val normalized = domain.normalizeDomain()
        if (normalized.isBlank() || normalized == UNKNOWN_DOMAIN) return
        prefs.edit()
            .putLong("$TEMP_BLOCK_PREFIX$normalized", expiresAt)
            .remove("$TEMP_TRUST_PREFIX$normalized")
            .apply()
        updateSet(TRUSTED_DOMAINS_KEY) { it - normalized }
    }

    fun removeDomainDecision(domain: String) {
        val normalized = domain.normalizeDomain()
        updateSet(TRUSTED_DOMAINS_KEY) { it - normalized }
        updateSet(BLOCKED_DOMAINS_KEY) { it - normalized }
        prefs.edit()
            .remove("$TEMP_TRUST_PREFIX$normalized")
            .remove("$TEMP_BLOCK_PREFIX$normalized")
            .apply()
    }

    fun isTrusted(domain: String): Boolean {
        val normalized = domain.normalizeDomain()
        return normalized in readSet(TRUSTED_DOMAINS_KEY) || timedDecisionActive(TEMP_TRUST_PREFIX, normalized)
    }

    fun isBlocked(domain: String): Boolean {
        val normalized = domain.normalizeDomain()
        return normalized in readSet(BLOCKED_DOMAINS_KEY) || timedDecisionActive(TEMP_BLOCK_PREFIX, normalized)
    }

    fun trustedDomains(): Set<String> = readSet(TRUSTED_DOMAINS_KEY)

    fun blockedDomains(): Set<String> = readSet(BLOCKED_DOMAINS_KEY)

    fun timedTrustDomains(): Map<String, Long> = timedDomains(TEMP_TRUST_PREFIX)

    fun timedBlockDomains(): Map<String, Long> = timedDomains(TEMP_BLOCK_PREFIX)

    fun addBlockedEvent(domain: String) {
        val next = (listOf("${System.currentTimeMillis()}|${domain.normalizeDomain()}") + blockedEvents()).take(20)
        val array = JSONArray()
        next.forEach { array.put(it) }
        prefs.edit().putString(BLOCKED_EVENTS_KEY, array.toString()).apply()
    }

    fun blockedEvents(): List<String> = readRawList(BLOCKED_EVENTS_KEY)

    fun exportBackup(): String {
        return JSONObject()
            .put("format", "SafeLinkBackup")
            .put("version", 1)
            .put("trusted", JSONArray(trustedDomains()))
            .put("blocked", JSONArray(blockedDomains()))
            .put("strict", getBoolean(MODE_STRICT))
            .put("silent", getBoolean(MODE_SILENT))
            .put("blockShorteners", getBoolean(BLOCK_SHORTENERS))
            .put("blockNoHttps", getBoolean(BLOCK_NO_HTTPS))
            .put("blockIp", getBoolean(BLOCK_IP_DOMAINS))
            .toString()
    }

    fun importBackup(raw: String): Boolean {
        return runCatching {
            val json = JSONObject(raw)
            if (json.optString("format") != "SafeLinkBackup") return false
            replaceSet(TRUSTED_DOMAINS_KEY, json.optJSONArray("trusted"))
            replaceSet(BLOCKED_DOMAINS_KEY, json.optJSONArray("blocked"))
            setBoolean(MODE_STRICT, json.optBoolean("strict", false))
            setBoolean(MODE_SILENT, json.optBoolean("silent", false))
            setBoolean(BLOCK_SHORTENERS, json.optBoolean("blockShorteners", false))
            setBoolean(BLOCK_NO_HTTPS, json.optBoolean("blockNoHttps", false))
            setBoolean(BLOCK_IP_DOMAINS, json.optBoolean("blockIp", false))
            true
        }.getOrDefault(false)
    }

    fun allowDomain(domain: String, durationMs: Long = TEMPORARY_DECISION_MS) {
        if (domain.isBlank() || domain == UNKNOWN_DOMAIN) return
        prefs.edit().putLong("allow_domain_$domain", System.currentTimeMillis() + durationMs).apply()
    }

    fun markDomainResolved(domain: String, durationMs: Long = RESOLVED_DECISION_MS) {
        val normalized = domain.normalizeDomain()
        if (normalized.isBlank() || normalized == UNKNOWN_DOMAIN) return
        prefs.edit().putLong("$RESOLVED_DOMAIN_PREFIX$normalized", System.currentTimeMillis() + durationMs).apply()
    }

    fun markUrlResolved(url: String, durationMs: Long = RESOLVED_DECISION_MS) {
        val normalized = url.normalizeUrl()
        if (normalized.isBlank()) return
        prefs.edit().putLong("$RESOLVED_URL_PREFIX${normalized.hashCode()}", System.currentTimeMillis() + durationMs).apply()
    }

    fun isDomainResolved(domain: String): Boolean {
        val normalized = domain.normalizeDomain()
        if (normalized.isBlank() || normalized == UNKNOWN_DOMAIN) return false
        return resolvedDecisionActive("$RESOLVED_DOMAIN_PREFIX$normalized")
    }

    fun isUrlResolved(url: String): Boolean {
        val normalized = url.normalizeUrl()
        if (normalized.isBlank()) return false
        return resolvedDecisionActive("$RESOLVED_URL_PREFIX${normalized.hashCode()}")
    }

    private fun load(): List<AnalysisResult> {
        val raw = prefs.getString("history", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                AnalysisResult(
                    url = item.getString("url"),
                    host = item.getString("host"),
                    level = RiskLevel.valueOf(item.getString("level")),
                    score = item.optInt("score", 0),
                    decision = item.optString("decision", "Analisado"),
                    reasons = item.getJSONArray("reasons").let { reasons ->
                        List(reasons.length()) { reasonIndex -> reasons.getString(reasonIndex) }
                    },
                    checkedAt = item.getLong("checkedAt"),
                    occurrences = item.optInt("occurrences", 1).coerceAtLeast(1),
                    eventTimes = item.optJSONArray("eventTimes")?.let { events ->
                        List(events.length()) { eventIndex -> events.optLong(eventIndex) }
                            .filter { it > 0L }
                    }.orEmpty().ifEmpty {
                        List(item.optInt("occurrences", 1).coerceAtLeast(1)) { item.getLong("checkedAt") }
                    },
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encode(items: List<AnalysisResult>): String {
        val array = JSONArray()
        items.forEach { result ->
            array.put(
                JSONObject()
                    .put("url", result.url)
                    .put("host", result.host)
                    .put("level", result.level.name)
                    .put("score", result.score)
                    .put("decision", result.decision)
                    .put("checkedAt", result.checkedAt)
                    .put("occurrences", result.occurrences.coerceAtLeast(1))
                    .put("eventTimes", JSONArray(result.eventTimes.normalizedEvents(result.checkedAt, result.occurrences)))
                    .put("reasons", JSONArray(result.reasons)),
            )
        }
        return array.toString()
    }

    private fun saveTerminalEntry(entry: TerminalEntry) {
        val next = (listOf(entry) + _terminalEntries.value).take(60)
        persistTerminalEntries(next)
    }

    private fun persistTerminalEntries(entries: List<TerminalEntry>) {
        prefs.edit().putString(TERMINAL_ENTRIES_KEY, encodeTerminalEntries(entries)).apply()
        _terminalEntries.value = entries
    }

    private fun loadTerminalEntries(): List<TerminalEntry> {
        val raw = prefs.getString(TERMINAL_ENTRIES_KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { index ->
                val item = array.getJSONObject(index)
                TerminalEntry(
                    type = item.optString("type", TerminalEntry.TYPE_DOMAIN),
                    value = item.optString("value"),
                    status = item.optString("status"),
                    host = item.optString("host"),
                    score = item.optInt("score", 0),
                    reasons = item.optJSONArray("reasons")?.let { reasons ->
                        List(reasons.length()) { reasonIndex -> reasons.optString(reasonIndex) }
                    }.orEmpty(),
                    checkedAt = item.optLong("checkedAt", 0L),
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeTerminalEntries(entries: List<TerminalEntry>): String {
        val array = JSONArray()
        entries.forEach { entry ->
            array.put(
                JSONObject()
                    .put("type", entry.type)
                    .put("value", entry.value)
                    .put("status", entry.status)
                    .put("host", entry.host)
                    .put("score", entry.score)
                    .put("reasons", JSONArray(entry.reasons))
                    .put("checkedAt", entry.checkedAt),
            )
        }
        return array.toString()
    }

    private fun readSet(key: String): Set<String> {
        return readRawList(key).toSet()
    }

    private fun readRawList(key: String): List<String> {
        val raw = prefs.getString(key, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            List(array.length()) { array.getString(it) }.filter { it.isNotBlank() }
        }.getOrDefault(emptyList())
    }

    private fun updateSet(key: String, transform: (Set<String>) -> Set<String>) {
        val array = JSONArray()
        transform(readSet(key)).sorted().forEach { array.put(it) }
        prefs.edit().putString(key, array.toString()).apply()
    }

    private fun clearTimedDomain(domain: String) {
        val normalized = domain.normalizeDomain()
        prefs.edit()
            .remove("$TEMP_TRUST_PREFIX$normalized")
            .remove("$TEMP_BLOCK_PREFIX$normalized")
            .apply()
    }

    private fun timedDecisionActive(prefix: String, domain: String): Boolean {
        val expiresAt = prefs.getLong("$prefix$domain", 0L)
        if (expiresAt <= 0L) return false
        if (expiresAt < System.currentTimeMillis()) {
            prefs.edit().remove("$prefix$domain").apply()
            return false
        }
        return true
    }

    private fun resolvedDecisionActive(key: String): Boolean {
        val expiresAt = prefs.getLong(key, 0L)
        if (expiresAt <= 0L) return false
        if (expiresAt < System.currentTimeMillis()) {
            prefs.edit().remove(key).apply()
            return false
        }
        return true
    }

    private fun timedDomains(prefix: String): Map<String, Long> {
        val now = System.currentTimeMillis()
        val editor = prefs.edit()
        val items = prefs.all.mapNotNull { (key, value) ->
            if (!key.startsWith(prefix) || value !is Long) return@mapNotNull null
            if (value <= now) {
                editor.remove(key)
                null
            } else {
                key.removePrefix(prefix) to value
            }
        }.toMap()
        editor.apply()
        return items
    }

    private fun replaceSet(key: String, array: JSONArray?) {
        val values = if (array == null) {
            emptySet()
        } else {
            List(array.length()) { index -> array.optString(index).normalizeDomain() }
                .filter { it.isNotBlank() }
                .toSet()
        }
        updateSet(key) { values }
    }

    private fun String.normalizeDomain(): String = trim().lowercase().removePrefix("www.")

    private fun String.normalizeUrl(): String = trim().lowercase().removeSuffix("/")

    private fun List<Long>.normalizedEvents(fallbackTime: Long, fallbackCount: Int): List<Long> {
        val cleaned = filter { it > 0L }
        return cleaned.ifEmpty { List(fallbackCount.coerceAtLeast(1)) { fallbackTime } }
            .takeLast(MAX_EVENT_TIMES)
    }

    companion object {
        const val LAYER_PROTECTION_VISIBLE = "layer_protection_visible"
        const val LAYER_BROWSER = "layer_browser"
        const val LAYER_ACCESSIBILITY = "layer_accessibility"
        const val LAYER_VPN = "layer_vpn"
        const val MODE_STRICT = "mode_strict"
        const val MODE_SILENT = "mode_silent"
        const val BLOCK_SHORTENERS = "block_shorteners"
        const val BLOCK_NO_HTTPS = "block_no_https"
        const val BLOCK_IP_DOMAINS = "block_ip_domains"
        private const val TRUSTED_DOMAINS_KEY = "trusted_domains"
        private const val BLOCKED_DOMAINS_KEY = "blocked_domains"
        private const val BLOCKED_EVENTS_KEY = "blocked_events"
        private const val TERMINAL_ENTRIES_KEY = "terminal_entries"
        private const val TEMP_TRUST_PREFIX = "temp_trust_"
        private const val TEMP_BLOCK_PREFIX = "temp_block_"
        private const val RESOLVED_DOMAIN_PREFIX = "resolved_domain_"
        private const val RESOLVED_URL_PREFIX = "resolved_url_"
        private const val TEMPORARY_DECISION_MS = 120_000L
        private const val RESOLVED_DECISION_MS = 24L * 60L * 60L * 1000L
        private const val MAX_EVENT_TIMES = 200
        private const val UNKNOWN_DOMAIN = "dominio nao identificado"
    }
}

data class TerminalEntry(
    val type: String,
    val value: String,
    val status: String,
    val host: String = "",
    val score: Int = 0,
    val reasons: List<String> = emptyList(),
    val checkedAt: Long = 0L,
) {
    companion object {
        const val TYPE_DOMAIN = "domain"
        const val TYPE_ANALYSIS = "analysis"
    }
}
