package de.peekandpoke.ultra.common.model

import kotlinx.serialization.Serializable

@Serializable
data object EmptyObject {
    operator fun invoke() = EmptyObject
}
