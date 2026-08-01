package io.peekandpoke.funktor.rest.acl

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.model.EmptyObject
import io.peekandpoke.ultra.remote.ApiAccessLevel
import io.peekandpoke.ultra.remote.TypedApiEndpoint

class ApiAclSpec : FreeSpec() {

    /** Named tuple for the grid below — `Triple` is two short. */
    private data class Row(
        val level: ApiAccessLevel,
        val canAccess: Boolean,
        val canFullyAccess: Boolean,
        val canPartiallyAccess: Boolean,
        val isDenied: Boolean,
    )

    private val getEvents = TypedApiEndpoint.Get(
        uri = "/events",
        response = EmptyObject.serializer(),
    )

    private val createEvent = TypedApiEndpoint.Post(
        uri = "/events",
        body = EmptyObject.serializer(),
        response = EmptyObject.serializer(),
    )

    private val deleteEvent = TypedApiEndpoint.Delete(
        uri = "/events/{id}",
        response = EmptyObject.serializer(),
    )

    /** An ACL in which `GET /events` sits at [level], and nothing else is listed. */
    private fun aclWith(level: ApiAccessLevel) = ApiAcl(
        UserApiAccessMatrix(
            entries = listOf(
                UserApiAccessMatrix.Entry(method = "GET", uri = "/events", level = level),
            )
        )
    )

    init {
        "every predicate against every level" - {

            // The full grid, stated exhaustively rather than two predicates per level as before —
            // that could not distinguish a correct implementation from one whose predicate ignores
            // its argument.
            listOf(
                Row(ApiAccessLevel.Granted, canAccess = true, canFullyAccess = true, canPartiallyAccess = false, isDenied = false),
                Row(ApiAccessLevel.Partial, canAccess = true, canFullyAccess = false, canPartiallyAccess = true, isDenied = false),
                Row(ApiAccessLevel.Denied, canAccess = false, canFullyAccess = false, canPartiallyAccess = false, isDenied = true),
            ).forEach { row ->

                "${row.level}" {
                    val acl = aclWith(row.level)

                    withClue("getAccessLevel") { acl.getAccessLevel(getEvents) shouldBe row.level }
                    withClue("canAccess") { acl.canAccess(getEvents) shouldBe row.canAccess }
                    withClue("canFullyAccess") { acl.canFullyAccess(getEvents) shouldBe row.canFullyAccess }
                    withClue("canPartiallyAccess") {
                        acl.canPartiallyAccess(getEvents) shouldBe row.canPartiallyAccess
                    }
                    withClue("isDenied") { acl.isDenied(getEvents) shouldBe row.isDenied }
                }
            }
        }

        "the documented invariants hold at every level" - {

            ApiAccessLevel.entries.forEach { level ->
                "$level" {
                    val acl = aclWith(level)

                    withClue("canAccess is the complement of isDenied") {
                        acl.canAccess(getEvents) shouldBe !acl.isDenied(getEvents)
                    }

                    withClue("canAccess is the union of the two positive predicates") {
                        acl.canAccess(getEvents) shouldBe
                                (acl.canFullyAccess(getEvents) || acl.canPartiallyAccess(getEvents))
                    }

                    withClue("the three exact predicates are mutually exclusive and exhaustive") {
                        listOf(
                            acl.canFullyAccess(getEvents),
                            acl.canPartiallyAccess(getEvents),
                            acl.isDenied(getEvents),
                        ).count { it } shouldBe 1
                    }
                }
            }
        }

        "an endpoint absent from the matrix is Denied" {
            // Not a defensive default: the server OMITS denied entries so the matrix does not
            // disclose the full API surface, so absence is how denial is transmitted.
            val acl = aclWith(ApiAccessLevel.Granted)

            acl.getAccessLevel(deleteEvent) shouldBe ApiAccessLevel.Denied
            acl.canAccess(deleteEvent) shouldBe false
            acl.isDenied(deleteEvent) shouldBe true
        }

        "an empty ACL denies everything" {
            ApiAcl.empty.getAccessLevel(getEvents) shouldBe ApiAccessLevel.Denied
            ApiAcl.empty.canAccess(getEvents) shouldBe false
            ApiAcl.empty.canFullyAccess(getEvents) shouldBe false
        }

        "the same URI under different methods is a different entry" {
            val acl = ApiAcl(
                UserApiAccessMatrix(
                    entries = listOf(
                        UserApiAccessMatrix.Entry("GET", "/events", ApiAccessLevel.Granted),
                        UserApiAccessMatrix.Entry("POST", "/events", ApiAccessLevel.Denied),
                    )
                )
            )

            acl.getAccessLevel(getEvents) shouldBe ApiAccessLevel.Granted
            acl.getAccessLevel(createEvent) shouldBe ApiAccessLevel.Denied
        }

        "several endpoints resolve independently" {
            val acl = ApiAcl(
                UserApiAccessMatrix(
                    entries = listOf(
                        UserApiAccessMatrix.Entry("GET", "/events", ApiAccessLevel.Granted),
                        UserApiAccessMatrix.Entry("POST", "/events", ApiAccessLevel.Partial),
                        UserApiAccessMatrix.Entry("DELETE", "/events/{id}", ApiAccessLevel.Denied),
                    )
                )
            )

            acl.canFullyAccess(getEvents) shouldBe true
            acl.canPartiallyAccess(createEvent) shouldBe true
            acl.canAccess(createEvent) shouldBe true
            acl.isDenied(deleteEvent) shouldBe true
        }
    }
}
