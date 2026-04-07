package io.peekandpoke.monko.lang

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.monko.lang.dsl.toFieldPath
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.datetime.MpLocalDate
import io.peekandpoke.ultra.datetime.MpLocalDateTime
import io.peekandpoke.ultra.datetime.MpZonedDateTime
import io.peekandpoke.ultra.reflection.kType

class SlumberPropertiesSpec : FreeSpec() {

    data class EntityWithType(
        val _type: String,
        val name: String,
    )

    data class EntityWithInstant(
        val createdAt: MpInstant,
    )

    data class EntityWithDates(
        val zoned: MpZonedDateTime,
        val localDt: MpLocalDateTime,
        val localD: MpLocalDate,
    )

    init {
        "_type property" - {

            "MongoIterableExpr._type should produce '_type' field path" {
                val inner = MongoNameExpr(name = "items", type = kType<List<EntityWithType>>())
                val r = MongoIterableExpr<EntityWithType>("r", inner)

                val path = r._type

                path.toFieldPath() shouldBe "_type"
            }

            "MongoPathExpr._type should produce '_type' field path" {
                val inner = MongoNameExpr(name = "items", type = kType<List<EntityWithType>>())
                val r = MongoIterableExpr<EntityWithType>("r", inner)
                val pathExpr = MongoPropertyPath.start(r)
                    .append<EntityWithType, EntityWithType>("nested")

                val path = pathExpr._type

                path.toFieldPath() shouldBe "nested._type"
            }
        }

        "MpInstant.ts property" - {

            "should produce 'ts' field path from MpInstant" {
                val inner = MongoNameExpr(name = "items", type = kType<List<EntityWithInstant>>())
                val r = MongoIterableExpr<EntityWithInstant>("r", inner)
                val instantPath = MongoPropertyPath.start(r)
                    .append<MpInstant, MpInstant>("createdAt")

                val tsPath = instantPath.ts

                tsPath.toFieldPath() shouldBe "createdAt.ts"
            }
        }

        "MpZonedDateTime.ts property" - {

            "should produce 'ts' field path from MpZonedDateTime" {
                val inner = MongoNameExpr(name = "items", type = kType<List<EntityWithDates>>())
                val r = MongoIterableExpr<EntityWithDates>("r", inner)
                val zonedPath = MongoPropertyPath.start(r)
                    .append<MpZonedDateTime, MpZonedDateTime>("zoned")

                val tsPath = zonedPath.ts

                tsPath.toFieldPath() shouldBe "zoned.ts"
            }
        }

        "MpLocalDateTime.ts property" - {

            "should produce 'ts' field path from MpLocalDateTime" {
                val inner = MongoNameExpr(name = "items", type = kType<List<EntityWithDates>>())
                val r = MongoIterableExpr<EntityWithDates>("r", inner)
                val localDtPath = MongoPropertyPath.start(r)
                    .append<MpLocalDateTime, MpLocalDateTime>("localDt")

                val tsPath = localDtPath.ts

                tsPath.toFieldPath() shouldBe "localDt.ts"
            }
        }

        "MpLocalDate.ts property" - {

            "should produce 'ts' field path from MpLocalDate" {
                val inner = MongoNameExpr(name = "items", type = kType<List<EntityWithDates>>())
                val r = MongoIterableExpr<EntityWithDates>("r", inner)
                val localDPath = MongoPropertyPath.start(r)
                    .append<MpLocalDate, MpLocalDate>("localD")

                val tsPath = localDPath.ts

                tsPath.toFieldPath() shouldBe "localD.ts"
            }
        }
    }
}
