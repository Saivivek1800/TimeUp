package com.vivek.reclaim.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.vivek.reclaim.BlockActivity
import com.vivek.reclaim.WarningActivity
import com.vivek.reclaim.data.settings.SettingsRepository
import com.vivek.reclaim.data.usage.UsageStatsRepository
import com.vivek.reclaim.domain.BlockDecision
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

@EntryPoint
@InstallIn(SingletonComponent::class)
interface BlockerEntryPoint {
    fun settings(): SettingsRepository
    fun usage(): UsageStatsRepository
}

/**
 * Watches which app is in front and sends you to [BlockActivity] once the daily limit is used up (PRD 5.3).
 *
 * - Heads-up: a few minutes before the limit, a small pop-up says how much you have used. Once a day.
 * - Instant block: once today's limit is reached the service remembers it, so opening a blocked app again redirects
 *   straight away without measuring anything.
 * - Privacy: it reads only the package name of the window that changed. The service config
 *   (res/xml/accessibility_service_config.xml) does not allow reading screen content at all.
 * - Battery: nothing runs while no blocked app is in front. While one is, usage is re-measured at most every
 *   15 seconds (FR-15), and the loop stops as soon as another app takes over.
 */
class BlockerAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var settings: SettingsRepository
    private lateinit var usage: UsageStatsRepository

    // Mirrors of the saved settings, so an event never has to wait for a disk read.
    @Volatile private var blocked: Set<String> = emptySet()
    @Volatile private var limitMinutes: Int = 0
    @Volatile private var active: Boolean = false

    /** The local date on which the limit was last found used up. Cleared whenever the limit or the apps change. */
    @Volatile private var reachedDate: LocalDate? = null

    /** The local date (yyyy-MM-dd) of the last heads-up, kept in step with the saved value. */
    @Volatile private var warnedDate: String? = null

    /** Windows that appear over an app without it having left: the status bar / shade and the keyboard. */
    @Volatile private var neutralPackages: Set<String> = setOf("com.android.systemui")

    private var monitorJob: Job? = null

    /** The blocked app that is currently in front, so the block screen can name it. */
    @Volatile private var currentBlocked: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val entry = EntryPointAccessors.fromApplication(applicationContext, BlockerEntryPoint::class.java)
        settings = entry.settings()
        usage = entry.usage()
        neutralPackages = neutralPackages + keyboardPackages()
        scope.launch {
            var lastConfig: Pair<Int, Set<String>>? = null
            settings.settings.collect { s ->
                val config = s.dailyLimitMinutes to s.blockedPackages
                // A different limit or app list means "reached" has to be worked out again.
                if (config != lastConfig) reachedDate = null
                lastConfig = config

                blocked = s.blockedPackages
                limitMinutes = s.dailyLimitMinutes
                active = BlockDecision.isActive(s.onboardingComplete, s.blockedPackages, s.dailyLimitMinutes)
                // ISO dates sort the same as calendar dates, so keeping the later one is safe against a slow write.
                warnedDate = listOfNotNull(warnedDate, s.limitWarningDate).maxOrNull()
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        // Our own heads-up floats over a blocked app; it must not end the monitoring of that app.
        if (pkg == packageName && event.className?.toString() == WarningActivity::class.java.name) return
        if (pkg in neutralPackages) return

        // FR-16: never act on our own screens, the launcher, the dialer or settings; only on apps the user chose.
        if (active && pkg != packageName && pkg in blocked) {
            currentBlocked = pkg
            if (reachedDate == LocalDate.now(zone())) {
                redirect() // already used up today: no measuring, no delay
            } else {
                startMonitoring()
            }
        } else {
            currentBlocked = null
            stopMonitoring()
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch {
            while (isActive) {
                val startedAt = System.nanoTime()
                val used = usedTodayMillis()
                if (BlockDecision.isLimitReached(used, limitMinutes)) {
                    reachedDate = LocalDate.now(zone())
                    redirect()
                    Log.d(TAG, "Limit reached, redirected in ${(System.nanoTime() - startedAt) / 1_000_000} ms")
                    return@launch
                }
                if (BlockDecision.isWarningDue(used, limitMinutes, warnedToday())) {
                    markWarned()
                    showWarning(used)
                }
                delay(BlockDecision.nextCheckDelayMillis(used, limitMinutes, warningAlreadyShown = warnedToday()))
            }
        }
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    /** FR-7: foreground time in the blocked apps from local midnight to now. */
    private suspend fun usedTodayMillis(): Long {
        val zone = zone()
        val midnight = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        return usage.foregroundMillis(blocked, midnight, System.currentTimeMillis()).values.sum()
    }

    private fun warnedToday(): Boolean = warnedDate == LocalDate.now(zone()).toString()

    private fun markWarned() {
        val today = LocalDate.now(zone()).toString()
        warnedDate = today
        scope.launch { settings.setLimitWarningDate(today) }
    }

    private fun showWarning(usedMillis: Long) {
        val intent = Intent(this, WarningActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(WarningActivity.EXTRA_USED_MINUTES, (usedMillis / 60_000L).toInt())
            .putExtra(WarningActivity.EXTRA_LIMIT_MINUTES, limitMinutes)
        launch(intent, "heads-up")
    }

    private fun redirect() {
        val intent = Intent(this, BlockActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(BlockActivity.EXTRA_BLOCKED_PACKAGE, currentBlocked)
        launch(intent, "block screen")
    }

    private fun launch(intent: Intent, what: String) {
        mainHandler.post {
            runCatching { startActivity(intent) }.onFailure { Log.w(TAG, "Could not open the $what", it) }
        }
    }

    private fun zone(): ZoneId = ZoneId.systemDefault()

    private fun keyboardPackages(): Set<String> {
        val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return emptySet()
        return runCatching { manager.enabledInputMethodList.map { it.packageName }.toSet() }.getOrDefault(emptySet())
    }

    private companion object {
        const val TAG = "ReclaimBlocker"
    }
}
