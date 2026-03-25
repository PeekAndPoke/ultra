package io.peekandpoke.kraft.vdom.preact

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx

/**
 * Properties passed to [PreactLLCProps].
 *
 * NOTICE: We define this interface as external to prevent property name-mangling.
 */
@Suppress("PropertyName", "detekt:VariableNaming")
internal external interface PreactLLCProps {
    var __ctx: Ctx<*>
    var __ctor: (Ctx<*>) -> Component<*>
    var __ref: PreactElements.PreactComponentRef<*>
}
