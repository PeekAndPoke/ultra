package de.peekandpoke.kraft.semanticui

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.stream.appendHTML

class SemanticIconSpec : StringSpec({

    "Rendering with 'icon'" {

        val content = buildString {
            appendHTML().body {
                div {
                    icon.plus()
                }
            }
        }

        content shouldBe """
            <body>
              <div><i class="plus icon"></i></div>
            </body>
            
        """.trimIndent()
    }

    "Rendering colors - for all SemanticColor.values()" {

        SemanticColor.values()
            .filter { it !in listOf(SemanticColor.none, SemanticColor.default) }
            .forEach { color ->
                withClue("Must work for '${color.name}'") {

                    buildString {
                        appendHTML().body {
                            div {
                                icon.color(color).plus {
                                }
                            }
                        }
                    } shouldBe """
                        <body>
                          <div><i class="${color.name} plus icon"></i></div>
                        </body>

                    """.trimIndent()
                }
            }
    }

    "Rendering colors - for SemanticColor.none" {

        val content = buildString {
            appendHTML().body {
                div {
                    icon.color(SemanticColor.none).plus()
                }
            }
        }

        content shouldBe """
            <body>
              <div><i class="plus icon"></i></div>
            </body>

        """.trimIndent()
    }

    "Rendering colors - for SemanticColor.default" {

        val content = buildString {
            appendHTML().body {
                div {
                    icon.color(SemanticColor.default).plus {
                    }
                }
            }
        }

        content shouldBe """
            <body>
              <div><i class="plus icon"></i></div>
            </body>

        """.trimIndent()
    }
})
