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

// {"sub":"1234567890","name":"John Doe","iat":1516239022}
private const val EDGE_JWT =
    "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9" +
            ".eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ" +
            ".SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"

@Suppress("TestFunctionName")
private fun Tag.JwtDecodeEdgeComponent() = comp { JwtDecodeEdgeComponent(it) }

private class JwtDecodeEdgeComponent(ctx: NoProps) : PureComponent(ctx) {
    private val jwt by subscribingTo(addons.jwtDecode)

    override fun VDom.render() {
        div(classes = "result") {
            val a = jwt
            if (a == null) {
                +"loading"
            } else {
                val obj = a.decodeJwt(EDGE_JWT)
                val claims = a.decodeJwtAsMap(EDGE_JWT)
                +"obj=${obj != null} sub=${claims["sub"] == "1234567890"} iat=${claims["iat"] != null}"
            }
        }
    }
}

/** Edge cases for jwtdecode: the raw-object decode and full claim extraction (sub + iat). */
class JwtDecodeEdgeSpec : StringSpec() {

    init {
        "decodeJwt returns a non-null object and decodeJwtAsMap exposes all claims" {
            TestBed.preact(
                appSetup = { addons { jwtDecode() } },
                view = { JwtDecodeEdgeComponent() },
            ) { root ->
                eventually {
                    root.selectCss(".result").textContent() shouldBe "obj=true sub=true iat=true"
                }
            }
        }
    }
}
