package com.vivek.reclaim.ui.settings

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivek.reclaim.data.apps.AppRepository
import com.vivek.reclaim.data.settings.SettingsRepository
import com.vivek.reclaim.ui.onboarding.AppsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditAppsUiState(
    val apps: AppsUiState = AppsUiState(),
    /** FR-29: while locked, apps can be added but not removed. */
    val removalLocked: Boolean = false,
    val nextChangeAtMillis: Long = 0L,
    /** Shown when the user tries to remove an app while locked, or to remove the last one. */
    val message: String? = null,
)

@HiltViewModel
class EditAppsViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val settings: SettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditAppsUiState())
    val state: StateFlow<EditAppsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            settings.refreshClock()
            val saved = settings.settings.first()
            val apps = appRepository.loadLaunchableApps()
            _state.value = EditAppsUiState(
                apps = AppsUiState(
                    loading = false,
                    apps = apps,
                    selected = saved.blockedPackages intersect apps.map { it.packageName }.toSet(),
                ),
                removalLocked = saved.isLimitLocked,
                nextChangeAtMillis = saved.lock?.nextChangeAtWallMillis(System.currentTimeMillis()) ?: 0L,
            )
        }
    }

    fun loadIcon(packageName: String): ImageBitmap? = appRepository.loadIcon(packageName)

    /** Adding is always allowed. Removing is allowed only once the lock is over, and never down to zero apps. */
    fun toggle(packageName: String) {
        val current = _state.value
        val selected = current.apps.selected
        if (packageName in selected) {
            val message = when {
                current.removalLocked -> "Removing apps is locked until the limit unlocks."
                selected.size <= 1 -> "Keep at least one app."
                else -> null
            }
            if (message != null) {
                _state.update { it.copy(message = message) }
                return
            }
            save(selected - packageName)
        } else {
            save(selected + packageName)
        }
    }

    fun addApps(packageNames: Set<String>) = save(_state.value.apps.selected + packageNames)

    private fun save(next: Set<String>) {
        _state.update { it.copy(apps = it.apps.copy(selected = next), message = null) }
        viewModelScope.launch { settings.setBlockedPackages(next) }
    }
}
