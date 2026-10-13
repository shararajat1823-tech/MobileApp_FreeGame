package com.miniplay.app.games.archers

/**
 * Weapon/arrow attributes kept as data (not scattered constants) so balancing is
 * one place and future unlockables are just new entries. Trade-offs are
 * meaningful: heavier arrows hit harder but fly slower/droopier; fast arrows fly
 * flat but knock back less.
 */
data class ArcherWeapon(
    val id: String,
    val displayName: String,
    /** Multiplier on launch speed from the same pull. */
    val speed: Float,
    /** Damage multiplier. */
    val damage: Float,
    /** Knockback impulse multiplier. */
    val knockback: Float,
    /** Mass affects gravity droop in flight (higher = droops more). */
    val mass: Float,
) {
    companion object {
        val STANDARD = ArcherWeapon("standard", "Standard", speed = 1f, damage = 1f, knockback = 1f, mass = 1f)
        val HEAVY = ArcherWeapon("heavy", "Heavy", speed = 0.85f, damage = 1.35f, knockback = 1.7f, mass = 1.5f)
        val FAST = ArcherWeapon("fast", "Fast", speed = 1.25f, damage = 0.85f, knockback = 0.7f, mass = 0.7f)

        val ALL = listOf(STANDARD, HEAVY, FAST)
        fun byId(id: String?): ArcherWeapon = ALL.firstOrNull { it.id == id } ?: STANDARD
    }
}
