package com.safelink.app

import android.Manifest
import android.app.ActivityManager
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.StatFs
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.InsertChartOutlined
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.TrendingFlat
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import com.safelink.app.model.AnalysisResult
import com.safelink.app.model.RiskLevel
import com.safelink.app.data.HistoryRepository
import com.safelink.app.data.TerminalEntry
import com.safelink.app.service.ProtectionService
import com.safelink.app.service.SafeLinkVpnService
import com.safelink.app.ui.HistoryItem
import com.safelink.app.ui.SafeLinkTheme
import java.io.File
import java.util.Calendar
import java.util.Locale
import kotlin.math.exp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var repository: HistoryRepository
    private var protectionVisible by mutableStateOf(false)
    private var browserConfigured by mutableStateOf(false)
    private var accessibilityConfigured by mutableStateOf(false)
    private var vpnEnabled by mutableStateOf(false)
    private var strictMode by mutableStateOf(false)
    private var silentMode by mutableStateOf(false)
    private var blockShorteners by mutableStateOf(false)
    private var blockNoHttps by mutableStateOf(false)
    private var blockIpDomains by mutableStateOf(false)

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        startProtectionService()
        repository.setLayerEnabled(HistoryRepository.LAYER_PROTECTION_VISIBLE, true)
        repository.setProtectionEnabled(true)
        protectionVisible = true
        showMessage("Protecao visivel ativada.")
    }

    private val vpnPermission = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            startLocalVpnGuard()
        } else {
            repository.setLayerEnabled(HistoryRepository.LAYER_VPN, false)
            vpnEnabled = false
            showMessage("Permissao de VPN nao concedida.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = safeLinkContainer.historyRepository
        refreshLayerStates()
        setContent {
            SafeLinkTheme {
                val history by repository.items.collectAsState()
                val terminalEntries by repository.terminalEntries.collectAsState()
                DashboardScreen(
                    protectionVisible = protectionVisible,
                    browserConfigured = browserConfigured,
                    accessibilityConfigured = accessibilityConfigured,
                    vpnEnabled = vpnEnabled,
                    history = history,
                    terminalEntries = terminalEntries,
                    onApplyProtectionPreset = { applyProtectionPreset(it) },
                    onSetProtectionVisible = { enabled ->
                        if (enabled) {
                            repository.setProtectionEnabled(true)
                            repository.setLayerEnabled(HistoryRepository.LAYER_PROTECTION_VISIBLE, true)
                            protectionVisible = true
                            requestNotificationAndStart()
                        } else {
                            repository.setLayerEnabled(HistoryRepository.LAYER_PROTECTION_VISIBLE, false)
                            protectionVisible = false
                            stopService(Intent(this, ProtectionService::class.java))
                            showMessage("Protecao visivel desativada.")
                        }
                    },
                    onSetBrowserConfigured = { enabled ->
                        repository.setLayerEnabled(HistoryRepository.LAYER_BROWSER, enabled)
                        browserConfigured = enabled
                        if (enabled) {
                            requestBrowserRole()
                        } else {
                            showMessage("Navegador seguro desativado no SafeLink.")
                        }
                    },
                    onSetAccessibilityConfigured = { enabled ->
                        repository.setLayerEnabled(HistoryRepository.LAYER_ACCESSIBILITY, enabled)
                        accessibilityConfigured = enabled
                        if (enabled) {
                            openAccessibilitySettings()
                        } else {
                            showMessage("Monitoramento de chats desativado no SafeLink.")
                        }
                    },
                    onStartVpn = {
                        repository.setLayerEnabled(HistoryRepository.LAYER_VPN, true)
                        vpnEnabled = true
                        requestVpnGuard()
                    },
                    onStopVpn = {
                        repository.setLayerEnabled(HistoryRepository.LAYER_VPN, false)
                        vpnEnabled = false
                        stopLocalVpnGuard()
                    },
                    onOpenHistoryReview = { openHistoryReview(it) },
                    onDeleteHistoryItem = { repository.delete(it.url) },
                    onTrustHistoryItem = {
                        repository.trustDomain(it.host)
                        repository.updateDecision(it.url, "Confiado")
                        showMessage("Dominio confiavel.")
                    },
                    onBlockHistoryItem = {
                        repository.blockDomain(it.host)
                        repository.updateDecision(it.url, "Bloqueado")
                        showMessage("Dominio bloqueado.")
                    },
                    onCopyHistoryItem = {
                        repository.updateDecision(it.url, "Copiado")
                        showMessage("Link copiado.")
                    },
                    onManualAnalyze = { openManualAnalyze(it) },
                    onTerminalAnalyze = { analyzeTerminalLink(it) },
                    onOpenCamera = { openCamera() },
                    strictMode = strictMode,
                    silentMode = silentMode,
                    blockShorteners = blockShorteners,
                    blockNoHttps = blockNoHttps,
                    blockIpDomains = blockIpDomains,
                    onBlockShortenersChange = {
                        blockShorteners = it
                        repository.setBoolean(HistoryRepository.BLOCK_SHORTENERS, it)
                    },
                    onBlockNoHttpsChange = {
                        blockNoHttps = it
                        repository.setBoolean(HistoryRepository.BLOCK_NO_HTTPS, it)
                    },
                    onBlockIpDomainsChange = {
                        blockIpDomains = it
                        repository.setBoolean(HistoryRepository.BLOCK_IP_DOMAINS, it)
                    },
                    trustedDomains = repository.trustedDomains().toList().sorted(),
                    blockedDomains = repository.blockedDomains().toList().sorted(),
                    onRemoveDomainDecision = { repository.removeDomainDecision(it) },
                    onRemoveTerminalEntry = { value, status ->
                        repository.deleteTerminalEntry(value, status)
                        repository.removeDomainDecision(value)
                    },
                    onTrustDomain = {
                        if (it.isBlank()) {
                            showMessage("Informe um dominio.")
                        } else {
                            val domain = normalizeCommandDomain(it)
                            repository.trustDomain(domain)
                            repository.addTerminalDomain(domain, "Confiado")
                            showMessage("Dominio confiavel adicionado.")
                        }
                    },
                    onBlockDomain = {
                        if (it.isBlank()) {
                            showMessage("Informe um dominio.")
                        } else {
                            val domain = normalizeCommandDomain(it)
                            repository.blockDomain(domain)
                            repository.addTerminalDomain(domain, "Bloqueado")
                            showMessage("Dominio bloqueado adicionado.")
                        }
                    },
                    onShareBackup = { shareBackup() },
                    onImportBackup = {
                        if (repository.importBackup(it)) {
                            refreshLayerStates()
                            showMessage("Backup importado.")
                        } else {
                            showMessage("Backup invalido.")
                        }
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized) refreshLayerStates()
    }

    private fun requestNotificationAndStart() {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startProtectionService()
            showMessage("Protecao visivel ativada.")
        }
    }

    private fun startProtectionService() {
        repository.setLayerEnabled(HistoryRepository.LAYER_PROTECTION_VISIBLE, true)
        repository.setProtectionEnabled(true)
        protectionVisible = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(Intent(this, ProtectionService::class.java))
        } else {
            startService(Intent(this, ProtectionService::class.java))
        }
    }

    private fun requestBrowserRole() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (
                roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)
            ) {
                startActivity(roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER))
                showMessage("Escolha SafeLink como navegador padrao.")
                return
            }
        }

        runCatching {
            startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
            showMessage("Abra Apps padrao e escolha SafeLink como navegador.")
        }.recover {
            startActivity(Intent(Settings.ACTION_SETTINGS))
            showMessage("Procure Apps padrao e selecione SafeLink como navegador.")
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        showMessage("Procure Monitoramento de links SafeLink.")
    }

    private fun openAppDetails() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            },
        )
        showMessage("No menu de tres pontos, permita configuracoes restritas se aparecer.")
    }

    private fun requestVpnGuard() {
        val prepareIntent = VpnService.prepare(this)
        if (prepareIntent != null) {
            vpnPermission.launch(prepareIntent)
            showMessage("Autorize a VPN local do SafeLink.")
        } else {
            startLocalVpnGuard()
        }
    }

    private fun startLocalVpnGuard() {
        repository.setLayerEnabled(HistoryRepository.LAYER_VPN, true)
        vpnEnabled = true
        startService(Intent(this, SafeLinkVpnService::class.java).apply {
            action = SafeLinkVpnService.ACTION_START
        })
        showMessage("VPN local solicitada.")
    }

    private fun stopLocalVpnGuard() {
        repository.setLayerEnabled(HistoryRepository.LAYER_VPN, false)
        vpnEnabled = false
        startService(Intent(this, SafeLinkVpnService::class.java).apply {
            action = SafeLinkVpnService.ACTION_STOP
        })
        showMessage("VPN local desativada.")
    }

    private fun setAllProtectionLayers(enabled: Boolean) {
        repository.setProtectionEnabled(enabled)
        repository.setLayerEnabled(HistoryRepository.LAYER_PROTECTION_VISIBLE, enabled)
        repository.setLayerEnabled(HistoryRepository.LAYER_BROWSER, enabled)
        repository.setLayerEnabled(HistoryRepository.LAYER_ACCESSIBILITY, enabled)
        repository.setLayerEnabled(HistoryRepository.LAYER_VPN, enabled)

        protectionVisible = enabled
        browserConfigured = enabled
        accessibilityConfigured = enabled
        vpnEnabled = enabled

        if (enabled) {
            requestNotificationAndStart()
            requestVpnGuard()
            showMessage("Camadas de protecao ativadas. Navegador e monitoramento podem exigir confirmacao nas configuracoes.")
        } else {
            stopService(Intent(this, ProtectionService::class.java))
            stopLocalVpnGuard()
            showMessage("Camadas de protecao desativadas.")
        }
    }

    private fun applyProtectionPreset(index: Int) {
        val presetName: String
        val protection: Boolean
        val browser: Boolean
        val accessibility: Boolean
        val vpn: Boolean
        val strict: Boolean
        val silent: Boolean
        val shorteners: Boolean
        val noHttps: Boolean
        val ipDomains: Boolean

        when (index.coerceIn(0, 4)) {
            0 -> {
                presetName = "Minimo"
                protection = true
                browser = false
                accessibility = false
                vpn = false
                strict = false
                silent = false
                shorteners = false
                noHttps = false
                ipDomains = false
            }
            1 -> {
                presetName = "Basico"
                protection = true
                browser = true
                accessibility = false
                vpn = false
                strict = false
                silent = false
                shorteners = true
                noHttps = false
                ipDomains = false
            }
            2 -> {
                presetName = "Recomendado"
                protection = true
                browser = true
                accessibility = true
                vpn = false
                strict = false
                silent = false
                shorteners = true
                noHttps = true
                ipDomains = false
            }
            3 -> {
                presetName = "Silencioso"
                protection = true
                browser = true
                accessibility = true
                vpn = true
                strict = false
                silent = true
                shorteners = true
                noHttps = true
                ipDomains = false
            }
            else -> {
                presetName = "Rigido"
                protection = true
                browser = true
                accessibility = true
                vpn = true
                strict = true
                silent = false
                shorteners = true
                noHttps = true
                ipDomains = true
            }
        }

        val wasProtectionVisible = protectionVisible
        val wasVpnEnabled = vpnEnabled

        repository.setProtectionEnabled(protection)
        repository.setLayerEnabled(HistoryRepository.LAYER_PROTECTION_VISIBLE, protection)
        repository.setLayerEnabled(HistoryRepository.LAYER_BROWSER, browser)
        repository.setLayerEnabled(HistoryRepository.LAYER_ACCESSIBILITY, accessibility)
        repository.setLayerEnabled(HistoryRepository.LAYER_VPN, vpn)
        repository.setBoolean(HistoryRepository.MODE_STRICT, strict)
        repository.setBoolean(HistoryRepository.MODE_SILENT, silent)
        repository.setBoolean(HistoryRepository.BLOCK_SHORTENERS, shorteners)
        repository.setBoolean(HistoryRepository.BLOCK_NO_HTTPS, noHttps)
        repository.setBoolean(HistoryRepository.BLOCK_IP_DOMAINS, ipDomains)

        protectionVisible = protection
        browserConfigured = browser
        accessibilityConfigured = accessibility
        vpnEnabled = vpn
        strictMode = strict
        silentMode = silent
        blockShorteners = shorteners
        blockNoHttps = noHttps
        blockIpDomains = ipDomains

        if (protection) {
            requestNotificationAndStart()
        } else if (wasProtectionVisible) {
            stopService(Intent(this, ProtectionService::class.java))
        }
        if (vpn) {
            requestVpnGuard()
        } else if (wasVpnEnabled) {
            stopLocalVpnGuard()
        }
        showMessage("Perfil $presetName aplicado.")
    }

    private fun openHistoryReview(result: AnalysisResult) {
        startActivity(
            Intent(this, HistoryDetailActivity::class.java).apply {
                putExtra(HistoryDetailActivity.EXTRA_URL, result.url)
                putExtra(HistoryDetailActivity.EXTRA_HOST, result.host)
                putExtra(HistoryDetailActivity.EXTRA_LEVEL, result.level.name)
                putExtra(HistoryDetailActivity.EXTRA_SCORE, result.score)
                putExtra(HistoryDetailActivity.EXTRA_DECISION, result.decision)
                putStringArrayListExtra(HistoryDetailActivity.EXTRA_REASONS, ArrayList(result.reasons))
                putExtra(HistoryDetailActivity.EXTRA_CHECKED_AT, result.checkedAt)
            },
        )
    }

    private fun openManualAnalyze(rawUrl: String) {
        if (rawUrl.isBlank()) {
            showMessage("Cole um link para analisar.")
            return
        }
        startActivity(
            Intent(this, LinkReviewActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = Uri.parse(rawUrl)
            },
        )
    }

    private fun analyzeTerminalLink(rawUrl: String) {
        val normalizedUrl = normalizeTerminalLink(rawUrl)
        if (!isLikelyLink(normalizedUrl)) {
            showMessage("Digite um link valido para analisar.")
            return
        }
        val analyzed = safeLinkContainer.analyzer.analyze(normalizedUrl)
        val result = when {
            repository.isTrusted(analyzed.host) -> analyzed.copy(
                level = RiskLevel.Safe,
                score = 0,
                reasons = listOf("O dominio esta na lista de confianca do usuario."),
            )
            repository.isBlocked(analyzed.host) -> analyzed.copy(
                level = RiskLevel.Dangerous,
                score = 100,
                reasons = listOf("O dominio esta na lista de bloqueio do usuario."),
            )
            else -> analyzed
        }
        repository.save(result)
        repository.addTerminalAnalysis(result)
        showMessage("Link analisado no terminal.")
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) == null) {
            showMessage("Nao encontrei um app de camera disponivel.")
            return
        }
        startActivity(intent)
    }

    private fun shareHistory() {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, repository.exportText())
                },
                "Compartilhar historico SafeLink",
            ),
        )
    }

    private fun refreshLayerStates() {
        protectionVisible = repository.isLayerEnabled(HistoryRepository.LAYER_PROTECTION_VISIBLE)
        browserConfigured = repository.isLayerEnabled(HistoryRepository.LAYER_BROWSER) || isBrowserRoleHeld()
        accessibilityConfigured = repository.isLayerEnabled(HistoryRepository.LAYER_ACCESSIBILITY) || isAccessibilityServiceEnabled()
        vpnEnabled = repository.isLayerEnabled(HistoryRepository.LAYER_VPN)
        strictMode = repository.getBoolean(HistoryRepository.MODE_STRICT)
        silentMode = repository.getBoolean(HistoryRepository.MODE_SILENT)
        blockShorteners = repository.getBoolean(HistoryRepository.BLOCK_SHORTENERS)
        blockNoHttps = repository.getBoolean(HistoryRepository.BLOCK_NO_HTTPS)
        blockIpDomains = repository.getBoolean(HistoryRepository.BLOCK_IP_DOMAINS)
    }

    private fun shareReport(history: List<AnalysisResult>) {
        val today = System.currentTimeMillis() - 24L * 60L * 60L * 1000L
        val week = System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L
        val todayItems = history.filter { it.checkedAt >= today }
        val weekItems = history.filter { it.checkedAt >= week }
        val text = """
            Relatorio SafeLink
            Hoje: ${todayItems.size} analisados, ${todayItems.count { it.level == RiskLevel.Dangerous }} bloqueios altos.
            Semana: ${weekItems.size} analisados, ${weekItems.count { it.level == RiskLevel.Dangerous }} bloqueios altos.
        """.trimIndent()
        shareText(text, "Compartilhar relatorio SafeLink")
    }

    private fun shareBackup() {
        shareText(repository.exportBackup(), "Compartilhar backup SafeLink")
    }

    private fun shareText(text: String, title: String) {
        startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                },
                title,
            ),
        )
    }

    private fun isBrowserRoleHeld(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            runCatching {
                val roleManager = getSystemService(RoleManager::class.java)
                roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER) &&
                    roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)
            }.getOrDefault(false)
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        return enabled.contains("com.safelink.app.service.SafeLinkAccessibilityService", ignoreCase = true)
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

}

@Composable
private fun DashboardScreen(
    protectionVisible: Boolean,
    browserConfigured: Boolean,
    accessibilityConfigured: Boolean,
    vpnEnabled: Boolean,
    history: List<AnalysisResult>,
    terminalEntries: List<TerminalEntry>,
    onApplyProtectionPreset: (Int) -> Unit,
    onSetProtectionVisible: (Boolean) -> Unit,
    onSetBrowserConfigured: (Boolean) -> Unit,
    onSetAccessibilityConfigured: (Boolean) -> Unit,
    onStartVpn: () -> Unit,
    onStopVpn: () -> Unit,
    onOpenHistoryReview: (AnalysisResult) -> Unit,
    onDeleteHistoryItem: (AnalysisResult) -> Unit,
    onTrustHistoryItem: (AnalysisResult) -> Unit,
    onBlockHistoryItem: (AnalysisResult) -> Unit,
    onCopyHistoryItem: (AnalysisResult) -> Unit,
    onManualAnalyze: (String) -> Unit,
    onTerminalAnalyze: (String) -> Unit,
    onOpenCamera: () -> Unit,
    strictMode: Boolean,
    silentMode: Boolean,
    blockShorteners: Boolean,
    blockNoHttps: Boolean,
    blockIpDomains: Boolean,
    onBlockShortenersChange: (Boolean) -> Unit,
    onBlockNoHttpsChange: (Boolean) -> Unit,
    onBlockIpDomainsChange: (Boolean) -> Unit,
    trustedDomains: List<String>,
    blockedDomains: List<String>,
    onRemoveDomainDecision: (String) -> Unit,
    onRemoveTerminalEntry: (String, String) -> Unit,
    onTrustDomain: (String) -> Unit,
    onBlockDomain: (String) -> Unit,
    onShareBackup: () -> Unit,
    onImportBackup: (String) -> Unit,
) {
    var filter by remember { mutableStateOf<RiskLevel?>(null) }
    var manualUrl by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    var selectedProtectionPreset by remember { mutableStateOf(0) }
    var terminalInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val sectionStart = with(LocalDensity.current) {
        (LocalConfiguration.current.screenHeightDp.dp + 10.dp).roundToPx()
    }
    val filteredHistory = (filter?.let { level -> history.filter { it.level == level } } ?: history)
        .filter { search.isBlank() || it.url.contains(search, ignoreCase = true) || it.host.contains(search, ignoreCase = true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 8.dp)
                .padding(bottom = 86.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SafeLinkDesignerDashboard(history)

            when (selectedTab) {
                0 -> ProtectionViewport(
                    protectionVisible = protectionVisible,
                    browserConfigured = browserConfigured,
                    accessibilityConfigured = accessibilityConfigured,
                    vpnEnabled = vpnEnabled,
                    selectedPreset = selectedProtectionPreset,
                    strictMode = strictMode,
                    silentMode = silentMode,
                    blockShorteners = blockShorteners,
                    blockNoHttps = blockNoHttps,
                    blockIpDomains = blockIpDomains,
                    onPresetSelected = { preset ->
                        selectedProtectionPreset = preset
                        onApplyProtectionPreset(preset)
                    },
                    onProtectionVisibleChange = onSetProtectionVisible,
                    onBrowserConfiguredChange = onSetBrowserConfigured,
                    onAccessibilityConfiguredChange = onSetAccessibilityConfigured,
                    onVpnChange = { enabled ->
                        if (enabled) onStartVpn() else onStopVpn()
                    },
                    onBlockShortenersChange = onBlockShortenersChange,
                    onBlockNoHttpsChange = onBlockNoHttpsChange,
                    onBlockIpDomainsChange = onBlockIpDomainsChange,
                )
                1 -> {
                    ManualAnalyzerCard(
                        url = manualUrl,
                        onUrlChange = { manualUrl = it },
                        onAnalyze = { onManualAnalyze(manualUrl) },
                    )
                    HowToTestCard(onAnalyze = onManualAnalyze)
                }
                2 -> HistorySection(
                    history = filteredHistory,
                    selectedFilter = filter,
                    onFilterChange = { filter = it },
                    search = search,
                    onSearchChange = { search = it },
                    onOpenReview = onOpenHistoryReview,
                    onDelete = onDeleteHistoryItem,
                    onTrust = onTrustHistoryItem,
                    onBlock = onBlockHistoryItem,
                    onCopy = onCopyHistoryItem,
                )
                3 -> {
                    TerminalCommandViewport(
                        entries = terminalEntries,
                        command = terminalInput,
                        onCommandChange = { terminalInput = it },
                        onAnalyze = {
                            onTerminalAnalyze(terminalInput)
                            terminalInput = ""
                        },
                        onTrustDomain = {
                            onTrustDomain(terminalInput)
                            terminalInput = ""
                        },
                        onBlockDomain = {
                            onBlockDomain(terminalInput)
                            terminalInput = ""
                        },
                        onRemove = onRemoveTerminalEntry,
                        onEditTrusted = { old, new ->
                            onRemoveTerminalEntry(old, "Confiado")
                            if (new.isNotBlank()) onTrustDomain(new)
                        },
                        onEditBlocked = { old, new ->
                            onRemoveTerminalEntry(old, "Bloqueado")
                            if (new.isNotBlank()) onBlockDomain(new)
                        },
                    )
                }
            }
        }
        SideNavigation(
            selected = selectedTab,
            onCamera = onOpenCamera,
            onSelected = { index ->
                selectedTab = index
                coroutineScope.launch {
                    scrollState.animateScrollTo(sectionStart)
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun TerminalCommandViewport(
    entries: List<TerminalEntry>,
    command: String,
    onCommandChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onTrustDomain: () -> Unit,
    onBlockDomain: () -> Unit,
    onRemove: (String, String) -> Unit,
    onEditTrusted: (String, String) -> Unit,
    onEditBlocked: (String, String) -> Unit,
) {
    val viewportHeight = LocalConfiguration.current.screenHeightDp.dp
    val detailRows = entries.sumOf { if (it.type == TerminalEntry.TYPE_ANALYSIS) 2 + it.reasons.size.coerceAtMost(4) else 2 }
    val panelHeight = viewportHeight - 70.dp + 18.dp * detailRows.coerceAtLeast(0)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(panelHeight + 40.dp)
            .padding(top = 8.dp, bottom = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = panelHeight)
                .background(Color.Black, RoundedCornerShape(8.dp)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 22.dp, end = 16.dp, bottom = 76.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                entries.forEach { entry ->
                    key("${entry.type}-${entry.value}-${entry.checkedAt}") {
                        TerminalEntryBlock(
                            entry = entry,
                            onRemove = onRemove,
                            onEditTrusted = onEditTrusted,
                            onEditBlocked = onEditBlocked,
                        )
                    }
                }
            }

            TerminalCommandInput(
                value = command,
                onValueChange = onCommandChange,
                onAnalyze = onAnalyze,
                onTrustDomain = onTrustDomain,
                onBlockDomain = onBlockDomain,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 14.dp, end = 14.dp, bottom = 10.dp),
            )
        }
    }
}

@Composable
private fun TerminalEntryBlock(
    entry: TerminalEntry,
    onRemove: (String, String) -> Unit,
    onEditTrusted: (String, String) -> Unit,
    onEditBlocked: (String, String) -> Unit,
) {
    if (entry.type == TerminalEntry.TYPE_DOMAIN) {
        TerminalDomainBlock(
            entry = entry,
            onRemove = onRemove,
            onEditTrusted = onEditTrusted,
            onEditBlocked = onEditBlocked,
        )
    } else {
        TerminalAnalysisBlock(entry)
    }
}

@Composable
private fun TerminalDomainBlock(
    entry: TerminalEntry,
    onRemove: (String, String) -> Unit,
    onEditTrusted: (String, String) -> Unit,
    onEditBlocked: (String, String) -> Unit,
) {
    val statusColor = if (entry.status == "Confiado") Color(0xFF22F29A) else Color(0xFFE53442)
    val editHandler = if (entry.status == "Confiado") onEditTrusted else onEditBlocked
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Dominio",
            style = terminalTextStyle(Color.White),
            maxLines = 1,
        )
        TerminalEditableDomainLine(
            value = entry.value,
            status = entry.status,
            statusColor = statusColor,
            onEdited = editHandler,
            onRemove = { onRemove(entry.value, entry.status) },
        )
    }
}

@Composable
private fun TerminalEditableDomainLine(
    value: String,
    status: String,
    statusColor: Color,
    onEdited: (String, String) -> Unit,
    onRemove: () -> Unit,
) {
    var draft by remember(value) { mutableStateOf(value) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = draft,
            onValueChange = { next ->
                draft = next
                val normalized = normalizeCommandDomain(next)
                if (next.isBlank()) {
                    onRemove()
                } else if (normalized != value && isLikelyDomain(normalized)) {
                    onEdited(value, normalized)
                }
            },
            singleLine = true,
            textStyle = TextStyle(
                color = Color.White.copy(alpha = 0.82f),
                fontSize = fixedSp(13.5f),
                lineHeight = fixedSp(16f),
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = status,
            color = statusColor,
            fontSize = fixedSp(13.5f),
            lineHeight = fixedSp(16f),
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.Monospace,
            maxLines = 1,
        )
    }
}

@Composable
private fun TerminalAnalysisBlock(entry: TerminalEntry) {
    val statusColor = when (entry.status) {
        RiskLevel.Safe.label -> Color(0xFF22F29A)
        RiskLevel.Suspicious.label -> Color(0xFFE0B448)
        RiskLevel.Dangerous.label -> Color(0xFFE53442)
        else -> Color.White.copy(alpha = 0.82f)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = "Analisado",
            style = terminalTextStyle(Color.White),
            maxLines = 1,
        )
        Text(
            text = entry.value,
            style = terminalTextStyle(Color.White.copy(alpha = 0.82f)),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${entry.status} | risco ${entry.score}/100 | ${entry.host.ifBlank { "dominio nao identificado" }}",
            style = terminalTextStyle(statusColor),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        entry.reasons.take(4).forEach { reason ->
            Text(
                text = "- $reason",
                style = terminalTextStyle(Color.White.copy(alpha = 0.64f)).copy(
                    fontSize = fixedSp(12f),
                    lineHeight = fixedSp(14f),
                    fontWeight = FontWeight.Normal,
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun terminalTextStyle(color: Color): TextStyle {
    return TextStyle(
        color = color,
        fontSize = fixedSp(13.5f),
        lineHeight = fixedSp(16f),
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.Monospace,
    )
}

@Composable
private fun TerminalCommandInput(
    value: String,
    onValueChange: (String) -> Unit,
    onAnalyze: () -> Unit,
    onTrustDomain: () -> Unit,
    onBlockDomain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val trimmed = value.trim()
    val isLink = isLikelyLink(trimmed)
    val isDomain = isLikelyDomain(normalizeCommandDomain(trimmed))
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(Color(0xFF080808), RoundedCornerShape(100.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(100.dp))
            .padding(start = 20.dp, end = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = fixedSp(15f),
                    lineHeight = fixedSp(17f),
                    fontWeight = FontWeight.Normal,
                ),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isBlank()) {
                            Text(
                                "Digite ou insira um link ou dominio...",
                                color = Color.White.copy(alpha = 0.46f),
                                fontSize = fixedSp(15f),
                                lineHeight = fixedSp(17f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.weight(1f),
            )
            if (trimmed.isNotBlank()) {
                if (isLink) {
                    TerminalRoundAction(Icons.Outlined.Send, onAnalyze)
                } else if (isDomain) {
                    TerminalRoundAction(Icons.Outlined.CheckCircle, onTrustDomain)
                    TerminalRoundAction(Icons.Outlined.Lock, onBlockDomain)
                }
            }
        }
    }
}

@Composable
private fun TerminalRoundAction(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(100.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(100.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

private fun isLikelyLink(value: String): Boolean {
    return value.startsWith("http://", ignoreCase = true) ||
        value.startsWith("https://", ignoreCase = true) ||
        value.startsWith("www.", ignoreCase = true)
}

private fun normalizeTerminalLink(value: String): String {
    val trimmed = value.trim()
    return if (trimmed.startsWith("www.", ignoreCase = true)) "https://$trimmed" else trimmed
}

private fun normalizeCommandDomain(value: String): String {
    return value
        .trim()
        .removePrefix("http://")
        .removePrefix("https://")
        .substringBefore("/")
        .substringBefore("?")
        .substringBefore("#")
        .lowercase()
        .removePrefix("www.")
}

private fun isLikelyDomain(value: String): Boolean {
    return Regex("""^[a-z0-9][a-z0-9.-]*\.[a-z]{2,}$""", RegexOption.IGNORE_CASE).matches(value)
}

@Composable
private fun SideNavigation(
    selected: Int,
    onCamera: () -> Unit,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val items = listOf(
        SideNavItem(Icons.Outlined.Security, "Protecao", tab = 0),
        SideNavItem(Icons.Outlined.History, "Historico", tab = 2),
        SideNavItem(Icons.Outlined.Terminal, "Terminal", tab = 3),
        SideNavItem(Icons.Outlined.QrCodeScanner, "QR", tab = null),
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(78.dp)
            .background(Color.Black)
            .border(0.5.dp, Color.White.copy(alpha = 0.08f)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val selectedItem = item.tab == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .clickable {
                        item.tab?.let(onSelected) ?: onCamera()
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    item.icon,
                    contentDescription = null,
                    tint = if (selectedItem) Color.White else Color.White.copy(alpha = 0.42f),
                    modifier = Modifier.size(if (selectedItem) 25.dp else 23.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = item.label,
                    color = if (selectedItem) Color.White else Color.White.copy(alpha = 0.48f),
                    fontSize = fixedSp(10.5f),
                    lineHeight = fixedSp(12f),
                    fontWeight = if (selectedItem) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private data class SideNavItem(
    val icon: ImageVector,
    val label: String,
    val tab: Int?,
)

@Composable
private fun ProtectionViewport(
    protectionVisible: Boolean,
    browserConfigured: Boolean,
    accessibilityConfigured: Boolean,
    vpnEnabled: Boolean,
    selectedPreset: Int,
    strictMode: Boolean,
    silentMode: Boolean,
    blockShorteners: Boolean,
    blockNoHttps: Boolean,
    blockIpDomains: Boolean,
    onPresetSelected: (Int) -> Unit,
    onProtectionVisibleChange: (Boolean) -> Unit,
    onBrowserConfiguredChange: (Boolean) -> Unit,
    onAccessibilityConfiguredChange: (Boolean) -> Unit,
    onVpnChange: (Boolean) -> Unit,
    onBlockShortenersChange: (Boolean) -> Unit,
    onBlockNoHttpsChange: (Boolean) -> Unit,
    onBlockIpDomainsChange: (Boolean) -> Unit,
) {
    val viewportHeight = LocalConfiguration.current.screenHeightDp.dp
    val rows = listOf(
        ProtectionToggleRow(Icons.Outlined.NotificationsActive, "Protecao visivel", "Mantem o SafeLink ativo e com alertas.", protectionVisible, onProtectionVisibleChange),
        ProtectionToggleRow(Icons.Outlined.Public, "Navegador seguro", "Faz links externos passarem pelo SafeLink.", browserConfigured, onBrowserConfiguredChange),
        ProtectionToggleRow(Icons.Outlined.ChatBubbleOutline, "Monitoramento de chats", "Detecta links visiveis com acessibilidade.", accessibilityConfigured, onAccessibilityConfiguredChange),
        ProtectionToggleRow(Icons.Outlined.VpnKey, "VPN local", "Bloqueia dominios suspeitos por DNS.", vpnEnabled, onVpnChange),
        ProtectionToggleRow(Icons.Outlined.QrCodeScanner, "Bloquear encurtadores", "Bloqueia dominios bit.ly e tinyurl.", blockShorteners, onBlockShortenersChange),
        ProtectionToggleRow(Icons.Outlined.PrivacyTip, "Bloquear sem HTTPS", "Bloqueia links iniciados com http://.", blockNoHttps, onBlockNoHttpsChange),
        ProtectionToggleRow(Icons.Outlined.Security, "Bloquear IP direto", "Bloqueia destinos com endereco numerico.", blockIpDomains, onBlockIpDomainsChange),
    )
    val rigidLevel = securityLevel(
        protectionVisible,
        browserConfigured,
        accessibilityConfigured,
        vpnEnabled,
        blockShorteners,
        blockNoHttps,
        blockIpDomains,
    )
    val silentLevel = securityLevel(
        protectionVisible,
        browserConfigured,
        accessibilityConfigured,
        vpnEnabled,
        blockShorteners,
        blockNoHttps,
    )
    val customLevel = rows.count { it.checked } * 10f / rows.size

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(viewportHeight),
    ) {
        fun y(referencePx: Float) = maxHeight * (referencePx / 1200f)
        val rowTops = listOf(132f, 218f, 304f, 390f, 476f, 562f, 648f)

        ProtectionProgressLine(
            selectedPreset = selectedPreset,
            onPresetSelected = onPresetSelected,
            modifier = Modifier
                .fillMaxWidth()
                .height(y(110f))
                .offset(y = y(23f)),
        )

        rows.forEachIndexed { index, row ->
            ProtectionToggleCard(
                row = row,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(y(76f))
                    .offset(y = y(rowTops[index])),
            )
        }

        SecurityLayersChart(
            rigidLevel = if (strictMode || rigidLevel >= 10f) rigidLevel else (rigidLevel * 0.55f),
            silentLevel = if (silentMode || vpnEnabled) silentLevel else (silentLevel * 0.35f),
            customLevel = customLevel,
            modifier = Modifier
                .fillMaxWidth()
                .height(y(443f))
                .offset(y = y(734f)),
        )
    }
}

private data class ProtectionToggleRow(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
)

private fun securityLevel(vararg enabled: Boolean): Float {
    if (enabled.isEmpty()) return 0f
    return enabled.count { it } * 10f / enabled.size
}

@Composable
private fun ProtectionProgressLine(
    selectedPreset: Int,
    onPresetSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        val selected = selectedPreset.coerceIn(0, 4)
        val pointFractions = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
        Canvas(modifier = Modifier.fillMaxSize()) {
        val centerY = size.height * 0.42f
        val startX = size.width * 0.10f
        val endX = size.width * 0.92f
        drawLine(
            color = Color.White,
            start = Offset(startX, centerY),
            end = Offset(endX, centerY),
            strokeWidth = 3.5f,
            cap = StrokeCap.Round,
        )
            pointFractions.forEachIndexed { index, point ->
            val radius = if (index == selected) size.height * 0.17f else size.height * 0.09f
            drawCircle(
                color = Color.White,
                radius = radius,
                center = Offset(startX + (endX - startX) * point, centerY),
            )
        }
        }
        pointFractions.forEachIndexed { index, point ->
            val centerX = maxWidth * (0.10f + (0.92f - 0.10f) * point)
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .offset(x = centerX - 26.dp, y = maxHeight * 0.42f - 26.dp)
                    .clickable { onPresetSelected(index) },
            )
        }
    }
}

@Composable
private fun ProtectionToggleCard(row: ProtectionToggleRow, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .background(Color(0xFF080808), RoundedCornerShape(4.dp))
            .clickable { row.onCheckedChange(!row.checked) },
    ) {
        fun x(referencePx: Float) = maxWidth * (referencePx / 500f)
        fun y(referencePx: Float) = maxHeight * (referencePx / 70f)

        Icon(
            imageVector = row.icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(y(24f))
                .offset(x = x(22f), y = y(23f)),
        )
        Text(
            text = row.title,
            color = Color.White,
            fontSize = fixedSp(16f),
            lineHeight = fixedSp(18f),
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = x(78f), end = x(92f))
                .offset(y = y(13f)),
        )
        Text(
            text = row.description,
            color = Color.White.copy(alpha = 0.62f),
            fontSize = fixedSp(12f),
            lineHeight = fixedSp(14f),
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = x(78f), end = x(80f))
                .offset(y = y(38f)),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .fillMaxWidth()
                .padding(start = x(78f))
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f)),
        )
        SafeLinkLayerSwitch(
            checked = row.checked,
            onCheckedChange = row.onCheckedChange,
            modifier = Modifier
                .width(x(76f))
                .height(y(37f))
                .align(Alignment.CenterEnd)
                .padding(end = x(8f)),
        )
    }
}

@Composable
private fun SafeLinkLayerSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackColor = if (checked) Color(0xFF16866F) else Color.White
    val knobColor = if (checked) Color.White else Color(0xFF8D8D8D)
    Box(
        modifier = modifier.clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(trackColor, RoundedCornerShape(100.dp))
                .border(1.dp, Color.White.copy(alpha = if (checked) 0.92f else 0.28f), RoundedCornerShape(100.dp)),
        ) {
            Box(
                modifier = Modifier
                    .padding(horizontal = 5.dp)
                    .size(22.dp)
                    .align(if (checked) Alignment.CenterEnd else Alignment.CenterStart)
                    .background(knobColor, RoundedCornerShape(100.dp))
                    .border(0.6.dp, Color.Black.copy(alpha = 0.12f), RoundedCornerShape(100.dp)),
            )
        }
    }
}

@Composable
private fun SecurityLayersChart(
    rigidLevel: Float,
    silentLevel: Float,
    customLevel: Float,
    modifier: Modifier = Modifier,
) {
    val values = listOf(
        ChartLayer("Rigido", rigidLevel.coerceIn(0f, 10f), Color.White),
        ChartLayer("Silencioso", silentLevel.coerceIn(0f, 10f), Color.White),
        ChartLayer("Customizado", customLevel.coerceIn(0f, 10f), Color.White),
    )

    BoxWithConstraints(
        modifier = modifier.background(Color(0xFF080808), RoundedCornerShape(4.dp)),
    ) {
        fun x(referencePx: Float) = maxWidth * (referencePx / 500f)
        fun y(referencePx: Float) = maxHeight * (referencePx / 350f)
        val chartTop = y(72f)
        val chartBottom = y(292f)
        val chartHeight = chartBottom - chartTop

        Icon(
            Icons.Outlined.Timeline,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .offset(x = x(22f), y = y(16f))
                .size(22.dp),
        )
        Text(
            text = "Camadas de Seguranca",
            color = Color.White,
            fontSize = fixedSp(16f),
            lineHeight = fixedSp(18f),
            fontWeight = FontWeight.Normal,
            maxLines = 1,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = x(78f), end = x(18f))
                .offset(y = y(18f)),
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val top = chartTop.toPx()
            val bottom = chartBottom.toPx()
            val height = bottom - top
            val barCenters = listOf(x(116f).toPx(), x(251f).toPx(), x(406f).toPx())
            val trackWidth = x(28f).toPx()
            val barWidth = x(18f).toPx()

            values.forEachIndexed { index, item ->
                drawLine(
                    color = Color.White.copy(alpha = 0.10f),
                    start = Offset(barCenters[index], bottom),
                    end = Offset(barCenters[index], top),
                    strokeWidth = trackWidth,
                    cap = StrokeCap.Round,
                )
                val value = item.value.coerceIn(0f, 10f)
                if (value > 0f) {
                    val topY = bottom - height * (value / 10f)
                    drawLine(
                        color = item.color,
                        start = Offset(barCenters[index], bottom),
                        end = Offset(barCenters[index], topY),
                        strokeWidth = barWidth,
                        cap = StrokeCap.Round,
                    )
                    drawCircle(
                        color = Color.White,
                        radius = x(3.2f).toPx(),
                        center = Offset(barCenters[index], topY),
                    )
                }
            }
        }

        values.forEachIndexed { index, item ->
            val labelCenter = when (index) {
                0 -> x(116f)
                1 -> x(251f)
                else -> x(406f)
            }
            val valueY = chartBottom - chartHeight * (item.value.coerceIn(0f, 10f) / 10f) - y(27f)
            Text(
                text = "${item.value.toInt()}/10",
                color = item.color,
                fontSize = fixedSp(10f),
                lineHeight = fixedSp(11f),
                fontWeight = FontWeight.Black,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(x(64f))
                    .offset(
                        x = labelCenter - x(32f),
                        y = if (valueY < chartTop - y(22f)) chartTop - y(22f) else valueY,
                    ),
            )
            Text(
                text = item.label,
                color = Color.White,
                fontSize = fixedSp(13f),
                lineHeight = fixedSp(15f),
                fontWeight = FontWeight.Black,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(x(116f))
                    .offset(x = labelCenter - x(58f), y = y(313f)),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.White.copy(alpha = 0.08f)),
        )
    }
}

private data class ChartLayer(
    val label: String,
    val value: Float,
    val color: Color,
)

@Composable
private fun SafeLinkDesignerDashboard(history: List<AnalysisResult>) {
    val stats = remember(history) { designerStats(history) }
    val context = LocalContext.current
    var deviceInfo by remember { mutableStateOf(dashboardDeviceInfo(context, null)) }
    LaunchedEffect(context) {
        var previousCpu = readCpuSnapshot()
        while (true) {
            delay(1000)
            val currentCpu = readCpuSnapshot()
            deviceInfo = dashboardDeviceInfo(context, cpuUsagePercent(previousCpu, currentCpu))
            previousCpu = currentCpu
        }
    }
    val dashboardHeight = LocalConfiguration.current.screenHeightDp.dp
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(dashboardHeight),
    ) {
        fun y(referencePx: Float) = maxHeight * (referencePx / 1200f)

        Text(
            text = "SafeLink",
            color = Color.White,
            fontSize = fixedSp(54f),
            lineHeight = fixedSp(56f),
            fontWeight = FontWeight.Black,
            maxLines = 1,
            letterSpacing = 0.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = y(34f)),
        )
        DesignerRiskRing(
            stats = stats,
            modifier = Modifier
                .fillMaxWidth()
                .height(y(330f))
                .offset(y = y(135f)),
        )
        DesignerStatsCard(
            stats = stats,
            modifier = Modifier
                .fillMaxWidth()
                .height(y(214f))
                .offset(y = y(500f)),
        )
        DashboardDeviceInfoPanel(
            info = deviceInfo,
            modifier = Modifier
                .offset(x = 0.dp, y = y(430f)),
        )
        DesignerFrequencyCard(
            items = stats.frequency,
            modifier = Modifier
                .fillMaxWidth()
                .height(y(214f))
                .offset(y = y(724f)),
        )
        DesignerWeeklyChart(
            analyzed = stats.weeklyAnalyzed,
            blocked = stats.weeklyBlocked,
            safeCount = stats.safeCount,
            dangerousCount = stats.dangerousCount,
            modifier = Modifier
                .fillMaxWidth()
                .height(y(252f))
                .offset(y = y(948f)),
        )
    }
}

@Composable
private fun DesignerRiskRing(stats: DesignerStats, modifier: Modifier = Modifier) {
    val score = stats.centerScore.coerceIn(0, 100)
    BoxWithConstraints(modifier = modifier) {
        val layoutScale = minOf(maxWidth.value / 500f, maxHeight.value / 380f)
        val layoutOriginX = ((maxWidth.value - 500f * layoutScale) / 2f).dp
        val layoutOriginY = ((maxHeight.value - 380f * layoutScale) / 2f).dp
        fun x(referencePx: Float) = layoutOriginX + (referencePx * layoutScale).dp
        fun y(referencePx: Float) = layoutOriginY + (referencePx * layoutScale).dp

        Canvas(modifier = Modifier.fillMaxSize()) {
            val scale = minOf(size.width / 500f, size.height / 380f)
            val originX = (size.width - 500f * scale) / 2f
            val originY = (size.height - 380f * scale) / 2f
            fun px(value: Float) = originX + value * scale
            fun py(value: Float) = originY + value * scale

            val strokeWidth = 20f * scale
            val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            val ringLeft = px(125f)
            val ringTop = py(45f)
            val ringSize = 250f * scale
            val minimumSweep = 38f
            val maximumSweep = 80f
            val remainingSweep = maximumSweep - minimumSweep
            fun statusSweep(count: Int): Float {
                if (count <= 0) return minimumSweep
                val intensity = (1f - exp(-count / 8.0).toFloat()).coerceIn(0f, 1f)
                return minimumSweep + remainingSweep * intensity
            }
            val safeSweep = statusSweep(stats.safeCount)
            val suspiciousSweep = statusSweep(stats.suspiciousCount)
            val dangerousSweep = statusSweep(stats.dangerousCount)
            fun centeredStart(centerAngle: Float, sweep: Float): Float {
                return centerAngle - sweep / 2f
            }

            drawArc(
                Color.White,
                startAngle = centeredStart(228f, safeSweep),
                sweepAngle = safeSweep,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(ringLeft, ringTop),
                size = androidx.compose.ui.geometry.Size(ringSize, ringSize),
                style = stroke,
            )
            drawArc(
                Color.White,
                startAngle = centeredStart(312f, suspiciousSweep),
                sweepAngle = suspiciousSweep,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(ringLeft, ringTop),
                size = androidx.compose.ui.geometry.Size(ringSize, ringSize),
                style = stroke,
            )
            drawArc(
                Color.White,
                startAngle = centeredStart(90f, dangerousSweep),
                sweepAngle = dangerousSweep,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(ringLeft, ringTop),
                size = androidx.compose.ui.geometry.Size(ringSize, ringSize),
                style = stroke,
            )

            val topMarkerLength = 30f * scale
            val bottomMarkerLength = 24f * scale
            val dangerousMarkerStart = androidx.compose.ui.geometry.Offset(px(250f), py(311f))

            drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(px(116f), py(64f)), end = androidx.compose.ui.geometry.Offset(px(116f) + topMarkerLength, py(64f)), strokeWidth = 1.dp.toPx())
            drawCircle(Color.White, radius = 1.6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(px(116f) + topMarkerLength, py(64f)))
            drawLine(Color.White, start = androidx.compose.ui.geometry.Offset(px(346f), py(64f)), end = androidx.compose.ui.geometry.Offset(px(346f) + topMarkerLength, py(64f)), strokeWidth = 1.dp.toPx())
            drawCircle(Color.White, radius = 1.6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(px(346f), py(64f)))
            drawLine(Color.White, start = dangerousMarkerStart, end = dangerousMarkerStart.copy(y = dangerousMarkerStart.y + bottomMarkerLength), strokeWidth = 1.dp.toPx())
            drawCircle(Color.White, radius = 1.6.dp.toPx(), center = dangerousMarkerStart)
        }
        Text(
            "$score%",
            color = Color.White,
            fontSize = fixedSp(19f),
            lineHeight = fixedSp(21f),
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = y(157f)),
        )
        Text(
            "Seguro",
            color = Color.White,
            fontSize = fixedSp(12f),
            lineHeight = fixedSp(14f),
            fontWeight = FontWeight.Black,
            modifier = Modifier.offset(x = x(48f), y = y(56f)),
        )
        Text(
            "Suspeito",
            color = Color.White,
            fontSize = fixedSp(12f),
            lineHeight = fixedSp(14f),
            fontWeight = FontWeight.Black,
            modifier = Modifier.offset(x = x(382f), y = y(56f)),
        )
        Text(
            "Perigoso",
            color = Color.White,
            fontSize = fixedSp(12f),
            lineHeight = fixedSp(14f),
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = y(338f)),
        )
    }
}

@Composable
private fun DashboardDeviceInfoPanel(info: DashboardDeviceInfo, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        Text("CPU ${info.cpu}", color = Color.White.copy(alpha = 0.78f), fontSize = fixedSp(9.4f), lineHeight = fixedSp(10.8f), fontWeight = FontWeight.Black, maxLines = 1)
        Text("RAM ${info.ram}", color = Color.White.copy(alpha = 0.78f), fontSize = fixedSp(9.4f), lineHeight = fixedSp(10.8f), fontWeight = FontWeight.Black, maxLines = 1)
        Text("DISCO ${info.disk}", color = Color.White.copy(alpha = 0.78f), fontSize = fixedSp(9.4f), lineHeight = fixedSp(10.8f), fontWeight = FontWeight.Black, maxLines = 1)
    }
}

private data class DashboardDeviceInfo(
    val ram: String,
    val cpu: String,
    val disk: String,
)

private data class CpuSnapshot(
    val idle: Long,
    val total: Long,
)

private fun dashboardDeviceInfo(context: Context, cpuUsage: Int?): DashboardDeviceInfo {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val memoryInfo = ActivityManager.MemoryInfo()
    activityManager?.getMemoryInfo(memoryInfo)
    val ram = if (memoryInfo.totalMem > 0L) {
        val used = memoryInfo.totalMem - memoryInfo.availMem
        "${formatGb(used)}/${formatGb(memoryInfo.totalMem)} GB (${percentOfBytes(used, memoryInfo.totalMem)}%)"
    } else {
        "--"
    }
    val cpu = "${cpuUsage?.let { "$it%" } ?: "--"} ${readCpuFrequencyGhz()}"
    val statFs = StatFs(context.filesDir.absolutePath)
    val totalDisk = statFs.totalBytes
    val usedDisk = totalDisk - statFs.availableBytes
    val disk = if (totalDisk > 0L) {
        "${formatGb(usedDisk)}/${formatGb(totalDisk)} GB (${percentOfBytes(usedDisk, totalDisk)}%)"
    } else {
        "--"
    }
    return DashboardDeviceInfo(ram = ram, cpu = cpu, disk = disk)
}

private fun readCpuSnapshot(): CpuSnapshot? {
    return runCatching {
        val parts = File("/proc/stat").readLines().first { it.startsWith("cpu ") }
            .trim()
            .split(Regex("\\s+"))
            .drop(1)
            .map { it.toLongOrNull() ?: 0L }
        val idle = parts.getOrElse(3) { 0L } + parts.getOrElse(4) { 0L }
        CpuSnapshot(idle = idle, total = parts.sum())
    }.getOrNull()
}

private fun cpuUsagePercent(previous: CpuSnapshot?, current: CpuSnapshot?): Int? {
    if (previous == null || current == null) return null
    val totalDelta = current.total - previous.total
    val idleDelta = current.idle - previous.idle
    if (totalDelta <= 0L) return null
    return (((totalDelta - idleDelta) * 100f) / totalDelta).toInt().coerceIn(0, 100)
}

private fun readCpuFrequencyGhz(): String {
    val processors = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
    val khz = (0 until processors).mapNotNull { index ->
        listOf(
            "/sys/devices/system/cpu/cpu$index/cpufreq/scaling_cur_freq",
            "/sys/devices/system/cpu/cpu$index/cpufreq/cpuinfo_cur_freq",
        ).firstNotNullOfOrNull { path ->
            runCatching { File(path).readText().trim().toLongOrNull() }.getOrNull()
        }
    }.maxOrNull()
    return if (khz != null && khz > 0L) {
        "${String.format(Locale.US, "%.2f", khz / 1_000_000f)}GHz"
    } else {
        "${processors}C"
    }
}

private fun formatGb(bytes: Long): String {
    val gb = bytes / 1024f / 1024f / 1024f
    val text = if (gb >= 10f && kotlin.math.abs(gb - gb.toInt()) < 0.08f) {
        gb.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", gb)
    }
    return text.replace('.', ',')
}

private fun percentOfBytes(used: Long, total: Long): Int {
    return if (total <= 0L) 0 else ((used * 100f) / total).toInt().coerceIn(0, 100)
}

@Composable
private fun DesignerStatsCard(stats: DesignerStats, modifier: Modifier = Modifier) {
    DesignerBlackCard(modifier = modifier) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            fun x(referencePx: Float) = maxWidth * (referencePx / 500f)
            fun y(referencePx: Float) = maxHeight * (referencePx / 191f)
            val cardIconSize = 20.dp
            val iconY = (maxHeight - cardIconSize) * 0.5f

            Icon(
                Icons.Outlined.InsertChartOutlined,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .offset(x = x(22f), y = iconY)
                    .size(cardIconSize),
            )
            DesignerStatBar("Seguros", stats.safeCount, stats.maxStatusCount, x(78f), y(44f), x(205f), x(166f))
            DesignerStatBar("Suspeito", stats.suspiciousCount, stats.maxStatusCount, x(78f), y(89f), x(205f), x(166f))
            DesignerStatBar("Perigoso", stats.dangerousCount, stats.maxStatusCount, x(78f), y(134f), x(205f), x(166f))
        }
    }
}

@Composable
private fun DesignerStatBar(
    label: String,
    count: Int,
    maxCount: Int,
    labelX: androidx.compose.ui.unit.Dp,
    y: androidx.compose.ui.unit.Dp,
    barX: androidx.compose.ui.unit.Dp,
    maxBarWidth: androidx.compose.ui.unit.Dp,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val safeMax = maxCount.coerceAtLeast(1)
        val barWidth = maxBarWidth * (count.coerceAtLeast(0) / safeMax.toFloat())
        Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = fixedSp(14f), lineHeight = fixedSp(16f), maxLines = 1, modifier = Modifier.offset(x = labelX, y = y))
        Box(
            modifier = Modifier
                .offset(x = barX, y = y + 5.dp)
                .width(barWidth)
                .height(6.dp)
                .background(Color.White),
        )
        Text("${count.coerceAtLeast(0)}X", color = Color.White.copy(alpha = 0.90f), fontWeight = FontWeight.SemiBold, fontSize = fixedSp(13f), lineHeight = fixedSp(15f), maxLines = 1, modifier = Modifier.offset(x = barX + barWidth + 13.dp, y = y))
    }
}

@Composable
private fun DesignerFrequencyCard(items: List<Pair<String, Int>>, modifier: Modifier = Modifier) {
    val rows = frequencyRows(items)
    DesignerBlackCard(modifier = modifier) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            fun x(referencePx: Float) = maxWidth * (referencePx / 500f)
            fun y(referencePx: Float) = maxHeight * (referencePx / 192f)
            val cardIconSize = 20.dp
            val iconY = (maxHeight - cardIconSize) * 0.5f

            Icon(
                Icons.Outlined.History,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .offset(x = x(22f), y = iconY)
                    .size(cardIconSize),
            )
            rows.forEachIndexed { index, (host, count) ->
                val rowY = when (index) {
                    0 -> 48f
                    1 -> 89f
                    else -> 130f
                }
                Row(
                    modifier = Modifier
                        .width(x(390f))
                        .offset(x = x(78f), y = y(rowY)),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        host,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = fixedSp(14f),
                        lineHeight = fixedSp(16f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Outlined.TrendingFlat,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.82f),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        "${count}X",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = fixedSp(14f),
                        lineHeight = fixedSp(16f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun frequencyRows(items: List<Pair<String, Int>>): List<Pair<String, Int>> {
    val fallback = listOf(
        "Dvepro.co" to 0,
        "Developer.android.com" to 0,
        "Dominio nao identificado" to 0,
    )
    return (items + fallback)
        .distinctBy { it.first.lowercase() }
        .take(3)
}

@Composable
private fun DesignerWeeklyChart(
    analyzed: List<Int>,
    blocked: List<Int>,
    safeCount: Int,
    dangerousCount: Int,
    modifier: Modifier = Modifier,
) {
    val analyzedColor = if (safeCount > dangerousCount) Color(0xFF22F29A) else Color(0xFF16735F)
    val blockedColor = if (dangerousCount > safeCount) Color(0xFFFF3B3B) else Color(0xFFE03232)
    DesignerBlackCard(modifier = modifier) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            fun x(referencePx: Float) = maxWidth * (referencePx / 500f)
            fun y(referencePx: Float) = maxHeight * (referencePx / 192f)
            val cardIconSize = 20.dp
            val legendCenterX = maxWidth * 0.52f

            Icon(
                Icons.Outlined.Timeline,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .offset(x = x(22f), y = y(86f))
                    .size(cardIconSize + 1.dp),
            )
            Icon(
                Icons.Outlined.Visibility,
                contentDescription = null,
                tint = analyzedColor,
                modifier = Modifier
                    .offset(x = legendCenterX - 24.dp, y = y(18f))
                    .size(17.dp),
            )
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = blockedColor.copy(alpha = 0.60f),
                modifier = Modifier
                    .offset(x = legendCenterX + 7.dp, y = y(18f))
                    .size(17.dp),
            )
            Canvas(
                modifier = Modifier
                    .fillMaxSize(),
            ) {
                val maxValue = (analyzed + blocked).maxOrNull()?.coerceAtLeast(1) ?: 1
                val scaleX = size.width / 500f
                val scaleY = size.height / 192f
                fun px(value: Float) = value * scaleX
                fun py(value: Float) = value * scaleY
                val lineStart = px(78f)
                val lineEnd = px(482f)
                val lineWidth = (lineEnd - lineStart).coerceAtLeast(1f)
                val chartTop = py(58f)
                val chartBottom = py(162f)
                val chartHeight = (chartBottom - chartTop).coerceAtLeast(1f)
                fun point(index: Int, value: Int): androidx.compose.ui.geometry.Offset {
                    val x = if (analyzed.size <= 1) lineStart else lineStart + index * (lineWidth / (analyzed.size - 1))
                    val y = chartBottom - (value / maxValue.toFloat()) * chartHeight
                    return androidx.compose.ui.geometry.Offset(x, y)
                }
                drawWeeklyPath(blocked, blockedColor, 0.75.dp.toPx(), 1.dp.toPx()) { index, value -> point(index, value) }
                drawWeeklyPath(analyzed, analyzedColor, 0.75.dp.toPx(), 1.dp.toPx()) { index, value -> point(index, value) }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWeeklyPath(
    values: List<Int>,
    color: Color,
    strokeWidth: Float,
    pointRadius: Float,
    pointFor: (Int, Int) -> androidx.compose.ui.geometry.Offset,
) {
    if (values.isEmpty()) return
    val path = Path()
    values.forEachIndexed { index, value ->
        val point = pointFor(index, value)
        if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        drawCircle(color, radius = pointRadius, center = point)
    }
    drawPath(path = path, color = color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
}

@Composable
private fun DesignerBlackCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF080808)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.08f)),
            )
        }
    }
}

private data class DesignerStats(
    val centerScore: Int,
    val safeCount: Int,
    val suspiciousCount: Int,
    val dangerousCount: Int,
    val maxStatusCount: Int,
    val frequency: List<Pair<String, Int>>,
    val weeklyAnalyzed: List<Int>,
    val weeklyBlocked: List<Int>,
)

private fun designerStats(history: List<AnalysisResult>): DesignerStats {
    val total = history.sumOf { it.realOccurrenceCount() }
    val safeCount = history.sumOf { if (it.level == RiskLevel.Safe) it.realOccurrenceCount() else 0 }
    val suspiciousCount = history.sumOf { if (it.level == RiskLevel.Suspicious) it.realOccurrenceCount() else 0 }
    val dangerousCount = history.sumOf { if (it.level == RiskLevel.Dangerous) it.realOccurrenceCount() else 0 }
    val frequency = history
        .groupBy { it.host.ifBlank { "Dominio nao identificado" } }
        .map { (host, items) -> host to items.sumOf { it.realOccurrenceCount() } }
        .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first.lowercase() })
        .take(3)
    val maxStatusCount = maxOf(safeCount, suspiciousCount, dangerousCount, 1)

    return DesignerStats(
        centerScore = percentOf(safeCount, total),
        safeCount = safeCount,
        suspiciousCount = suspiciousCount,
        dangerousCount = dangerousCount,
        maxStatusCount = maxStatusCount,
        frequency = frequency,
        weeklyAnalyzed = weeklyCounts(history) { true },
        weeklyBlocked = weeklyCounts(history) { it.level == RiskLevel.Dangerous || it.decision == "Bloqueado" || it.decision == "Cancelado" },
    )
}

private fun percentOf(count: Int, total: Int): Int {
    return if (total <= 0) 0 else ((count * 100) + total / 2) / total
}

private fun AnalysisResult.realOccurrenceCount(): Int {
    return eventTimes.filter { it > 0L }.size.takeIf { it > 0 } ?: occurrences.coerceAtLeast(1)
}

@Composable
private fun OnboardingCard(
    protectionVisible: Boolean,
    browserConfigured: Boolean,
    accessibilityConfigured: Boolean,
    vpnEnabled: Boolean,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Checklist inicial", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            ChecklistRow("1. Ativar protecao visivel", protectionVisible)
            ChecklistRow("2. Definir SafeLink como navegador seguro", browserConfigured)
            ChecklistRow("3. Ativar monitoramento de chats", accessibilityConfigured)
            ChecklistRow("4. Ativar VPN local", vpnEnabled)
        }
    }
}

@Composable
private fun ChecklistRow(label: String, checked: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            if (checked) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        )
        Text(label)
    }
}

@Composable
private fun HowToTestCard(onAnalyze: (String) -> Unit) {
    val examples = listOf(
        "Seguro" to "https://developer.android.com/privacy-and-security",
        "Suspeito" to "https://bit.ly/teste-safelink",
        "Perigoso" to "http://login-banco-premio-verificar-senha.com",
    )
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Como testar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            examples.forEach { (label, url) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(label, fontWeight = FontWeight.SemiBold)
                        Text(url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(onClick = { onAnalyze(url) }, shape = RoundedCornerShape(8.dp)) {
                        Text("Testar")
                    }
                }
            }
        }
    }
}

@Composable
private fun CentralProtectionCard(
    protectionVisible: Boolean,
    browserConfigured: Boolean,
    accessibilityConfigured: Boolean,
    vpnEnabled: Boolean,
    strictMode: Boolean,
) {
    val active = listOf(protectionVisible, browserConfigured, accessibilityConfigured, vpnEnabled).count { it }
    val title = when {
        active == 4 && strictMode -> "Protegido em modo maximo"
        active == 4 -> "Protegido"
        active >= 2 -> "Atencao"
        else -> "Desativado"
    }
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column {
                    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("$active de 4 camadas ativas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                }
            }
            Text(
                "Modo rigido: ${if (strictMode) "ativo" else "inativo"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )
        }
    }
}

@Composable
private fun Header(history: List<AnalysisResult>, allLayersActive: Boolean, activeLayers: Int) {
    val blocked = history.count { it.level == RiskLevel.Dangerous }
    val status = if (allLayersActive) "Protecao completa" else "Faltam ${4 - activeLayers} etapas"
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("SafeLink", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text(
            "$status contra links suspeitos antes da abertura.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MetricCard("Analises", history.size.toString(), Icons.Outlined.History, Modifier.weight(1f))
            MetricCard("Bloqueios", blocked.toString(), Icons.Outlined.Security, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ManualAnalyzerCard(
    url: String,
    onUrlChange: (String) -> Unit,
    onAnalyze: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Analisar link manualmente", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = url,
                    onValueChange = onUrlChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Cole um link") },
                )
                Button(onClick = onAnalyze, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Outlined.Send, contentDescription = null)
                }
                OutlinedButton(onClick = onAnalyze, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
                }
            }
            Text(
                "Para QR Code, leia o codigo com a camera e cole ou compartilhe o link aqui antes de abrir.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            )
        }
    }
}

@Composable
private fun OptionsCard(
    strictMode: Boolean,
    silentMode: Boolean,
    onStrictModeChange: (Boolean) -> Unit,
    onSilentModeChange: (Boolean) -> Unit,
    blockShorteners: Boolean,
    blockNoHttps: Boolean,
    blockIpDomains: Boolean,
    onBlockShortenersChange: (Boolean) -> Unit,
    onBlockNoHttpsChange: (Boolean) -> Unit,
    onBlockIpDomainsChange: (Boolean) -> Unit,
    trustedCount: Int,
    blockedCount: Int,
    onShareHistory: () -> Unit,
    onShareReport: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Acoes e politicas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            PolicyRow("Modo rigido", "Links perigosos nao podem ser abertos.", strictMode, onStrictModeChange)
            PolicyRow("Modo silencioso", "A VPN bloqueia sem repetir notificacoes.", silentMode, onSilentModeChange)
            PolicyRow("Bloquear encurtadores", "Bloqueia dominios como bit.ly e tinyurl.", blockShorteners, onBlockShortenersChange)
            PolicyRow("Bloquear sem HTTPS", "Bloqueia links que comecam com http://.", blockNoHttps, onBlockNoHttpsChange)
            PolicyRow("Bloquear IP direto", "Bloqueia destinos com endereco numerico.", blockIpDomains, onBlockIpDomainsChange)
            Text(
                "Confiaveis: $trustedCount | Bloqueados: $blockedCount",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onShareHistory, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                    Text("Exportar")
                }
                OutlinedButton(onClick = onShareReport, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                    Text("Relatorio")
                }
            }
        }
    }
}

@Composable
private fun ReputationCard(
    history: List<AnalysisResult>,
    trustedDomains: List<String>,
    blockedDomains: List<String>,
) {
    val mostSeen = history.groupingBy { it.host }.eachCount().entries.sortedByDescending { it.value }.take(3)
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Reputacao local", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (mostSeen.isEmpty()) {
                Text("Sem dados suficientes ainda.", style = MaterialTheme.typography.bodySmall)
            } else {
                mostSeen.forEach { Text("${it.key}: ${it.value} vezes", maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            Text("Mais confiados: ${trustedDomains.take(3).joinToString().ifBlank { "nenhum" }}", style = MaterialTheme.typography.bodySmall)
            Text("Mais bloqueados: ${blockedDomains.take(3).joinToString().ifBlank { "nenhum" }}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun StatisticsCard(history: List<AnalysisResult>) {
    val safe = history.count { it.level == RiskLevel.Safe }
    val suspicious = history.count { it.level == RiskLevel.Suspicious }
    val dangerous = history.count { it.level == RiskLevel.Dangerous }
    val total = history.size.coerceAtLeast(1)
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Estatisticas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            StatBar("Seguro", safe, total, Color(0xFF16735F))
            StatBar("Suspeito", suspicious, total, Color(0xFFB56A00))
            StatBar("Perigoso", dangerous, total, Color(0xFFB3261E))
        }
    }
}

@Composable
private fun StatBar(label: String, value: Int, total: Int, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(0.9f), style = MaterialTheme.typography.bodySmall)
        Box(
            modifier = Modifier
                .weight(1.8f)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(20.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(value / total.toFloat())
                    .padding(vertical = 5.dp)
                    .background(color, RoundedCornerShape(20.dp)),
            )
        }
        Text(value.toString(), modifier = Modifier.weight(0.35f), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun BlockQueueCard(blockedEvents: List<String>) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Fila da VPN", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (blockedEvents.isEmpty()) {
                Text("Nenhum bloqueio recente", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f))
            } else {
                blockedEvents.take(5).forEach {
                    Text(it.substringAfter("|", it), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun BackupCard(
    backupText: String,
    onBackupTextChange: (String) -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Backup validado", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = backupText,
                onValueChange = onBackupTextChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Cole o backup SafeLink") },
                minLines = 2,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onExport, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                    Text("Exportar")
                }
                Button(onClick = onImport, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                    Text("Importar")
                }
            }
        }
    }
}

@Composable
private fun ManagedListsCard(
    trustedDomains: List<String>,
    blockedDomains: List<String>,
    domain: String,
    onDomainChange: (String) -> Unit,
    onTrustDomain: () -> Unit,
    onBlockDomain: () -> Unit,
    onRemove: (String) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Listas de dominios", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = domain,
                onValueChange = onDomainChange,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Adicionar dominio") },
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onTrustDomain, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                    Text("Confiar")
                }
                OutlinedButton(onClick = onBlockDomain, shape = RoundedCornerShape(8.dp), modifier = Modifier.weight(1f)) {
                    Text("Bloquear")
                }
            }
            DomainList("Confiaveis", trustedDomains, onRemove)
            DomainList("Bloqueados", blockedDomains, onRemove)
        }
    }
}

@Composable
private fun DomainList(title: String, items: List<String>, onRemove: (String) -> Unit) {
    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    if (items.isEmpty()) {
        Text("Nenhum dominio", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f))
    } else {
        items.take(5).forEach { domain ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(domain, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                OutlinedButton(onClick = { onRemove(domain) }, shape = RoundedCornerShape(8.dp)) {
                    Text("Remover")
                }
            }
        }
    }
}

@Composable
private fun PolicyRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
    }
}

@Composable
private fun MetricCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column {
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
            }
        }
    }
}

@Composable
private fun ProtectionSummary(
    protectionVisible: Boolean,
    browserConfigured: Boolean,
    accessibilityConfigured: Boolean,
    vpnEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onStartProtection: () -> Unit,
    onRequestBrowserRole: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAppDetails: () -> Unit,
    onStartVpn: () -> Unit,
    onStopVpn: () -> Unit,
    showAdvancedGuards: Boolean,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Camadas de protecao", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Configure as defesas principais.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                }
                Switch(checked = protectionVisible, onCheckedChange = onEnabledChange)
            }

            ProtectionLayer(
                icon = Icons.Outlined.NotificationsActive,
                title = "Protecao visivel",
                subtitle = "Mantem o SafeLink ativo e com alertas.",
                active = protectionVisible,
                action = "Ativar",
                onAction = onStartProtection,
            )
            ProtectionLayer(
                icon = Icons.Outlined.Public,
                title = "Navegador seguro",
                subtitle = "Faz links externos passarem pelo SafeLink.",
                active = browserConfigured,
                action = "Definir",
                onAction = onRequestBrowserRole,
            )
            if (showAdvancedGuards) {
                ProtectionLayer(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    title = "Monitoramento de chats",
                    subtitle = "Detecta links visiveis com acessibilidade.",
                    active = accessibilityConfigured,
                    action = "Abrir",
                    onAction = onOpenAccessibilitySettings,
                    secondaryAction = "Liberar",
                    onSecondaryAction = onOpenAppDetails,
                )
                ProtectionLayer(
                    icon = Icons.Outlined.VpnKey,
                    title = "VPN local",
                    subtitle = "Bloqueia dominios suspeitos por DNS.",
                    active = vpnEnabled,
                    action = "Ativar",
                    onAction = onStartVpn,
                    secondaryAction = "Parar",
                    onSecondaryAction = onStopVpn,
                )
            }
        }
    }
}

@Composable
private fun ProtectionLayer(
    icon: ImageVector,
    title: String,
    subtitle: String,
    active: Boolean,
    action: String,
    onAction: () -> Unit,
    secondaryAction: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    val statusColor = if (active) Color(0xFF16735F) else Color(0xFF6F7772)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (active) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(16.dp),
                )
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), horizontalAlignment = Alignment.End) {
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(8.dp),
                contentPadding = ButtonDefaults.ContentPadding,
            ) {
                Text(action)
            }
            if (secondaryAction != null && onSecondaryAction != null) {
                OutlinedButton(onClick = onSecondaryAction, shape = RoundedCornerShape(8.dp)) {
                    Text(secondaryAction)
                }
            }
        }
    }
}

@Composable
private fun HistorySection(
    history: List<AnalysisResult>,
    selectedFilter: RiskLevel?,
    onFilterChange: (RiskLevel?) -> Unit,
    search: String,
    onSearchChange: (String) -> Unit,
    onOpenReview: (AnalysisResult) -> Unit,
    onDelete: (AnalysisResult) -> Unit,
    onTrust: (AnalysisResult) -> Unit,
    onBlock: (AnalysisResult) -> Unit,
    onCopy: (AnalysisResult) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterRow(selectedFilter, onFilterChange)
        HistorySearchBar(
            value = search,
            onValueChange = onSearchChange,
        )
        if (history.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface,
            ) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Nenhum link analisado ainda")
                }
            }
        } else {
            history.groupBy { historyBucket(it.checkedAt) }.forEach { (_, items) ->
                items.forEach { item ->
                    key(item.url) {
                        HistoryItem(
                            result = item,
                            onOpenReview = { onOpenReview(item) },
                            onDelete = { onDelete(item) },
                            onTrust = { onTrust(item) },
                            onBlock = { onBlock(item) },
                            onCopy = { onCopy(item) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(selected: RiskLevel?, onChange: (RiskLevel?) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        FilterButton("Todos", selected == null, { onChange(null) }, Modifier.weight(1f))
        FilterButton("Seguro", selected == RiskLevel.Safe, { onChange(RiskLevel.Safe) }, Modifier.weight(1f))
        FilterButton("Suspeito", selected == RiskLevel.Suspicious, { onChange(RiskLevel.Suspicious) }, Modifier.weight(1f))
        FilterButton("Perigo", selected == RiskLevel.Dangerous, { onChange(RiskLevel.Dangerous) }, Modifier.weight(1f))
    }
}

@Composable
private fun FilterButton(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = if (selected) {
        ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.16f), contentColor = Color.White)
    } else {
        ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
    }
    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            colors = colors,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
        ) {
            Text(label, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = fixedSp(10f), lineHeight = fixedSp(12f))
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(8.dp),
            colors = colors,
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
        ) {
            Text(label, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = fixedSp(10f), lineHeight = fixedSp(12f))
        }
    }
}

@Composable
private fun HistorySearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .background(Color(0xFF080808), RoundedCornerShape(100.dp))
            .border(0.5.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(100.dp))
            .padding(start = 16.dp, end = 10.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.72f), modifier = Modifier.size(20.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(
                    color = Color.White,
                    fontSize = fixedSp(14f),
                    lineHeight = fixedSp(16f),
                    fontWeight = FontWeight.Normal,
                ),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isBlank()) {
                            Text(
                                "Buscar por dominio ou link...",
                                color = Color.White.copy(alpha = 0.46f),
                                fontSize = fixedSp(14f),
                                lineHeight = fixedSp(16f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                },
                modifier = Modifier.weight(1f),
            )
            if (value.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(100.dp))
                        .clickable { onValueChange("") },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun fixedSp(value: Float): TextUnit {
    val fontScale = LocalDensity.current.fontScale
    return (value / fontScale).sp
}

@Composable
private fun RiskTabs(history: List<AnalysisResult>) {
    val dangerous = history.count { it.level == RiskLevel.Dangerous }
    val suspicious = history.count { it.level == RiskLevel.Suspicious }
    Text(
        text = "$dangerous altos / $suspicious suspeitos",
        style = MaterialTheme.typography.labelMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
    )
}

private fun historyBucket(time: Long): String = when {
    isSameDay(time, System.currentTimeMillis()) -> "Hoje"
    isYesterday(time) -> "Ontem"
    time >= System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L -> "Esta semana"
    else -> "Mais antigo"
}

private fun weeklyCounts(
    history: List<AnalysisResult>,
    predicate: (AnalysisResult) -> Boolean,
): List<Int> {
    val today = Calendar.getInstance()
    val start = Calendar.getInstance().apply {
        timeInMillis = today.timeInMillis
        add(Calendar.DAY_OF_YEAR, -6)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return List(7) { index ->
        val day = Calendar.getInstance().apply {
            timeInMillis = start.timeInMillis
            add(Calendar.DAY_OF_YEAR, index)
        }
        history.sumOf { item ->
            if (!predicate(item)) {
                0
            } else {
                val events = item.eventTimes.filter { it > 0L }.ifEmpty {
                    List(item.occurrences.coerceAtLeast(1)) { item.checkedAt }
                }
                events.count { eventTime -> isSameDay(eventTime, day.timeInMillis) }
            }
        }
    }
}

private fun isYesterday(time: Long): Boolean {
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }.timeInMillis
    return isSameDay(time, yesterday)
}

private fun isSameDay(left: Long, right: Long): Boolean {
    val a = Calendar.getInstance().apply { timeInMillis = left }
    val b = Calendar.getInstance().apply { timeInMillis = right }
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}
