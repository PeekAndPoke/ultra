package io.peekandpoke.funktor.rest.acl

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.rest.ApiRoute
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.ultra.model.EmptyObject
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.TypedApiEndpoint
import io.peekandpoke.ultra.remote.api

/**
 * A mounted route's pattern must equal its endpoint's declared uri, verbatim.
 *
 * This is the invariant the access matrix rests on. `ApiAccessDescriptor` keys entries by
 * `route.pattern.pattern`, `ApiAcl` looks them up by `endpoint.uri` — so the moment those two can
 * differ, every lookup for the affected group misses and returns `Denied`. Fail-closed and
 * completely silent: controls simply stop appearing.
 *
 * It used to be breakable. `ApiRoutes` took a `mountPoint` prepended when building the pattern
 * (`UriPattern(mountPoint + this)`) and invisible to the endpoint. Worse than the ACL miss: the
 * Kotlin client builds its request URL from `endpoint.uri` too, so a non-empty mount point meant
 * calling an unprefixed path and getting a 404. No value could be correct, because the prefix lived
 * in server-only code while the endpoint is shared `commonMain` — so the parameter was REMOVED
 * (2026-08-02) rather than guarded.
 *
 * This spec exists so re-adding one fails here, loudly, rather than in a frontend three modules away.
 */
class RouteUriParitySpec : FreeSpec() {

    companion object {
        private val ListEvents = TypedApiEndpoint.Get(
            uri = "/events",
            response = EmptyObject.serializer().api(),
        )

        private val LatestEvent = TypedApiEndpoint.Get(
            uri = "/events/latest",
            response = EmptyObject.serializer().api(),
        )

        /** Deliberately multi-segment: a prefix would be indistinguishable from a deep path. */
        private val AdminEvents = TypedApiEndpoint.Get(
            uri = "/admin/events/archive",
            response = EmptyObject.serializer().api(),
        )
    }

    private class Group : ApiRoutes("parity", authFloor = { public() }) {
        val list = ListEvents.mount { handle { ApiResponse.ok(EmptyObject.default) } }
        val latest = LatestEvent.mount { handle { ApiResponse.ok(EmptyObject.default) } }
        val admin = AdminEvents.mount { handle { ApiResponse.ok(EmptyObject.default) } }
    }

    init {
        "a mounted route's pattern is its endpoint's uri, verbatim" - {

            listOf(
                ListEvents,
                LatestEvent,
                AdminEvents,
            ).forEach { endpoint ->

                "${endpoint.httpMethod} ${endpoint.uri}" {
                    val route = Group().all.single { it.pattern.pattern == endpoint.uri }

                    withClue("the matrix keys on pattern.pattern; ApiAcl looks up endpoint.uri") {
                        route.pattern.pattern shouldBe endpoint.uri
                    }

                    withClue("and the method, the other half of the key") {
                        route.method.value shouldBe endpoint.httpMethod
                    }
                }
            }
        }

        "`all` cannot be mutated by a caller" {
            // Found by mutation. Replacing the old per-access `.toList()` with a bare `get() =
            // allRoutes` removed the copy AND the protection: Kotlin's List is not runtime-immutable,
            // so a caller could cast it back and add routes to a live group. An unmodifiable VIEW
            // keeps both properties — no per-access allocation, and no way in.
            val all = Group().all

            shouldThrow<UnsupportedOperationException> {
                @Suppress("UNCHECKED_CAST")
                (all as MutableList<ApiRoute<*>>).clear()
            }
        }

        "the ACL key built from the routes equals the one built from the endpoints" {
            // Stated the way the two consumers actually build it, rather than as a bare string
            // compare — if either side ever gains normalization, this fails even where the
            // per-endpoint compare above might not.
            val fromRoutes = Group().all
                .map { "${it.method.value}|${it.pattern.pattern}" }
                .sorted()

            val fromEndpoints = listOf(ListEvents, LatestEvent, AdminEvents)
                .map { "${it.httpMethod}|${it.uri}" }
                .sorted()

            fromRoutes shouldBe fromEndpoints
        }
    }
}
