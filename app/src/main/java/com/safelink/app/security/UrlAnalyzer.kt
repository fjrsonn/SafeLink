package com.safelink.app.security

import android.net.Uri
import com.safelink.app.model.AnalysisResult
import com.safelink.app.model.RiskLevel
import java.net.IDN
import java.util.Locale

class UrlAnalyzer {
    private val shorteners = setOf(
        "bit.ly",
        "tinyurl.com",
        "t.co",
        "goo.gl",
        "ow.ly",
        "is.gd",
        "cutt.ly",
        "rebrand.ly",
        "s.id",
    )

    private val sensitiveBrands = setOf(
        "banco",
        "bank",
        "pix",
        "gov",
        "receita",
        "correios",
        "whatsapp",
        "instagram",
        "facebook",
        "paypal",
    )

    private val suspiciousWords = setOf(
        "login",
        "verify",
        "verificar",
        "senha",
        "password",
        "bonus",
        "brinde",
        "premio",
        "urgente",
        "bloqueado",
        "seguranca",
        "atualizar",
        "confirmar",
        "pix",
    )

    fun analyze(rawUrl: String): AnalysisResult {
        val normalized = normalize(rawUrl)
        val uri = Uri.parse(normalized)
        val host = uri.host.orEmpty().lowercase(Locale.ROOT)
        val asciiHost = runCatching { IDN.toASCII(host) }.getOrDefault(host)
        val reasons = mutableListOf<String>()
        var score = 0

        if (uri.scheme != "https") {
            score += 28
            reasons += "[Seguranca] O link nao usa HTTPS."
        }

        if (host in shorteners) {
            score += 26
            reasons += "[Redirecionamento] O dominio e um encurtador e pode esconder o destino real."
        }

        if (asciiHost != host) {
            score += 24
            reasons += "[Dominio] O dominio usa caracteres internacionais que podem imitar sites conhecidos."
        }

        if (asciiHost.contains("xn--")) {
            score += 32
            reasons += "[Dominio] O dominio usa punycode, tecnica comum em ataques de homografo."
        }

        if (hasMixedScriptRisk(host)) {
            score += 26
            reasons += "[Dominio] O dominio mistura alfabetos parecidos, o que pode camuflar letras reais."
        }

        if (host.count { it == '-' } >= 3 || host.length > 45) {
            score += 15
            reasons += "[Dominio] O dominio tem formato incomum."
        }

        val allText = "$host ${uri.path.orEmpty()} ${uri.query.orEmpty()}".lowercase(Locale.ROOT)
        val matchedWords = suspiciousWords.filter { allText.contains(it) }
        if (matchedWords.isNotEmpty()) {
            score += 9 + matchedWords.size * 4
            reasons += "[Conteudo] Foram encontrados termos comuns em golpes: ${matchedWords.take(3).joinToString()}."
        }

        if (sensitiveBrands.any { allText.contains(it) } && host.split(".").size > 3) {
            score += 18
            reasons += "[Dominio] O link combina marca sensivel com subdominios, padrao comum em phishing."
        }

        if (sensitiveBrands.any { host.contains(it) }) {
            score += 12
            reasons += "[Conteudo] O dominio cita marca ou servico sensivel."
        }

        detectLookalikeBrand(host)?.let { brand ->
            score += 30
            reasons += "[Dominio] O dominio parece imitar a marca $brand."
        }

        if (Regex("""\b\d{1,3}(\.\d{1,3}){3}\b""").containsMatchIn(host)) {
            score += 24
            reasons += "[Dominio] O endereco usa IP direto em vez de dominio legivel."
        }

        if (reasons.isEmpty()) {
            reasons += "[Seguranca] Nenhum sinal forte de risco foi encontrado na analise local."
        }

        val boundedScore = score.coerceIn(0, 100)
        val level = when {
            boundedScore >= 50 -> RiskLevel.Dangerous
            boundedScore >= 25 -> RiskLevel.Suspicious
            else -> RiskLevel.Safe
        }

        return AnalysisResult(
            url = normalized,
            host = host.ifBlank { "dominio nao identificado" },
            level = level,
            reasons = reasons,
            score = boundedScore,
        )
    }

    private fun detectLookalikeBrand(host: String): String? {
        val skeleton = host
            .replace("0", "o")
            .replace("1", "l")
            .replace("3", "e")
            .replace("4", "a")
            .replace("5", "s")
            .replace("7", "t")
            .replace("!", "i")
            .replace("@", "a")
            .replace("$", "s")
            .replace("ı", "i")
            .replace("ⅼ", "l")
            .replace("ӏ", "l")
            .replace("а", "a")
            .replace("е", "e")
            .replace("о", "o")
            .replace("р", "p")
            .replace("с", "c")
            .replace("х", "x")
            .replace("у", "y")
        return sensitiveBrands.firstOrNull { brand ->
            skeleton.contains(brand) && !host.contains(brand)
        }
    }

    private fun hasMixedScriptRisk(host: String): Boolean {
        val hasLatin = host.any { it.code in 0x0041..0x007A }
        val hasCyrillic = host.any { it.code in 0x0400..0x052F }
        val hasGreek = host.any { it.code in 0x0370..0x03FF }
        return hasLatin && (hasCyrillic || hasGreek)
    }

    private fun normalize(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }
}
