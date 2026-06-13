package com.safelink.app

import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safelink.app.model.AnalysisResult
import com.safelink.app.model.RiskLevel
import com.safelink.app.ui.SafeLinkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class LinkReviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        render(intent)
    }

    private fun render(sourceIntent: Intent) {
        val url = extractUrl(sourceIntent)
        val analyzed = safeLinkContainer.analyzer.analyze(url)
        val repository = safeLinkContainer.historyRepository
        val categorized = applyCategoryPolicies(analyzed, repository)
        val fromAccessibility = sourceIntent.getBooleanExtra(EXTRA_FROM_ACCESSIBILITY, false)
        if (fromAccessibility && (repository.isUrlResolved(url) || repository.isDomainResolved(categorized.host))) {
            finish()
            return
        }
        val result = when {
            repository.isTrusted(categorized.host) -> categorized.copy(
                level = RiskLevel.Safe,
                score = 0,
                reasons = listOf("O dominio esta na lista de confianca do usuario."),
            )
            repository.isBlocked(categorized.host) -> categorized.copy(
                level = RiskLevel.Dangerous,
                score = 100,
                reasons = listOf("O dominio esta na lista de bloqueio do usuario."),
            )
            else -> categorized
        }
        safeLinkContainer.historyRepository.save(result)
        setContent {
            SafeLinkTheme {
                LinkReviewScreen(
                    result = result,
                    strictMode = repository.getBoolean(com.safelink.app.data.HistoryRepository.MODE_STRICT),
                    resolveFinalUrl = { resolveFinalUrl(it) },
                    onOpen = { targetUrl ->
                        repository.updateDecision(result.url, "Aberto")
                        markReviewResolved(result)
                        safeLinkContainer.historyRepository.allowDomain(result.host)
                        cancelBlockNotification(result.host)
                        openInExternalBrowser(targetUrl)
                        finish()
                    },
                    onCopy = {
                        repository.updateDecision(result.url, "Copiado")
                        markReviewResolved(result)
                        cancelBlockNotification(result.host)
                        finish()
                    },
                    onCancel = {
                        repository.updateDecision(result.url, "Cancelado")
                        markReviewResolved(result)
                        cancelBlockNotification(result.host)
                        finish()
                    },
                )
            }
        }
    }

    private fun extractUrl(intent: Intent): String {
        intent.dataString?.let { return it }
        if (intent.action == Intent.ACTION_SEND) {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty()
            Regex("""https?://\S+|www\.\S+""").find(text)?.value?.let { return it }
            if (text.isNotBlank()) return text
        }
        return "https://example.com"
    }

    private fun applyCategoryPolicies(
        result: AnalysisResult,
        repository: com.safelink.app.data.HistoryRepository,
    ): AnalysisResult {
        val reasons = result.reasons.toMutableList()
        var forced = false
        if (repository.getBoolean(com.safelink.app.data.HistoryRepository.BLOCK_NO_HTTPS) && result.url.startsWith("http://")) {
            reasons += "[Politica] Links sem HTTPS estao bloqueados."
            forced = true
        }
        if (repository.getBoolean(com.safelink.app.data.HistoryRepository.BLOCK_SHORTENERS) && result.host in SHORTENERS) {
            reasons += "[Politica] Encurtadores estao bloqueados."
            forced = true
        }
        if (repository.getBoolean(com.safelink.app.data.HistoryRepository.BLOCK_IP_DOMAINS) && Regex("""\b\d{1,3}(\.\d{1,3}){3}\b""").containsMatchIn(result.host)) {
            reasons += "[Politica] Enderecos por IP direto estao bloqueados."
            forced = true
        }
        return if (forced) result.copy(level = RiskLevel.Dangerous, score = 100, reasons = reasons) else result
    }

    private fun openInExternalBrowser(url: String) {
        val baseIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        val matches = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                baseIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()),
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(baseIntent, PackageManager.MATCH_ALL)
        }
        val external = matches
            .filter { it.activityInfo.packageName != packageName }
            .sortedWith(
                compareByDescending<android.content.pm.ResolveInfo> {
                    KNOWN_BROWSER_PACKAGES.contains(it.activityInfo.packageName)
                }.thenBy { it.activityInfo.packageName },
            )
            .firstOrNull()
        val fallbackPackage = KNOWN_BROWSER_PACKAGES.firstOrNull { isPackageInstalled(it) }
        val targetPackage = external?.activityInfo?.packageName ?: fallbackPackage
        val intent = if (targetPackage == null) {
            Intent.createChooser(baseIntent, "Abrir link com")
        } else {
            Intent(baseIntent).apply {
                setPackage(targetPackage)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
        runCatching { startActivity(intent) }
            .onFailure {
                Toast.makeText(this, "Nao encontrei um navegador externo para abrir o link.", Toast.LENGTH_LONG).show()
            }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
        }.isSuccess
    }

    private fun cancelBlockNotification(host: String) {
        if (host.isBlank() || host == "dominio nao identificado") return
        getSystemService(NotificationManager::class.java)
            .cancel(BLOCK_NOTIFICATION_BASE_ID + (host.hashCode() and 0x0FFF))
    }

    private fun markReviewResolved(result: AnalysisResult) {
        safeLinkContainer.historyRepository.markDomainResolved(result.host)
        safeLinkContainer.historyRepository.markUrlResolved(result.url)
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private suspend fun resolveFinalUrl(url: String): String = withContext(Dispatchers.IO) {
        runCatching {
            var current = url
            repeat(5) {
                val connection = (URL(current).openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    connectTimeout = 2_500
                    readTimeout = 2_500
                    requestMethod = "HEAD"
                    setRequestProperty("User-Agent", "SafeLink/0.9")
                }
                val code = connection.responseCode
                val location = connection.getHeaderField("Location")
                connection.disconnect()
                if (code in 300..399 && !location.isNullOrBlank()) {
                    current = URL(URL(current), location).toString()
                } else {
                    return@runCatching current
                }
            }
            current
        }.getOrDefault(url)
    }

    companion object {
        const val EXTRA_FROM_ACCESSIBILITY = "com.safelink.app.extra.FROM_ACCESSIBILITY"
        private const val BLOCK_NOTIFICATION_BASE_ID = 9000
        private val SHORTENERS = setOf("bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd", "cutt.ly", "rebrand.ly", "s.id")
        private val KNOWN_BROWSER_PACKAGES = setOf(
            "com.android.chrome",
            "com.google.android.apps.chrome",
            "com.mi.globalbrowser",
            "com.android.browser",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "com.opera.browser",
            "com.brave.browser",
        )
    }
}

@Composable
private fun LinkReviewScreen(
    result: AnalysisResult,
    strictMode: Boolean,
    resolveFinalUrl: suspend (String) -> String,
    onOpen: (String) -> Unit,
    onCopy: () -> Unit,
    onCancel: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    val profile = remember(result.level) { reviewProfile(result.level) }
    var accepted by remember(result.url) { mutableStateOf(result.level == RiskLevel.Safe) }
    var remaining by remember(result.url) { mutableIntStateOf(DECISION_TIMEOUT_SECONDS) }
    var actionTaken by remember(result.url) { mutableStateOf(false) }
    var finalUrl by remember(result.url) { mutableStateOf(result.url) }
    var redirectStatus by remember(result.url) { mutableStateOf("Verificando redirecionamento...") }

    LaunchedEffect(result.url) {
        finalUrl = resolveFinalUrl(result.url)
        val finalHost = runCatching { Uri.parse(finalUrl).host.orEmpty() }.getOrDefault("")
        redirectStatus = if (finalHost.isBlank() || finalUrl == result.url) {
            "Destino final: ${result.host}"
        } else {
            "Redireciona para: $finalHost"
        }
    }

    LaunchedEffect(result.url) {
        remaining = DECISION_TIMEOUT_SECONDS
        while (remaining > 0) {
            delay(1_000L)
            remaining -= 1
        }
        if (!actionTaken) {
            actionTaken = true
            onCancel()
        }
    }

    val canProceed = (accepted || result.level == RiskLevel.Safe) &&
        !(strictMode && result.level == RiskLevel.Dangerous)
    val okLabel = "${profile.okText} ($remaining)"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(18.dp))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(92.dp)) {
            CircularProgressIndicator(
                progress = remaining / DECISION_TIMEOUT_SECONDS.toFloat(),
                modifier = Modifier.fillMaxSize(),
                color = profile.accent,
                trackColor = Color.White.copy(alpha = 0.12f),
                strokeWidth = 5.dp,
            )
            Icon(
                imageVector = profile.icon,
                contentDescription = null,
                tint = profile.accent,
                modifier = Modifier.size(58.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = profile.status,
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White.copy(alpha = 0.88f),
            fontWeight = FontWeight.Normal,
        )
        Text(
            text = "Risco ${result.score}/100",
            style = MaterialTheme.typography.titleMedium,
            color = profile.accent,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(22.dp))
        Text(
            text = profile.message(result),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.9f),
            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = result.url,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.68f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "Dominio: ${result.host}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.46f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = redirectStatus,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.72f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(18.dp))

        profile.items(result).take(2).forEach { item ->
            WarningCapability(item)
            Spacer(Modifier.height(14.dp))
        }

        if (result.reasons.isNotEmpty()) {
            Text(
                text = "Motivos encontrados:",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.88f),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            result.reasons.take(2).forEach { reason ->
                Text(
                    text = "- $reason",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.62f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        if (result.level != RiskLevel.Safe) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Checkbox(
                    checked = accepted,
                    onCheckedChange = { accepted = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = profile.accent,
                        uncheckedColor = Color.White.copy(alpha = 0.8f),
                        checkmarkColor = Color.White,
                    ),
                )
                Text(
                    text = "Estou ciente dos possiveis riscos, e assumo toda e qualquer consequencia voluntariamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.82f),
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.weight(1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = {
                    actionTaken = true
                    onCancel()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF101010)),
            ) {
                Text("Cancelar", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Normal)
            }
            Button(
                onClick = {
                    actionTaken = true
                    onOpen(finalUrl)
                },
                enabled = canProceed,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = profile.accent,
                    disabledContainerColor = profile.accent.copy(alpha = 0.42f),
                    disabledContentColor = Color.White.copy(alpha = 0.78f),
                ),
            ) {
                Text(okLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Normal)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Copiar link",
            color = Color.White.copy(alpha = 0.58f),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .padding(8.dp)
                .align(Alignment.CenterHorizontally)
                .clickable {
                    actionTaken = true
                    clipboard.setText(AnnotatedString(result.url))
                    onCopy()
                },
        )
    }
}

@Composable
private fun WarningCapability(item: CapabilityItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF080808), RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(item.icon, contentDescription = null, tint = Color.White.copy(alpha = 0.86f))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.52f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private data class ReviewProfile(
    val status: String,
    val accent: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val okText: String,
    val message: (AnalysisResult) -> String,
    val items: (AnalysisResult) -> List<CapabilityItem>,
)

private data class CapabilityItem(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val subtitle: String,
)

private fun reviewProfile(level: RiskLevel): ReviewProfile = when (level) {
    RiskLevel.Safe -> ReviewProfile(
        status = "Seguro",
        accent = Color(0xFF18A36F),
        icon = Icons.Outlined.CheckCircle,
        okText = "Abrir",
        message = { "\"SafeLink\" nao encontrou sinais fortes de golpe. Confirme se o dominio corresponde ao site que voce espera acessar." },
        items = {
            listOf(
                CapabilityItem(Icons.Outlined.Shield, "Conexao com baixo risco", "O dominio nao apresentou padroes comuns de fraude."),
                CapabilityItem(Icons.Outlined.Visibility, "Revisao concluida", "O link foi registrado no historico para consulta posterior."),
                CapabilityItem(Icons.Outlined.PlayCircleOutline, "Abertura controlada", "O SafeLink liberara o destino somente apos esta confirmacao."),
            )
        },
    )
    RiskLevel.Suspicious -> ReviewProfile(
        status = "Suspeito",
        accent = Color(0xFFE0A121),
        icon = Icons.Outlined.WarningAmber,
        okText = "OK",
        message = { "\"SafeLink\" encontrou sinais de atencao. Se continuar, confira dados, senhas e pagamentos antes de interagir." },
        items = {
            listOf(
                CapabilityItem(Icons.Outlined.Visibility, "Verificar o conteudo antes de confiar", "O link pode ser legitimo, mas possui caracteristicas incomuns."),
                CapabilityItem(Icons.Outlined.TouchApp, "Evitar acoes automaticas", "Nao preencha formularios nem autorize pagamentos sem confirmar a origem."),
                CapabilityItem(Icons.Outlined.Shield, "Prosseguir com cuidado", "Use o OK apenas se reconhecer o remetente e o dominio."),
            )
        },
    )
    RiskLevel.Dangerous -> ReviewProfile(
        status = "Perigo",
        accent = Color(0xFFFF2B2B),
        icon = Icons.Outlined.WarningAmber,
        okText = "OK",
        message = { "\"SafeLink\" classificou este link como perigoso. Informacoes privadas podem vazar e dados pessoais podem ser colocados em risco." },
        items = {
            listOf(
                CapabilityItem(Icons.Outlined.Visibility, "Ler dados sensiveis", "A pagina pode tentar capturar senhas, tokens, documentos ou dados bancarios."),
                CapabilityItem(Icons.Outlined.TouchApp, "Induzir acoes perigosas", "O conteudo pode simular marcas conhecidas e pressionar voce a agir rapido."),
                CapabilityItem(Icons.Outlined.PlayCircleOutline, "Abrir destino de alto risco", "O SafeLink recomenda cancelar e nao carregar este endereco."),
            )
        },
    )
}

private const val DECISION_TIMEOUT_SECONDS = 6
