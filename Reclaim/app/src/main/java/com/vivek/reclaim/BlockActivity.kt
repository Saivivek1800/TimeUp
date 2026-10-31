package com.vivek.reclaim

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vivek.reclaim.domain.model.ThemeMode
import com.vivek.reclaim.service.BlockScreenState
import com.vivek.reclaim.ui.block.BlockScreen
import com.vivek.reclaim.ui.theme.ReclaimTheme
import com.vivek.reclaim.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

/**
 * The screen the accessibility service sends you to instead of a blocked app (PRD FR-12).
 * Back goes to the phone's home screen, never back into the blocked app (FR-14).
 */
@AndroidEntryPoint
class BlockActivity : ComponentActivity() {
    private val themeViewModel: ThemeViewModel by viewModels()

    /** The app that was intercepted, so the screen can say "Instagram paused". */
    private var blockedPackage by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })
        setContent {
            val mode by themeViewModel.themeMode.collectAsStateWithLifecycle()
            val dark = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            ReclaimTheme(darkTheme = dark) {
                BlockScreen(blockedPackage = blockedPackage, onGoHome = ::goHome)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        BlockScreenState.resumed = true
        BlockScreenState.onShown?.invoke()
    }

    override fun onPause() {
        BlockScreenState.resumed = false
        super.onPause()
    }

    /** singleTask: a second interception while this screen is open arrives here instead of making a new one. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        blockedPackage = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: blockedPackage
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finishAndRemoveTask()
    }

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "com.vivek.reclaim.extra.BLOCKED_PACKAGE"
    }
}
