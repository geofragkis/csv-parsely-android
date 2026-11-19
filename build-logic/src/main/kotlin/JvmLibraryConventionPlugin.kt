import convention.configureKotlinJvm
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by Giorgos Fragkis on 13-11-2026
 *
 * `csvparsely.jvm-library`: a plain Kotlin/JVM module with no Android dependencies (`:core:domain`).
 * It builds and tests faster than an Android module and cannot reach the framework by accident.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            configureKotlinJvm()
        }
    }
}
