package com.vivek.reclaim.ui.home

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vivek.reclaim.R
import com.vivek.reclaim.domain.HomeLogic
import com.vivek.reclaim.domain.LimitStatus
import com.vivek.reclaim.domain.model.DailyLimit
import com.vivek.reclaim.ui.components.AppIcon
import com.vivek.reclaim.ui.components.AppPickerSheet
import com.vivek.reclaim.ui.components.UsageRing
import com.vivek.reclaim.ui.theme.ReclaimTheme
import kotlinx.coroutines.delay

private const val REFRESH_MILLIS = 30_000L

/** The main screen (FR-30): today's usage against the limit, time left, per-app usage and the streak. */
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Keep the numbers fresh while Home is visible; stops by itself when the app goes to the background.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                viewModel.refresh()
                delay(REFRESH_MILLIS)
            }
        }
    }

    HomeContent(
        state = state,
        loadIcon = viewModel::loadIcon,
        onAllowUsageAccess = { viewModel.openUsageAccessSettings(context) },
        onAllowAccessibility = { viewModel.openAccessibilitySettings(context) },
        onAddApps = viewModel::addApps,
        modifier = modifier,
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    loadIcon: (String) -> ImageBitmap?,
    onAllowUsageAccess: () -> Unit,
    onAllowAccessibility: () -> Unit,
    onAddApps: (Set<String>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (state.loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            return@Box
        }
        val status = HomeLogic.status(state.usedMinutes, state.limitMinutes)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_logo_mark),
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Reclaim", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (HomeLogic.isNight(state.hourOfDay)) Icons.Outlined.DarkMode else Icons.Outlined.WbSunny,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    HomeLogic.dayPartLabel(state.hourOfDay),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    letterSpacing = 1.sp,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "${HomeLogic.greeting(state.hourOfDay)}. ${HomeLogic.message(status)}",
                style = MaterialTheme.typography.headlineMedium.copy(lineHeight = 36.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(20.dp))

            if (!state.accessibilityGranted) {
                PermissionOffCard(
                    title = "Blocking is off",
                    message = "Reclaim can’t stop your apps right now. Phones switch this off when Reclaim is swiped away from recent apps or force-stopped. Turn it back on, then lock Reclaim in your recent apps so it stays on.",
                    onAllow = onAllowAccessibility,
                )
                Spacer(Modifier.height(16.dp))
            }
            if (!state.usageAccessGranted) {
                PermissionOffCard(
                    title = "Usage Access is off",
                    message = "Reclaim can’t see your screen time until you turn it back on.",
                    onAllow = onAllowUsageAccess,
                )
                Spacer(Modifier.height(16.dp))
            }

            UsageCard(state = state, status = status)
            Spacer(Modifier.height(16.dp))
            StreakCard(days = state.streakDays)
            Spacer(Modifier.height(24.dp))
            BreakdownSection(state = state, loadIcon = loadIcon, onAddApps = { showPicker = true })
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showPicker) {
        AppPickerSheet(
            apps = state.pickable,
            loadIcon = loadIcon,
            onAdd = {
                onAddApps(it)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}

@Composable
private fun UsageCard(state: HomeUiState, status: LimitStatus) {
    val remaining = HomeLogic.remainingMinutes(state.usedMinutes, state.limitMinutes)
    val ringColor = if (status == LimitStatus.ON_TRACK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            UsageRing(
                progress = HomeLogic.progress(state.usedMinutes, state.limitMinutes),
                progressColor = ringColor,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        DailyLimit.formatCompact(state.usedMinutes),
                        fontSize = 46.sp,
                        lineHeight = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "/ ${DailyLimit.formatCompact(state.limitMinutes)} limit",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ringColor))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            when (status) {
                                LimitStatus.ON_TRACK -> "On track"
                                LimitStatus.CLOSE -> "Getting close"
                                LimitStatus.REACHED -> "Limit reached"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Outlined.HourglassBottom,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (remaining > 0) "${DailyLimit.formatCompact(remaining)} left today" else "Limit reached for today",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }
    }
}

@Composable
private fun StreakCard(days: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(28.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    when (days) {
                        0 -> "Your streak starts tomorrow"
                        1 -> "1 day within your limit"
                        else -> "$days days within your limit"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (days == 0) "Stay within today’s limit and your first day counts." else "Keep it steady. You’re building a habit.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun BreakdownSection(state: HomeUiState, loadIcon: (String) -> ImageBitmap?, onAddApps: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Today’s breakdown", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(2.dp))
            Text(
                if (state.apps.size == 1) "1 app monitored" else "${state.apps.size} apps monitored",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onAddApps) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add apps", style = MaterialTheme.typography.titleMedium)
        }
    }
    Spacer(Modifier.height(12.dp))
    if (state.apps.isEmpty()) {
        Text(
            "No apps are being monitored yet.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
        )
        return
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            state.apps.forEach { app ->
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(packageName = app.packageName, loadIcon = loadIcon, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            app.label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                        )
                        Text(
                            DailyLimit.formatCompact(app.minutes),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    // Usage only (no per-app caps): the bar shows this app's share of the one daily limit.
                    LinearProgressIndicator(
                        progress = { HomeLogic.progress(app.minutes, state.limitMinutes) },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.secondaryContainer,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionOffCard(title: String, message: String, onAllow: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = onAllow) { Text("Turn it on") }
        }
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun HomePreview() {
    ReclaimTheme(darkTheme = false) {
        HomeContent(
            state = HomeUiState(
                loading = false,
                usedMinutes = 80,
                limitMinutes = 120,
                apps = listOf(AppUsage("a", "Instagram", 45), AppUsage("b", "YouTube", 25), AppUsage("c", "Reddit", 10)),
                streakDays = 5,
            ),
            loadIcon = { null },
            onAllowUsageAccess = {},
            onAllowAccessibility = {},
            onAddApps = {},
        )
    }
}
