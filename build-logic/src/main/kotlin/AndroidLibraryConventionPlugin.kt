import com.android.build.api.dsl.LibraryExtension
import convention.configureKotlinAndroid
import convention.defaultNamespace
import convention.libs
import convention.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * `csvparsely.android-library`: every Android library module.
 *
 * Applies `com.android.library` with the shared Android baseline and derives the namespace and a resource prefix
 * from the module path, so modules never clash on package or resource names. Libraries have no target SDK of
 * their own; `testOptions.targetSdk` only sets the SDK their tests run against.
 */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                namespace = defaultNamespace()
                testOptions.targetSdk = libs.version("android-targetSdk").toInt()
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                // `:core:design-system` -> `core_design_system_`; lint warns about resources without it.
                resourcePrefix = path.split(Regex("\\W")).drop(1).joinToString("_").lowercase() + "_"
            }
        }
    }
}
