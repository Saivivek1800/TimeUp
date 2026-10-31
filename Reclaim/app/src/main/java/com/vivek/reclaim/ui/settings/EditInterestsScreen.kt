package com.vivek.reclaim.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivek.reclaim.data.settings.UserSettings
import com.vivek.reclaim.domain.model.Interests
import com.vivek.reclaim.ui.components.SubScreenTopBar
import com.vivek.reclaim.ui.onboarding.InterestChip

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditInterestsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditInterestsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            SubScreenTopBar(title = "Edit interests", onBack = onBack)
            Spacer(Modifier.height(20.dp))
            Text(
                "These decide which reads you see when your time is up. Changes save as you tap.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${state.selected.size} selected • keep at least ${UserSettings.MIN_INTERESTS}",
                style = MaterialTheme.typography.labelLarge,
                color = if (state.atMinimum) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(20.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Interests.all.forEach { interest ->
                    InterestChip(
                        label = interest,
                        selected = interest in state.selected,
                        onToggle = { viewModel.toggle(interest) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
