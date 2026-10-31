package com.vivek.reclaim.domain

import com.vivek.reclaim.data.settings.UserSettings

enum class MissingRequirement { PERMISSIONS, INTERESTS, APPS }

/** FR-6: setup completes only when both permissions are granted, 3+ interests are chosen and 1+ app is selected. */
object OnboardingRequirements {
    fun firstMissing(
        settings: UserSettings,
        usageAccessGranted: Boolean,
        accessibilityGranted: Boolean,
    ): MissingRequirement? = when {
        !usageAccessGranted || !accessibilityGranted -> MissingRequirement.PERMISSIONS
        settings.interests.size < UserSettings.MIN_INTERESTS -> MissingRequirement.INTERESTS
        settings.blockedPackages.isEmpty() -> MissingRequirement.APPS
        else -> null
    }
}
