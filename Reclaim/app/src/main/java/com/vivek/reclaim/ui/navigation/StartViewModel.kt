package com.vivek.reclaim.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivek.reclaim.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Decides once, at launch, whether to show onboarding or go straight to Home. Null while it is being read. */
@HiltViewModel
class StartViewModel @Inject constructor(
    settings: SettingsRepository,
) : ViewModel() {

    private val _startRoute = MutableStateFlow<String?>(null)
    val startRoute: StateFlow<String?> = _startRoute.asStateFlow()

    init {
        viewModelScope.launch {
            val done = settings.settings.first().onboardingComplete
            _startRoute.value = if (done) Route.Home.path else Route.Welcome.path
        }
    }
}
