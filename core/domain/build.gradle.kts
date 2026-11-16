plugins {
    id("csvparsely.jvm-library")
}

dependencies {
    // api: the use case's constructor takes a CoroutineDispatcher
    api(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
