package com.vivek.reclaim.ui.settings

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

data class EditInterestsUiState(
    val selected: Set<String> = emptySet(),
    /** Shown when the user tries to go below the minimum. */
    val atMinimum: Boolean = false,
)

/** Interests can be edited any time (PRD FR-29), but never below the minimum of three. Changes save immediately. */
@HiltViewModel
class EditInterestsViewModel @Inject constructor(
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditInterestsUiState())
    val state: StateFlow<EditInterestsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = EditInterestsUiState(selected = settings.settings.first().interests)
        }
    }

    fun toggle(interest: String) {
        val current = _state.value.selected
        val next = when {
            interest !in current -> current + interest
            current.size <= UserSettings.MIN_INTERESTS -> {
                _state.update { it.copy(atMinimum = true) }
                return
            }
            else -> current - interest
        }
        _state.value = EditInterestsUiState(selected = next)
        viewModelScope.launch { settings.setInterests(next) }
    }
}
