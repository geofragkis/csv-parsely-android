plugins {
    id("csvparsely.android-library")
    id("csvparsely.compose")
    id("csvparsely.hilt")
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.designSystem)
    implementation(libs.androidx.lifecycle.viewmodel)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
