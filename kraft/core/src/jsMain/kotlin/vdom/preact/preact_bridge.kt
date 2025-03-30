@file:JsModule("preact")
@file:JsNonModule
@file:Suppress("PackageDirectoryMismatch", "unused")

package preact

// //////////////////////////// //
//                              //
//     https://preactjs.com     //
//                              //
// //////////////////////////// //

import org.w3c.dom.Element

/**
 * See https://preactjs.com/guide/v8/getting-started/#rendering-jsx
 */
external fun render(vnode: dynamic, container: Element)

/**
 * See https://preactjs.com/guide/v8/getting-started/#rendering-jsx
 */
external fun h(type: dynamic, props: dynamic): dynamic

/**
 * See https://preactjs.com/guide/v8/getting-started/#rendering-jsx
 */
external fun h(type: dynamic, props: dynamic, children: dynamic): dynamic

/**
 * See https://preactjs.com/guide/v10/components
 */
open external class Component(props: dynamic, context: dynamic) {

    /** The Base Dom Element or Text */
    val base: dynamic

    fun forceUpdate(): dynamic

    fun setState(nextState: dynamic): dynamic

    @Deprecated(
        message = "Use 'componentDidMount' instead",
        replaceWith = ReplaceWith("componentDidMount"),
        level = DeprecationLevel.ERROR
    )
    open fun componentWillMount(): dynamic

    open fun componentDidMount(): dynamic

    open fun componentWillUnmount(): dynamic

    open fun shouldComponentUpdate(nextProps: dynamic, nextState: dynamic): Boolean

    open fun componentDidUpdate(prevProps: dynamic, prevState: dynamic, snapshot: dynamic): dynamic

    open fun componentDidCatch(error: dynamic): dynamic

    open fun render(
        props: dynamic,
        state: dynamic,
        context: dynamic,
    ): dynamic
}
