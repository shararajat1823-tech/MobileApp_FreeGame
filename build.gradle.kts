// Top-level build file. Plugins are declared with `apply false` so that each
// module can opt in, keeping the root project free of configuration.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
