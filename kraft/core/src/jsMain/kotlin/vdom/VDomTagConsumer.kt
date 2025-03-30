package de.peekandpoke.kraft.vdom

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.ComponentRef
import de.peekandpoke.kraft.components.Ctx
import kotlinx.html.TagConsumer
import kotlin.reflect.KClass

interface VDomTagConsumer : TagConsumer<VDomElement> {

    val engine: VDomEngine

    val host: Component<*>?

    val isDebugMode: Boolean get() = engine.options.debugMode

    fun <P, C : Component<P>> onComponent(
        props: P,
        creatorFn: (Ctx<P>) -> C,
        cls: KClass<C>,
    ): ComponentRef<C>
}
