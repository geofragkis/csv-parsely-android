plugins {
    id("csvparsely.android-library")
    id("csvparsely.compose")
    id("csvparsely.hilt")
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.designSystem)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
