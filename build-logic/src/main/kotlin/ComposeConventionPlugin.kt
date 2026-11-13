import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import convention.configureAndroidCompose
import convention.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType

/**
 * `csvparsely.compose`: Jetpack Compose for an Android module.
 *
 * Applies the Compose compiler plugin and, once the module's Android plugin is applied, enables Compose with its
 * libraries. Only the app also gets activity-compose (`setContent { }`), because only the app hosts an activity.
 */
class ComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.withPlugin("com.android.application") {
                configureAndroidCompose(extensions.getByType<ApplicationExtension>())
                dependencies {
                    "implementation"(libs.findLibrary("androidx-activity-compose").get())
                }
            }
            pluginManager.withPlugin("com.android.library") {
                configureAndroidCompose(extensions.getByType<LibraryExtension>())
            }
        }
    }
}
