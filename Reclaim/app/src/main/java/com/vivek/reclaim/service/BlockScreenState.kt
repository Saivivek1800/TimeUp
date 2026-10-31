package com.vivek.reclaim.service

/**
 * Whether the Block screen is in front right now. The Block screen sets this itself, and the accessibility service
 * (which runs in the same app) reads it, so the service never has to guess from system events, which do not always
 * arrive when a screen simply returns to the front.
 */
object BlockScreenState {
    @Volatile
    var resumed: Boolean = false

    /** Set by the service so it can remove its backup panel the moment the real Block screen shows up. */
    @Volatile
    var onShown: (() -> Unit)? = null
}
