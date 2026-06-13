package com.safelink.app.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.safelink.app.LinkReviewActivity

class SafeLinkAccessibilityService : AccessibilityService() {
    private var lastUrl: String? = null
    private var lastOpenedAt: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isProtectionEnabled()) return
        val sourcePackage = event?.packageName?.toString().orEmpty()
        if (sourcePackage == packageName) return
        if (event?.eventType != AccessibilityEvent.TYPE_VIEW_CLICKED &&
            event?.eventType != AccessibilityEvent.TYPE_VIEW_LONG_CLICKED
        ) {
            return
        }

        val eventText = event?.text.orEmpty().joinToString(" ")
        val contentText = event?.contentDescription?.toString().orEmpty()
        val nodeText = event?.source?.let { source ->
            try {
                collectText(source)
            } finally {
                source.recycle()
            }
        }.orEmpty()
        val url = findUrl("$eventText $contentText $nodeText") ?: return
        openReview(url)
    }

    override fun onInterrupt() = Unit

    private fun isProtectionEnabled(): Boolean {
        return getSharedPreferences("safelink_private", MODE_PRIVATE)
            .getBoolean("protection_enabled", false)
    }

    private fun openReview(url: String) {
        val now = System.currentTimeMillis()
        if (url == lastUrl && now - lastOpenedAt < DEBOUNCE_MS) return
        val host = Uri.parse(url).host.orEmpty().lowercase()
        if (isRecentlyResolved(host, url)) return
        lastUrl = url
        lastOpenedAt = now

        val intent = Intent(this, LinkReviewActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(url)
            putExtra(LinkReviewActivity.EXTRA_FROM_ACCESSIBILITY, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    private fun isRecentlyResolved(host: String, url: String): Boolean {
        val prefs = getSharedPreferences("safelink_private", MODE_PRIVATE)
        val normalizedHost = host.lowercase().removePrefix("www.")
        val domainExpiresAt = if (normalizedHost.isBlank()) {
            0L
        } else {
            prefs.getLong("resolved_domain_$normalizedHost", 0L)
        }
        val normalizedUrl = url.trim().lowercase().removeSuffix("/")
        val urlExpiresAt = prefs.getLong("resolved_url_${normalizedUrl.hashCode()}", 0L)
        val now = System.currentTimeMillis()
        return domainExpiresAt > now || urlExpiresAt > now
    }

    private fun collectText(node: AccessibilityNodeInfo, depth: Int = 0): String {
        if (depth > MAX_NODE_DEPTH) return ""
        val values = buildList {
            node.text?.toString()?.let(::add)
            node.contentDescription?.toString()?.let(::add)
        }
        val children = (0 until node.childCount).joinToString(" ") { index ->
            node.getChild(index)?.let { child ->
                try {
                    collectText(child, depth + 1)
                } finally {
                    child.recycle()
                }
            }.orEmpty()
        }
        return (values + children).joinToString(" ")
    }

    private fun findUrl(text: String): String? {
        return URL_REGEX.find(text)?.value
            ?.trimEnd('.', ',', ';', ')', ']', '}', '"', '\'')
            ?.let { value ->
                if (value.startsWith("www.")) "https://$value" else value
            }
    }

    companion object {
        private const val DEBOUNCE_MS = 15_000L
        private const val MAX_NODE_DEPTH = 8
        private val URL_REGEX = Regex("""https?://[^\s<>"']+|www\.[^\s<>"']+""")
    }
}
