import convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * `csvparsely.hilt`: Hilt dependency injection for an Android module.
 *
 * Applies KSP before the Hilt Gradle plugin (Hilt looks for it), adds the Hilt runtime and runs its compiler
 * through KSP.
 */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.google.devtools.ksp")
            pluginManager.apply("com.google.dagger.hilt.android")
            dependencies {
                "implementation"(libs.findLibrary("hilt-android").get())
                "ksp"(libs.findLibrary("hilt-compiler").get())
            }
        }
    }
}
