package com.safelink.app.security

import com.safelink.app.model.RiskLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UrlAnalyzerTest {
    private val analyzer = UrlAnalyzer()

    @Test
    fun whatsappPhishingLinkIsDangerous() {
        val result = analyzer.analyze("http://login-banco-premio-verificar-senha.com")

        assertEquals(RiskLevel.Dangerous, result.level)
        assertTrue(result.reasons.any { it.contains("HTTPS", ignoreCase = true) })
        assertTrue(result.reasons.any { it.contains("golpes", ignoreCase = true) })
    }

    @Test
    fun instagramLookalikeLinkIsSuspiciousOrDangerous() {
        val result = analyzer.analyze("https://instagram-premio-login.example.com/verificar")

        assertTrue(result.level == RiskLevel.Suspicious || result.level == RiskLevel.Dangerous)
        assertTrue(result.reasons.isNotEmpty())
    }

    @Test
    fun tiktokShortenerLinkIsSuspicious() {
        val result = analyzer.analyze("https://bit.ly/presente-pix")

        assertEquals(RiskLevel.Suspicious, result.level)
        assertTrue(result.reasons.any { it.contains("encurtador", ignoreCase = true) })
    }

    @Test
    fun emailAccountThreatLinkIsDangerous() {
        val result = analyzer.analyze("http://seguranca-paypal-confirmar.example.com/login")

        assertEquals(RiskLevel.Dangerous, result.level)
    }

    @Test
    fun smsDeliveryScamLinkIsDangerous() {
        val result = analyzer.analyze("http://correios-taxas-urgente.example.com")

        assertEquals(RiskLevel.Dangerous, result.level)
    }

    @Test
    fun knownDocumentationLinkIsSafe() {
        val result = analyzer.analyze("https://developer.android.com/privacy-and-security")

        assertEquals(RiskLevel.Safe, result.level)
    }
}
