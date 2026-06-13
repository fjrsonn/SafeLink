package com.safelink.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safelink.app.model.AnalysisResult
import com.safelink.app.model.RiskLevel
import com.safelink.app.ui.RiskPill
import com.safelink.app.ui.SafeLinkTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val result = AnalysisResult(
            url = intent.getStringExtra(EXTRA_URL).orEmpty(),
            host = intent.getStringExtra(EXTRA_HOST).orEmpty(),
            level = RiskLevel.valueOf(intent.getStringExtra(EXTRA_LEVEL) ?: RiskLevel.Safe.name),
            score = intent.getIntExtra(EXTRA_SCORE, 0),
            decision = intent.getStringExtra(EXTRA_DECISION).orEmpty().ifBlank { "Analisado" },
            reasons = intent.getStringArrayListExtra(EXTRA_REASONS).orEmpty(),
            checkedAt = intent.getLongExtra(EXTRA_CHECKED_AT, System.currentTimeMillis()),
        )
        setContent {
            SafeLinkTheme {
                HistoryDetailScreen(result)
            }
        }
    }

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_HOST = "host"
        const val EXTRA_LEVEL = "level"
        const val EXTRA_SCORE = "score"
        const val EXTRA_DECISION = "decision"
        const val EXTRA_REASONS = "reasons"
        const val EXTRA_CHECKED_AT = "checkedAt"
    }
}

@Composable
private fun HistoryDetailScreen(result: AnalysisResult) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Detalhe do historico",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Normal,
            color = Color.White,
        )
        Card(
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF080808)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    ScoreRing(result.score, result.level)
                    Column(Modifier.weight(1f)) {
                        RiskPill(result.level)
                        Text(result.host, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Normal, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Risco ${result.score}/100", color = Color.White.copy(alpha = 0.72f), fontWeight = FontWeight.Normal)
                    }
                }
                Text(result.url, color = Color.White.copy(alpha = 0.72f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("Decisao: ${result.decision}", color = Color.White.copy(alpha = 0.84f))
                Text(
                    "Data: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(result.checkedAt))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.54f),
                )
            }
        }
        Card(
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF080808)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Linha do tempo", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Normal)
                Text("1. Analisado pelo SafeLink", color = Color.White.copy(alpha = 0.72f))
                Text("2. Classificado como ${result.level.label}", color = Color.White.copy(alpha = 0.72f))
                Text("3. Decisao registrada: ${result.decision}", color = Color.White.copy(alpha = 0.72f))
            }
        }
        Card(
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF080808)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Motivos da analise", style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.Normal)
                result.reasons.forEach { reason ->
                    Text("- $reason", color = Color.White.copy(alpha = 0.72f))
                }
            }
        }
    }
}

@Composable
private fun ScoreRing(score: Int, level: RiskLevel) {
    val color = when (level) {
        RiskLevel.Safe -> Color(0xFF16735F)
        RiskLevel.Suspicious -> Color(0xFFB56A00)
        RiskLevel.Dangerous -> Color(0xFFB3261E)
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(86.dp)) {
        CircularProgressIndicator(
            progress = score.coerceIn(0, 100) / 100f,
            modifier = Modifier.fillMaxSize(),
            color = color,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            strokeWidth = 6.dp,
        )
        Text(score.toString(), style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.Normal)
    }
}
