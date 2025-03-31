package de.peekandpoke.kraft.vdom.preact

import de.peekandpoke.kraft.utils.jsObject
import de.peekandpoke.kraft.vdom.preact.PreactLLC.Companion.getLowLevelComponentCtor
import org.w3c.dom.HTMLElement
import kotlin.reflect.KClass
import de.peekandpoke.kraft.components.Component as KraftComponent
import preact.Component as PreactComponent

/**
 * Internal representation of a low level [PreactComponent].
 *
 * Notice that we create a separate subclass of [PreactLLC] for each component class.
 * This is done in [getLowLevelComponentCtor].
 */
internal abstract class PreactLLC(
    val props: PreactLLCProps,
    context: dynamic,
) : PreactComponent(props, context) {

    companion object {

        /**
         * Caches the low level [PreactComponent] constructor for each [KraftComponent].
         */
        private val compClass2LLC = mutableMapOf<KClass<out KraftComponent<*>>, dynamic>()

        /**
         * Creates a subclass of [PreactLLC] for every unique [cls] given.
         *
         * The created subclass is reused for the same [cls].
         *
         * Why do we do this:
         *
         * This helps the Preact VDOM in diffing.
         * There are some situation, where one component is swapped with another.
         * This was leading to a lot of weird [ClassCastException] errors.
         * This approach seems to prevent the problem.
         */
        internal fun getLowLevelComponentCtor(cls: KClass<out KraftComponent<*>>): dynamic {
            return compClass2LLC.getOrPut(cls) {

                // console.log("===== component classes: ${compClass2LLC.size} ==")
                // console.log("++ ", cls.js)

                //
                // NOTICE: This needs to be exactly here. Only like this we get a unique subclass for every cls.
                //
                class Impl(props: PreactLLCProps, context: dynamic) : PreactLLC(props, context)

                // Return the newly created ctor
                ::Impl.also { ctor ->
                    // We need to do this to trick, so Preact will see this as a component type:
                    // see https://github.com/preactjs/preact/blob/10.5.14/src/diff/index.js#L75
                    ctor.asDynamic().prototype = jsObject {
                        render = {}
                    }

                    // console.log(">> $ctor")
                    // console.log(">> ${ctor.hashCode()}")
                }
            }
        }
    }

    /**
     * The [KraftComponent] which is wrapped by this low level component
     */
    @Suppress("UNCHECKED_CAST")
    private var component: KraftComponent<Any?>? = (props.__ctor(props.__ctx) as KraftComponent<Any?>).also {
        // Set this low level instance on the component
        it.lowLevelBridgeComponent = this@PreactLLC
        // Set this low level instance on the ref
        props.__ref.ll = this@PreactLLC
    }

    /**
     * Get the [KraftComponent] which is wrapped by this low level component
     */
    internal fun getComponent(): KraftComponent<Any?>? {
        return component
    }

    /**
     * Hook, called when the component was mounted into the DOM
     *
     * See [PreactComponent.componentDidMount]
     */
    override fun componentDidMount() {
        // Update the ref
        props.__ref.ll = this

        component?.onMount(getDom())
    }

    /**
     * Hook, called when the component was updated / re-rendered
     *
     * See [PreactComponent.componentDidUpdate]
     */
    override fun componentDidUpdate(prevProps: dynamic, prevState: dynamic, snapshot: dynamic) {
        component?.onUpdate(getDom())
    }

    /**
     * Hook, called when the component is about to be unmounted from the DOM
     *
     * See [PreactComponent.componentWillUnmount]
     */
    override fun componentWillUnmount() {
        component?.onUnmount()
        component = null
    }

    /**
     * Callback to determine whether the component should update / re-render itself
     *
     * See [PreactComponent.shouldComponentUpdate]
     */
    override fun shouldComponentUpdate(nextProps: PreactLLCProps, nextState: dynamic): Boolean {
        // Update the ref
        nextProps.__ref.ll = this

        return component?._internalShouldComponentUpdate(nextProps.__ctx) ?: false
    }

    /**
     * Renders the contents of the component.
     *
     * See [PreactComponent.render]
     */
    override fun render(props: PreactLLCProps, state: dynamic, context: dynamic): dynamic {
        return component?._internalRender()
    }

    /**
     * Catches component errors.
     *
     * See [PreactComponent.componentDidCatch]
     */
    override fun componentDidCatch(error: dynamic) {
        console.log("componentDidCatch", component)
        console.error("Error", error)

        super.componentDidCatch(error)
    }

    /**
     * Get the root [HTMLElement] of the rendered content of the component
     *
     * See [PreactComponent.base]
     */
    private fun getDom(): HTMLElement? = this.base as? HTMLElement
}
