package io.peekandpoke.kraft.addons.avatars

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.addons.registry.AddonRegistry.Companion.addons
import io.peekandpoke.kraft.addons.registry.addons
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.testing.textContent
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.coroutines.delay
import kotlinx.html.Tag
import kotlinx.html.div
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** Polls until [check] passes or [timeout] is reached. */
private suspend fun eventually(
    timeout: Duration = 2.seconds,
    poll: Duration = 25.milliseconds,
    check: suspend () -> Unit,
) {
    val deadline = kotlinx.datetime.Clock.System.now().plus(timeout)

    while (true) {
        try {
            check()
            return
        } catch (e: Throwable) {
            if (kotlinx.datetime.Clock.System.now() > deadline) throw e
            delay(poll)
        }
    }
}

// Test component: subscribes to avatars addon and renders "loading" or "ready"
@Suppress("TestFunctionName")
private fun Tag.AvatarsLoaderComponent() = comp { AvatarsLoaderComponent(it) }

private class AvatarsLoaderComponent(ctx: NoProps) : PureComponent(ctx) {
    private val avatarsAddon by subscribingTo(addons.avatars)

    override fun VDom.render() {
        div(classes = "addon-status") {
            if (avatarsAddon != null) {
                +"ready"
            } else {
                +"loading"
            }
        }
    }
}

// Test component: subscribes to avatars addon and renders SVG check result
@Suppress("TestFunctionName")
private fun Tag.AvatarsSvgComponent() = comp { AvatarsSvgComponent(it) }

private class AvatarsSvgComponent(ctx: NoProps) : PureComponent(ctx) {
    private val avatarsAddon by subscribingTo(addons.avatars)

    override fun VDom.render() {
        div(classes = "svg-status") {
            val addon = avatarsAddon
            if (addon != null) {
                val svg = addon.get("test")
                if (svg.contains("<svg")) {
                    +"has-svg"
                } else {
                    +"no-svg"
                }
            } else {
                +"loading"
            }
        }
    }
}

// Test component: subscribes to avatars addon and renders data URL check result
@Suppress("TestFunctionName")
private fun Tag.AvatarsDataUrlComponent() = comp { AvatarsDataUrlComponent(it) }

private class AvatarsDataUrlComponent(ctx: NoProps) : PureComponent(ctx) {
    private val avatarsAddon by subscribingTo(addons.avatars)

    override fun VDom.render() {
        div(classes = "dataurl-status") {
            val addon = avatarsAddon
            if (addon != null) {
                val dataUrl = addon.getDataUrl("test")
                if (dataUrl.startsWith("data:image/svg")) {
                    +"has-dataurl"
                } else {
                    +"no-dataurl"
                }
            } else {
                +"loading"
            }
        }
    }
}

class AvatarsAddonSpec : StringSpec() {

    init {
        "avatars addon loads and provides AvatarsAddon" {

            TestBed.preact(
                appSetup = {
                    addons {
                        avatars()
                    }
                },
                view = {
                    AvatarsLoaderComponent()
                },
            ) { root ->
                eventually {
                    root.selectCss(".addon-status").textContent() shouldBe "ready"
                }
            }
        }

        "AvatarsAddon generates SVG identicons" {

            TestBed.preact(
                appSetup = {
                    addons {
                        avatars()
                    }
                },
                view = {
                    AvatarsSvgComponent()
                },
            ) { root ->
                eventually {
                    root.selectCss(".svg-status").textContent() shouldBe "has-svg"
                }
            }
        }

        "AvatarsAddon generates data URLs" {

            TestBed.preact(
                appSetup = {
                    addons {
                        avatars()
                    }
                },
                view = {
                    AvatarsDataUrlComponent()
                },
            ) { root ->
                eventually {
                    root.selectCss(".dataurl-status").textContent() shouldBe "has-dataurl"
                }
            }
        }
    }
}
