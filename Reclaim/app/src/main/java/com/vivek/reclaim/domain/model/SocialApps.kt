package com.vivek.reclaim.domain.model

/** An installed, launchable app. [category] is set only for apps on the default social list. */
data class AppInfo(
    val packageName: String,
    val label: String,
    val category: String? = null,
    val optional: Boolean = false,
)

/** The default social list (PRD FR-4). Only entries that are actually installed are shown. */
object SocialApps {
    data class Entry(
        val packageName: String,
        val label: String,
        val category: String,
        /** Optional apps are listed but start switched off. */
        val optional: Boolean = false,
    )

    val entries: List<Entry> = listOf(
        Entry("com.instagram.android", "Instagram", "Social"),
        Entry("com.google.android.youtube", "YouTube", "Video"),
        Entry("com.twitter.android", "X", "Social"),
        Entry("com.zhiliaoapp.musically", "TikTok", "Video"),
        Entry("com.ss.android.ugc.trill", "TikTok", "Video"),
        Entry("com.zhiliaoapp.musically.go", "TikTok Lite", "Video"),
        Entry("com.reddit.frontpage", "Reddit", "Social"),
        Entry("com.snapchat.android", "Snapchat", "Social"),
        Entry("com.facebook.katana", "Facebook", "Social"),
        Entry("com.facebook.lite", "Facebook Lite", "Social"),
        Entry("com.instagram.lite", "Instagram Lite", "Social"),
        Entry("com.instagram.barcelona", "Threads", "Social"),
        Entry("com.linkedin.android", "LinkedIn", "Social"),
        Entry("com.pinterest", "Pinterest", "Social"),
        Entry("in.mohalla.sharechat", "ShareChat", "Social"),
        Entry("in.mohalla.video", "Moj", "Video"),
        Entry("org.telegram.messenger", "Telegram", "Messaging"),
        Entry("com.whatsapp", "WhatsApp", "Messaging", optional = true),
    )

    private val byPackage = entries.associateBy { it.packageName }

    fun find(packageName: String): Entry? = byPackage[packageName]

    /** Position in the default list, used to keep popular apps first. Unknown apps sort last. */
    fun rank(packageName: String): Int = entries.indexOfFirst { it.packageName == packageName }.takeIf { it >= 0 } ?: Int.MAX_VALUE
}
