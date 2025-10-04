package de.peekandpoke.kraft.routing

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.vdom.VDomTagConsumer
import kotlinx.html.A
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun A.href(route: Route.Bound) {
    val c = consumer as? VDomTagConsumer ?: error("Consumer must be a VDomTagConsumer")
    val router = c.host.router

    href = router.strategy.render(route)
}

fun <C> Component<C>.urlParam(
    name: String, default: Int, onChange: ((Int) -> Unit)? = null,
) = urlParams(
    fromParams = { it[name]?.toIntOrNull() ?: default },
    toParams = { mapOf(name to "$it") },
    onChange = onChange,
)

fun <C> Component<C>.urlParam(
    name: String, default: String, onChange: ((String) -> Unit)? = null,
) = urlParams(
    fromParams = { it[name] ?: default },
    toParams = { mapOf(name to it) },
    onChange = onChange,
)

fun <C, T> Component<C>.urlParams(
    fromParams: (Map<String, String>) -> T,
    toParams: (T) -> Map<String, Any?>,
    onChange: ((T) -> Unit)? = null,
): ReadWriteProperty<Component<C>, T> {

    return object : ReadWriteProperty<Component<C>, T> {

        private val initialRoute = router.current()

        var current: T = fromParams(initialRoute.matchedRoute.allParams)

        init {
            router.current { nextRoute ->
                // Only accept the change, if we stay on the same page
                if (initialRoute.route == nextRoute.route) {
                    setValueInternal(
                        fromParams(nextRoute.matchedRoute.allParams)
                    )
                }
            }
        }

        override fun getValue(thisRef: Component<C>, property: KProperty<*>): T {
            return current
        }

        override fun setValue(thisRef: Component<C>, property: KProperty<*>, value: T) {
            setValueInternal(value)
        }

        private fun setValueInternal(value: T) {
            if (value == current) {
                return
            }

            // remember the next value
            current = value

            // Call onChange callback
            onChange?.invoke(current)

            // Modify the params in the route
            val params = toParams(value)
                .mapValues { (_, v) ->
                    when (v) {
                        null -> ""
                        is Iterable<*> -> v.joinToString(",")
                        else -> v.toString()
                    }
                }

            // We will remove empty parameters from the url
            val toBeRemoved = params
                .filter { (_, v) -> v.isBlank() }
                .map { (n, _) -> n }

            // We will also keep additional parameters on the url
            val currentRoute = router.current().matchedRoute
            val currentParams = currentRoute.queryParams
            // Merge the currentParams with the incoming params
            val updatedParams = currentParams.plus(params)
                // And remove the empty ones
                .filter { (n, _) -> n !in toBeRemoved }

            // Keep the current value
            current = fromParams(updatedParams)

            router.replaceUri(route = currentRoute.withQueryParams(updatedParams))

            triggerRedraw()
        }
    }
}
