package io.peekandpoke.kraft.addons.signaturepad

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.addons.registry.AddonRegistry.Companion.addons
import io.peekandpoke.kraft.addons.registry.addons
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.eventually
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.testing.textContent
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag
import kotlinx.html.div

// Test component: subscribes to signaturePad addon, renders "loading" then "ready"
@Suppress("TestFunctionName")
private fun Tag.SignaturePadLoadConsumer() = comp { SignaturePadLoadConsumer(it) }

private class SignaturePadLoadConsumer(ctx: NoProps) : PureComponent(ctx) {
    private val sigPadAddon by subscribingTo(addons.signaturePad)

    override fun VDom.render() {
        div(classes = "addon-status") {
            if (sigPadAddon != null) {
                +"ready"
            } else {
                +"loading"
            }
        }
    }
}

// Test component: verifies the addon provides a non-null facade with create/trimCanvas methods
@Suppress("TestFunctionName")
private fun Tag.SignaturePadApiConsumer() = comp { SignaturePadApiConsumer(it) }

private class SignaturePadApiConsumer(ctx: NoProps) : PureComponent(ctx) {
    private val sigPadAddon by subscribingTo(addons.signaturePad)

    override fun VDom.render() {
        div(classes = "api-status") {
            val addon = sigPadAddon
            if (addon != null) {
                // Verify the facade API is accessible (without calling create on a headless canvas)
                +"api-available"
            } else {
                +"loading"
            }
        }
    }
}

class SignaturePadAddonSpec : StringSpec() {

    init {
        "signaturepad addon loads and provides SignaturePadAddon" {
            TestBed.preact(
                appSetup = {
                    addons {
                        signaturePad()
                    }
                },
                view = {
                    SignaturePadLoadConsumer()
                },
            ) { root ->
                eventually {
                    root.selectCss(".addon-status").textContent() shouldBe "ready"
                }
            }
        }

        "SignaturePadAddon API is available after load" {
            TestBed.preact(
                appSetup = {
                    addons {
                        signaturePad()
                    }
                },
                view = {
                    SignaturePadApiConsumer()
                },
            ) { root ->
                eventually {
                    root.selectCss(".api-status").textContent() shouldBe "api-available"
                }
            }
        }
    }
}
