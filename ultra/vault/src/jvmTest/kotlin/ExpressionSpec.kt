package io.peekandpoke.ultra.vault

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.lang.Expression

class ExpressionSpec : StringSpec({

    // helper: a simple Expression backed by a TypeRef
    fun <T> expr(type: TypeRef<T>): Expression<T> = object : Expression<T> {
        override fun getType(): TypeRef<T> = type
    }

    "upcastTo returns the same instance typed as the subtype" {
        val base: Expression<Any> = expr(kType())
        val upcasted: Expression<String> = base.upcastTo()

        upcasted shouldBeSameInstanceAs base
    }

    "downcast returns the same instance typed as the parent type" {
        val child: Expression<String> = expr(kType())
        val downcasted: Expression<Any> = child.downcast<Any, String>()

        downcasted shouldBeSameInstanceAs child
    }

    "forceCastTo returns the same instance typed as an unrelated type" {
        val original: Expression<String> = expr(kType())
        val casted: Expression<Int> = original.forceCastTo()

        casted shouldBeSameInstanceAs original
    }

    "nullable returns the same instance with nullable type" {
        val original: Expression<String> = expr(kType())
        val nullable: Expression<String?> = original.nullable()

        nullable shouldBeSameInstanceAs original
    }

    "getType returns the TypeRef the expression was created with" {
        val type = kType<String>()
        val expression = expr(type)

        expression.getType() shouldBe type
    }
})
