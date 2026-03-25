package io.peekandpoke.mutator.domain

import io.peekandpoke.mutator.Mutable

@Mutable
sealed interface SealedInterface {

    data class One(
        val value: String,
    ) : SealedInterface

    data class Two(
        val value: Int,
    ) : SealedInterface
}
