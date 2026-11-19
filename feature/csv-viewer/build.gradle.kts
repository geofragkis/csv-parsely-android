plugins {
    id("csvparsely.android-library")
    id("csvparsely.compose")
    id("csvparsely.hilt")
}

dependencies {
    // api: the public ViewModel and UI state expose domain types (CsvTable, CsvError)
    api(projects.core.domain)
    implementation(projects.core.designSystem)
    // api: CsvViewerViewModel extends ViewModel
    api(libs.androidx.lifecycle.viewmodel)
    // api: CsvViewerScreen takes a Modifier
    api(libs.androidx.compose.ui)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.junit)
    androidTestRuntimeOnly(libs.androidx.test.runner)
}
