package com.vivek.reclaim.ui.onboarding

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivek.reclaim.ui.components.OnboardingTopBar
import com.vivek.reclaim.ui.components.StepLabel
import com.vivek.reclaim.ui.theme.ReclaimTheme

/** Onboarding step 1 of 4 (FR-2): Usage Access and Accessibility Service, each with status and a deep link. */
@Composable
fun PermissionsScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PermissionsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    // The user grants these in system settings, so re-read when they come back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    PermissionsContent(
        state = state,
        onBack = onBack,
        onContinue = onContinue,
        onAllowUsageAccess = { viewModel.openUsageAccess(context) },
        onAllowAccessibility = { viewModel.openAccessibility(context) },
        modifier = modifier,
    )
}

@Composable
private fun PermissionsContent(
    state: PermissionsUiState,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    onAllowUsageAccess: () -> Unit,
    onAllowAccessibility: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showWhy by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OnboardingTopBar(step = 1, onBack = onBack)
            Spacer(Modifier.height(20.dp))
            StepLabel(step = 1, title = "System access", icon = Icons.Outlined.Shield, centered = true)
            Spacer(Modifier.height(16.dp))
            HeaderIcon()
            Spacer(Modifier.height(20.dp))
            Text(
                "Two quick permissions",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, lineHeight = 38.sp),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "Reclaim needs these to notice your screen time in the background without draining your battery.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            PermissionCard(
                icon = Icons.Outlined.QueryStats,
                iconTint = MaterialTheme.colorScheme.primary,
                iconBackground = MaterialTheme.colorScheme.primaryContainer,
                title = "Usage Access",
                description = "So we can see how long you spend in each app.",
                granted = state.usageAccessGranted,
                onAllow = onAllowUsageAccess,
                footer = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.BatterySaver,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Optimized for low power",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
            )
            Spacer(Modifier.height(14.dp))
            PermissionCard(
                icon = Icons.Outlined.Shield,
                iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                iconBackground = MaterialTheme.colorScheme.tertiaryContainer,
                title = "Accessibility Service",
                description = "So we know when you open a blocked app. We never read your messages or personal data.",
                granted = state.accessibilityGranted,
                onAllow = onAllowAccessibility,
                footer = {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Zero data collection • 100% on-device",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                },
                hint = if (!state.accessibilityGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    "If the switch is greyed out: open App info for Reclaim, tap ⋮, then “Allow restricted settings”."
                } else {
                    null
                },
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onContinue,
                enabled = state.allGranted,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = CircleShape,
            ) {
                Text("Continue", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(10.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
            }
            if (!state.allGranted) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Allow both to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = { showWhy = true }) {
                Text("Why do we need this?", style = MaterialTheme.typography.titleMedium)
            }
        }
    }

    if (showWhy) {
        AlertDialog(
            onDismissRequest = { showWhy = false },
            confirmButton = { TextButton(onClick = { showWhy = false }) { Text("Got it") } },
            title = { Text("Why these two?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Usage Access lets Reclaim add up the minutes you spend in the apps you choose.")
                    Text("The Accessibility Service tells Reclaim which app just opened, so it can show your reading list once your limit is reached. It reads only the app’s name, never your screen, messages or text.")
                    Text("Everything stays on your phone. There is no account, no analytics and no server.")
                }
            },
        )
    }
}

@Composable
private fun HeaderIcon() {
    Box(modifier = Modifier.size(104.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(32.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
        )
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.HourglassBottom,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(40.dp),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun PermissionCard(
    icon: ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    iconBackground: androidx.compose.ui.graphics.Color,
    title: String,
    description: String,
    granted: Boolean,
    onAllow: () -> Unit,
    footer: @Composable () -> Unit,
    hint: String? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(iconBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) { footer() }
                Spacer(Modifier.width(12.dp))
                if (granted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Allowed", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    FilledTonalButton(onClick = onAllow) {
                        Text("Allow", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (hint != null) {
                Spacer(Modifier.height(10.dp))
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
private fun PermissionsPreview() {
    ReclaimTheme(darkTheme = false) {
        PermissionsContent(
            state = PermissionsUiState(usageAccessGranted = true),
            onBack = {},
            onContinue = {},
            onAllowUsageAccess = {},
            onAllowAccessibility = {},
        )
    }
}
