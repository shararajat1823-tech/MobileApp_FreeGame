# MiniPlay release shrink rules.
#
# The app has no reflection-based serialisation and no network layer, so the
# default Android optimise rules cover almost everything. The entries below
# guard the pieces R8 cannot see statically.

# --- Room ---
# Room generates implementations that are referenced only by the generated
# database class; keep the generated code and the entity constructors.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# --- Kotlin coroutines ---
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# --- Compose ---
# The Compose compiler already emits the keep rules it needs; this only
# silences warnings from optional tooling artifacts stripped in release.
-dontwarn androidx.compose.ui.tooling.**

# Keep enum values used by name in DataStore / Room type converters.
-keepclassmembers enum com.miniplay.app.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
