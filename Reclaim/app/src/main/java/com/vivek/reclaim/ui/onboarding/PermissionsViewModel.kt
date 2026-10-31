package com.vivek.reclaim.ui.onboarding

import android.content.ActivityNotFoundException
import android.content.Context
import androidx.lifecycle.ViewModel
import com.vivek.reclaim.data.permissions.PermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class PermissionsUiState(
    val usageAccessGranted: Boolean = false,
    val accessibilityGranted: Boolean = false,
) {
    /** FR-6: onboarding cannot continue until both are granted. */
    val allGranted: Boolean get() = usageAccessGranted && accessibilityGranted
}

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val checker: PermissionChecker,
) : ViewModel() {

    private val _state = MutableStateFlow(read())
    val state: StateFlow<PermissionsUiState> = _state.asStateFlow()

    /** Call when the screen comes back to the foreground, since the user grants these in system settings. */
    fun refresh() {
        _state.value = read()
    }

    fun openUsageAccess(context: Context) {
        try {
            context.startActivity(checker.usageAccessSettingsIntent())
        } catch (_: ActivityNotFoundException) {
            context.startActivity(checker.usageAccessGeneralSettingsIntent())
        }
    }

    fun openAccessibility(context: Context) {
        context.startActivity(checker.accessibilitySettingsIntent())
    }

    private fun read() = PermissionsUiState(
        usageAccessGranted = checker.isUsageAccessGranted(),
        accessibilityGranted = checker.isAccessibilityServiceEnabled(),
    )
}
