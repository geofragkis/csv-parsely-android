plugins {
    id("csvparsely.android-application")
    id("csvparsely.compose")
}

android {
    namespace = "gio.frag.csvparsely"

    defaultConfig {
        applicationId = "gio.frag.csvparsely"
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
}

dependencies {
    implementation(projects.feature.csvViewer)
    implementation(projects.core.data)
    implementation(projects.core.domain)
    implementation(projects.core.designSystem)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
