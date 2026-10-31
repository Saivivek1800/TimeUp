package com.vivek.reclaim.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivek.reclaim.data.settings.SettingsRepository
import com.vivek.reclaim.data.settings.UserSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InterestsUiState(val selected: Set<String> = emptySet()) {
    /** FR-3: at least [UserSettings.MIN_INTERESTS] interests are required. */
    val canContinue: Boolean get() = selected.size >= UserSettings.MIN_INTERESTS
    val remaining: Int get() = (UserSettings.MIN_INTERESTS - selected.size).coerceAtLeast(0)
}

@HiltViewModel
class InterestsViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(InterestsUiState())
    val state: StateFlow<InterestsUiState> = _state.asStateFlow()

    init {
        // Coming back to this step keeps what the user already picked.
        viewModelScope.launch {
            val saved = settings.settings.first().interests
            _state.update { current -> if (current.selected.isEmpty()) InterestsUiState(saved) else current }
        }
    }

    fun toggle(interest: String) {
        _state.update { current ->
            val next = if (interest in current.selected) current.selected - interest else current.selected + interest
            InterestsUiState(next)
        }
    }

    fun saveAndContinue(onSaved: () -> Unit) {
        val current = _state.value
        if (!current.canContinue) return
        viewModelScope.launch {
            settings.setInterests(current.selected)
            onSaved()
        }
    }
}
