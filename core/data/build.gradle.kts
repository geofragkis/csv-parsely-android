plugins {
    id("csvparsely.android-library")
    id("csvparsely.hilt")
}

dependencies {
    implementation(projects.core.domain)
    implementation(libs.okhttp)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.robolectric)
}
