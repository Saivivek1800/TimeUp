package com.vivek.reclaim.data.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.provider.Settings
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.vivek.reclaim.domain.model.AppInfo
import com.vivek.reclaim.domain.model.SocialApps
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Lists launchable apps without needing QUERY_ALL_PACKAGES (PRD section 10), and loads their icons. */
@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val packageManager get() = context.packageManager
    private val iconCache = LruCache<String, ImageBitmap>(96)

    suspend fun loadLaunchableApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val blockedFromList = protectedPackages()
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        query(launcher)
            .map { it.activityInfo.packageName }
            .distinct()
            .filterNot { it in blockedFromList }
            .map { pkg ->
                val known = SocialApps.find(pkg)
                AppInfo(
                    packageName = pkg,
                    label = known?.label ?: labelOf(pkg),
                    category = known?.category,
                    optional = known?.optional ?: false,
                )
            }
    }

    /** Called from composables on a background dispatcher; results are cached. */
    fun loadIcon(packageName: String): ImageBitmap? {
        iconCache.get(packageName)?.let { return it }
        val bitmap = runCatching { packageManager.getApplicationIcon(packageName).toBitmap(120, 120).asImageBitmap() }
            .getOrNull() ?: return null
        iconCache.put(packageName, bitmap)
        return bitmap
    }

    fun labelOf(packageName: String): String =
        runCatching { packageManager.getApplicationInfo(packageName, 0).loadLabel(packageManager).toString() }
            .getOrDefault(packageName)

    /** FR-16: Reclaim itself, the launcher, the dialer and system settings can never be blocked. */
    private fun protectedPackages(): Set<String> {
        val result = mutableSetOf(
            context.packageName,
            "com.android.settings",
            "com.android.systemui",
            "com.android.phone",
            "com.android.dialer",
            "com.google.android.dialer",
            "com.android.server.telecom",
        )
        listOf(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            Intent(Intent.ACTION_DIAL),
            Intent(Settings.ACTION_SETTINGS),
        ).forEach { intent -> query(intent).forEach { result += it.activityInfo.packageName } }
        return result
    }

    private fun query(intent: Intent): List<ResolveInfo> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
}
