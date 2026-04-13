package io.peekandpoke.monko

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.monko.lang.MongoIterableExpr
import io.peekandpoke.monko.lang.MongoPrinter
import io.peekandpoke.monko.lang.MongoPropertyPath
import io.peekandpoke.ultra.reflection.kType

class MonkoRepositoryFieldSpec : FreeSpec() {

    data class Address(
        val city: String,
        val zip: String,
    )

    data class Person(
        val name: String,
        val age: Int,
        val address: Address,
    )

    private class PersonRepo : MonkoRepository<Person>(
        name = "persons",
        storedType = kType(),
        driver = createMinimalDriver(),
    )

    companion object {
        private fun createMinimalDriver(): MonkoDriver = MonkoDriver(
            lazyCodec = lazy { error("Not needed for field tests") },
            lazyClient = lazy { error("Not needed for field tests") },
            lazyDatabase = lazy { error("Not needed for field tests") },
        )
    }

    init {
        "MonkoRepository.field" - {

            "should extract field path for simple property" {
                val repo = PersonRepo()

                val fieldPath = repo.field { r ->
                    MongoPropertyPath.start(r).append<String, String>("name")
                }

                fieldPath shouldBe "name"
            }

            "should extract field path for nested property" {
                val repo = PersonRepo()

                val fieldPath = repo.field { r ->
                    MongoPropertyPath.start(r)
                        .append<Address, Address>("address")
                        .property<String>("city")
                }

                fieldPath shouldBe "address.city"
            }
        }

        "MonkoRepository" - {

            "name should return collection name" {
                val repo = PersonRepo()

                repo.name shouldBe "persons"
            }

            "storedType should return the correct type" {
                val repo = PersonRepo()

                repo.storedType shouldBe kType<Person>()
            }

            "repo should return itself" {
                val repo = PersonRepo()

                repo.repo shouldBe repo
            }

            "print should print the repository name" {
                val repo = PersonRepo()
                val printer = MongoPrinter()
                repo.print(printer)

                val result = printer.build()
                result.query shouldBe "persons"
            }

            "repoExpr should be an iterable expression" {
                val repo = PersonRepo()

                val expr = repo.repoExpr

                expr.printQuery() shouldBe "repo"
            }
        }
    }

    private fun MongoIterableExpr<Person>.printQuery(): String {
        val printer = MongoPrinter()
        this.print(printer)
        return printer.build().query
    }
}
