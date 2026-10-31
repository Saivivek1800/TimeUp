package com.vivek.reclaim.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
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

    /** True while our own Block screen is in front. The Block screen reports this itself, so it is always right. */
    private val blockScreenVisible: Boolean get() = BlockScreenState.resumed

    override fun onServiceConnected() {
        super.onServiceConnected()
        val entry = EntryPointAccessors.fromApplication(applicationContext, BlockerEntryPoint::class.java)
        settings = entry.settings()
        usage = entry.usage()
        neutralPackages = neutralPackages + keyboardPackages()
        // The moment the real Block screen is in front, the backup panel (if any) has done its job.
        BlockScreenState.onShown = { mainHandler.post { hideOverlay() } }
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
        val className = event.className?.toString()

        if (pkg == packageName) {
            // Our own heads-up floats over a blocked app; it must not end the monitoring of that app.
            if (className == WarningActivity::class.java.name) return
            if (className == BlockActivity::class.java.name) hideOverlay()
            // Anything else of ours, including the backup panel's own window, never counts as "leaving".
            currentBlocked = null
            stopMonitoring()
            return
        }
        if (pkg in neutralPackages) return

        // FR-16: never act on our own screens, the launcher, the dialer or settings; only on apps the user chose.
        if (active && pkg in blocked) {
            currentBlocked = pkg
            if (reachedDate == LocalDate.now(zone())) {
                Log.d(TAG, "$pkg opened, limit already used up today: blocking at once")
                redirect() // already used up today: no measuring, no delay
            } else {
                Log.d(TAG, "$pkg opened, checking today's usage against the ${limitMinutes} min limit")
                startMonitoring()
            }
        } else {
            currentBlocked = null
            stopMonitoring()
            hideOverlay() // the user has left the blocked app
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        BlockScreenState.onShown = null
        hideOverlay()
        scope.cancel()
        super.onDestroy()
    }

    private fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch {
            while (isActive) {
                val startedAt = System.nanoTime()
                val used = usedTodayMillis()
                Log.d(TAG, "check: used ${used / 60_000L} min of $limitMinutes min across ${blocked.size} apps")
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

    /**
     * Blocks the app that is in front. First choice: open our reading screen. Some phones (vivo, iQOO, Xiaomi,
     * Oppo...) refuse to let an app open screens from the background, and say nothing. So if our screen has not
     * appeared a moment later, we cover the blocked app with our own panel, which needs no permission at all.
     */
    private fun redirect() {
        val intercepted = currentBlocked
        launch(blockScreenIntent(intercepted), "block screen")
        mainHandler.postDelayed({
            if (BlockDecision.needsBackupPanel(blockScreenVisible, stillInBlockedApp = currentBlocked != null)) {
                Log.w(TAG, "The block screen did not appear (the phone may block background screens): showing the backup panel")
                showOverlay(intercepted)
            }
        }, BlockDecision.BACKUP_PANEL_DELAY_MILLIS)
    }

    private fun blockScreenIntent(blockedPackage: String?) = Intent(this, BlockActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .putExtra(BlockActivity.EXTRA_BLOCKED_PACKAGE, blockedPackage)

    private fun launch(intent: Intent, what: String) {
        mainHandler.post {
            runCatching { startActivity(intent) }.onFailure { Log.w(TAG, "Could not open the $what", it) }
        }
    }

    // ---- The backup panel ----------------------------------------------------------------------------------

    private var overlay: View? = null // only touched on the main thread

    private fun showOverlay(blockedPackage: String?) {
        if (overlay != null) return
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val panel = buildPanel(blockedPackage)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            // Windows of this type may be added by an enabled accessibility service without any extra permission.
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE,
        )
        val added = runCatching { windowManager.addView(panel, params) }
        if (added.isSuccess) {
            overlay = panel
        } else {
            // Last resort: at least take the person out of the blocked app.
            Log.w(TAG, "Could not show the backup panel, sending the user to the home screen", added.exceptionOrNull())
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    private fun hideOverlay() {
        val view = overlay ?: return
        overlay = null
        runCatching { (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(view) }
    }

    private fun buildPanel(blockedPackage: String?): View {
        val dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val panelColor = if (dark) 0xFF0B1215.toInt() else 0xFFF8FAFC.toInt()
        val text = if (dark) 0xFFE6EEF2.toInt() else 0xFF0F172A.toInt()
        val muted = if (dark) 0xFF9FB3BF.toInt() else 0xFF475569.toInt()
        val teal = if (dark) 0xFF2DD4BF.toInt() else 0xFF0F766E.toInt()
        val onTeal = if (dark) 0xFF042F2B.toInt() else 0xFFFFFFFF.toInt()
        val soft = if (dark) 0xFF1B2A33.toInt() else 0xFFE8F1FB.toInt()
        val density = resources.displayMetrics.density
        fun px(dp: Int) = (dp * density).toInt()

        val appName = blockedPackage?.let {
            runCatching { packageManager.getApplicationInfo(it, 0).loadLabel(packageManager).toString() }.getOrNull()
        } ?: "This app"

        fun label(content: String, sizeSp: Float, color: Int, bold: Boolean) = TextView(this).apply {
            this.text = content
            textSize = sizeSp
            setTextColor(color)
            gravity = Gravity.CENTER
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }

        fun pill(content: String, fill: Int, color: Int, onClick: () -> Unit) = Button(this).apply {
            this.text = content
            isAllCaps = false
            textSize = 17f
            setTextColor(color)
            setTypeface(typeface, Typeface.BOLD)
            background = GradientDrawable().apply { cornerRadius = px(32).toFloat(); setColor(fill) }
            stateListAnimator = null
            setOnClickListener { onClick() }
        }

        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(panelColor)
            setPadding(px(28), px(48), px(28), px(48))
        }
        val gap = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        fun buttonParams() = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, px(58)).apply { topMargin = px(14) }

        column.addView(label("Reclaim shield", 14f, teal, true))
        column.addView(label("You've reached today's limit", 28f, text, true), gap.apply { topMargin = px(14) })
        column.addView(
            label("$appName is paused until tomorrow. Take a breath, or read something worth your time instead.", 17f, muted, false),
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = px(14) },
        )
        column.addView(
            pill("See reading list", teal, onTeal) {
                launch(blockScreenIntent(blockedPackage), "block screen")
                // If even that is refused, do not leave the blocked app uncovered: go home instead.
                mainHandler.postDelayed({
                    if (!blockScreenVisible) {
                        performGlobalAction(GLOBAL_ACTION_HOME)
                        hideOverlay()
                    }
                }, BlockDecision.BACKUP_PANEL_DELAY_MILLIS)
            },
            buttonParams().apply { topMargin = px(32) },
        )
        column.addView(
            pill("Go to home screen", soft, text) {
                performGlobalAction(GLOBAL_ACTION_HOME)
                hideOverlay()
            },
            buttonParams(),
        )
        return column
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
