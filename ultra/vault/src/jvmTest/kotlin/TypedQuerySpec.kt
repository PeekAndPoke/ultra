package io.peekandpoke.ultra.vault

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.lang.Expression

class TypedQuerySpec : StringSpec({

    "TypedQuery.of creates a query with empty string and no vars" {
        val query = TypedQuery.of(kType<String>())

        query.query shouldBe ""
        query.vars shouldBe emptyMap()
    }

    "TypedQuery.of root expression carries the correct list type" {
        val query = TypedQuery.of(kType<Int>())

        query.root.shouldBeInstanceOf<Expression<List<Int>>>()
    }

    "TypedQuery.of root expression getType returns List type" {
        val typeRef = kType<String>()
        val query = TypedQuery.of(typeRef)

        query.root.getType() shouldBe typeRef.list
    }

    "TypedQuery.of with different types creates distinct queries" {
        val stringQuery = TypedQuery.of(kType<String>())
        val intQuery = TypedQuery.of(kType<Int>())

        (stringQuery.root.getType() == intQuery.root.getType()) shouldBe false
    }
})
