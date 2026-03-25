package de.peekandpoke.ultra.slumber

import de.peekandpoke.ultra.common.TypedAttributes
import de.peekandpoke.ultra.common.TypedKey
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.slumber.builtin.BuiltInModule
import de.peekandpoke.ultra.slumber.builtin.datetime.javatime.JavaTimeModule
import de.peekandpoke.ultra.slumber.builtin.datetime.kotlinx.KotlinxTimeModule
import de.peekandpoke.ultra.slumber.builtin.datetime.mp.MpDateTimeModule
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

class SlumberConfigSpec : StringSpec({

    "Default config has modules in correct order" {
        val config = SlumberConfig.default

        config.modules shouldBe listOf(
            MpDateTimeModule,
            KotlinxTimeModule,
            JavaTimeModule,
            BuiltInModule,
        )
    }

    "appendModules adds modules at the end" {
        val custom = object : SlumberModule {
            override fun getAwaker(type: kotlin.reflect.KType, attributes: TypedAttributes): Awaker? = null
            override fun getSlumberer(type: kotlin.reflect.KType, attributes: TypedAttributes): Slumberer? = null
        }

        val config = SlumberConfig.default.appendModules(custom)

        config.modules.last() shouldBeSameInstanceAs custom
        config.modules.size shouldBe 5
    }

    "prependModules adds modules at the front" {
        val custom = object : SlumberModule {
            override fun getAwaker(type: kotlin.reflect.KType, attributes: TypedAttributes): Awaker? = null
            override fun getSlumberer(type: kotlin.reflect.KType, attributes: TypedAttributes): Slumberer? = null
        }

        val config = SlumberConfig.default.prependModules(custom)

        config.modules.first() shouldBeSameInstanceAs custom
        config.modules.size shouldBe 5
    }

    "appendModules resets the lookup cache" {
        val config = SlumberConfig.default
        val original = config.lookup

        val custom = object : SlumberModule {
            override fun getAwaker(type: kotlin.reflect.KType, attributes: TypedAttributes): Awaker? = null
            override fun getSlumberer(type: kotlin.reflect.KType, attributes: TypedAttributes): Slumberer? = null
        }

        val appended = config.appendModules(custom)

        appended.lookup shouldNotBeSameInstanceAs original
    }

    "prependModules resets the lookup cache" {
        val config = SlumberConfig.default
        val original = config.lookup

        val custom = object : SlumberModule {
            override fun getAwaker(type: kotlin.reflect.KType, attributes: TypedAttributes): Awaker? = null
            override fun getSlumberer(type: kotlin.reflect.KType, attributes: TypedAttributes): Slumberer? = null
        }

        val prepended = config.prependModules(custom)

        prepended.lookup shouldNotBeSameInstanceAs original
    }

    "plusAttributes merges attributes" {
        val key = TypedKey<String>("test-key")

        val config = SlumberConfig.default.plusAttributes(
            TypedAttributes.of { add(key, "test-value") }
        )

        config.attributes[key] shouldBe "test-value"
    }

    "getAwaker caches results per type" {
        val config = SlumberConfig.default

        val awaker1 = config.getAwaker(kType<String>().type)
        val awaker2 = config.getAwaker(kType<String>().type)

        awaker1 shouldBeSameInstanceAs awaker2
    }

    "getSlumberer caches results per type" {
        val config = SlumberConfig.default

        val slumberer1 = config.getSlumberer(kType<String>().type)
        val slumberer2 = config.getSlumberer(kType<String>().type)

        slumberer1 shouldBeSameInstanceAs slumberer2
    }

    "getAwaker returns different instances for different types" {
        val config = SlumberConfig.default

        val stringAwaker = config.getAwaker(kType<String>().type)
        val intAwaker = config.getAwaker(kType<Int>().type)

        stringAwaker shouldNotBeSameInstanceAs intAwaker
    }

    "codec() creates a Codec backed by this config" {
        val config = SlumberConfig.default
        val codec = config.codec()

        codec.config shouldBeSameInstanceAs config
    }
})
