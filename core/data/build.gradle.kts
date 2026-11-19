plugins {
    id("csvparsely.android-library")
}

dependencies {
    // api: the public classes implement and return domain types (CsvRepository, RawCsv)
    api(projects.core.domain)
    // api: CsvDownloader's public constructor takes an OkHttpClient
    api(libs.okhttp)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockwebserver)
    androidTestImplementation(libs.androidx.junit)
    androidTestRuntimeOnly(libs.androidx.test.runner)
}
