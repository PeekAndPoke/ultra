package io.peekandpoke.funktor.logging

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.routing.urlParams
import kotlin.properties.ReadWriteProperty

fun <C> Component<C>.logsFilter(
    onChange: ((LogsFilter) -> Unit)? = null,
): ReadWriteProperty<Component<C>, LogsFilter> = urlParams(
    fromParams = { LogsFilter.fromMap(it) },
    toParams = { it.toMap() },
    onChange = onChange,
)
