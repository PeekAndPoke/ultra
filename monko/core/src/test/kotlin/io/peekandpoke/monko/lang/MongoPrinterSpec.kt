package io.peekandpoke.monko.lang

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.maps.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.monko.lang.MongoPrinter.Companion.printQuery
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType

class MongoPrinterSpec : FreeSpec() {

    init {
        "MongoPrinter" - {

            "append string" {
                val printer = MongoPrinter()
                printer.append("hello")

                val result = printer.build()
                result.query shouldBe "hello"
                result.vars.shouldBeEmpty()
            }

            "append multiple strings" {
                val printer = MongoPrinter()
                printer.append("hello")
                printer.append(" ")
                printer.append("world")

                val result = printer.build()
                result.query shouldBe "hello world"
            }

            "name should escape properly" {
                val printer = MongoPrinter()
                printer.name("myField")

                val result = printer.build()
                result.query shouldBe "myField"
            }

            "appendLine should add newline" {
                val printer = MongoPrinter()
                printer.appendLine("first")
                printer.append("second")

                val result = printer.build()
                result.query shouldContain "first"
                result.query shouldContain "second"
                result.query shouldContain "\n"
            }

            "nl should add newline" {
                val printer = MongoPrinter()
                printer.append("before")
                printer.nl()
                printer.append("after")

                val result = printer.build()
                result.query shouldContain "\n"
            }

            "indent should increase indentation" {
                val printer = MongoPrinter()
                printer.appendLine("outer")
                printer.indent {
                    appendLine("inner")
                }
                printer.append("outer again")

                val result = printer.build()
                result.query shouldContain "    inner"
            }

            "nested indent" {
                val printer = MongoPrinter()
                printer.indent {
                    appendLine("level1")
                    indent {
                        appendLine("level2")
                    }
                }

                val result = printer.build()
                result.query shouldContain "    level1"
                result.query shouldContain "        level2"
            }
        }

        "MongoNameExpr" - {

            "should print its name" {
                val expr = MongoNameExpr(name = "myCollection", type = TypeRef.String)
                val result = expr.printQuery()

                result shouldBe "myCollection"
            }

            "should return correct type" {
                val expr = MongoNameExpr(name = "test", type = TypeRef.String)

                expr.getType() shouldBe TypeRef.String
            }
        }

        "MongoIterableExpr" - {

            "should print its name" {
                val inner = MongoNameExpr(name = "items", type = kType<List<String>>())
                val expr = MongoIterableExpr<String>("r", inner)

                val result = expr.printQuery()

                result shouldBe "r"
            }

            "downcast should return same instance" {
                val inner = MongoNameExpr(name = "items", type = kType<List<String>>())
                val expr = MongoIterableExpr<String>("r", inner)

                val downcasted = expr.downcast<Any, String>()

                downcasted shouldBe expr
            }
        }

        "MongoPropertyPath" - {

            "printQuery for a simple property" {
                val inner = MongoNameExpr(name = "items", type = kType<List<String>>())
                val r = MongoIterableExpr<String>("r", inner)
                val path = MongoPropertyPath.start(r).append<String, String>("name")

                val result = path.printQuery()

                result shouldContain "name"
            }

            "printQuery for nested property" {
                val inner = MongoNameExpr(name = "items", type = kType<List<String>>())
                val r = MongoIterableExpr<String>("r", inner)
                val path = MongoPropertyPath.start(r)
                    .append<String, String>("address")
                    .property<String>("city")

                val result = path.printQuery()

                result shouldContain "address"
                result shouldContain "city"
            }

            "getAsList should return all steps" {
                val inner = MongoNameExpr(name = "items", type = kType<List<String>>())
                val r = MongoIterableExpr<String>("r", inner)
                val path = MongoPropertyPath.start(r)
                    .append<String, String>("a")
                    .property<String>("b")

                val list = path.getAsList()

                // Root step + "a" step + "b" step = 3 elements
                list.size shouldBe 3
            }

            "getRoot returns null (recursive base case)" {
                val inner = MongoNameExpr(name = "items", type = kType<List<String>>())
                val r = MongoIterableExpr<String>("r", inner)
                val path = MongoPropertyPath.start(r)
                    .append<String, String>("field")

                val root = path.getRoot()

                root shouldBe null
            }

            "dropRoot should drop the root step" {
                val inner = MongoNameExpr(name = "items", type = kType<List<String>>())
                val r = MongoIterableExpr<String>("r", inner)
                val path = MongoPropertyPath.start(r)
                    .append<String, String>("field")

                val dropped = path.dropRoot()

                dropped?.previous shouldBe null
            }
        }
    }
}
