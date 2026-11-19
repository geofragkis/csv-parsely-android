package convention

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * Created by Giorgos Fragkis on 13-11-2026
 *
 * The shared `libs` version catalog. Plugin code has no type-safe accessors, so entries are looked up by alias;
 * `.get()` fails the build at configuration time when an alias doesn't exist.
 */
internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** The value of a `[versions]` entry, e.g. `libs.version("android-minSdk")`. */
internal fun VersionCatalog.version(alias: String): String = findVersion(alias).get().requiredVersion

/**
 * Namespace derived from the module path, so module build files never declare one:
 * `:core:design-system` -> `gio.frag.csvparsely.core.design_system`.
 */
internal fun Project.defaultNamespace(): String {
    val suffix = path.trim(':')
        .split(':')
        .joinToString(".") { it.lowercase().replace('-', '_') }
    return "gio.frag.csvparsely.$suffix"
}
