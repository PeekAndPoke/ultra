package de.peekandpoke.funktor.logging

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.routing.urlParams
import kotlin.properties.ReadWriteProperty

fun <C> Component<C>.logsFilter(
    onChange: ((LogsFilter) -> Unit)? = null,
): ReadWriteProperty<Component<C>, LogsFilter> = urlParams(
    fromParams = { LogsFilter.fromMap(it) },
    toParams = { it.toMap() },
    onChange = onChange,
)
