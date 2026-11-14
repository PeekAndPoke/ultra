package io.peekandpoke.ultra.kontainer.e2e.characterization

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.kontainer.e2e.Reported
import io.peekandpoke.ultra.kontainer.e2e.reportedOf
import io.peekandpoke.ultra.kontainer.kontainer
import kotlin.reflect.KClass

class ExtendTarget
class ExtendUser(val target: ExtendTarget)

/**
 * Pins the redefinition rule across `extend` for every pair of base and extension declaration (ULTRA-00330,
 * ULTRA-00331): what the redefined service reports, and what a singleton injecting it becomes.
 */
class ExtendRedefinitionCharacterizationSpec : StringSpec({

    class Row(
        val base: Declared,
        val extension: Declared,
        val target: Reported,
        val user: Reported,
        val userShared: Boolean,
    )

    val static = Reported.StaticSingleton
    val semi = Reported.SemiDynamicSingleton

    listOf(
        Row(Declared.Singleton, Declared.Singleton, Reported.StaticSingleton, static, true),
        Row(Declared.Singleton, Declared.Prototype, Reported.StaticPrototype, static, true),
        Row(Declared.Singleton, Declared.Dynamic, Reported.DynamicSingleton, semi, false),
        Row(Declared.Singleton, Declared.DynamicPrototype, Reported.DynamicPrototype, semi, false),

        Row(Declared.Prototype, Declared.Singleton, Reported.StaticPrototype, static, true),
        Row(Declared.Prototype, Declared.Prototype, Reported.StaticPrototype, static, true),
        Row(Declared.Prototype, Declared.Dynamic, Reported.DynamicPrototype, semi, false),
        Row(Declared.Prototype, Declared.DynamicPrototype, Reported.DynamicPrototype, semi, false),

        Row(Declared.Dynamic, Declared.Singleton, Reported.DynamicSingleton, semi, false),
        Row(Declared.Dynamic, Declared.Prototype, Reported.DynamicSingleton, semi, false),
        Row(Declared.Dynamic, Declared.Dynamic, Reported.DynamicSingleton, semi, false),
        Row(Declared.Dynamic, Declared.DynamicPrototype, Reported.DynamicSingleton, semi, false),

        Row(Declared.DynamicPrototype, Declared.Singleton, Reported.DynamicPrototype, semi, false),
        Row(Declared.DynamicPrototype, Declared.Prototype, Reported.DynamicPrototype, semi, false),
        Row(Declared.DynamicPrototype, Declared.Dynamic, Reported.DynamicPrototype, semi, false),
        Row(Declared.DynamicPrototype, Declared.DynamicPrototype, Reported.DynamicPrototype, semi, false),
    ).forEach { row ->

        "Extend: ${row.base} in the base, ${row.extension} in the extension: ${row.target}, the user ${row.user}" {

            @Suppress("UNCHECKED_CAST")
            val target = ExtendTarget::class as KClass<Any>

            val extended = kontainer {
                declare(row.base)(target)
                singleton(ExtendUser::class)
            }.extend {
                declare(row.extension)(target)
            }

            val one = extended.create()
            val two = extended.create()

            assertSoftly {
                one.reportedOf(ExtendTarget::class) shouldBe row.target
                one.reportedOf(ExtendUser::class) shouldBe row.user
                (one.get(ExtendUser::class) === two.get(ExtendUser::class)) shouldBe row.userShared
            }
        }
    }
})
