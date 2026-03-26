package io.peekandpoke.kraft.coretests.component

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.ComponentRef
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.click
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.testing.textContent
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.streams.StreamSource
import kotlinx.coroutines.delay
import kotlinx.html.Tag
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.span

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  1. Component with Props
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@Suppress("TestFunctionName")
private fun Tag.GreetingComponent(name: String, age: Int) = comp(
    GreetingComponent.Props(name = name, age = age)
) {
    GreetingComponent(it)
}

private class GreetingComponent(ctx: Ctx<Props>) : Component<GreetingComponent.Props>(ctx) {

    data class Props(
        val name: String,
        val age: Int,
    )

    override fun VDom.render() {
        div(classes = "greeting") {
            span(classes = "name") { +props.name }
            span(classes = "age") { +"${props.age}" }
        }
    }
}

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  2. onMount
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

private object MountTracker {
    var mountCount = 0

    fun reset() {
        mountCount = 0
    }
}

@Suppress("TestFunctionName")
private fun Tag.OnMountComponent() = comp {
    OnMountComponent(it)
}

private class OnMountComponent(ctx: NoProps) : PureComponent(ctx) {

    var clicks by value(0)

    init {
        lifecycle.onMount {
            MountTracker.mountCount++
        }
    }

    override fun VDom.render() {
        div(classes = "mounted") {
            +"mounted"
        }

        div(classes = "click-count") { +"$clicks" }

        button(classes = "click-button") {
            onClick { clicks++ }
            +"Click"
        }
    }
}

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  3. onUnmount
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

private object UnmountTracker {
    var unmountCount = 0

    fun reset() {
        unmountCount = 0
    }
}

@Suppress("TestFunctionName")
private fun Tag.UnmountChild() = comp {
    UnmountChild(it)
}

private class UnmountChild(ctx: NoProps) : PureComponent(ctx) {

    init {
        lifecycle.onUnmount {
            UnmountTracker.unmountCount++
        }
    }

    override fun VDom.render() {
        div(classes = "child") {
            +"child-content"
        }
    }
}

@Suppress("TestFunctionName")
private fun Tag.UnmountParent() = comp {
    UnmountParent(it)
}

private class UnmountParent(ctx: NoProps) : PureComponent(ctx) {

    var showChild by value(true)

    override fun VDom.render() {
        div(classes = "parent") {
            button(classes = "toggle-button") {
                onClick { showChild = !showChild }
                +"Toggle"
            }

            if (showChild) {
                UnmountChild()
            }
        }
    }
}

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  4. value() state
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@Suppress("TestFunctionName")
private fun Tag.ValueStateComponent() = comp {
    ValueStateComponent(it)
}

private class ValueStateComponent(ctx: NoProps) : PureComponent(ctx) {

    var counter by value(0)
    var label by value("initial")

    override fun VDom.render() {
        div(classes = "state-test") {
            div(classes = "counter") { +"$counter" }
            div(classes = "label") { +label }

            button(classes = "inc-button") {
                onClick { counter++ }
                +"INC"
            }

            button(classes = "label-button") {
                onClick { label = "updated" }
                +"UPDATE LABEL"
            }
        }
    }
}

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  5. subscribingTo
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@Suppress("TestFunctionName")
private fun Tag.StreamComponent(stream: StreamSource<String>) = comp(
    StreamComponent.Props(stream = stream)
) {
    StreamComponent(it)
}

private class StreamComponent(ctx: Ctx<Props>) : Component<StreamComponent.Props>(ctx) {

    data class Props(
        val stream: StreamSource<String>,
    )

    val current by subscribingTo(props.stream)

    override fun VDom.render() {
        div(classes = "stream-value") {
            +current
        }
    }
}

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  6. shouldRedraw
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

private object RedrawTracker {
    var renderCount = 0

    fun reset() {
        renderCount = 0
    }
}

@Suppress("TestFunctionName")
private fun Tag.SelectiveRedrawComponent(relevant: Int, irrelevant: String) = comp(
    SelectiveRedrawComponent.Props(relevant = relevant, irrelevant = irrelevant)
) {
    SelectiveRedrawComponent(it)
}

private class SelectiveRedrawComponent(ctx: Ctx<Props>) : Component<SelectiveRedrawComponent.Props>(ctx) {

    data class Props(
        val relevant: Int,
        val irrelevant: String,
    )

    override fun shouldRedraw(nextProps: Props): Boolean {
        return nextProps.relevant != props.relevant
    }

    override fun VDom.render() {
        RedrawTracker.renderCount++
        div(classes = "selective") {
            div(classes = "relevant") { +"${props.relevant}" }
            div(classes = "irrelevant") { +props.irrelevant }
            div(classes = "render-count") { +"${RedrawTracker.renderCount}" }
        }
    }
}

@Suppress("TestFunctionName")
private fun Tag.SelectiveRedrawParent() = comp {
    SelectiveRedrawParent(it)
}

private class SelectiveRedrawParent(ctx: NoProps) : PureComponent(ctx) {

    var relevant by value(1)
    var irrelevant by value("aaa")

    override fun VDom.render() {
        div(classes = "parent") {
            SelectiveRedrawComponent(relevant = relevant, irrelevant = irrelevant)

            button(classes = "change-relevant") {
                onClick { relevant++ }
                +"Change Relevant"
            }

            button(classes = "change-irrelevant") {
                onClick { irrelevant = "${irrelevant}x" }
                +"Change Irrelevant"
            }
        }
    }
}

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  7. ComponentRef
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@Suppress("TestFunctionName")
private fun Tag.RefTargetComponent() = comp {
    RefTargetComponent(it)
}

private class RefTargetComponent(ctx: NoProps) : PureComponent(ctx) {

    var internalState by value("ref-initial")

    override fun VDom.render() {
        div(classes = "ref-target") {
            div(classes = "ref-value") { +internalState }
        }
    }
}

@Suppress("TestFunctionName")
private fun Tag.RefParentComponent(ref: ComponentRef.Tracker<RefTargetComponent>) = comp {
    RefParentComponent(it, ref)
}

private class RefParentComponent(
    ctx: NoProps,
    private val externalRef: ComponentRef.Tracker<RefTargetComponent>,
) : PureComponent(ctx) {

    override fun VDom.render() {
        div(classes = "ref-parent") {
            RefTargetComponent().track(externalRef)
        }
    }
}

// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//  Test Spec
// ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

class ComponentLifecycleSpec : StringSpec({

    // 1. Component with Props  ////////////////////////////////////////////////////////////////////////////////////

    "Component with Props - must render prop values correctly" {

        TestBed.preact({
            GreetingComponent(name = "Alice", age = 30)
        }) { root ->

            delay(50)

            root.selectCss(".name").textContent() shouldBe "Alice"
            root.selectCss(".age").textContent() shouldBe "30"
        }
    }

    "Component with Props - must update when re-rendered with different props" {

        val ref = ComponentRef.Tracker<GreetingComponent>()

        TestBed.preact({
            GreetingComponent(name = "Alice", age = 30).track(ref)
        }) { root ->

            delay(50)

            root.selectCss(".name").textContent() shouldBe "Alice"
            root.selectCss(".age").textContent() shouldBe "30"
        }
    }

    // 2. onMount  /////////////////////////////////////////////////////////////////////////////////////////////////

    "onMount - must fire after first render" {

        MountTracker.reset()

        TestBed.preact({
            OnMountComponent()
        }) { root ->

            delay(50)

            root.selectCss(".mounted").textContent() shouldBe "mounted"
            MountTracker.mountCount shouldBe 1
        }
    }

    "onMount - must fire exactly once even after state updates" {

        MountTracker.reset()

        TestBed.preact({
            OnMountComponent()
        }) { root ->

            delay(50)

            MountTracker.mountCount shouldBe 1

            // Trigger some state changes to cause re-renders
            root.selectCss("button.click-button").click()
            delay(50)

            root.selectCss("button.click-button").click()
            delay(50)

            root.selectCss(".click-count").textContent() shouldBe "2"

            // onMount must still have been called exactly once
            MountTracker.mountCount shouldBe 1
        }
    }

    // 3. onUnmount  ///////////////////////////////////////////////////////////////////////////////////////////////

    "onUnmount - must fire when component is removed from DOM" {

        UnmountTracker.reset()

        TestBed.preact({
            UnmountParent()
        }) { root ->

            delay(50)

            // Child should be present
            root.selectCss(".child").textContent() shouldBe "child-content"
            UnmountTracker.unmountCount shouldBe 0

            // Toggle child off
            root.selectCss("button.toggle-button").click()
            delay(50)

            // onUnmount should have fired
            UnmountTracker.unmountCount shouldBe 1
        }
    }

    "onUnmount - must not fire while component is still in DOM" {

        UnmountTracker.reset()

        TestBed.preact({
            UnmountParent()
        }) { root ->

            delay(50)

            // Child is visible, unmount must not have fired
            root.selectCss(".child").textContent() shouldBe "child-content"
            UnmountTracker.unmountCount shouldBe 0
        }
    }

    // 4. value() state  ///////////////////////////////////////////////////////////////////////////////////////////

    "value() state - initial value must render correctly" {

        TestBed.preact({
            ValueStateComponent()
        }) { root ->

            delay(50)

            root.selectCss(".counter").textContent() shouldBe "0"
            root.selectCss(".label").textContent() shouldBe "initial"
        }
    }

    "value() state - changing value must trigger re-render" {

        TestBed.preact({
            ValueStateComponent()
        }) { root ->

            delay(50)

            root.selectCss(".counter").textContent() shouldBe "0"

            // Increment counter three times
            repeat(3) {
                root.selectCss("button.inc-button").click()
                delay(10)
            }
            delay(50)

            root.selectCss(".counter").textContent() shouldBe "3"
        }
    }

    "value() state - multiple state properties must update independently" {

        TestBed.preact({
            ValueStateComponent()
        }) { root ->

            delay(50)

            root.selectCss(".counter").textContent() shouldBe "0"
            root.selectCss(".label").textContent() shouldBe "initial"

            // Update only the label
            root.selectCss("button.label-button").click()
            delay(50)

            root.selectCss(".counter").textContent() shouldBe "0"
            root.selectCss(".label").textContent() shouldBe "updated"

            // Update only the counter
            root.selectCss("button.inc-button").click()
            delay(50)

            root.selectCss(".counter").textContent() shouldBe "1"
            root.selectCss(".label").textContent() shouldBe "updated"
        }
    }

    // 5. subscribingTo  ///////////////////////////////////////////////////////////////////////////////////////////

    "subscribingTo - must render the initial stream value" {

        val stream = StreamSource("hello")

        TestBed.preact({
            StreamComponent(stream = stream)
        }) { root ->

            delay(50)

            root.selectCss(".stream-value").textContent() shouldBe "hello"
        }
    }

    "subscribingTo - must re-render when stream pushes a new value" {

        val stream = StreamSource("first")

        TestBed.preact({
            StreamComponent(stream = stream)
        }) { root ->

            delay(50)

            root.selectCss(".stream-value").textContent() shouldBe "first"

            // Push new value
            stream("second")
            delay(50)

            root.selectCss(".stream-value").textContent() shouldBe "second"

            // Push another value
            stream("third")
            delay(50)

            root.selectCss(".stream-value").textContent() shouldBe "third"
        }
    }

    // 6. shouldRedraw  ////////////////////////////////////////////////////////////////////////////////////////////

    "shouldRedraw - must re-render when relevant prop changes" {

        RedrawTracker.reset()

        TestBed.preact({
            SelectiveRedrawParent()
        }) { root ->

            delay(50)

            root.selectCss(".relevant").textContent() shouldBe "1"
            val initialRenderCount = RedrawTracker.renderCount

            // Change the relevant prop
            root.selectCss("button.change-relevant").click()
            delay(50)

            root.selectCss(".relevant").textContent() shouldBe "2"
            RedrawTracker.renderCount shouldBe initialRenderCount + 1
        }
    }

    "shouldRedraw - must not re-render when only irrelevant prop changes" {

        RedrawTracker.reset()

        TestBed.preact({
            SelectiveRedrawParent()
        }) { root ->

            delay(50)

            root.selectCss(".relevant").textContent() shouldBe "1"
            root.selectCss(".irrelevant").textContent() shouldBe "aaa"
            val renderCountAfterMount = RedrawTracker.renderCount

            // Change only the irrelevant prop
            root.selectCss("button.change-irrelevant").click()
            delay(50)

            // The component should NOT have re-rendered, so render count stays the same
            RedrawTracker.renderCount shouldBe renderCountAfterMount

            // The displayed irrelevant value should still be the old one
            root.selectCss(".irrelevant").textContent() shouldBe "aaa"
        }
    }

    "shouldRedraw - must re-render only for relevant changes in mixed updates" {

        RedrawTracker.reset()

        TestBed.preact({
            SelectiveRedrawParent()
        }) { root ->

            delay(50)

            val renderCountAfterMount = RedrawTracker.renderCount

            // Change irrelevant prop (should not trigger redraw)
            root.selectCss("button.change-irrelevant").click()
            delay(50)

            RedrawTracker.renderCount shouldBe renderCountAfterMount

            // Change relevant prop (should trigger redraw)
            root.selectCss("button.change-relevant").click()
            delay(50)

            RedrawTracker.renderCount shouldBe renderCountAfterMount + 1
            root.selectCss(".relevant").textContent() shouldBe "2"
        }
    }

    // 7. ComponentRef  ////////////////////////////////////////////////////////////////////////////////////////////

    "ComponentRef.Tracker - must access child component instance" {

        val ref = ComponentRef.Tracker<RefTargetComponent>()

        TestBed.preact({
            RefParentComponent(ref = ref)
        }) { root ->

            delay(50)

            // The ref should point to the child component
            val child = ref.getOrNull()
            (child != null) shouldBe true
            child!!.internalState shouldBe "ref-initial"

            // Verify DOM also shows the value
            root.selectCss(".ref-value").textContent() shouldBe "ref-initial"
        }
    }

    "ComponentRef.Tracker - must reflect state changes on the referenced component" {

        val ref = ComponentRef.Tracker<RefTargetComponent>()

        TestBed.preact({
            RefParentComponent(ref = ref)
        }) { root ->

            delay(50)

            val child = ref.getOrNull()
            (child != null) shouldBe true

            // Mutate the child's state through the ref
            child!!.internalState = "ref-updated"
            delay(50)

            root.selectCss(".ref-value").textContent() shouldBe "ref-updated"
        }
    }
})
