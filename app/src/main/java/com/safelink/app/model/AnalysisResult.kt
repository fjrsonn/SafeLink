package com.safelink.app.model

data class AnalysisResult(
    val url: String,
    val host: String,
    val level: RiskLevel,
    val reasons: List<String>,
    val score: Int = 0,
    val decision: String = "Analisado",
    val checkedAt: Long = System.currentTimeMillis(),
    val occurrences: Int = 1,
    val eventTimes: List<Long> = listOf(checkedAt),
)
