package io.peekandpoke.monko

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class MonkoDriverFindQueryBuilderSpec : FreeSpec() {

    init {
        "FindQueryBuilder" - {

            "default builder" - {

                "print should show null fields" {
                    val builder = MonkoDriver.FindQueryBuilder()
                    val printed = builder.print()

                    printed shouldContain "\"filter\""
                    printed shouldContain "\"sort\""
                    printed shouldContain "\"limit\""
                    printed shouldContain "\"skip\""
                }

                "filter should be null initially" {
                    val builder = MonkoDriver.FindQueryBuilder()

                    builder.filter shouldBe null
                }
            }

            "filter" - {

                "should set a simple equality filter" {
                    val builder = MonkoDriver.FindQueryBuilder()
                    builder.filter(Filters.eq("name", "Alice"))

                    val printed = builder.print()
                    printed shouldContain "name"
                    printed shouldContain "Alice"
                }

                "should set an AND filter" {
                    val builder = MonkoDriver.FindQueryBuilder()
                    builder.filter(
                        Filters.and(
                            Filters.eq("name", "Alice"),
                            Filters.gt("age", 18),
                        )
                    )

                    val printed = builder.print()
                    printed shouldContain "name"
                    printed shouldContain "age"
                }
            }

            "sort" - {

                "should set ascending sort" {
                    val builder = MonkoDriver.FindQueryBuilder()
                    builder.sort(Sorts.ascending("name"))

                    val printed = builder.print()
                    printed shouldContain "sort"
                    printed shouldContain "name"
                }

                "should set descending sort" {
                    val builder = MonkoDriver.FindQueryBuilder()
                    builder.sort(Sorts.descending("createdAt"))

                    val printed = builder.print()
                    printed shouldContain "createdAt"
                }

                "should set compound sort" {
                    val builder = MonkoDriver.FindQueryBuilder()
                    builder.sort(
                        Sorts.orderBy(
                            Sorts.ascending("name"),
                            Sorts.descending("age"),
                        )
                    )

                    val printed = builder.print()
                    printed shouldContain "name"
                    printed shouldContain "age"
                }
            }

            "limit" - {

                "should set the limit" {
                    val builder = MonkoDriver.FindQueryBuilder()
                    builder.limit(10)

                    builder.print() shouldContain "\"limit\": 10"
                }

                "should allow limit of 1" {
                    val builder = MonkoDriver.FindQueryBuilder()
                    builder.limit(1)

                    builder.print() shouldContain "\"limit\": 1"
                }
            }

            "skip" - {

                "should set the skip" {
                    val builder = MonkoDriver.FindQueryBuilder()
                    builder.skip(20)

                    builder.print() shouldContain "\"skip\": 20"
                }

                "should allow skip of 0" {
                    val builder = MonkoDriver.FindQueryBuilder()
                    builder.skip(0)

                    builder.print() shouldContain "\"skip\": 0"
                }
            }

            "page" - {

                "page 1 with epp 20 should skip 0 and limit 20" {
                    val builder = MonkoDriver.FindQueryBuilder()
                    builder.page(page = 1, epp = 20)

                    val printed = builder.print()
                    printed shouldContain "\"skip\": 0"
                    printed shouldContain "\"limit\": 20"
                }

                "page 5 with epp 25 should skip 100 and limit 25" {
                    val builder = MonkoDriver.FindQueryBuilder()
                    builder.page(page = 5, epp = 25)

                    val printed = builder.print()
                    printed shouldContain "\"skip\": 100"
                    printed shouldContain "\"limit\": 25"
                }

                "page 0 should be clamped to page 1 (skip 0)" {
                    val builder = MonkoDriver.FindQueryBuilder()
                    builder.page(page = 0, epp = 10)

                    val printed = builder.print()
                    printed shouldContain "\"skip\": 0"
                    printed shouldContain "\"limit\": 10"
                }

                "negative page should be clamped to page 1 (skip 0)" {
                    val builder = MonkoDriver.FindQueryBuilder()
                    builder.page(page = -10, epp = 10)

                    val printed = builder.print()
                    printed shouldContain "\"skip\": 0"
                    printed shouldContain "\"limit\": 10"
                }

                "default epp should be 20" {
                    val builder = MonkoDriver.FindQueryBuilder()
                    builder.page(page = 1)

                    builder.print() shouldContain "\"limit\": 20"
                }

                "large page number" {
                    val builder = MonkoDriver.FindQueryBuilder()
                    builder.page(page = 100, epp = 50)

                    val printed = builder.print()
                    printed shouldContain "\"skip\": 4950"
                    printed shouldContain "\"limit\": 50"
                }
            }

            "combination of filter, sort, limit, skip" {
                val builder = MonkoDriver.FindQueryBuilder()
                builder.filter(Filters.eq("status", "active"))
                builder.sort(Sorts.ascending("name"))
                builder.limit(5)
                builder.skip(10)

                val printed = builder.print()
                printed shouldContain "status"
                printed shouldContain "active"
                printed shouldContain "name"
                printed shouldContain "\"limit\": 5"
                printed shouldContain "\"skip\": 10"
            }
        }
    }
}
