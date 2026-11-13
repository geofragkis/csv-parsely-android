plugins {
    id("csvparsely.android-library")
    id("csvparsely.compose")
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.designSystem)
}
