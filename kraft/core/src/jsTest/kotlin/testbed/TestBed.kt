package de.peekandpoke.kraft.testbed

import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.kraft.vdom.VDomEngine
import de.peekandpoke.kraft.vdom.preact.PreactVDomEngine
import kotlinx.browser.document
import kotlinx.coroutines.delay
import kotlinx.dom.appendElement
import org.w3c.dom.Element
import org.w3c.dom.HTMLBodyElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.get

class TestBed<E : VDomEngine> private constructor(
    private val engine: E,
) {
    companion object {

        val defaultVDomEngineOptions: VDomEngine.Options = VDomEngine.Options(
            debugMode = true,
        )

        fun preact(
            options: VDomEngine.Options = defaultVDomEngineOptions,
        ): TestBed<PreactVDomEngine> {
            return TestBed(
                engine = PreactVDomEngine(options = options),
            )
        }

        suspend fun preact(
            view: VDom.() -> Any?,
            options: VDomEngine.Options = defaultVDomEngineOptions,
            test: suspend (element: KQuery<Element>) -> Unit,
        ) {
            preact(options = options).render(view, test)
        }
    }

    suspend fun render(view: VDom.() -> Any?, test: suspend (element: KQuery<Element>) -> Unit) {

        val body = document.getElementsByTagName("body")[0] as HTMLBodyElement

        val testBed = body.appendElement("div") {
            id = "kraft-testbed"
        } as HTMLDivElement

        try {
            engine.mount(testBed, view)

            delay(3)

            test(KQuery(listOf(testBed)))

            delay(3)
        } finally {
            testBed.remove()
        }
    }
}
