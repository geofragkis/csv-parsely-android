// build-logic is an included build: Gradle compiles its convention plugins before configuring the main build,
// so modules can apply them by id. As a separate build it does not inherit the main build's settings, so the
// repositories and the version catalog are declared again here.

pluginManagement {
    repositories {
        google {
            // Gradle asks Google's repository only for Google/AndroidX groups; for any other dependency it skips this
            // repository and looks in mavenCentral() and then gradlePluginPortal().
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
    // One catalog for the whole project, shared with the main build.
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
