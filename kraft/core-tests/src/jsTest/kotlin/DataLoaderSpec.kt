package io.peekandpoke.kraft.coretests

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.awaitCss
import io.peekandpoke.kraft.testing.click
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.testing.textContent
import io.peekandpoke.kraft.utils.DataLoader
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.utils.dataLoaderOf
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.html.Tag
import kotlinx.html.button
import kotlinx.html.div

// =====================================================================================================================
// Helper components
// =====================================================================================================================

@Suppress("TestFunctionName")
private fun Tag.SimpleLoaderComponent() = comp {
    SimpleLoaderComponent(it)
}

private class SimpleLoaderComponent(ctx: NoProps) : PureComponent(ctx) {

    val loader: DataLoader<String> = dataLoader {
        flow {
            delay(50)
            emit("Hello World")
        }
    }

    override fun VDom.render() {
        loader(this) {
            loading {
                div(classes = "state") { +"loading" }
            }
            loaded { data ->
                div(classes = "state") { +"loaded" }
                div(classes = "data") { +data }
            }
            error { err ->
                div(classes = "state") { +"error" }
                div(classes = "error-msg") { +(err.message ?: "unknown") }
            }
        }
    }
}

@Suppress("TestFunctionName")
private fun Tag.ManualErrorComponent() = comp {
    ManualErrorComponent(it)
}

private class ManualErrorComponent(ctx: NoProps) : PureComponent(ctx) {

    val loader: DataLoader<String> = dataLoader {
        flow {
            delay(50)
            emit("data loaded")
        }
    }

    override fun VDom.render() {
        div {
            button(classes = "set-error-btn") {
                onClick {
                    loader.setState(DataLoader.State.Error(RuntimeException("manual error")))
                }
                +"Set Error"
            }

            loader(this) {
                loading {
                    div(classes = "state") { +"loading" }
                }
                loaded { data ->
                    div(classes = "state") { +"loaded" }
                    div(classes = "data") { +data }
                }
                error { err ->
                    div(classes = "state") { +"error" }
                    div(classes = "error-msg") { +(err.message ?: "unknown") }
                }
            }
        }
    }
}

@Suppress("TestFunctionName")
private fun Tag.ErrorLoaderComponent() = comp {
    ErrorLoaderComponent(it)
}

private class ErrorLoaderComponent(ctx: NoProps) : PureComponent(ctx) {

    val loader: DataLoader<String> = dataLoader {
        flow {
            delay(50)
            error("load failed")
        }
    }

    override fun VDom.render() {
        div {
            loader(this) {
                loading {
                    div(classes = "state") { +"loading" }
                }
                loaded { data ->
                    div(classes = "state") { +"loaded" }
                    div(classes = "data") { +data }
                }
                error { err ->
                    div(classes = "state") { +"error" }
                    div(classes = "error-msg") { +(err.message ?: "unknown") }
                }
            }
        }
    }
}

@Suppress("TestFunctionName")
private fun Tag.ReloadLoaderComponent() = comp {
    ReloadLoaderComponent(it)
}

private class ReloadLoaderComponent(ctx: NoProps) : PureComponent(ctx) {

    var counter = 0

    val loader: DataLoader<String> = dataLoader {
        flow {
            delay(50)
            counter++
            emit("value-$counter")
        }
    }

    override fun VDom.render() {
        loader(this) {
            loading {
                div(classes = "state") { +"loading" }
            }
            loaded { data ->
                div(classes = "state") { +"loaded" }
                div(classes = "data") { +data }
            }
            error { err ->
                div(classes = "state") { +"error" }
            }
        }

        button(classes = "reload-btn") {
            onClick { loader.reload(0) }
            +"Reload"
        }

        button(classes = "reload-silent-btn") {
            onClick { loader.reloadSilently(0) }
            +"Reload Silent"
        }

        button(classes = "modify-btn") {
            onClick { loader.modifyValue { "$it-modified" } }
            +"Modify"
        }
    }
}

@Suppress("TestFunctionName")
private fun Tag.DataLoaderOfComponent() = comp {
    DataLoaderOfComponent(it)
}

private class DataLoaderOfComponent(ctx: NoProps) : PureComponent(ctx) {

    val loader: DataLoader<String> = dataLoaderOf("immediate-value")

    override fun VDom.render() {
        loader(this) {
            loading {
                div(classes = "state") { +"loading" }
            }
            loaded { data ->
                div(classes = "state") { +"loaded" }
                div(classes = "data") { +data }
            }
            error { err ->
                div(classes = "state") { +"error" }
            }
        }
    }
}

@Suppress("TestFunctionName")
private fun Tag.StateInspectionComponent() = comp {
    StateInspectionComponent(it)
}

private class StateInspectionComponent(ctx: NoProps) : PureComponent(ctx) {

    val loader: DataLoader<String> = dataLoader {
        flow {
            delay(50)
            emit("data")
        }
    }

    override fun VDom.render() {
        div {
            div(classes = "is-loading") { +"${loader.isLoading()}" }
            div(classes = "is-not-loading") { +"${loader.isNotLoading()}" }
            div(classes = "is-loaded") { +"${loader.isLoaded()}" }
            div(classes = "is-not-loaded") { +"${loader.isNotLoaded()}" }
            div(classes = "is-error") { +"${loader.isError()}" }
            div(classes = "is-not-error") { +"${loader.isNotError()}" }

            loader(this) {
                loading {
                    div(classes = "state") { +"loading" }
                }
                loaded { data ->
                    div(classes = "state") { +"loaded" }
                    div(classes = "data") { +data }
                }
                error { err ->
                    div(classes = "state") { +"error" }
                }
            }
        }
    }
}

// =====================================================================================================================
// Tests
// =====================================================================================================================

class DataLoaderSpec : StringSpec({

    "Initial state is Loading and render shows loading indicator" {
        TestBed.preact({
            SimpleLoaderComponent()
        }) { root ->
            root.selectCss(".state").textContent() shouldBe "loading"
        }
    }

    "After flow emits, state becomes Loaded and render shows data" {
        TestBed.preact({
            SimpleLoaderComponent()
        }) { root ->
            root.selectCss(".state").textContent() shouldBe "loading"

            root.awaitCss(".data", timeoutMs = 2000)

            root.selectCss(".state").textContent() shouldBe "loaded"
            root.selectCss(".data").textContent() shouldBe "Hello World"
        }
    }

    "Error state - explicit setState to Error renders error block" {
        TestBed.preact({
            ManualErrorComponent()
        }) { root ->
            // Initially loaded
            root.awaitCss(".data", timeoutMs = 2000)
            root.selectCss(".state").textContent() shouldBe "loaded"

            // Click button to set error state explicitly
            root.selectCss(".set-error-btn").click()
            delay(50)

            root.selectCss(".state").textContent() shouldBe "error"
            root.selectCss(".error-msg").textContent() shouldBe "manual error"
        }
    }

    "Error state - flow throws, render shows error" {
        TestBed.preact({
            ErrorLoaderComponent()
        }) { root ->
            root.selectCss(".state").textContent() shouldBe "loading"

            // Poll until the DataLoader transitions to error state
            root.awaitCss(".error-msg", timeoutMs = 3000)

            root.selectCss(".state").textContent() shouldBe "error"
            root.selectCss(".error-msg").textContent() shouldBe "load failed"
        }
    }

    "reload() resets to Loading then re-fetches" {
        TestBed.preact({
            ReloadLoaderComponent()
        }) { root ->
            // Wait for initial load
            root.awaitCss(".data", timeoutMs = 2000)
            root.selectCss(".state").textContent() shouldBe "loaded"
            root.selectCss(".data").textContent() shouldBe "value-1"

            // Trigger reload
            root.selectCss(".reload-btn").click()
            delay(1)

            // Should be loading again
            root.selectCss(".state").textContent() shouldBe "loading"

            // Wait for reload to complete
            root.awaitCss(".data", timeoutMs = 2000)
            root.selectCss(".state").textContent() shouldBe "loaded"
            root.selectCss(".data").textContent() shouldBe "value-2"
        }
    }

    "reloadSilently() re-fetches without showing Loading state" {
        TestBed.preact({
            ReloadLoaderComponent()
        }) { root ->
            // Wait for initial load
            root.awaitCss(".data", timeoutMs = 2000)
            root.selectCss(".data").textContent() shouldBe "value-1"

            // Trigger silent reload — state should remain loaded (not switch to loading)
            root.selectCss(".reload-silent-btn").click()
            delay(1)

            root.selectCss(".state").textContent() shouldBe "loaded"

            // Wait for the silent reload to complete with new data
            delay(200)
            root.selectCss(".state").textContent() shouldBe "loaded"
            root.selectCss(".data").textContent() shouldBe "value-2"
        }
    }

    "dataLoaderOf() starts in Loaded state immediately" {
        TestBed.preact({
            DataLoaderOfComponent()
        }) { root ->
            // dataLoaderOf wraps in flowOf, so it loads almost immediately
            root.awaitCss(".data", timeoutMs = 2000)

            root.selectCss(".state").textContent() shouldBe "loaded"
            root.selectCss(".data").textContent() shouldBe "immediate-value"
        }
    }

    "modifyValue() transforms loaded data without reload" {
        TestBed.preact({
            ReloadLoaderComponent()
        }) { root ->
            // Wait for initial load
            root.awaitCss(".data", timeoutMs = 2000)
            root.selectCss(".data").textContent() shouldBe "value-1"

            // Modify the value
            root.selectCss(".modify-btn").click()
            delay(50)

            root.selectCss(".state").textContent() shouldBe "loaded"
            root.selectCss(".data").textContent() shouldBe "value-1-modified"
        }
    }

    "state inspection helpers while loading" {
        TestBed.preact({
            StateInspectionComponent()
        }) { root ->
            root.selectCss(".is-loading").textContent() shouldBe "true"
            root.selectCss(".is-not-loading").textContent() shouldBe "false"
            root.selectCss(".is-loaded").textContent() shouldBe "false"
            root.selectCss(".is-not-loaded").textContent() shouldBe "true"
            root.selectCss(".is-error").textContent() shouldBe "false"
            root.selectCss(".is-not-error").textContent() shouldBe "true"
        }
    }

    "state inspection helpers when loaded" {
        TestBed.preact({
            StateInspectionComponent()
        }) { root ->
            root.awaitCss(".data", timeoutMs = 2000)

            root.selectCss(".is-loading").textContent() shouldBe "false"
            root.selectCss(".is-not-loading").textContent() shouldBe "true"
            root.selectCss(".is-loaded").textContent() shouldBe "true"
            root.selectCss(".is-not-loaded").textContent() shouldBe "false"
            root.selectCss(".is-error").textContent() shouldBe "false"
            root.selectCss(".is-not-error").textContent() shouldBe "true"
        }
    }
})
