package app.lifeguide.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.MinimalExternalModuleDependency
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.getByType

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.lib(alias: String): Provider<MinimalExternalModuleDependency> =
    findLibrary(alias).orElseThrow { IllegalArgumentException("libs.versions.toml 里没有 $alias") }

internal fun VersionCatalog.int(alias: String): Int = findVersion(alias).get().requiredVersion.toInt()

/** :core:corpus → app.lifeguide.core.corpus，Android namespace 与 Kotlin 包名同一套 */
internal val Project.lifeguideNamespace: String
    get() = "app.lifeguide" + path.replace(':', '.').replace('-', '_')
