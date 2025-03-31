package de.peekandpoke.funktor.logging

import de.peekandpoke.kraft.addons.routing.Router
import de.peekandpoke.kraft.addons.routing.urlParams
import de.peekandpoke.kraft.components.Component
import kotlin.properties.ReadWriteProperty

fun <C> Component<C>.logsFilter(
    router: Router,
    onChange: ((LogsFilter) -> Unit)? = null,
): ReadWriteProperty<Component<C>, LogsFilter> = urlParams(
    router = router,
    fromParams = { LogsFilter.fromMap(it) },
    toParams = { it.toMap() },
    onChange = onChange,
)
