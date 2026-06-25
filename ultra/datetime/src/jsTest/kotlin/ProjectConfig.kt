package io.peekandpoke.ultra.datetime

import io.kotest.core.config.AbstractProjectConfig

/**
 * Runs the whole JS test suite against the native, zero-data Intl-backed timezone provider
 * (instead of bundling `@js-joda/timezone`). If the named-zone / DST specs pass here, native
 * timezone support is proven for the browser.
 */
@Suppress("unused")
class ProjectConfig : AbstractProjectConfig() {
    override suspend fun beforeProject() {
        installNativeTimezones()
    }
}
