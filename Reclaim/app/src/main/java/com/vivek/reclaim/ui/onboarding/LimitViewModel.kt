package com.vivek.reclaim.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivek.reclaim.data.permissions.PermissionChecker
import com.vivek.reclaim.data.settings.SettingsRepository
import com.vivek.reclaim.data.usage.UsageStatsRepository
import com.vivek.reclaim.domain.MissingRequirement
import com.vivek.reclaim.domain.OnboardingRequirements
import com.vivek.reclaim.domain.model.DailyLimit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LimitUiState(
    val limitMinutes: Int = DailyLimit.snap(120),
    /** Average daily minutes over the last 7 days for the chosen apps; null when it cannot be measured. */
    val baselineMinutes: Int? = null,
    val showConfirm: Boolean = false,
    val saving: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class LimitViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val usage: UsageStatsRepository,
    private val permissions: PermissionChecker,
) : ViewModel() {

    private val _state = MutableStateFlow(LimitUiState())
    val state: StateFlow<LimitUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val saved = settings.settings.first()
            _state.update { it.copy(limitMinutes = DailyLimit.snap(saved.dailyLimitMinutes)) }
            // The reference number is best-effort: no permission or no data simply hides the card.
            val baseline = runCatching { usage.averageDailyMinutes(saved.blockedPackages) }.getOrNull()
            _state.update { it.copy(baselineMinutes = baseline?.takeIf { minutes -> minutes > 0 }) }
        }
    }

    fun setLimit(minutes: Int) {
        _state.update { it.copy(limitMinutes = DailyLimit.snap(minutes), error = null) }
    }

    fun askToConfirm() {
        _state.update { it.copy(showConfirm = true, error = null) }
    }

    fun dismissConfirm() {
        _state.update { it.copy(showConfirm = false) }
    }

    /** Saves the limit (this first save starts the 30-day lock, FR-27) and marks onboarding complete. */
    fun lockInAndFinish(onFinished: () -> Unit) {
        if (_state.value.saving) return
        _state.update { it.copy(showConfirm = false, saving = true) }
        viewModelScope.launch {
            val current = settings.settings.first()
            val missing = OnboardingRequirements.firstMissing(
                current,
                usageAccessGranted = permissions.isUsageAccessGranted(),
                accessibilityGranted = permissions.isAccessibilityServiceEnabled(),
            )
            if (missing != null) {
                _state.update { it.copy(saving = false, error = messageFor(missing)) }
                return@launch
            }
            settings.applyLimitChange(minutes = _state.value.limitMinutes)
            settings.setOnboardingComplete(true)
            _state.update { it.copy(saving = false) }
            onFinished()
        }
    }

    private fun messageFor(missing: MissingRequirement) = when (missing) {
        MissingRequirement.PERMISSIONS -> "Both permissions need to be allowed. Go back to step 1."
        MissingRequirement.INTERESTS -> "Pick at least 3 interests. Go back to step 2."
        MissingRequirement.APPS -> "Pick at least one app. Go back to step 3."
    }
}
