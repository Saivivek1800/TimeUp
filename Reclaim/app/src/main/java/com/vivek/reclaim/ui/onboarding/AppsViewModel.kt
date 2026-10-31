package com.vivek.reclaim.ui.onboarding

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivek.reclaim.data.apps.AppRepository
import com.vivek.reclaim.data.settings.SettingsRepository
import com.vivek.reclaim.domain.model.AppInfo
import com.vivek.reclaim.domain.model.SocialApps
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppsUiState(
    val loading: Boolean = true,
    val apps: List<AppInfo> = emptyList(),
    val selected: Set<String> = emptySet(),
) {
    /** Suggested social apps that are installed, plus any other app the user added. Popular apps come first. */
    val listed: List<AppInfo>
        get() = apps
            .filter { it.category != null || it.packageName in selected }
            .sortedWith(compareBy({ SocialApps.rank(it.packageName) }, { it.label.lowercase() }))

    /** Installed apps that are not shown yet, offered by "Add another app". */
    val pickable: List<AppInfo>
        get() {
            val shown = listed.map { it.packageName }.toSet()
            return apps.filter { it.packageName !in shown }.sortedBy { it.label.lowercase() }
        }

    /** FR-6: at least one app must be chosen. */
    val canContinue: Boolean get() = selected.isNotEmpty()

    val allListedSelected: Boolean get() = listed.isNotEmpty() && listed.all { it.packageName in selected }
}

@HiltViewModel
class AppsViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AppsUiState())
    val state: StateFlow<AppsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val apps = appRepository.loadLaunchableApps()
            val saved = settings.settings.first().blockedPackages
            // Nothing is on by default; only a previous choice (e.g. coming back to this step) is restored.
            _state.value = AppsUiState(
                loading = false,
                apps = apps,
                selected = saved intersect apps.map { it.packageName }.toSet(),
            )
        }
    }

    fun loadIcon(packageName: String): ImageBitmap? = appRepository.loadIcon(packageName)

    fun toggle(packageName: String) {
        _state.update { s ->
            s.copy(selected = if (packageName in s.selected) s.selected - packageName else s.selected + packageName)
        }
    }

    fun addAll(packageNames: Set<String>) {
        _state.update { it.copy(selected = it.selected + packageNames) }
    }

    fun toggleAll() {
        _state.update { s ->
            val shown = s.listed.map { it.packageName }.toSet()
            s.copy(selected = if (s.allListedSelected) s.selected - shown else s.selected + shown)
        }
    }

    fun saveAndContinue(onSaved: () -> Unit) {
        val current = _state.value
        if (!current.canContinue) return
        viewModelScope.launch {
            settings.setBlockedPackages(current.selected)
            onSaved()
        }
    }
}
