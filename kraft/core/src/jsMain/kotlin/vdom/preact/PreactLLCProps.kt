package de.peekandpoke.kraft.vdom.preact

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx

/**
 * Properties passed to [PreactLLCProps].
 *
 * NOTICE: We define this interface as external to prevent property name-mangling.
 */
@Suppress("PropertyName")
internal external interface PreactLLCProps {
    var __ctx: Ctx<*>
    var __ctor: (Ctx<*>) -> Component<*>
    var __ref: PreactElements.PreactComponentRef<*>
}
