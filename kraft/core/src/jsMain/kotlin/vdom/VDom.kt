package de.peekandpoke.kraft.vdom

import de.peekandpoke.kraft.components.Component
import kotlinx.html.FlowContent

class VDom(engine: VDomEngine, val component: Component<*>? = null) : FlowContent {

    fun render(builder: VDom.() -> Any?): dynamic {
        builder()
        return consumer.finalize().render()
    }

    override val consumer = engine.createTagConsumer(component)

    override val attributes: MutableMap<String, String>
        get() = mutableMapOf()

    override val attributesEntries: Collection<Map.Entry<String, String>>
        get() = emptyList()

    override val emptyTag: Boolean = false

    override val inlineTag: Boolean = false

    override val namespace: String? = null

    override val tagName: String = ""
}
