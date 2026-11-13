import com.android.build.api.dsl.ApplicationExtension
import convention.configureKotlinAndroid
import convention.libs
import convention.version
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * `csvparsely.android-application`: the `:app` module.
 *
 * Applies `com.android.application` with the shared Android baseline, sets `targetSdk` from the catalog and uses the
 * AndroidX test runner. App-specific values (`applicationId`, versions, build types) stay in `app/build.gradle.kts`.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = libs.version("android-targetSdk").toInt()
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
        }
    }
}
