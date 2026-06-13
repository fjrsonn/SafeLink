package com.safelink.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.safelink.app.LinkReviewActivity
import com.safelink.app.R
import com.safelink.app.data.HistoryRepository
import com.safelink.app.model.RiskLevel
import com.safelink.app.security.UrlAnalyzer
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.concurrent.thread

class SafeLinkVpnService : VpnService() {
    private var vpnInterface: ParcelFileDescriptor? = null
    @Volatile private var running = false
    private val analyzer = UrlAnalyzer()
    private var lastReviewedDomain: String? = null
    private var lastReviewedAt: Long = 0L

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopVpn()
            else -> startVpn()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }

    private fun startVpn() {
        if (running) return
        vpnInterface = Builder()
            .setSession("SafeLink Local Guard")
            .addAddress(CLIENT_ADDRESS, 32)
            .addDnsServer(DNS_ADDRESS)
            .addRoute(DNS_ADDRESS, 32)
            .allowFamily(android.system.OsConstants.AF_INET)
            .establish()

        running = vpnInterface != null
        if (running) {
            getSharedPreferences("safelink_private", MODE_PRIVATE)
                .edit()
                .putBoolean("layer_vpn", true)
                .apply()
            thread(name = "SafeLinkDnsGuard", isDaemon = true) {
                processDnsPackets()
            }
        }
    }

    private fun stopVpn() {
        running = false
        runCatching { vpnInterface?.close() }
        vpnInterface = null
        getSharedPreferences("safelink_private", MODE_PRIVATE)
            .edit()
            .putBoolean("layer_vpn", false)
            .apply()
        stopSelf()
    }

    private fun processDnsPackets() {
        val descriptor = vpnInterface?.fileDescriptor ?: return
        val input = FileInputStream(descriptor)
        val output = FileOutputStream(descriptor)
        val packet = ByteArray(MAX_PACKET_SIZE)

        while (running) {
            val length = runCatching { input.read(packet) }.getOrDefault(-1)
            if (length <= 0) continue
            val response = handlePacket(packet, length) ?: continue
            runCatching { output.write(response) }
        }
    }

    private fun handlePacket(packet: ByteArray, length: Int): ByteArray? {
        if (length < IPV4_HEADER_MIN + UDP_HEADER_SIZE + DNS_HEADER_SIZE) return null
        val version = (packet[0].toInt() ushr 4) and 0xF
        if (version != 4) return null
        val ipHeaderLength = (packet[0].toInt() and 0xF) * 4
        if (ipHeaderLength < IPV4_HEADER_MIN || length < ipHeaderLength + UDP_HEADER_SIZE + DNS_HEADER_SIZE) return null
        val protocol = packet[9].toInt() and 0xFF
        if (protocol != UDP_PROTOCOL) return null

        val udpOffset = ipHeaderLength
        val destinationPort = readUShort(packet, udpOffset + 2)
        if (destinationPort != DNS_PORT) return null

        val dnsOffset = udpOffset + UDP_HEADER_SIZE
        val dnsPayload = packet.copyOfRange(dnsOffset, length)
        val query = parseDnsQuery(dnsPayload) ?: return null
        val allowed = isTemporarilyAllowed(query.domain)
        val trusted = isInDomainSet("trusted_domains", query.domain)
        val blockedByUser = isInDomainSet("blocked_domains", query.domain)
        val blockedByPolicy = isBlockedByDnsPolicy(query.domain)
        val isBlocked = !trusted && (blockedByUser || blockedByPolicy || (!allowed && analyzer.analyze("https://${query.domain}").level != RiskLevel.Safe))
        if (isBlocked && !isRecentlyResolved(query.domain) && !isSilentMode()) {
            HistoryRepository(this).addBlockedEvent(query.domain)
            notifyBlockedDomain(query.domain)
        }
        val dnsResponse = if (isBlocked) {
            buildFailureDnsResponse(dnsPayload, query, NXDOMAIN_FLAGS)
        } else {
            resolveDnsUpstream(dnsPayload) ?: buildFailureDnsResponse(dnsPayload, query, SERVFAIL_FLAGS)
        }
        return buildUdpIpv4Response(packet, ipHeaderLength, udpOffset, dnsResponse)
    }

    private fun parseDnsQuery(dns: ByteArray): DnsQuery? {
        if (dns.size < DNS_HEADER_SIZE) return null
        val questionCount = readUShort(dns, 4)
        if (questionCount < 1) return null

        var offset = DNS_HEADER_SIZE
        val labels = mutableListOf<String>()
        while (offset < dns.size) {
            val labelLength = dns[offset].toInt() and 0xFF
            offset += 1
            if (labelLength == 0) break
            if (offset + labelLength > dns.size) return null
            labels += dns.copyOfRange(offset, offset + labelLength).toString(Charsets.UTF_8)
            offset += labelLength
        }
        if (labels.isEmpty() || offset + 4 > dns.size) return null
        val type = readUShort(dns, offset)
        val dnsClass = readUShort(dns, offset + 2)
        val questionEnd = offset + 4
        return DnsQuery(labels.joinToString(".").lowercase(), type, dnsClass, questionEnd)
    }

    private fun isTemporarilyAllowed(domain: String): Boolean {
        val prefs = getSharedPreferences("safelink_private", MODE_PRIVATE)
        val expiresAt = prefs.getLong("allow_domain_$domain", 0L)
        return expiresAt > System.currentTimeMillis()
    }

    private fun isRecentlyResolved(domain: String): Boolean {
        val normalized = domain.lowercase().removePrefix("www.")
        val prefs = getSharedPreferences("safelink_private", MODE_PRIVATE)
        val expiresAt = prefs.getLong("resolved_domain_$normalized", 0L)
        return expiresAt > System.currentTimeMillis()
    }

    private fun isSilentMode(): Boolean {
        return getSharedPreferences("safelink_private", MODE_PRIVATE)
            .getBoolean("mode_silent", false)
    }

    private fun isBlockedByDnsPolicy(domain: String): Boolean {
        val prefs = getSharedPreferences("safelink_private", MODE_PRIVATE)
        val normalized = domain.lowercase().removePrefix("www.")
        val shorteners = setOf("bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd", "cutt.ly", "rebrand.ly", "s.id")
        val directIp = Regex("""\b\d{1,3}(\.\d{1,3}){3}\b""").containsMatchIn(normalized)
        return (prefs.getBoolean("block_shorteners", false) && normalized in shorteners) ||
            (prefs.getBoolean("block_ip_domains", false) && directIp)
    }

    private fun isInDomainSet(key: String, domain: String): Boolean {
        val raw = getSharedPreferences("safelink_private", MODE_PRIVATE).getString(key, null) ?: return false
        return raw.contains("\"${domain.lowercase().removePrefix("www.")}\"")
    }

    private fun notifyBlockedDomain(domain: String) {
        val now = System.currentTimeMillis()
        if (domain == lastReviewedDomain && now - lastReviewedAt < REVIEW_DEBOUNCE_MS) return
        lastReviewedDomain = domain
        lastReviewedAt = now

        ensureAlertChannel()
        val reviewIntent = Intent(this, LinkReviewActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = android.net.Uri.parse("https://$domain")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            domain.hashCode(),
            reviewIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Link bloqueado pelo SafeLink")
            .setContentText("Toque para analisar $domain antes de continuar.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("O SafeLink bloqueou $domain antes do carregamento. Toque para ver a análise e decidir se deseja continuar."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        getSystemService(NotificationManager::class.java)
            .notify(BLOCK_NOTIFICATION_BASE_ID + (domain.hashCode() and 0x0FFF), notification)
    }

    private fun ensureAlertChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Alertas de links SafeLink",
                NotificationManager.IMPORTANCE_HIGH,
            )
            channel.description = "Alertas quando a VPN local bloqueia um dominio suspeito."
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun resolveDnsUpstream(request: ByteArray): ByteArray? {
        for (dnsServer in UPSTREAM_DNS_SERVERS) {
            val response = runCatching {
                DatagramSocket().use { socket ->
                    protect(socket)
                    socket.soTimeout = UPSTREAM_TIMEOUT_MS
                    val server = InetSocketAddress(dnsServer, DNS_PORT)
                    socket.send(DatagramPacket(request, request.size, server))
                    val buffer = ByteArray(MAX_DNS_RESPONSE_SIZE)
                    val packet = DatagramPacket(buffer, buffer.size)
                    socket.receive(packet)
                    buffer.copyOf(packet.length)
                }
            }.getOrNull()
            if (response != null) {
                return response
            }
        }
        return null
    }

    private fun buildFailureDnsResponse(request: ByteArray, query: DnsQuery, flags: Short): ByteArray {
        val response = ByteBuffer.allocate(512).order(ByteOrder.BIG_ENDIAN)
        response.put(request[0])
        response.put(request[1])
        response.putShort(flags)
        response.putShort(1)
        response.putShort(0)
        response.putShort(0)
        response.putShort(0)
        if (query.questionEnd > DNS_HEADER_SIZE) {
            response.put(request, DNS_HEADER_SIZE, query.questionEnd - DNS_HEADER_SIZE)
        }
        return response.array().copyOf(response.position())
    }

    private fun buildUdpIpv4Response(
        request: ByteArray,
        ipHeaderLength: Int,
        udpOffset: Int,
        dnsResponse: ByteArray,
    ): ByteArray {
        val totalLength = ipHeaderLength + UDP_HEADER_SIZE + dnsResponse.size
        val response = ByteArray(totalLength)
        response[0] = 0x45
        response[1] = 0
        writeUShort(response, 2, totalLength)
        writeUShort(response, 4, 0)
        writeUShort(response, 6, 0)
        response[8] = 64
        response[9] = UDP_PROTOCOL.toByte()
        System.arraycopy(request, 16, response, 12, 4)
        System.arraycopy(request, 12, response, 16, 4)
        writeUShort(response, 10, ipv4Checksum(response, 0, IPV4_HEADER_MIN))

        val responseUdpOffset = IPV4_HEADER_MIN
        val sourcePort = readUShort(request, udpOffset + 2)
        val destinationPort = readUShort(request, udpOffset)
        writeUShort(response, responseUdpOffset, sourcePort)
        writeUShort(response, responseUdpOffset + 2, destinationPort)
        writeUShort(response, responseUdpOffset + 4, UDP_HEADER_SIZE + dnsResponse.size)
        writeUShort(response, responseUdpOffset + 6, 0)
        System.arraycopy(dnsResponse, 0, response, responseUdpOffset + UDP_HEADER_SIZE, dnsResponse.size)
        return response
    }

    private fun readUShort(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
    }

    private fun writeUShort(data: ByteArray, offset: Int, value: Int) {
        data[offset] = ((value ushr 8) and 0xFF).toByte()
        data[offset + 1] = (value and 0xFF).toByte()
    }

    private fun ipv4Checksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var index = offset
        while (index < offset + length) {
            if (index != 10) {
                sum += readUShort(data, index)
            }
            index += 2
        }
        while ((sum ushr 16) != 0) {
            sum = (sum and 0xFFFF) + (sum ushr 16)
        }
        return sum.inv() and 0xFFFF
    }

    private data class DnsQuery(
        val domain: String,
        val type: Int,
        val dnsClass: Int,
        val questionEnd: Int,
    )

    companion object {
        const val ACTION_START = "com.safelink.app.START_VPN"
        const val ACTION_STOP = "com.safelink.app.STOP_VPN"
        private const val CLIENT_ADDRESS = "10.10.0.2"
        private const val DNS_ADDRESS = "10.10.0.1"
        private val UPSTREAM_DNS_SERVERS = listOf("1.1.1.1", "8.8.8.8", "9.9.9.9")
        private const val MAX_PACKET_SIZE = 32767
        private const val MAX_DNS_RESPONSE_SIZE = 4096
        private const val IPV4_HEADER_MIN = 20
        private const val UDP_HEADER_SIZE = 8
        private const val DNS_HEADER_SIZE = 12
        private const val UDP_PROTOCOL = 17
        private const val DNS_PORT = 53
        private const val UPSTREAM_TIMEOUT_MS = 2500
        private const val REVIEW_DEBOUNCE_MS = 10_000L
        private const val ALERT_CHANNEL_ID = "safelink_link_alerts"
        private const val BLOCK_NOTIFICATION_BASE_ID = 9000
        private const val NXDOMAIN_FLAGS = 0x8183.toShort()
        private const val SERVFAIL_FLAGS = 0x8182.toShort()
    }
}
