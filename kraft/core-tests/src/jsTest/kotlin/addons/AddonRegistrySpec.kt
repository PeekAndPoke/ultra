package io.peekandpoke.kraft.coretests.addons

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.peekandpoke.kraft.addons.registry.Addon
import io.peekandpoke.kraft.addons.registry.AddonKey
import io.peekandpoke.kraft.addons.registry.AddonRegistry
import io.peekandpoke.kraft.addons.registry.AddonRegistry.Companion.addons
import io.peekandpoke.kraft.addons.registry.AddonRegistryBuilder
import io.peekandpoke.kraft.addons.registry.addons
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.eventually
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.testing.textContent
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.coroutines.delay
import kotlinx.html.Tag
import kotlinx.html.div

// Test addon type
private class TestAddon(val greeting: String)

private val testAddonKey = AddonKey<TestAddon>("test")

private val AddonRegistry.test: Addon<TestAddon>
    get() = this[testAddonKey]

// Test component that uses an addon
@Suppress("TestFunctionName")
private fun Tag.AddonConsumer() = comp { AddonConsumer(it) }

private class AddonConsumer(ctx: NoProps) : PureComponent(ctx) {
    private val testAddon by subscribingTo(addons.test)

    override fun VDom.render() {
        div(classes = "addon-status") {
            val addon = testAddon
            if (addon != null) {
                +addon.greeting
            } else {
                +"loading"
            }
        }
    }
}

class AddonRegistrySpec : StringSpec() {

    init {
        "addon starts as null when lazy" {
            val builder = AddonRegistryBuilder()

            val addon = builder.register(
                key = testAddonKey,
                name = "test",
                lazy = true,
            ) {
                delay(100)
                TestAddon("hello")
            }

            builder.build().loadEager()

            addon() shouldBe null
        }

        "addon loads eagerly by default" {
            val builder = AddonRegistryBuilder()

            val addon = builder.register(
                key = testAddonKey,
                name = "test",
                lazy = false,
            ) {
                delay(50)
                TestAddon("hello")
            }

            builder.build().loadEager()

            eventually {
                addon() shouldNotBe null
                addon()!!.greeting shouldBe "hello"
            }
        }

        "lazy addon loads on first subscribe" {
            val builder = AddonRegistryBuilder()

            val addon = builder.register(
                key = testAddonKey,
                name = "test",
                lazy = true,
            ) {
                delay(50)
                TestAddon("lazy-loaded")
            }

            builder.build().loadEager()

            addon() shouldBe null

            val unsub = addon.subscribeToStream { }

            eventually {
                addon() shouldNotBe null
                addon()!!.greeting shouldBe "lazy-loaded"
            }

            unsub()
        }

        // ULTRA-00357: loadEager must not start a lazy addon; reading or subscribing starts it, once
        "loadEager does not call the loader of a lazy addon" {
            val builder = AddonRegistryBuilder()
            var calls = 0

            val addon = builder.register(
                key = testAddonKey,
                name = "test",
                lazy = true,
            ) {
                calls++
                TestAddon("lazy")
            }

            builder.build().loadEager()
            delay(50)

            calls shouldBe 0

            val unsub = addon.subscribeToStream { }
            addon.subscribeToStream { }()
            addon()

            eventually {
                addon()?.greeting shouldBe "lazy"
            }
            calls shouldBe 1

            unsub()
        }

        "a read alone starts loading a lazy addon" {
            val builder = AddonRegistryBuilder()
            var calls = 0

            val addon = builder.register(
                key = testAddonKey,
                name = "test",
                lazy = true,
            ) {
                calls++
                TestAddon("read")
            }

            builder.build().loadEager()

            addon() shouldBe null

            eventually {
                addon()?.greeting shouldBe "read"
            }
            calls shouldBe 1
        }

        "loadEager calls the loader of a non-lazy addon once" {
            val builder = AddonRegistryBuilder()
            var calls = 0

            builder.register(
                key = testAddonKey,
                name = "test",
                lazy = false,
            ) {
                calls++
                TestAddon("eager")
            }

            val registry = builder.build()
            registry.loadEager()
            registry.loadEager()

            eventually {
                calls shouldBe 1
            }
        }

        "accessing unregistered addon throws" {
            val registry = AddonRegistryBuilder().build()

            var threw = false
            try {
                registry[testAddonKey]
            } catch (e: IllegalStateException) {
                threw = true
            }

            threw shouldBe true
        }

        "getOrNull returns null for unregistered addon" {
            val registry = AddonRegistryBuilder().build()

            registry.getOrNull(testAddonKey) shouldBe null
        }

        "multiple addons load independently" {
            val key1 = AddonKey<String>("addon1")
            val key2 = AddonKey<String>("addon2")

            val builder = AddonRegistryBuilder()

            val addon1 = builder.register(key = key1, name = "addon1") {
                delay(50)
                "first"
            }

            val addon2 = builder.register(key = key2, name = "addon2") {
                delay(200)
                "second"
            }

            builder.build().loadEager()

            eventually {
                addon1() shouldBe "first"
            }

            eventually {
                addon2() shouldBe "second"
            }
        }

        "addon stream notifies subscribers on load" {
            val builder = AddonRegistryBuilder()

            val addon = builder.register(
                key = testAddonKey,
                name = "test",
            ) {
                delay(50)
                TestAddon("streamed")
            }

            builder.build().loadEager()

            val received = mutableListOf<TestAddon?>()
            val unsub = addon.subscribeToStream { received.add(it) }

            eventually {
                received.size shouldBe 2
                received[0] shouldBe null
                received[1]!!.greeting shouldBe "streamed"
            }

            unsub()
        }

        "component redraws when addon becomes ready" {

            TestBed.preact(
                appSetup = {
                    addons {
                        register(key = testAddonKey, name = "test") {
                            delay(100)
                            TestAddon("addon-ready")
                        }
                    }
                },
                view = {
                    AddonConsumer()
                },
            ) { root ->
                root.selectCss(".addon-status").textContent() shouldBe "loading"

                eventually {
                    root.selectCss(".addon-status").textContent() shouldBe "addon-ready"
                }
            }
        }

        "failed loader leaves addon as null" {
            val builder = AddonRegistryBuilder()

            val addon = builder.register(
                key = testAddonKey,
                name = "test",
            ) {
                error("load failed")
            }

            builder.build().loadEager()

            delay(200)

            addon() shouldBe null
        }
    }
}
