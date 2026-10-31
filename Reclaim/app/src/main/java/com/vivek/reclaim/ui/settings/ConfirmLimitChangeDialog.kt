package com.vivek.reclaim.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vivek.reclaim.domain.LimitLock
import com.vivek.reclaim.domain.model.DailyLimit
import com.vivek.reclaim.ui.theme.ReclaimTheme
import com.vivek.reclaim.ui.util.formatLongDate
import kotlin.math.abs

/** Shown before a limit change is saved (FR-26): what changes, and when the next change is allowed. */
@Composable
fun ConfirmLimitChangeDialog(
    oldMinutes: Int,
    newMinutes: Int,
    nextChangeAtMillis: Long,
    onKeep: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onKeep) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier.size(56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(28.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Change your daily limit?",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Your limit locks for ${LimitLock.LOCK_DAYS} days after this change. Are you sure you want to adjust your mindful allowance today?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(18.dp))
                InfoRow(icon = Icons.Outlined.HourglassBottom, label = "New daily limit") {
                    Text(
                        DailyLimit.format(newMinutes),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        deltaLabel(newMinutes - oldMinutes),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(10.dp))
                InfoRow(icon = Icons.Outlined.CalendarMonth, label = "Next change allowed") {
                    Text(
                        formatLongDate(nextChangeAtMillis),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(Modifier.height(22.dp))
                FilledTonalButton(
                    onClick = onKeep,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) { Text("Keep current limit", style = MaterialTheme.typography.titleMedium) }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) {
                    Icon(Icons.Outlined.Shield, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Yes, change it", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(verticalAlignment = Alignment.CenterVertically) { value() }
    }
}

/** "(-15m)", "(+30m)", "(+1h 15m)". */
internal fun deltaLabel(deltaMinutes: Int): String {
    val sign = if (deltaMinutes < 0) "−" else "+"
    return "($sign${DailyLimit.formatCompact(abs(deltaMinutes))})"
}

@Preview(showBackground = true)
@Composable
private fun ConfirmLimitChangePreview() {
    ReclaimTheme(darkTheme = false) {
        ConfirmLimitChangeDialog(oldMinutes = 120, newMinutes = 105, nextChangeAtMillis = System.currentTimeMillis(), onKeep = {}, onConfirm = {})
    }
}
