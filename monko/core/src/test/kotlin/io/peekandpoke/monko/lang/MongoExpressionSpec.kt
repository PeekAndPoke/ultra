package io.peekandpoke.monko.lang

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.peekandpoke.monko.lang.MongoPrinter.Companion.printQuery
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType

class MongoExpressionSpec : FreeSpec() {

    init {
        "MongoNameExpr" - {

            "should store and return the name" {
                val expr = MongoNameExpr(name = "collection", type = TypeRef.String)

                expr.printQuery() shouldBe "collection"
            }

            "should store and return the type" {
                val expr = MongoNameExpr(name = "test", type = TypeRef.String)

                expr.getType() shouldBe TypeRef.String
            }

            "should print to a MongoPrinter" {
                val expr = MongoNameExpr(name = "myExpr", type = TypeRef.String)

                val printer = MongoPrinter()
                expr.print(printer)

                printer.build().query shouldBe "myExpr"
            }
        }

        "MongoIterableExpr" - {

            "should print its own name (not the inner expression)" {
                val inner = MongoNameExpr(name = "inner", type = kType<List<String>>())
                val expr = MongoIterableExpr<String>("outer", inner)

                expr.printQuery() shouldBe "outer"
            }

            "should derive type from inner expression" {
                val inner = MongoNameExpr(name = "items", type = kType<List<String>>())
                val expr = MongoIterableExpr<String>("r", inner)

                expr.getType() shouldBe kType<String>()
            }

            "downcast should return the same instance typed differently" {
                val inner = MongoNameExpr(name = "items", type = kType<List<String>>())
                val expr = MongoIterableExpr<String>("r", inner)

                val result = expr.downcast<Any, String>()

                result.shouldBeInstanceOf<MongoIterableExpr<Any>>()
            }
        }

        "MongoPropertyPath" - {

            "start should create path from expression" {
                val inner = MongoNameExpr(name = "items", type = kType<List<String>>())
                val r = MongoIterableExpr<String>("r", inner)

                val path = MongoPropertyPath.start(r)

                path.previous shouldBe null
            }

            "property should chain from existing path" {
                val inner = MongoNameExpr(name = "items", type = kType<List<String>>())
                val r = MongoIterableExpr<String>("r", inner)

                val path = MongoPropertyPath.start(r)
                    .property<String>("child")

                val prev = path.previous
                prev.shouldNotBeNull()
                prev.previous shouldBe null
            }

            "getAsList for single step returns just the root step" {
                val inner = MongoNameExpr(name = "items", type = kType<List<String>>())
                val r = MongoIterableExpr<String>("r", inner)

                val path = MongoPropertyPath.start(r)
                val list = path.getAsList()

                list.size shouldBe 1
            }

            "getAsList for chained path returns all steps in order" {
                val inner = MongoNameExpr(name = "items", type = kType<List<String>>())
                val r = MongoIterableExpr<String>("r", inner)

                val path = MongoPropertyPath.start(r)
                    .append<String, String>("a")
                    .property<String>("b")
                    .property<String>("c")

                val list = path.getAsList()

                list.size shouldBe 4  // root + a + b + c
            }

            "getRoot for single step returns null (no previous)" {
                val inner = MongoNameExpr(name = "items", type = kType<List<String>>())
                val r = MongoIterableExpr<String>("r", inner)

                val path = MongoPropertyPath.start(r)

                path.getRoot() shouldBe null
            }

            "getRoot for chained path returns null (recursive base case)" {
                val inner = MongoNameExpr(name = "items", type = kType<List<String>>())
                val r = MongoIterableExpr<String>("r", inner)

                val path = MongoPropertyPath.start(r)
                    .append<String, String>("field")

                val root = path.getRoot()

                root shouldBe null
            }

            "dropRoot returns null for root-only path" {
                val inner = MongoNameExpr(name = "items", type = kType<List<String>>())
                val r = MongoIterableExpr<String>("r", inner)

                val path = MongoPropertyPath.start(r)

                path.dropRoot() shouldBe null
            }

            "dropRoot returns path without root" {
                val inner = MongoNameExpr(name = "items", type = kType<List<String>>())
                val r = MongoIterableExpr<String>("r", inner)

                val path = MongoPropertyPath.start(r)
                    .append<String, String>("field")

                val dropped = path.dropRoot()

                dropped?.previous shouldBe null
                dropped?.getAsList()?.size shouldBe 1
            }
        }
    }
}
