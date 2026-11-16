plugins {
    id("csvparsely.jvm-library")
}

dependencies {
    implementation(libs.javax.inject)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}
