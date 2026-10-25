plugins {
    alias(libs.plugins.android.application) apply false
    // AGP compiles Kotlin itself; declaring the plugin here only pins the Kotlin version it uses.
    alias(libs.plugins.kotlin.android) apply false
}
