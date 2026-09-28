package io.peekandpoke.kraft.coretests.routing

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.peekandpoke.kraft.routing.Router
import io.peekandpoke.kraft.routing.Static
import io.peekandpoke.kraft.routing.router
import io.peekandpoke.kraft.testing.KQuery
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.span
import org.w3c.dom.Element
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.url.URL
import org.w3c.files.Blob

/**
 * Real clicks through a real [Router.PathRoutingStrategy].
 *
 * A window listener registered AFTER the router records whether the router prevented the default, then
 * prevents it itself so the test page never actually navigates away — unless [allowDefault] is set.
 */
private class Harness {
    private val originalHref = window.location.href

    val router: Router = router {
        usePathStrategy()
        mount(Static("/app/page")) {}
        mount(Static("/app/users/settings")) {}
        catchAll {}
    }

    /** Whether the router called preventDefault on the last click, i.e. intercepted it. */
    var intercepted: Boolean? = null

    /** Lets the browser perform the default action. Only safe for in-document navigation. */
    var allowDefault = false

    private val recorder: (Event) -> Unit = { event ->
        intercepted = event.defaultPrevented
        if (!allowDefault) event.preventDefault()
    }

    init {
        window.addEventListener("click", recorder)
        // Like an app booting: resolve the current location first
        router.navigateToWindowUri()
    }

    fun close() {
        window.removeEventListener("click", recorder)
        router.disable()
        window.history.replaceState(null, "", originalHref)
    }

    fun click(element: Element, init: (dynamic) -> Unit = {}): Boolean {
        val options: dynamic = js("{}")
        options.bubbles = true
        options.cancelable = true
        options.button = 0
        init(options)

        intercepted = null
        element.dispatchEvent(MouseEvent("click", options))

        return intercepted!!
    }

    suspend fun click(root: KQuery<Element>, css: String, init: (dynamic) -> Unit = {}): Boolean {
        return click(root.selectCss(css).first(), init)
    }
}

private suspend fun withLinks(view: VDom.() -> Any?, test: suspend Harness.(KQuery<Element>) -> Unit) {
    val harness = Harness()

    try {
        TestBed.preact(view) { root -> harness.test(root) }
    } finally {
        harness.close()
    }
}

class RouterClickInterceptionSpec : StringSpec() {
    init {
        "A plain click on a same-origin path is routed in-app" {
            withLinks({ a(href = "/app/page", classes = "l") { span(classes = "inner") { +"go" } } }) { root ->
                click(root, ".inner") shouldBe true
                window.location.pathname shouldBe "/app/page"
                router.current().uri shouldBe "/app/page"
            }
        }

        "Hrefs the browser's URL parser reads as absolute are left to the browser" {
            val origin = window.location.origin
            val host = window.location.host
            val tab = Char(9).toString()

            withLinks({
                a(classes = "backslashes") { attributes["href"] = "\\\\$host/app/page"; +"go" }
                a(classes = "slash-backslash") { attributes["href"] = "/\\$host/app/page"; +"go" }
                a(classes = "slash-tab-slash") { attributes["href"] = "/$tab/$host/app/page"; +"go" }
                a(classes = "tab-in-scheme") { attributes["href"] = origin.replaceFirst("ht", "ht$tab") + "/app/page"; +"go" }
                a(classes = "leading-control") { attributes["href"] = Char(1) + "$origin/app/page"; +"go" }
            }) { root ->
                val before = window.location.href

                click(root, ".backslashes") shouldBe false
                click(root, ".slash-backslash") shouldBe false
                click(root, ".slash-tab-slash") shouldBe false
                click(root, ".tab-in-scheme") shouldBe false
                click(root, ".leading-control") shouldBe false

                window.location.href shouldBe before
            }
        }

        "A relative href resolving to another host or protocol is left to the browser" {
            val otherProtocol = if (window.location.protocol == "https:") "http:" else "https:"

            for ((base, expected) in listOf(
                // Same protocol, other host; and same host, other protocol: each check on its own
                window.location.protocol + "//other.example/" to false,
                "$otherProtocol//${window.location.host}/" to false,
                window.location.origin + "/" to true,
            )) {
                val element = document.createElement("base")
                element.setAttribute("href", base)
                document.head!!.appendChild(element)

                try {
                    withLinks({ a(href = "app/page", classes = "l") { +"go" } }) { root ->
                        document.baseURI shouldBe base
                        click(root, ".l") shouldBe expected
                    }
                } finally {
                    element.remove()
                }
            }
        }

        "Absolute and scheme-relative URLs are left to the browser, even on the same origin" {
            // Karma serves from localhost: the host without a TLD that the old regex mistook for a path
            val origin = window.location.origin

            withLinks({
                a(href = "$origin/app/page", classes = "absolute") { +"go" }
                a(href = "  $origin/app/page", classes = "padded") { +"go" }
                a(href = origin.uppercase() + "/app/page", classes = "upper") { +"go" }
                a(href = "//" + window.location.host + "/app/page", classes = "scheme-relative") { +"go" }
            }) { root ->
                val before = window.location.href

                click(root, ".absolute") shouldBe false
                click(root, ".padded") shouldBe false
                click(root, ".upper") shouldBe false
                click(root, ".scheme-relative") shouldBe false

                window.location.href shouldBe before
            }
        }

        "A root-relative href keeps its query and fragment, and the fragment stays out of the params" {
            withLinks({ a(href = "/app/page?x=1#frag", classes = "l") { +"go" } }) { root ->
                click(root, ".l") shouldBe true
                window.location.pathname shouldBe "/app/page"
                window.location.search shouldBe "?x=1"
                window.location.hash shouldBe "#frag"
                router.current().route.pattern shouldBe "/app/page"
                router.current().matchedRoute.queryParams shouldBe mapOf("x" to "1")
            }
        }

        "A relative href resolves against the current path like the browser does" {
            withLinks({ a(href = "settings", classes = "l") { +"go" } }) { root ->
                window.history.replaceState(null, "", "/app/users/42")

                click(root, ".l") shouldBe true
                window.location.pathname shouldBe "/app/users/settings"
            }
        }

        "A target other than _self is left to the browser" {
            withLinks({
                a(href = "/app/page", target = "_blank", classes = "blank") { +"go" }
                a(href = "/app/page", target = "other-frame", classes = "named") { +"go" }
                a(href = "/app/page", target = "_self", classes = "self") { +"go" }
                a(href = "/app/page", target = "_SELF", classes = "self-upper") { +"go" }
                a(href = "/app/page", target = " ", classes = "whitespace") { +"go" }
            }) { root ->
                click(root, ".blank") shouldBe false
                click(root, ".named") shouldBe false
                click(root, ".self") shouldBe true
                click(root, ".self-upper") shouldBe true
                click(root, ".whitespace") shouldBe false
            }
        }

        "A <base target> applies to links without their own target" {
            val base = document.createElement("base")
            base.setAttribute("target", "_blank")
            document.head!!.appendChild(base)

            try {
                withLinks({
                    a(href = "/app/page", classes = "inherits") { +"go" }
                    a(href = "/app/page", target = "_self", classes = "overrides") { +"go" }
                    a(href = "/app/page", classes = "empty-overrides") { attributes["target"] = ""; +"go" }
                }) { root ->
                    click(root, ".inherits") shouldBe false
                    click(root, ".overrides") shouldBe true
                    click(root, ".empty-overrides") shouldBe true
                }
            } finally {
                base.remove()
            }
        }

        "download and rel=external links are left to the browser" {
            withLinks({
                a(href = "/app/page", classes = "download") { attributes["download"] = ""; +"go" }
                a(href = "/app/page", classes = "external") { attributes["rel"] = "noopener External"; +"go" }
                a(href = "/app/page", classes = "other-rel") { attributes["rel"] = "noopener externals"; +"go" }
            }) { root ->
                click(root, ".download") shouldBe false
                click(root, ".external") shouldBe false
                click(root, ".other-rel") shouldBe true
            }
        }

        "Other origins and non-http schemes are left to the browser" {
            val blobUrl = URL.createObjectURL(Blob(arrayOf("x")))

            withLinks({
                a(href = "https://example.com/a,b", classes = "cross") { +"go" }
                a(href = "//example.com/app/page", classes = "scheme-relative") { +"go" }
                a(href = "mailto:x@example.com", classes = "mailto") { +"go" }
                a(href = "tel:+49123", classes = "tel") { +"go" }
                a(href = "data:text/plain,hi", classes = "data") { +"go" }
                a(href = blobUrl, classes = "blob") { +"go" }
            }) { root ->
                val before = window.location.href

                click(root, ".cross") shouldBe false
                click(root, ".scheme-relative") shouldBe false
                click(root, ".mailto") shouldBe false
                click(root, ".tel") shouldBe false
                click(root, ".data") shouldBe false
                click(root, ".blob") shouldBe false

                window.location.href shouldBe before
            }
        }

        "A fragment link within the current document is left to the browser" {
            withLinks({
                a(href = "#section", classes = "hash") { +"go" }
                a(href = "#", classes = "empty-hash") { +"go" }
                a(href = "/app/other#section", classes = "other-page") { +"go" }
            }) { root ->
                click(root, ".hash") shouldBe false
                click(root, ".empty-hash") shouldBe false
                click(root, ".other-page") shouldBe true
            }
        }

        "An in-page fragment navigation by the browser keeps the active route" {
            withLinks({
                a(href = "#section", classes = "hash") { +"go" }
                div { id = "section"; +"target" }
            }) { root ->
                val before = router.current()
                allowDefault = true

                click(root, ".hash") shouldBe false
                delay(50)

                window.location.hash shouldBe "#section"
                router.current() shouldBeSameInstanceAs before

                // Back to the entry without the fragment: still the same route, no re-resolve
                window.history.back()
                delay(100)

                window.location.hash shouldBe ""
                router.current() shouldBeSameInstanceAs before
            }
        }

        "A deep link with a fragment resolves its route without the fragment" {
            withLinks({}) {
                router.resolveRouteForUri("/app/page#section")?.second?.route?.pattern shouldBe "/app/page"
                router.resolveRouteForUri("/app/page?x=1#section")?.second?.queryParams shouldBe mapOf("x" to "1")
            }
        }

        "_top and _parent are the current context only when the page is not framed" {
            withLinks({
                a(href = "/app/page", target = "_top", classes = "top") { +"go" }
                a(href = "/app/page", target = "_parent", classes = "parent") { +"go" }
            }) { root ->
                // Karma usually runs the tests in an iframe; either way the answer must follow the framing
                click(root, ".top") shouldBe (window.top == window)
                click(root, ".parent") shouldBe (window.parent == window)
            }
        }

        "Relative hrefs resolve against <base href>, like the browser" {
            val base = document.createElement("base")
            base.setAttribute("href", window.location.origin + "/app/")
            document.head!!.appendChild(base)

            try {
                withLinks({ a(href = "page", classes = "l") { +"go" } }) { root ->
                    click(root, ".l") shouldBe true
                    window.location.pathname shouldBe "/app/page"
                }
            } finally {
                base.remove()
            }
        }

        "Modifier keys and non-primary buttons are left to the browser" {
            withLinks({ a(href = "/app/page", classes = "l") { +"go" } }) { root ->
                click(root, ".l") { it.ctrlKey = true } shouldBe false
                click(root, ".l") { it.metaKey = true } shouldBe false
                click(root, ".l") { it.shiftKey = true } shouldBe false
                click(root, ".l") { it.altKey = true } shouldBe false
                click(root, ".l") { it.button = 1 } shouldBe false
                click(root, ".l") { it.button = 2 } shouldBe false
            }
        }

        "A click already handled by the page is not routed again" {
            withLinks({ a(href = "/app/page", classes = "l") { +"go" } }) { root ->
                val before = window.location.href
                val anchor = root.selectCss(".l").first()
                anchor.addEventListener("click", { it.preventDefault() })

                click(anchor) shouldBe true
                window.location.href shouldBe before
            }
        }

        "A disabled router intercepts nothing" {
            withLinks({ a(href = "/app/page", classes = "l") { +"go" } }) { root ->
                router.disable()

                click(root, ".l") shouldBe false
            }
        }
    }
}
