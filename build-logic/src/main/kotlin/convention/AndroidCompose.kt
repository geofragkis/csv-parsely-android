package convention

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Created by Giorgos Fragkis on 13-11-2026
 *
 * Enables Compose on an Android module and adds what every Compose module needs: the BOM (which sets the version
 * of all Compose libraries), UI, Material 3, previews, and the Compose UI test libraries.
 */
internal fun Project.configureAndroidCompose(commonExtension: CommonExtension) {
    commonExtension.buildFeatures.apply {
        compose = true
    }
    dependencies {
        val bom = libs.findLibrary("androidx-compose-bom").get()
        "implementation"(platform(bom))
        "androidTestImplementation"(platform(bom))
        "implementation"(libs.findLibrary("androidx-compose-ui").get())
        "implementation"(libs.findLibrary("androidx-compose-ui-graphics").get())
        "implementation"(libs.findLibrary("androidx-compose-material3").get())
        "implementation"(libs.findLibrary("androidx-compose-ui-tooling-preview").get())
        "debugImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
        "androidTestImplementation"(libs.findLibrary("androidx-compose-ui-test-junit4").get())
        // ui-test-junit4 brings Espresso 3.5, which crashes on Android 17 (InputManager.getInstance was removed)
        "androidTestImplementation"(libs.findLibrary("androidx-espresso-core").get())
        "debugImplementation"(libs.findLibrary("androidx-compose-ui-test-manifest").get())
    }
}
