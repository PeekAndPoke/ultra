package routing

import de.peekandpoke.kraft.routing.Static
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class StaticSpec : StringSpec() {
    init {
        "Static route - simple pattern without parameters" {
            val route = Static("/home")

            route.pattern shouldBe "/home"
            route() shouldBe "#/home"
            route.buildUri() shouldBe "#/home"
        }

        "Static route - pattern with trailing slash" {
            val route = Static("/about/")

            route.pattern shouldBe "/about/"
            route() shouldBe "#/about/"
        }

        "Static route - root pattern" {
            val route = Static("/")

            route.pattern shouldBe "/"
            route() shouldBe "#/"
        }

        "Static route - nested path pattern" {
            val route = Static("/admin/dashboard")

            route.pattern shouldBe "/admin/dashboard"
            route() shouldBe "#/admin/dashboard"
        }

        "Static route - matching valid URI" {
            val route = Static("/contact")

            val match = route.match("/contact")
            match shouldNotBe null
            match?.routeParams shouldBe emptyMap()
        }

        "Static route - matching invalid URI" {
            val route = Static("/contact")

            val match = route.match("/about")
            match shouldBe null
        }

        "Static route - matching with trailing slash mismatch" {
            val route = Static("/contact")

            val match = route.match("/contact/")
            match shouldBe null
        }

        "Static route - pattern with placeholders should throw error" {
            shouldThrow<IllegalStateException> {
                Static("/user/{id}")
            }
        }
    }
}
