plugins {
    id("csvparsely.android-library")
    id("csvparsely.hilt")
}

dependencies {
    implementation(projects.core.domain)
}
