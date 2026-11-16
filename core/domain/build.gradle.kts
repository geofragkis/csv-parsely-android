plugins {
    id("csvparsely.jvm-library")
}

dependencies {
    // api: the use case's constructor takes a CoroutineDispatcher
    api(libs.kotlinx.coroutines.core)
    // api: the public @DefaultDispatcher qualifier is annotated with javax.inject.Qualifier
    api(libs.javax.inject)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
}
