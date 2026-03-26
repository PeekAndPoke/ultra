package io.peekandpoke.kraft.testing

import io.peekandpoke.kraft.kraftApp
import io.peekandpoke.kraft.testing.TestBed.Companion.preact
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.kraft.vdom.VDomEngine
import io.peekandpoke.kraft.vdom.preact.PreactVDomEngine
import kotlinx.browser.document
import kotlinx.coroutines.delay
import kotlinx.dom.appendElement
import org.w3c.dom.Element
import org.w3c.dom.HTMLBodyElement
import org.w3c.dom.HTMLDivElement

/**
 * Test harness for rendering Kraft components in a real browser DOM.
 *
 * Creates a temporary `<div>` in the document body, mounts a Kraft app into it,
 * and provides a [KQuery] handle for assertions. The test element is removed after the test.
 */
class TestBed<E : VDomEngine> private constructor(
    private val engine: E,
) {
    companion object {

        /** Default VDom engine options used by [preact], with debug mode enabled. */
        val defaultVDomEngineOptions: VDomEngine.Options = VDomEngine.Options(
            debugMode = true,
        )

        /** Creates a [TestBed] backed by the Preact VDom engine. */
        fun preact(
            options: VDomEngine.Options = defaultVDomEngineOptions,
        ): TestBed<PreactVDomEngine> {
            return TestBed(
                engine = PreactVDomEngine(options = options),
            )
        }

        /** Convenience: creates a Preact test bed, renders [view], runs [test], and cleans up. */
        suspend fun preact(
            view: VDom.() -> Any?,
            options: VDomEngine.Options = defaultVDomEngineOptions,
            test: suspend (element: KQuery<Element>) -> Unit,
        ) {
            preact(options = options).render(view, test)
        }
    }

    /** Mounts the [view] into a temporary DOM element, runs [test] with a [KQuery] handle, then cleans up. */
    suspend fun render(view: VDom.() -> Any?, test: suspend (element: KQuery<Element>) -> Unit) {

        val body = document.querySelector("body") as HTMLBodyElement

        val testBed = body.appendElement("div") {
            id = "kraft-testbed"
        } as HTMLDivElement

        val app = kraftApp { }

        try {
            app.mount(element = testBed, engine = engine, view = view)

            delay(1)

            test(KQuery(listOf(testBed)))

            delay(1)
        } finally {
            testBed.remove()
        }
    }
}
