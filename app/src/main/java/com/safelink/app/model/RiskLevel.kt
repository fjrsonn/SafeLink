package com.safelink.app.model

enum class RiskLevel(val label: String, val score: Int) {
    Safe("Seguro", 18),
    Suspicious("Suspeito", 58),
    Dangerous("Perigoso", 88),
}
