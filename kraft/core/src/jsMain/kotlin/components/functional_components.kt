@file:Suppress("Detekt.TooManyFunctions")

package de.peekandpoke.kraft.components

import de.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag

/**
 * Creates a functional component with no parameters
 */
fun component(
    func: VDom.() -> Unit,
): Tag.() -> Unit = {
    comp { FuncComp0(it, func) }
}

/**
 * Creates a functional component with 1 parameter
 */
fun <P1> component(
    func: VDom.(P1) -> Unit,
): Tag.(P1) -> Unit = { p1: P1 ->
    comp(FuncComp1.Props(p1)) { FuncComp1(it, func) }
}

/**
 * Creates a functional component with 2 parameters
 */
fun <P1, P2> component(
    func: VDom.(P1, P2) -> Unit,
): Tag.(P1, P2) -> Unit =
    { p1: P1, p2: P2 ->
        comp(FuncComp2.Props(p1, p2)) { FuncComp2(it, func) }
    }

/**
 * Creates a functional component with 3 parameters
 */
fun <P1, P2, P3> component(
    func: VDom.(P1, P2, P3) -> Unit,
): Tag.(P1, P2, P3) -> Unit =
    { p1: P1, p2: P2, p3: P3 ->
        comp(FuncComp3.Props(p1, p2, p3)) { FuncComp3(it, func) }
    }

/**
 * Creates a functional component with 4 parameters
 */
fun <P1, P2, P3, P4> component(
    func: VDom.(P1, P2, P3, P4) -> Unit,
): Tag.(P1, P2, P3, P4) -> Unit =
    { p1: P1, p2: P2, p3: P3, p4: P4 ->
        comp(FuncComp4.Props(p1, p2, p3, p4)) { FuncComp4(it, func) }
    }

/**
 * Creates a functional component with 5 parameters
 */
fun <P1, P2, P3, P4, P5> component(
    func: VDom.(P1, P2, P3, P4, P5) -> Unit,
): Tag.(P1, P2, P3, P4, P5) -> Unit =
    { p1: P1, p2: P2, p3: P3, p4: P4, p5: P5 ->
        comp(FuncComp5.Props(p1, p2, p3, p4, p5)) { FuncComp5(it, func) }
    }

/**
 * Creates a functional component with 6 parameters
 */
fun <P1, P2, P3, P4, P5, P6> component(
    func: VDom.(P1, P2, P3, P4, P5, P6) -> Unit,
): Tag.(P1, P2, P3, P4, P5, P6) -> Unit =
    { p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6 ->
        comp(FuncComp6.Props(p1, p2, p3, p4, p5, p6)) { FuncComp6(it, func) }
    }

/**
 * Creates a functional component with 7 parameters
 */
fun <P1, P2, P3, P4, P5, P6, P7> component(
    func: VDom.(P1, P2, P3, P4, P5, P6, P7) -> Unit,
): Tag.(P1, P2, P3, P4, P5, P6, P7) -> Unit =
    { p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7 ->
        comp(FuncComp7.Props(p1, p2, p3, p4, p5, p6, p7)) { FuncComp7(it, func) }
    }

/**
 * Creates a functional component with 8 parameters
 */
fun <P1, P2, P3, P4, P5, P6, P7, P8> component(
    func: VDom.(P1, P2, P3, P4, P5, P6, P7, P8) -> Unit,
): Tag.(P1, P2, P3, P4, P5, P6, P7, P8) -> Unit =
    { p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8 ->
        comp(FuncComp8.Props(p1, p2, p3, p4, p5, p6, p7, p8)) { FuncComp8(it, func) }
    }

/**
 * Creates a functional component with 9 parameters
 */
fun <P1, P2, P3, P4, P5, P6, P7, P8, P9> component(
    func: VDom.(P1, P2, P3, P4, P5, P6, P7, P8, P9) -> Unit,
): Tag.(P1, P2, P3, P4, P5, P6, P7, P8, P9) -> Unit =
    { p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9 ->
        comp(FuncComp9.Props(p1, p2, p3, p4, p5, p6, p7, p8, p9)) { FuncComp9(it, func) }
    }

/**
 * Creates a functional component with 9 parameters
 */
fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, P10> component(
    func: VDom.(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> Unit,
): Tag.(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> Unit =
    { p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9, p10: P10 ->
        comp(FuncComp10.Props(p1, p2, p3, p4, p5, p6, p7, p8, p9, p10)) { FuncComp10(it, func) }
    }

internal class FuncComp0(
    ctx: NoProps,
    private val fn: VDom.() -> Unit,
) : PureComponent(ctx) {
    override fun VDom.render() {
        fn()
    }
}

internal class FuncComp1<P1>(
    ctx: Ctx<Props<P1>>,
    private val fn: VDom.(P1) -> Unit,
) : Component<FuncComp1.Props<P1>>(ctx) {

    data class Props<P1>(
        val p1: P1,
    )

    override fun VDom.render() {
        fn(
            props.p1,
        )
    }
}

internal class FuncComp2<P1, P2>(
    ctx: Ctx<Props<P1, P2>>,
    private val fn: VDom.(P1, P2) -> Unit,
) : Component<FuncComp2.Props<P1, P2>>(ctx) {

    data class Props<P1, P2>(
        val p1: P1,
        val p2: P2,
    )

    override fun VDom.render() {
        fn(
            props.p1,
            props.p2,
        )
    }
}

internal class FuncComp3<P1, P2, P3>(
    ctx: Ctx<Props<P1, P2, P3>>,
    private val fn: VDom.(P1, P2, P3) -> Unit,
) : Component<FuncComp3.Props<P1, P2, P3>>(ctx) {

    data class Props<P1, P2, P3>(
        val p1: P1,
        val p2: P2,
        val p3: P3,
    )

    override fun VDom.render() {
        fn(
            props.p1,
            props.p2,
            props.p3,
        )
    }
}

internal class FuncComp4<P1, P2, P3, P4>(
    ctx: Ctx<Props<P1, P2, P3, P4>>,
    private val fn: VDom.(P1, P2, P3, P4) -> Unit,
) : Component<FuncComp4.Props<P1, P2, P3, P4>>(ctx) {

    data class Props<P1, P2, P3, P4>(
        val p1: P1,
        val p2: P2,
        val p3: P3,
        val p4: P4,
    )

    override fun VDom.render() {
        fn(
            props.p1,
            props.p2,
            props.p3,
            props.p4,
        )
    }
}

internal class FuncComp5<P1, P2, P3, P4, P5>(
    ctx: Ctx<Props<P1, P2, P3, P4, P5>>,
    private val fn: VDom.(P1, P2, P3, P4, P5) -> Unit,
) : Component<FuncComp5.Props<P1, P2, P3, P4, P5>>(ctx) {

    data class Props<P1, P2, P3, P4, P5>(
        val p1: P1,
        val p2: P2,
        val p3: P3,
        val p4: P4,
        val p5: P5,
    )

    override fun VDom.render() {
        fn(
            props.p1,
            props.p2,
            props.p3,
            props.p4,
            props.p5,
        )
    }
}

internal class FuncComp6<P1, P2, P3, P4, P5, P6>(
    ctx: Ctx<Props<P1, P2, P3, P4, P5, P6>>,
    private val fn: VDom.(P1, P2, P3, P4, P5, P6) -> Unit,
) : Component<FuncComp6.Props<P1, P2, P3, P4, P5, P6>>(ctx) {

    data class Props<P1, P2, P3, P4, P5, P6>(
        val p1: P1,
        val p2: P2,
        val p3: P3,
        val p4: P4,
        val p5: P5,
        val p6: P6,
    )

    override fun VDom.render() {
        fn(
            props.p1,
            props.p2,
            props.p3,
            props.p4,
            props.p5,
            props.p6,
        )
    }
}

internal class FuncComp7<P1, P2, P3, P4, P5, P6, P7>(
    ctx: Ctx<Props<P1, P2, P3, P4, P5, P6, P7>>,
    private val fn: VDom.(P1, P2, P3, P4, P5, P6, P7) -> Unit,
) : Component<FuncComp7.Props<P1, P2, P3, P4, P5, P6, P7>>(ctx) {

    data class Props<P1, P2, P3, P4, P5, P6, P7>(
        val p1: P1,
        val p2: P2,
        val p3: P3,
        val p4: P4,
        val p5: P5,
        val p6: P6,
        val p7: P7,
    )

    override fun VDom.render() {
        fn(
            props.p1,
            props.p2,
            props.p3,
            props.p4,
            props.p5,
            props.p6,
            props.p7,
        )
    }
}

internal class FuncComp8<P1, P2, P3, P4, P5, P6, P7, P8>(
    ctx: Ctx<Props<P1, P2, P3, P4, P5, P6, P7, P8>>,
    private val fn: VDom.(P1, P2, P3, P4, P5, P6, P7, P8) -> Unit,
) : Component<FuncComp8.Props<P1, P2, P3, P4, P5, P6, P7, P8>>(ctx) {

    data class Props<P1, P2, P3, P4, P5, P6, P7, P8>(
        val p1: P1,
        val p2: P2,
        val p3: P3,
        val p4: P4,
        val p5: P5,
        val p6: P6,
        val p7: P7,
        val p8: P8,
    )

    override fun VDom.render() {
        fn(
            props.p1,
            props.p2,
            props.p3,
            props.p4,
            props.p5,
            props.p6,
            props.p7,
            props.p8,
        )
    }
}

internal class FuncComp9<P1, P2, P3, P4, P5, P6, P7, P8, P9>(
    ctx: Ctx<Props<P1, P2, P3, P4, P5, P6, P7, P8, P9>>,
    private val fn: VDom.(P1, P2, P3, P4, P5, P6, P7, P8, P9) -> Unit,
) : Component<FuncComp9.Props<P1, P2, P3, P4, P5, P6, P7, P8, P9>>(ctx) {

    data class Props<P1, P2, P3, P4, P5, P6, P7, P8, P9>(
        val p1: P1,
        val p2: P2,
        val p3: P3,
        val p4: P4,
        val p5: P5,
        val p6: P6,
        val p7: P7,
        val p8: P8,
        val p9: P9,
    )

    override fun VDom.render() {
        fn(
            props.p1,
            props.p2,
            props.p3,
            props.p4,
            props.p5,
            props.p6,
            props.p7,
            props.p8,
            props.p9,
        )
    }
}

internal class FuncComp10<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10>(
    ctx: Ctx<Props<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10>>,
    private val fn: VDom.(P1, P2, P3, P4, P5, P6, P7, P8, P9, P10) -> Unit,
) : Component<FuncComp10.Props<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10>>(ctx) {

    data class Props<P1, P2, P3, P4, P5, P6, P7, P8, P9, P10>(
        val p1: P1,
        val p2: P2,
        val p3: P3,
        val p4: P4,
        val p5: P5,
        val p6: P6,
        val p7: P7,
        val p8: P8,
        val p9: P9,
        val p10: P10,
    )

    override fun VDom.render() {
        fn(
            props.p1,
            props.p2,
            props.p3,
            props.p4,
            props.p5,
            props.p6,
            props.p7,
            props.p8,
            props.p9,
            props.p10,
        )
    }
}
