package io.peekandpoke.kraft.vdom

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.ComponentRef
import io.peekandpoke.kraft.components.Ctx
import kotlinx.html.TagConsumer
import kotlin.reflect.KClass

interface VDomTagConsumer : TagConsumer<VDomElement> {

    val engine: VDomEngine

    val host: Component<*>

    val isDebugMode: Boolean get() = engine.options.debugMode

    fun <P, C : Component<P>> onComponent(
        props: P,
        creatorFn: (Ctx<P>) -> C,
        cls: KClass<C>,
    ): ComponentRef<C>
}
