package com.vivek.reclaim.ui.home

import android.content.Context
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vivek.reclaim.data.apps.AppRepository
import com.vivek.reclaim.data.permissions.PermissionChecker
import com.vivek.reclaim.data.settings.SettingsRepository
import com.vivek.reclaim.data.usage.DailyUsageRepository
import com.vivek.reclaim.data.usage.UsageStatsRepository
import com.vivek.reclaim.domain.model.AppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject

data class AppUsage(val packageName: String, val label: String, val minutes: Int)

data class HomeUiState(
    val loading: Boolean = true,
    val usedMinutes: Int = 0,
    val limitMinutes: Int = 120,
    val apps: List<AppUsage> = emptyList(),
    /** Installed apps that are not monitored yet, offered by "Add apps". */
    val pickable: List<AppInfo> = emptyList(),
    val streakDays: Int = 0,
    val usageAccessGranted: Boolean = true,
    val hourOfDay: Int = 9,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val usage: UsageStatsRepository,
    private val dailyUsage: DailyUsageRepository,
    private val appRepository: AppRepository,
    private val permissions: PermissionChecker,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var refreshJob: Job? = null

    /** Re-reads today's usage. Cheap enough to call every half minute while Home is on screen. */
    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            settings.refreshClock() // keeps the 30-day lock clock moving while the app is in use
            val saved = settings.settings.first()
            val granted = permissions.isUsageAccessGranted()
            val now = LocalDateTime.now()
            if (!granted) {
                _state.update {
                    it.copy(
                        loading = false,
                        limitMinutes = saved.dailyLimitMinutes,
                        usageAccessGranted = false,
                        hourOfDay = now.hour,
                    )
                }
                return@launch
            }

            // FR-7: from local midnight to now. Recomputed every time, so a new day or a time-zone change just works.
            val zone = ZoneId.systemDefault()
            val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
            val perApp = usage.foregroundMillis(saved.blockedPackages, start, System.currentTimeMillis())

            val installed = appRepository.loadLaunchableApps()
            val names = installed.associate { it.packageName to it.label }
            val pickable = installed.filter { it.packageName !in saved.blockedPackages }.sortedBy { it.label.lowercase() }
            val apps = saved.blockedPackages
                .filter { it in names } // skip apps that have been uninstalled
                .map { pkg -> AppUsage(pkg, names.getValue(pkg), ((perApp[pkg] ?: 0L) / 60_000L).toInt()) }
                .sortedWith(compareByDescending<AppUsage> { it.minutes }.thenBy { it.label.lowercase() })

            runCatching { dailyUsage.rollover(saved.dailyLimitMinutes, saved.blockedPackages, saved.limitLastChangedAt) }
            val streak = runCatching { dailyUsage.currentStreak() }.getOrDefault(0)

            _state.value = HomeUiState(
                loading = false,
                usedMinutes = (perApp.values.sum() / 60_000L).toInt(),
                limitMinutes = saved.dailyLimitMinutes,
                apps = apps,
                pickable = pickable,
                streakDays = streak,
                usageAccessGranted = true,
                hourOfDay = now.hour,
            )
        }
    }

    fun loadIcon(packageName: String): ImageBitmap? = appRepository.loadIcon(packageName)

    /** Adding apps is always allowed (PRD FR-29). They join the one combined daily limit straight away. */
    fun addApps(packageNames: Set<String>) {
        if (packageNames.isEmpty()) return
        viewModelScope.launch {
            val current = settings.settings.first().blockedPackages
            settings.setBlockedPackages(current + packageNames)
            refresh()
        }
    }

    fun openUsageAccessSettings(context: Context) {
        context.startActivity(permissions.usageAccessSettingsIntent())
    }
}
