// Convention plugins shared by every module of the main build. Each module's build file then only lists plugin
// ids and its dependencies, and SDK levels, Java/Kotlin targets and Compose setup live in one place.

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "gio.frag.csvparsely.buildlogic"

// The plugins run inside Gradle, so they target the JDK that runs Gradle, not the modules' target.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

// Compiled against only: at runtime the main build provides these plugins through the `apply false`
// declarations in the root build.gradle.kts, so every module uses a single version of each.
dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.compiler.gradlePlugin)
}

tasks {
    validatePlugins {
        enableStricterValidation = true
        failOnWarning = true
    }
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "csvparsely.android-application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "csvparsely.android-library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("jvmLibrary") {
            id = "csvparsely.jvm-library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("compose") {
            id = "csvparsely.compose"
            implementationClass = "ComposeConventionPlugin"
        }
        register("hilt") {
            id = "csvparsely.hilt"
            implementationClass = "HiltConventionPlugin"
        }
    }
}
