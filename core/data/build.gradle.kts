plugins {
    id("csvparsely.android-library")
    id("csvparsely.hilt")
}

dependencies {
    implementation(projects.core.domain)
    implementation(libs.kotlinx.coroutines.core)
}
