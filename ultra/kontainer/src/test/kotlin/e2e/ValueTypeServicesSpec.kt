package io.peekandpoke.ultra.kontainer.e2e

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.kontainer.module
import kotlin.reflect.KClass

class ValueTypeImpl

class NeedsNumbers(val numbers: List<Number>)

@JvmInline
value class ValueTypeId(val value: String)

/**
 * Pins that value types (numbers, chars, strings and the like) cannot be services (ULTRA-00354): every registration
 * form refuses them, in both JVM forms (`int` and `java.lang.Integer`), and so does an override at create or clone.
 */
class ValueTypeServicesSpec : StringSpec({

    /** One refused class in one JVM form, with a [sample] value of it and the [name] the messages must give. */
    class ValueType(val cls: KClass<*>, val sample: Any, val name: String) {
        @Suppress("UNCHECKED_CAST")
        val any = cls as KClass<Any>

        /** Names the JVM form: `kotlin.Int` is `int` or `java.lang.Integer`. */
        val label = "$name (${cls.java.name})"
    }

    /** `Int::class` is Java `int`; its object type is `java.lang.Integer`, the class of a boxed value. */
    val primitiveForms = listOf(
        ValueType(Boolean::class, true, "kotlin.Boolean"),
        ValueType(Byte::class, 1.toByte(), "kotlin.Byte"),
        ValueType(Short::class, 1.toShort(), "kotlin.Short"),
        ValueType(Int::class, 5, "kotlin.Int"),
        ValueType(Long::class, 5L, "kotlin.Long"),
        ValueType(Float::class, 1.5f, "kotlin.Float"),
        ValueType(Double::class, 2.5, "kotlin.Double"),
        ValueType(Char::class, 'c', "kotlin.Char"),
    )

    val objectForms = primitiveForms.map { v -> ValueType(v.cls.javaObjectType.kotlin, v.sample, v.name) }

    /** One JVM form each: the unsigned types are value classes, boxed as `kotlin.UInt` and so on. */
    val singleForms = listOf(
        ValueType(UByte::class, 1.toUByte(), "kotlin.UByte"),
        ValueType(UShort::class, 1.toUShort(), "kotlin.UShort"),
        ValueType(UInt::class, 5u, "kotlin.UInt"),
        ValueType(ULong::class, 5uL, "kotlin.ULong"),
        ValueType(String::class, "text", "kotlin.String"),
        ValueType(Unit::class, Unit, "kotlin.Unit"),
    )

    val refused = primitiveForms + objectForms + singleForms

    fun outcome(block: () -> Unit): String = try {
        block()
        "accepted"
    } catch (e: Exception) {
        "${e::class.simpleName}: ${e.message}"
    }

    fun notRegistered(v: ValueType) = "KontainerInconsistent: ${v.name} is a value type and cannot be registered"

    fun notOverridden(name: String) = "KontainerInconsistent: $name is a value type and cannot be overridden; " +
            "name the service class instead: with(Service::class) { ... }"

    class Form(val name: String, val register: KontainerBuilder.(ValueType) -> Unit)

    val builders = listOf<Pair<String, KontainerBuilder.() -> KontainerBuilder.ServiceBuilder>>(
        "singleton" to { singleton },
        "prototype" to { prototype },
        "dynamic" to { dynamic },
        "dynamicPrototype" to { dynamicPrototype },
    )

    val forms: List<Form> = builders.flatMap { (name, builder) ->
        listOf(
            Form("$name(cls)") { v -> builder()(v.any) },
            Form("$name(cls) { value }") { v -> builder()(v.any) { v.sample } },
            Form("$name(cls, Impl::class)") { v -> builder()(v.any, ValueTypeImpl::class) },
            Form("$name(Any::class, cls)") { v -> builder()(Any::class, v.any) },
        )
    } + listOf(
        Form("instance(value)") { v -> instance(v.sample) },
        Form("instance(cls, value)") { v -> instance(v.any, v.sample) },
    )

    "The fixture: both JVM forms of each primitive are equal as KClass, but not as Java classes" {

        assertSoftly {
            primitiveForms.map { it.cls.java.isPrimitive } shouldBe List(primitiveForms.size) { true }
            objectForms.map { it.cls.java.isPrimitive } shouldBe List(primitiveForms.size) { false }
            primitiveForms.map { it.cls } shouldBe objectForms.map { it.cls }
            objectForms.map { it.cls.java } shouldBe objectForms.map { it.sample::class.java }
            singleForms.map { it.cls.java } shouldBe singleForms.map { it.sample::class.java }
        }
    }

    forms.forEach { form ->

        "${form.name} refuses every value type in every JVM form, at the defining call" {

            refused.associate { v ->
                var reachedNextLine = false

                v.label to outcome {
                    kontainer {
                        form.register(this, v)
                        reachedNextLine = true
                    }
                }.let { if (reachedNextLine) "after the call: $it" else it }
            } shouldBe refused.associate { v -> v.label to notRegistered(v) }
        }
    }

    "In a module and in extend, every form refuses a value type in both JVM forms" {

        val ints = refused.filter { it.cls == Int::class }

        forms.forEach { form ->
            ints.forEach { v ->
                assertSoftly {
                    outcome { kontainer { module(module { form.register(this, v) }) } } shouldBe notRegistered(v)
                    outcome { kontainer { }.extend { form.register(this, v) } } shouldBe notRegistered(v)
                }
            }
        }
    }

    // Overrides at create and clone ///////////////////////////////////////////////////////////////////////////////

    val emptyBlueprint = kontainer { }

    /** A dynamic of a base of every value type: an override by the object form fits it by Java assignability. */
    val anyBlueprint = kontainer { dynamic(Any::class) { Any() } }

    listOf("no dynamics" to emptyBlueprint, "a dynamic Any" to anyBlueprint).forEach { (name, blueprint) ->

        "With $name, an override of a value type at create is refused in every JVM form" {

            refused.associate { v ->
                v.label to outcome { blueprint.create { with(v.any) { v.sample } } }
            } shouldBe refused.associate { v -> v.label to notOverridden(v.name) }
        }

        "With $name, an override of a value type at clone is refused in every JVM form" {

            val original = blueprint.create()

            refused.associate { v ->
                v.label to outcome { original.clone { with(v.any) { v.sample } } }
            } shouldBe refused.associate { v -> v.label to notOverridden(v.name) }
        }
    }

    "The reified with<Int> and with { text } are refused" {

        assertSoftly {
            outcome { anyBlueprint.create { with<Int> { 5 } } } shouldBe notOverridden("kotlin.Int")
            outcome { anyBlueprint.create { with { "text" } } } shouldBe notOverridden("kotlin.String")
        }
    }

    // What stays allowed //////////////////////////////////////////////////////////////////////////////////////////

    "Number, CharSequence, arrays and a value class of one's own are services; a factory may return a value" {

        val ints = intArrayOf(1, 2)
        val strings = arrayOf("a", "b")
        val id = ValueTypeId("id")

        val subject = kontainer {
            instance(Number::class, 5)
            singleton(CharSequence::class) { "text" }
            instance(ints)
            instance(strings)
            instance(id)
            dynamic(Comparable::class) { 2.5 }
        }.create()

        assertSoftly {
            subject.get(Number::class) shouldBe 5
            subject.get(CharSequence::class) shouldBe "text"
            subject.get(IntArray::class) shouldBeSameInstanceAs ints
            subject.get(Array<String>::class) shouldBeSameInstanceAs strings
            subject.get(ValueTypeId::class) shouldBe id
            subject.get(Comparable::class) shouldBe 2.5
        }
    }

    "An override of Number at create is accepted, with a value as the instance" {

        val subject = kontainer { dynamic(Number::class) { 0.5 } }.create { with(Number::class) { 5 } }

        subject.get(Number::class) shouldBe 5
    }

    "The lookups by a value type keep their answers: not found, null, empty, in both JVM forms" {

        val subject = kontainer {
            instance(Number::class, 5)
            singleton(NeedsNumbers::class)
        }.create()

        val forms = listOf(Int::class, Int::class.javaObjectType.kotlin)

        assertSoftly {
            forms.map { cls -> outcome { subject.get(cls) } } shouldBe
                    List(2) { "ServiceNotFound: Service kotlin.Int was not found" }
            forms.map { cls -> subject.getOrNull(cls) } shouldBe listOf(null, null)
            forms.map { cls -> subject.getAll(cls) } shouldBe listOf(emptyList(), emptyList())
            subject.get(NeedsNumbers::class).numbers shouldBe listOf(5)
        }
    }
})
