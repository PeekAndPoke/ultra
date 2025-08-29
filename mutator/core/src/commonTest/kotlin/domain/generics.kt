package de.peekandpoke.mutator.domain

import de.peekandpoke.mutator.Mutable

@Mutable
data class OneGenericParam<T>(
    val value: T,
)

@Mutable
data class OneBoundedGenericParam<T : CharSequence>(
    val value: T,
)

@Mutable
data class OneBoundedNullableGenericParamNoMutator<T : Any?>(
    val value: T,
)

@Mutable
data class TwoGenericParams<T1, T2>(
    val value1: T1,
    val value2: T2,
)
