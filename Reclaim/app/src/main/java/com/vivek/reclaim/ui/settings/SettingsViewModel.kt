package com.vivek.reclaim.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivek.reclaim.data.settings.SettingsRepository
import com.vivek.reclaim.domain.model.DailyLimit
import com.vivek.reclaim.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val loading: Boolean = true,
    val limitMinutes: Int = 120,
    /** FR-25: while true the limit control is disabled and "Locked until ..." is shown. */
    val locked: Boolean = false,
    /** Wall-clock millis of the next allowed change, for display. */
    val nextChangeAtMillis: Long = 0L,
    val interests: List<String> = emptyList(),
    val blockedCount: Int = 0,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Non-null while the "change limit" sheet is open. */
    val draftLimit: Int? = null,
    val confirming: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settings.refreshClock()
            settings.settings.collect { s ->
                val now = System.currentTimeMillis()
                _state.update {
                    it.copy(
                        loading = false,
                        limitMinutes = s.dailyLimitMinutes,
                        locked = s.isLimitLocked,
                        nextChangeAtMillis = s.lock?.nextChangeAtWallMillis(now) ?: 0L,
                        interests = s.interests.sorted(),
                        blockedCount = s.blockedPackages.size,
                        themeMode = s.themeMode,
                    )
                }
            }
        }
    }

    /** Called when the screen comes back to the foreground, so the lock clock keeps up. */
    fun refresh() {
        viewModelScope.launch { settings.refreshClock() }
    }

    fun openChangeSheet() {
        _state.update { if (it.locked) it else it.copy(draftLimit = it.limitMinutes, error = null) }
    }

    fun setDraft(minutes: Int) {
        _state.update { if (it.draftLimit == null) it else it.copy(draftLimit = DailyLimit.snap(minutes)) }
    }

    fun closeChangeSheet() {
        _state.update { it.copy(draftLimit = null, confirming = false) }
    }

    fun review() {
        _state.update { s -> if (s.draftLimit != null && s.draftLimit != s.limitMinutes) s.copy(confirming = true) else s }
    }

    /** "Keep current limit": back out of the confirmation, nothing is saved. */
    fun keepCurrent() {
        _state.update { it.copy(confirming = false, draftLimit = null) }
    }

    fun confirmChange() {
        val draft = _state.value.draftLimit ?: return
        viewModelScope.launch {
            // Re-check against the trusted clock right before saving, never trust what was on screen.
            settings.refreshClock()
            val current = settings.settings.first()
            if (current.isLimitLocked) {
                _state.update { it.copy(confirming = false, draftLimit = null, error = "Your limit is still locked.") }
                return@launch
            }
            settings.applyLimitChange(draft)
            _state.update { it.copy(confirming = false, draftLimit = null, error = null) }
        }
    }

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settings.setThemeMode(mode) }
    }
}
