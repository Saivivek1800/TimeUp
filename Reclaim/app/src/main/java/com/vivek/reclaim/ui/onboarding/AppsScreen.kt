package com.vivek.reclaim.ui.onboarding

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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivek.reclaim.domain.model.AppInfo
import com.vivek.reclaim.ui.components.AppIcon
import com.vivek.reclaim.ui.components.AppPickerSheet
import com.vivek.reclaim.ui.components.OnboardingTopBar
import com.vivek.reclaim.ui.components.StepLabel

/** Onboarding step 3 of 4 (FR-4): choose which apps count as social media. */
@Composable
fun AppsScreen(
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppsContent(
        state = state,
        loadIcon = viewModel::loadIcon,
        onToggle = viewModel::toggle,
        onToggleAll = viewModel::toggleAll,
        onAddApps = viewModel::addAll,
        onBack = onBack,
        onContinue = { viewModel.saveAndContinue(onContinue) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppsContent(
    state: AppsUiState,
    loadIcon: (String) -> ImageBitmap?,
    onToggle: (String) -> Unit,
    onToggleAll: () -> Unit,
    onAddApps: (Set<String>) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showPicker by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            OnboardingTopBar(step = 3, onBack = onBack)
            Spacer(Modifier.height(20.dp))
            StepLabel(step = 3, title = "Distraction shield", icon = Icons.Outlined.Shield)
            Spacer(Modifier.height(8.dp))
            Text(
                "Which apps steal your time?",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp, lineHeight = 38.sp),
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Select the apps that pull you in. They all count toward one combined daily limit.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Outlined.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (state.selected.size == 1) "1 app selected" else "${state.selected.size} apps selected",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
                if (state.listed.isNotEmpty()) {
                    TextButton(onClick = onToggleAll) {
                        Text(
                            if (state.allListedSelected) "Clear all" else "Select all",
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            when {
                state.loading -> Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }

                state.listed.isEmpty() -> Text(
                    "We couldn’t find any popular social apps on this phone. Add the apps you want to limit.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                )

                else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    state.listed.forEach { app ->
                        AppRow(
                            app = app,
                            checked = app.packageName in state.selected,
                            loadIcon = loadIcon,
                            onToggle = { onToggle(app.packageName) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = { showPicker = true },
                enabled = !state.loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = CircleShape,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add apps", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(18.dp))
            InfoCard(count = state.selected.size)
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onContinue,
                enabled = state.canContinue,
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = CircleShape,
            ) {
                Text("Continue to step 4", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(10.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null)
            }
            if (!state.canContinue && !state.loading) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Pick at least one app to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
            Spacer(Modifier.height(8.dp))
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

/** [locked] keeps the switch from being turned off (removing an app is locked, PRD FR-29); [note] replaces the category line. */
@Composable
internal fun AppRow(
    app: AppInfo,
    checked: Boolean,
    loadIcon: (String) -> ImageBitmap?,
    onToggle: () -> Unit,
    locked: Boolean = false,
    note: String? = null,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .toggleable(value = checked, enabled = !locked, role = Role.Switch, onValueChange = { onToggle() }),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(packageName = app.packageName, loadIcon = loadIcon)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        app.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                    )
                    if (app.optional) {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "(optional)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Text(
                    note ?: app.category ?: "Added by you",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            // The row is the toggle target, so the switch itself is display-only.
            Switch(
                checked = checked,
                enabled = !locked,
                onCheckedChange = null,
                thumbContent = if (checked) {
                    { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun InfoCard(count: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Bolt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "One shared limit",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    if (count == 1) {
                        "When your daily limit is reached, this app redirects you to your reading list."
                    } else {
                        "When your daily limit is reached, all $count apps redirect you to your reading list."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

