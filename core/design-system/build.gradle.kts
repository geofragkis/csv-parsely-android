plugins {
    id("csvparsely.android-library")
    id("csvparsely.compose")
}

dependencies {
    // api: the public Typography is a Material 3 type
    api(libs.androidx.compose.material3)
}
