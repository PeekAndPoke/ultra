package io.peekandpoke.kraft.addons.jwtdecode

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

// Test JWT: {"sub":"1234567890","name":"John Doe","iat":1516239022}
private const val TEST_JWT =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
            ".eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ" +
            ".SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

// Component that shows addon loading status
@Suppress("TestFunctionName")
private fun Tag.JwtDecodeStatus() = comp { JwtDecodeStatus(it) }

private class JwtDecodeStatus(ctx: NoProps) : PureComponent(ctx) {
    private val jwtDecode by subscribingTo(addons.jwtDecode)

    override fun VDom.render() {
        div(classes = "addon-status") {
            if (jwtDecode != null) {
                +"ready"
            } else {
                +"loading"
            }
        }
    }
}

// Component that decodes a JWT and renders the "name" claim
@Suppress("TestFunctionName")
private fun Tag.JwtDecodeResult() = comp { JwtDecodeResult(it) }

private class JwtDecodeResult(ctx: NoProps) : PureComponent(ctx) {
    private val jwtDecode by subscribingTo(addons.jwtDecode)

    override fun VDom.render() {
        div(classes = "jwt-result") {
            val addon: JwtDecodeAddon? = jwtDecode
            if (addon != null) {
                val claims = addon.decodeJwtAsMap(TEST_JWT)
                +(claims["name"] as? String ?: "unknown")
            } else {
                +"loading"
            }
        }
    }
}

class JwtDecodeAddonSpec : StringSpec() {

    init {
        "jwtdecode addon loads and provides JwtDecodeAddon" {
            TestBed.preact(
                appSetup = {
                    addons {
                        jwtDecode()
                    }
                },
                view = {
                    JwtDecodeStatus()
                },
            ) { root ->
                eventually {
                    root.selectCss(".addon-status").textContent() shouldBe "ready"
                }
            }
        }

        "decodeJwtAsMap returns correct claims" {
            TestBed.preact(
                appSetup = {
                    addons {
                        jwtDecode()
                    }
                },
                view = {
                    JwtDecodeResult()
                },
            ) { root ->
                eventually {
                    root.selectCss(".jwt-result").textContent() shouldBe "John Doe"
                }
            }
        }
    }
}
