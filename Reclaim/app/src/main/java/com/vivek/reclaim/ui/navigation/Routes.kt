package com.vivek.reclaim.ui.navigation

/**
 * Navigation destinations. The Block screen is not here: it is its own Activity (added in M4)
 * so the accessibility service can launch it. The limit-change confirmation is a dialog inside Settings.
 */
sealed class Route(val path: String) {
    // Onboarding
    data object Welcome : Route("welcome")
    data object Permissions : Route("permissions")
    data object Interests : Route("interests")
    data object Apps : Route("apps")
    data object Limit : Route("limit")

    // Main (bottom navigation)
    data object Home : Route("home")
    data object Articles : Route("articles")
    data object Settings : Route("settings")

    // Opened from Settings
    data object EditInterests : Route("settings/interests")
    data object EditApps : Route("settings/apps")

    companion object {
        val onboardingFlow = listOf(Welcome, Permissions, Interests, Apps, Limit)
    }
}
