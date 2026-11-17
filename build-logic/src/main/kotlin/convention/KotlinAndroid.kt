package convention

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Created by Giorgos Fragkis on 13-11-2026
 *
 * Baseline for every Android module: `compileSdk` and `minSdk` from the catalog, Java and Kotlin target 17.
 *
 * AGP 9 compiles Kotlin itself, so `org.jetbrains.kotlin.android` is not applied; only the compiler options of
 * the `kotlin` extension that AGP registers are set.
 */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk = libs.version("android-compileSdk").toInt()
        defaultConfig.apply {
            minSdk = libs.version("android-minSdk").toInt()
        }
        compileOptions.apply {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
}

/** Baseline for plain Kotlin/JVM modules: the same Java and Kotlin target as the Android modules. */
internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    extensions.configure<KotlinJvmProjectExtension> {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_17
        }
    }
}
