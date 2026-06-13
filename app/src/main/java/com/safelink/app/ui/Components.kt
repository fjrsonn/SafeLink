package com.safelink.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.safelink.app.model.AnalysisResult
import com.safelink.app.model.RiskLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RiskPill(level: RiskLevel, modifier: Modifier = Modifier) {
    val (color, icon) = riskVisual(level)
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color)
        Text(level.label, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryItem(
    result: AnalysisResult,
    onOpenReview: () -> Unit,
    onDelete: () -> Unit,
    onTrust: () -> Unit,
    onBlock: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    val dismissState = rememberSwipeToDismissBoxState(
        positionalThreshold = { distance -> distance * 0.34f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    onOpenReview()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier.fillMaxWidth(),
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            HistorySwipeBackground(dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
        },
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF080808)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Row(Modifier.height(IntrinsicSize.Min)) {
                Box(
                    Modifier
                        .width(5.dp)
                        .fillMaxHeight()
                        .background(riskVisual(result.level).first),
                )
                Column(Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Link, contentDescription = null, tint = Color.White)
                            Text(
                                text = result.host,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        RiskPill(result.level)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Pontuacao de risco: ${result.score}/100",
                        style = MaterialTheme.typography.labelMedium,
                        color = riskVisual(result.level).first,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = result.url,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        historyBadges(result).take(4).forEach { badge ->
                            BadgeChip(badge)
                        }
                    }
                    Text(
                        text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(result.checkedAt)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        QuickActionButton("Confiar", onTrust, Modifier.weight(1f))
                        QuickActionButton("Bloquear", onBlock, Modifier.weight(1f))
                        QuickIconActionButton(
                            icon = Icons.Outlined.ContentCopy,
                            contentDescription = "Copiar",
                            onClick = {
                                clipboard.setText(AnnotatedString(result.url))
                                onCopy()
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BadgeChip(label: String) {
    Box(
        modifier = Modifier
            .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
    }
}

@Composable
private fun QuickActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
    ) {
        Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun QuickIconActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(32.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        contentPadding = PaddingValues(0.dp),
    ) {
        Icon(icon, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(17.dp))
    }
}

private fun historyBadges(result: AnalysisResult): List<String> {
    val source = when {
        result.reasons.any { it.contains("VPN", ignoreCase = true) } -> "VPN"
        result.decision == "Analisado" -> "Manual"
        else -> "WhatsApp"
    }
    return buildList {
        add(source)
        if (result.reasons.any { it.contains("[Politica]") }) add("Politica")
        if (result.reasons.any { it.contains("lista", ignoreCase = true) }) add("Lista")
        add(result.decision)
    }
}

@Composable
private fun HistorySwipeBackground(isDeleting: Boolean) {
    val color = if (isDeleting) Color(0xFFB3261E) else Color(0xFF16735F)
    val icon = if (isDeleting) Icons.Outlined.DeleteOutline else Icons.Outlined.OpenInNew
    val label = if (isDeleting) "Excluir" else "Revisar"
    val alignment = if (isDeleting) Alignment.CenterEnd else Alignment.CenterStart

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color, RoundedCornerShape(8.dp))
            .padding(horizontal = 22.dp),
        contentAlignment = alignment,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            if (!isDeleting) {
                Icon(icon, contentDescription = null, tint = Color.White)
                Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
            } else {
                Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
                Icon(icon, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
fun riskVisual(level: RiskLevel): Pair<Color, ImageVector> = when (level) {
    RiskLevel.Safe -> Color(0xFF16735F) to Icons.Outlined.CheckCircle
    RiskLevel.Suspicious -> Color(0xFFB56A00) to Icons.Outlined.WarningAmber
    RiskLevel.Dangerous -> Color(0xFFB3261E) to Icons.Outlined.ErrorOutline
}
