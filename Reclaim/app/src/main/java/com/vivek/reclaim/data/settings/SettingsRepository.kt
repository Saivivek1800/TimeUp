package com.vivek.reclaim.data.settings

import android.content.Context
import android.os.SystemClock
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vivek.reclaim.domain.LimitLock
import com.vivek.reclaim.domain.LockSnapshot
import com.vivek.reclaim.domain.TrustedClock
import com.vivek.reclaim.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Storage for the settings plus the lock bookkeeping. Whether a given edit is *allowed* (for example removing
 * a blocked app while locked) is decided by the caller; this class only keeps the lock clock honest.
 */
@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store get() = context.settingsDataStore

    val settings: Flow<UserSettings> = store.data.map { p ->
        UserSettings(
            dailyLimitMinutes = p[Keys.DAILY_LIMIT] ?: UserSettings.DEFAULT_LIMIT_MINUTES,
            blockedPackages = p[Keys.BLOCKED_PACKAGES] ?: emptySet(),
            interests = p[Keys.INTERESTS] ?: emptySet(),
            limitLastChangedAt = p[Keys.LIMIT_LAST_CHANGED_AT] ?: 0L,
            lastElapsedRealtimeAtChange = p[Keys.LAST_ELAPSED_AT_CHANGE] ?: 0L,
            onboardingComplete = p[Keys.ONBOARDING_COMPLETE] ?: false,
            lock = p.readLock(),
            themeMode = p[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            articlesRefreshedAt = p[Keys.ARTICLES_REFRESHED_AT] ?: 0L,
            limitWarningDate = p[Keys.LIMIT_WARNING_DATE],
        )
    }

    suspend fun setLimitWarningDate(isoDate: String) {
        store.edit { it[Keys.LIMIT_WARNING_DATE] = isoDate }
    }

    suspend fun setArticlesRefreshedAt(epochMillis: Long) {
        store.edit { it[Keys.ARTICLES_REFRESHED_AT] = epochMillis }
    }

    suspend fun setInterests(interests: Set<String>) {
        store.edit { it[Keys.INTERESTS] = interests }
    }

    suspend fun setBlockedPackages(packages: Set<String>) {
        store.edit { it[Keys.BLOCKED_PACKAGES] = packages }
    }

    /**
     * Saves a new daily limit and starts a fresh lock. The first call (end of onboarding) counts as the first
     * change, so the 30-day lock begins right away (FR-27).
     */
    suspend fun applyLimitChange(
        minutes: Int,
        wallNowMillis: Long = System.currentTimeMillis(),
        elapsedNowMillis: Long = SystemClock.elapsedRealtime(),
    ) {
        val lock = LimitLock.startLock(wallNowMillis, elapsedNowMillis)
        store.edit {
            it[Keys.DAILY_LIMIT] = minutes
            it[Keys.LIMIT_LAST_CHANGED_AT] = wallNowMillis
            it[Keys.LAST_ELAPSED_AT_CHANGE] = elapsedNowMillis
            it.writeLock(lock)
        }
    }

    /**
     * Moves the trusted clock forward by the time that really passed since the last look. Call whenever the app
     * wakes up (Home refresh, opening Settings, after a reboot). Does nothing before the lock has started.
     */
    suspend fun refreshClock(
        wallNowMillis: Long = System.currentTimeMillis(),
        elapsedNowMillis: Long = SystemClock.elapsedRealtime(),
    ) {
        store.edit { prefs ->
            val current = prefs.readLock() ?: return@edit
            prefs.writeLock(TrustedClock.advance(current, wallNowMillis, elapsedNowMillis))
        }
    }

    suspend fun setOnboardingComplete(complete: Boolean) {
        store.edit { it[Keys.ONBOARDING_COMPLETE] = complete }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[Keys.THEME_MODE] = mode.name }
    }

    private fun Preferences.readLock(): LockSnapshot? {
        val ends = this[Keys.LOCK_ENDS_AT] ?: return null
        return LockSnapshot(
            trustedNow = this[Keys.TRUSTED_NOW] ?: 0L,
            lockEndsAt = ends,
            lastWall = this[Keys.LAST_WALL] ?: 0L,
            lastElapsed = this[Keys.LAST_ELAPSED] ?: 0L,
        )
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.writeLock(lock: LockSnapshot) {
        this[Keys.TRUSTED_NOW] = lock.trustedNow
        this[Keys.LOCK_ENDS_AT] = lock.lockEndsAt
        this[Keys.LAST_WALL] = lock.lastWall
        this[Keys.LAST_ELAPSED] = lock.lastElapsed
    }

    private object Keys {
        val DAILY_LIMIT = intPreferencesKey("daily_limit_minutes")
        val BLOCKED_PACKAGES = stringSetPreferencesKey("blocked_packages")
        val INTERESTS = stringSetPreferencesKey("interests")
        val LIMIT_LAST_CHANGED_AT = longPreferencesKey("limit_last_changed_at")
        val LAST_ELAPSED_AT_CHANGE = longPreferencesKey("last_elapsed_realtime_at_change")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ARTICLES_REFRESHED_AT = longPreferencesKey("articles_refreshed_at")
        val LIMIT_WARNING_DATE = stringPreferencesKey("limit_warning_date")
        val TRUSTED_NOW = longPreferencesKey("lock_trusted_now")
        val LOCK_ENDS_AT = longPreferencesKey("lock_ends_at")
        val LAST_WALL = longPreferencesKey("lock_last_wall")
        val LAST_ELAPSED = longPreferencesKey("lock_last_elapsed")
    }
}
