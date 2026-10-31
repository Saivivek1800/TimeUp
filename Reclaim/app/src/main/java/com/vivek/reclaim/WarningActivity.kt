package com.vivek.reclaim

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivek.reclaim.domain.BlockDecision
import com.vivek.reclaim.domain.model.DailyLimit
import com.vivek.reclaim.domain.model.ThemeMode
import com.vivek.reclaim.ui.theme.ReclaimTheme
import com.vivek.reclaim.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * A small "almost out of time" pop-up over the app you are in. It appears once a day, a few minutes before the
 * daily limit runs out, and goes away with "Got it" or a tap outside. It never blocks anything itself.
 */
@AndroidEntryPoint
class WarningActivity : ComponentActivity() {
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val used = intent.getIntExtra(EXTRA_USED_MINUTES, 0)
        val limit = intent.getIntExtra(EXTRA_LIMIT_MINUTES, 0)
        setContent {
            val mode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            ReclaimTheme(darkTheme = dark) {
                WarningContent(usedMinutes = used, limitMinutes = limit, onDismiss = ::finish)
            }
        }
    }

    companion object {
        const val EXTRA_USED_MINUTES = "com.vivek.reclaim.extra.USED_MINUTES"
        const val EXTRA_LIMIT_MINUTES = "com.vivek.reclaim.extra.LIMIT_MINUTES"
    }
}

@Composable
private fun WarningContent(usedMinutes: Int, limitMinutes: Int, onDismiss: () -> Unit) {
    val leftMinutes = (limitMinutes - usedMinutes).coerceIn(1, BlockDecision.WARNING_LEAD_MILLIS.toInt() / 60_000)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                // Taps on the card itself must not dismiss it.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 6.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier.size(60.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Outlined.HourglassBottom,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    "Almost out of time",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    buildAnnotatedString {
                        append("You’ve used ")
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                            append(DailyLimit.formatCompact(usedMinutes))
                        }
                        append(" of your ${DailyLimit.formatCompact(limitMinutes)} daily limit, about $leftMinutes ${if (leftMinutes == 1) "minute" else "minutes"} left.")
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "After that, these apps open again tomorrow.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(22.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                    Text("Got it", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
