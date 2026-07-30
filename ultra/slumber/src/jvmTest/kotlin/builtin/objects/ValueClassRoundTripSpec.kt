package io.peekandpoke.ultra.slumber.builtin.objects

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.slumber.slumber
import kotlin.time.Duration.Companion.seconds

// Value classes must be top-level / member (not local), so declare the fixtures here.

@JvmInline
value class VcEmail(val value: String)

@JvmInline
value class VcIntId(val value: Int)

// Private ctor + transform factory — the canonical `Email` shape. Slumber must construct via the ctor
// (no factory), matching kotlinx auto handling; canonicalization is the write boundary's job.
@JvmInline
value class VcEmailPriv private constructor(val value: String) {
    companion object {
        fun of(raw: String) = VcEmailPriv(raw.trim().lowercase())
    }
}

data class VcHolder(
    val email: VcEmail,
    val opt: VcEmail? = null,
    val many: List<VcEmail> = emptyList(),
    val id: VcIntId = VcIntId(0),
)

// Generic value class (Kotlin 1.7+). Its underlying is the reified type parameter.
@JvmInline
value class VcBoxed<T>(val value: T)

data class VcGenericHolder(
    val str: VcBoxed<String>,
    val num: VcBoxed<Int>,
)

// Value class over a NULLABLE underlying — N(null) must round-trip (not collapse to a null field).
@JvmInline
value class VcNullableInner(val value: String?)

// Value class over a COMPLEX (data class) inner — serializes as the inner object, not a scalar.
data class VcPoint(val x: Int, val y: Int)

@JvmInline
value class VcWrappedPoint(val value: VcPoint)

// Value class IMPLEMENTING an interface (value classes may implement interfaces).
interface VcHasName {
    val name: String
}

@JvmInline
value class VcNamed(val value: String) : VcHasName {
    override val name: String get() = value
}

// NON-optional (no default) nullable value-class field — exercises the awake null-guard.
data class VcRequiredNullable(val opt: VcEmail?)

// Value class over another value class (allowed; not self-referential).
@JvmInline
value class VcWrappedEmail(val value: VcEmail)

class ValueClassRoundTripSpec : StringSpec({

    val codec = Codec.default

    "String-backed value class slumbers to a plain string (matches kotlinx)" {
        codec.slumber(VcEmail("foo")) shouldBe "foo"
        codec.awake(VcEmail::class, "foo") shouldBe VcEmail("foo")
    }

    "Int-backed value class slumbers to a plain number (inner codec, matches kotlinx)" {
        codec.slumber(VcIntId(42)) shouldBe 42
        codec.awake(VcIntId::class, 42) shouldBe VcIntId(42)
        // The inner codec coerces like a bare Int would (string "7" -> 7).
        codec.awake(VcIntId::class, "7") shouldBe VcIntId(7)
    }

    "value class nested in a data class flattens to scalars (nested / nullable / list / int)" {
        val source = VcHolder(
            email = VcEmail("foo"),
            opt = VcEmail("bar"),
            many = listOf(VcEmail("a"), VcEmail("b")),
            id = VcIntId(7),
        )

        val slumbered = codec.slumber(source)

        slumbered shouldBe mapOf(
            "email" to "foo",
            "opt" to "bar",
            "many" to listOf("a", "b"),
            "id" to 7,
        )

        codec.awake(VcHolder::class, slumbered) shouldBe source
    }

    "nullable value-class field round-trips when absent" {
        val source = VcHolder(email = VcEmail("foo"))

        val slumbered = codec.slumber(source)
        // Round-trip is the invariant that matters; the null-field shape is slumber's data-class behavior.
        codec.awake(VcHolder::class, slumbered) shouldBe source
    }

    "generic value class round-trips with the inner type reified (String- and Int-parameterized)" {
        codec.slumber(VcBoxed("gen")) shouldBe "gen"
        codec.awake<VcBoxed<String>>("gen") shouldBe VcBoxed("gen")

        codec.slumber(VcBoxed(3)) shouldBe 3
        codec.awake<VcBoxed<Int>>(3) shouldBe VcBoxed(3)
    }

    "generic value class nested in a data class flattens each to its inner scalar" {
        val source = VcGenericHolder(str = VcBoxed("gen"), num = VcBoxed(9))

        val slumbered = codec.slumber(source)

        slumbered shouldBe mapOf("str" to "gen", "num" to 9)

        codec.awake(VcGenericHolder::class, slumbered) shouldBe source
    }

    "value class over a NULLABLE underlying round-trips a null inner" {
        codec.slumber(VcNullableInner("x")) shouldBe "x"
        codec.awake(VcNullableInner::class, "x") shouldBe VcNullableInner("x")

        codec.slumber(VcNullableInner(null)) shouldBe null
        codec.awake(VcNullableInner::class, null) shouldBe VcNullableInner(null)
    }

    "value class over a COMPLEX (data-class) inner serializes as the inner object" {
        val source = VcWrappedPoint(VcPoint(1, 2))

        val slumbered = codec.slumber(source)

        slumbered shouldBe mapOf("x" to 1, "y" to 2)

        codec.awake(VcWrappedPoint::class, slumbered) shouldBe source
    }

    "value class as a Map key and as a Map value" {
        val byKey: Map<VcEmail, Int> = mapOf(VcEmail("a") to 1, VcEmail("b") to 2)
        codec.slumber(byKey) shouldBe mapOf("a" to 1, "b" to 2)
        codec.awake<Map<VcEmail, Int>>(mapOf("a" to 1, "b" to 2)) shouldBe byKey

        val byValue: Map<String, VcEmail> = mapOf("k" to VcEmail("v"))
        codec.slumber(byValue) shouldBe mapOf("k" to "v")
        codec.awake<Map<String, VcEmail>>(mapOf("k" to "v")) shouldBe byValue
    }

    "value class implementing an interface round-trips as its underlying" {
        codec.slumber(VcNamed("bob")) shouldBe "bob"
        codec.awake(VcNamed::class, "bob") shouldBe VcNamed("bob")
    }

    "NON-optional nullable value-class field round-trips null (regression for the awake null-guard)" {
        val nullCase = VcRequiredNullable(null)
        codec.slumber(nullCase) shouldBe mapOf("opt" to null)
        codec.awake(VcRequiredNullable::class, mapOf("opt" to null)) shouldBe nullCase

        val presentCase = VcRequiredNullable(VcEmail("x"))
        codec.awake(VcRequiredNullable::class, codec.slumber(presentCase)) shouldBe presentCase
    }

    "collections carrying nullable value classes round-trip null entries" {
        codec.awake<List<VcEmail?>>(listOf("a", null)) shouldBe listOf(VcEmail("a"), null)
        codec.awake<Map<String, VcEmail?>>(mapOf("k" to null, "j" to "v")) shouldBe
                mapOf("k" to null, "j" to VcEmail("v"))
    }

    "value class over another value class flattens to the innermost scalar" {
        codec.slumber(VcWrappedEmail(VcEmail("deep"))) shouldBe "deep"
        codec.awake(VcWrappedEmail::class, "deep") shouldBe VcWrappedEmail(VcEmail("deep"))
    }

    "stdlib value classes are NOT handled generically — they fail fast, not silently mis-serialized" {
        shouldThrowAny { codec.slumber(5u) }           // UInt -> would emit a signed Int
        shouldThrowAny { codec.slumber(3.seconds) }    // kotlin.time.Duration -> would emit a packed Long
    }

    "private-ctor value class: slumber uses the (already-canonical) stored value; awake constructs via ctor" {
        // Stored canonical (of() normalized before storage) -> emitted as-is.
        codec.slumber(VcEmailPriv.of("FOO@X")) shouldBe "foo@x"
        // Awake constructs via the ctor with NO transform (matches kotlinx auto). The DB only ever holds
        // canonical values, so this is correct; a raw non-canonical input would NOT be normalized here.
        codec.awake(VcEmailPriv::class, "foo@x") shouldBe VcEmailPriv.of("foo@x")
        codec.awake(VcEmailPriv::class, "MiXeD@X")!!.value shouldBe "MiXeD@X"
    }
})
