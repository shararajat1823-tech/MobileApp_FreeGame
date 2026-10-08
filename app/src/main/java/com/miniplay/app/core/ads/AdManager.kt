package com.miniplay.app.core.ads

/**
 * Ad abstraction. v1 intentionally ships [NoOpAdManager] — there are NO ads, and
 * never any inside gameplay. The interface exists so AdMob (or another provider)
 * can be added later by implementing this once, with zero changes to games.
 */
interface AdManager {
    val adsEnabled: Boolean

    /** Called at natural break points (e.g. returning to the hub). No-op in v1. */
    fun maybeShowInterstitial(placement: String)

    fun preload()
}

class NoOpAdManager : AdManager {
    override val adsEnabled: Boolean = false
    override fun maybeShowInterstitial(placement: String) = Unit
    override fun preload() = Unit
}
