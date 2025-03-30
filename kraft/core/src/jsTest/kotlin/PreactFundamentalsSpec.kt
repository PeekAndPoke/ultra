package de.peekandpoke.kraft

import de.peekandpoke.kraft.testbed.TestBed
import de.peekandpoke.kraft.testbed.textContent
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.html.h1
import org.w3c.dom.HTMLHeadingElement

class PreactFundamentalsSpec : StringSpec({

    "Render 'Hello world'" {

        TestBed.preact(
            { h1 { +"Hello World!" } }
        ) {
            it.textContent() shouldContain "Hello World"
        }
    }

    "Render 'Hello Mars'" {

        TestBed.preact(
            { h1 { +"Hello Mars!" } }
        ) { root ->

            val h1 = root.selectCss("h1")

            h1.size shouldBe 1

            h1.textContent() shouldContain "Hello Mars!"

            h1.first().let {
                it.tagName shouldBe "H1"
                it.shouldBeInstanceOf<HTMLHeadingElement>()
            }
        }
    }
})
