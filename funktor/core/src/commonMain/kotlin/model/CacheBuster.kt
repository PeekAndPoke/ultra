package io.peekandpoke.funktor.core.model

import kotlinx.serialization.Serializable

/** Content hash appended to static asset URLs to bust browser caches on deploy. */
@Serializable
data class CacheBuster(val key: String) {

    companion object {
        fun of(appInfo: AppInfo) = CacheBuster(appInfo.versionHash)
    }
}
