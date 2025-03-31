package de.peekandpoke.funktor.core.model

import kotlinx.serialization.Serializable

@Serializable
data class CacheBuster(val key: String) {

    companion object {
        fun of(appInfo: AppInfo) = CacheBuster(appInfo.versionHash)
    }
}
