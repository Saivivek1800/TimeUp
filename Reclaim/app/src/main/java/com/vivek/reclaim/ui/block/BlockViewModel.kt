package com.vivek.reclaim.ui.block

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivek.reclaim.data.apps.AppRepository
import com.vivek.reclaim.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class BlockUiState(
    /** The app that was intercepted, or null if the screen was opened some other way. */
    val appPackage: String? = null,
    val appLabel: String? = null,
    val limitMinutes: Int = 120,
)

@HiltViewModel
class BlockViewModel @Inject constructor(
    settings: SettingsRepository,
    private val appRepository: AppRepository,
) : ViewModel() {

    private val blockedPackage = MutableStateFlow<String?>(null)

    val state: StateFlow<BlockUiState> = combine(
        settings.settings.map { it.dailyLimitMinutes }.distinctUntilChanged(),
        blockedPackage,
    ) { limit, pkg ->
        BlockUiState(appPackage = pkg, appLabel = pkg?.let(appRepository::labelOf), limitMinutes = limit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlockUiState())

    fun setBlockedPackage(packageName: String?) {
        blockedPackage.value = packageName
    }

    fun loadIcon(packageName: String): ImageBitmap? = appRepository.loadIcon(packageName)
}
