package com.vivek.reclaim.ui.settings

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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivek.reclaim.ui.components.SubScreenTopBar
import com.vivek.reclaim.ui.components.AppPickerSheet
import com.vivek.reclaim.ui.onboarding.AppRow
import com.vivek.reclaim.ui.util.formatShortDate

/** Edit blocked apps (FR-29): add any time; removing is locked together with the limit. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAppsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditAppsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }
    val apps = state.apps

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            SubScreenTopBar(title = "Edit blocked apps", onBack = onBack)
            Spacer(Modifier.height(20.dp))

            if (state.removalLocked) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "You can add apps any time, but removing one is locked until ${formatShortDate(state.nextChangeAtMillis)}, together with your limit.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            when {
                apps.loading -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    apps.listed.forEach { app ->
                        val on = app.packageName in apps.selected
                        AppRow(
                            app = app,
                            checked = on,
                            loadIcon = viewModel::loadIcon,
                            onToggle = { viewModel.toggle(app.packageName) },
                            // Locked rows can't be switched off, so tapping them explains why instead.
                            locked = false,
                            note = if (on && state.removalLocked) "Locked" else null,
                        )
                    }
                }
            }
            if (state.message != null) {
                Spacer(Modifier.height(12.dp))
                Text(state.message!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(14.dp))
            OutlinedButton(
                onClick = { showPicker = true },
                enabled = !apps.loading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = CircleShape,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Add apps", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showPicker) {
        AppPickerSheet(
            apps = apps.pickable,
            loadIcon = viewModel::loadIcon,
            onAdd = {
                viewModel.addApps(it)
                showPicker = false
            },
            onDismiss = { showPicker = false },
        )
    }
}
