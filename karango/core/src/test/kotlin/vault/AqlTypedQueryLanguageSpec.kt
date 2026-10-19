package io.peekandpoke.karango.vault

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.karango.aql.EQ
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.buildAqlQuery
import io.peekandpoke.karango.e2e.database
import io.peekandpoke.karango.testdomain.name
import io.peekandpoke.karango.testdomain.testPersons
import io.peekandpoke.ultra.reflection.kType

/**
 * The label a query's text is profiled under. `VaultCollector` keeps the text of `aql` entries, so only text
 * the DSL printed -- every value a placeholder -- may carry it.
 */
class AqlTypedQueryLanguageSpec : StringSpec() {

    private fun printed() = buildAqlQuery {
        FOR("iter", database.testPersons) {
            FILTER(it.name EQ "a-secret")
            RETURN(it)
        }
    }

    init {
        "text printed from the DSL is aql" {
            printed().queryLanguage shouldBe AqlTypedQuery.LANGUAGE
            AqlTypedQuery.LANGUAGE shouldBe "aql"
        }

        "raw text from AqlTypedQuery.of is aql-raw: it can hold literals" {
            val raw = AqlTypedQuery.of(kType<Any>(), "FOR d IN c FILTER d.token == 'a-secret' RETURN d")

            raw.queryLanguage shouldBe AqlTypedQuery.LANGUAGE_RAW
            AqlTypedQuery.LANGUAGE_RAW shouldBe "aql-raw"
        }

        "a printed query copied with other text is aql-raw too" {
            val query = printed()

            query.copy(query = query.query.replace("@v_1", "'a-secret'")).queryLanguage shouldBe AqlTypedQuery.LANGUAGE_RAW
            query.copy(vars = emptyMap()).queryLanguage shouldBe AqlTypedQuery.LANGUAGE
        }
    }
}
