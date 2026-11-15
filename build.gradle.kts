// Every Gradle plugin the convention plugins in build-logic apply is declared here with `apply false`,
// so all modules resolve the same plugin versions from the catalog.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    // Development only: `./gradlew buildHealth` reports unused or wrongly declared dependencies.
    alias(libs.plugins.dependency.analysis) apply false
}

// Development only: the Dependency Analysis plugin must also be applied to every module it analyzes. Applying it from
// here keeps it in one place, so removing it later means deleting this block, the line above and its catalog entry.
subprojects {
    apply(plugin = "com.autonomousapps.dependency-analysis")
}
