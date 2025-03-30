package de.peekandpoke.kraft.semanticui

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.html.body
import kotlinx.html.stream.appendHTML

class SemanticTagSpec : StringSpec({

    "Rendering with 'ui'" {

        val content = buildString {
            appendHTML().body {
                ui.segment {
                }
            }
        }

        content shouldBe """
            <body>
              <div class="ui segment"></div>
            </body>
            
        """.trimIndent()
    }

    "Rendering with 'noui'" {

        val content = buildString {
            appendHTML().body {
                noui.segment {
                }
            }
        }

        content shouldBe """
            <body>
              <div class="segment"></div>
            </body>
            
        """.trimIndent()
    }

    "Rendering numbers - for all SemanticNumber.values()" {

        SemanticNumber.values()
            .forEach { number ->
                withClue("Must work for '${number.name}'") {

                    buildString {
                        appendHTML().body {
                            ui.number(number).segment {
                            }
                        }
                    } shouldBe """
                        <body>
                          <div class="ui ${number.name} segment"></div>
                        </body>
                        
                    """.trimIndent()

                    buildString {
                        appendHTML().body {
                            noui.number(number) {
                            }
                        }
                    } shouldBe """
                        <body>
                          <div class="${number.name}"></div>
                        </body>
                        
                    """.trimIndent()
                }
            }
    }

    "Rendering numbers - from int" {

        (-10..20)
            .forEach { number ->
                withClue("Must work for '$number'") {

                    val expected = when {
                        number < 1 -> SemanticNumber.one
                        number > 16 -> SemanticNumber.sixteen
                        else -> SemanticNumber.of(number)
                    }

                    buildString {
                        appendHTML().body {
                            ui.number(number).segment {
                            }
                        }
                    } shouldBe """
                        <body>
                          <div class="ui ${expected.name} segment"></div>
                        </body>
                        
                    """.trimIndent()

                    buildString {
                        appendHTML().body {
                            noui.number(number) {
                            }
                        }
                    } shouldBe """
                        <body>
                          <div class="${expected.name}"></div>
                        </body>
                        
                    """.trimIndent()
                }
            }
    }

    "Rendering colors - for all SemanticColor.values()" {

        SemanticColor.values()
            .filter { it !in listOf(SemanticColor.none, SemanticColor.default) }
            .forEach { color ->
                withClue("Must work for '${color.name}'") {

                    buildString {
                        appendHTML().body {
                            ui.color(color).segment {
                            }
                        }
                    } shouldBe """
                        <body>
                          <div class="ui ${color.name} segment"></div>
                        </body>
                        
                    """.trimIndent()

                    buildString {
                        appendHTML().body {
                            noui.color(color) {
                            }
                        }
                    } shouldBe """
                        <body>
                          <div class="${color.name}"></div>
                        </body>
                        
                    """.trimIndent()
                }
            }
    }

    "Rendering colors - for SemanticColor.none" {

        val content = buildString {
            appendHTML().body {
                ui.color(SemanticColor.none).segment {
                }
            }
        }

        content shouldBe """
            <body>
              <div class="ui segment"></div>
            </body>
            
        """.trimIndent()
    }

    "Rendering colors - for SemanticColor.default" {

        val content = buildString {
            appendHTML().body {
                ui.color(SemanticColor.default).segment {
                }
            }
        }

        content shouldBe """
            <body>
              <div class="ui segment"></div>
            </body>
            
        """.trimIndent()
    }
})
