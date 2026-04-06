package io.peekandpoke.funktor.rest.acl

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.model.EmptyObject
import io.peekandpoke.ultra.remote.ApiAccessLevel
import io.peekandpoke.ultra.remote.TypedApiEndpoint

class ApiAclSpec : StringSpec({

    val getEvents = TypedApiEndpoint.Get(
        uri = "/events",
        response = EmptyObject.serializer(),
    )

    val createEvent = TypedApiEndpoint.Post(
        uri = "/events",
        body = EmptyObject.serializer(),
        response = EmptyObject.serializer(),
    )

    val deleteEvent = TypedApiEndpoint.Delete(
        uri = "/events/{id}",
        response = EmptyObject.serializer(),
    )

    "empty ACL denies access to all endpoints" {
        val acl = ApiAcl.empty

        acl.getAccessLevel(getEvents) shouldBe ApiAccessLevel.Denied
        acl.hasAccessTo(getEvents) shouldBe false
        acl.hasAnyAccessTo(getEvents) shouldBe false
    }

    "lookup by method and uri works for Granted" {
        val matrix = UserApiAccessMatrix(
            entries = listOf(
                UserApiAccessMatrix.Entry(method = "GET", uri = "/events", level = ApiAccessLevel.Granted),
            )
        )
        val acl = ApiAcl(matrix)

        acl.getAccessLevel(getEvents) shouldBe ApiAccessLevel.Granted
        acl.hasAccessTo(getEvents) shouldBe true
        acl.hasAnyAccessTo(getEvents) shouldBe true
    }

    "lookup by method and uri works for Partial" {
        val matrix = UserApiAccessMatrix(
            entries = listOf(
                UserApiAccessMatrix.Entry(method = "GET", uri = "/events", level = ApiAccessLevel.Partial),
            )
        )
        val acl = ApiAcl(matrix)

        acl.getAccessLevel(getEvents) shouldBe ApiAccessLevel.Partial
        acl.hasAccessTo(getEvents) shouldBe false
        acl.hasAnyAccessTo(getEvents) shouldBe true
    }

    "lookup by method and uri works for Denied" {
        val matrix = UserApiAccessMatrix(
            entries = listOf(
                UserApiAccessMatrix.Entry(method = "GET", uri = "/events", level = ApiAccessLevel.Denied),
            )
        )
        val acl = ApiAcl(matrix)

        acl.getAccessLevel(getEvents) shouldBe ApiAccessLevel.Denied
        acl.hasAccessTo(getEvents) shouldBe false
        acl.hasAnyAccessTo(getEvents) shouldBe false
    }

    "unknown endpoint defaults to Denied" {
        val matrix = UserApiAccessMatrix(
            entries = listOf(
                UserApiAccessMatrix.Entry(method = "GET", uri = "/events", level = ApiAccessLevel.Granted),
            )
        )
        val acl = ApiAcl(matrix)

        acl.getAccessLevel(deleteEvent) shouldBe ApiAccessLevel.Denied
        acl.hasAccessTo(deleteEvent) shouldBe false
    }

    "same URI with different methods are distinct" {
        val matrix = UserApiAccessMatrix(
            entries = listOf(
                UserApiAccessMatrix.Entry(method = "GET", uri = "/events", level = ApiAccessLevel.Granted),
                UserApiAccessMatrix.Entry(method = "POST", uri = "/events", level = ApiAccessLevel.Denied),
            )
        )
        val acl = ApiAcl(matrix)

        acl.getAccessLevel(getEvents) shouldBe ApiAccessLevel.Granted
        acl.getAccessLevel(createEvent) shouldBe ApiAccessLevel.Denied
    }

    "multiple endpoints in matrix" {
        val matrix = UserApiAccessMatrix(
            entries = listOf(
                UserApiAccessMatrix.Entry(method = "GET", uri = "/events", level = ApiAccessLevel.Granted),
                UserApiAccessMatrix.Entry(method = "POST", uri = "/events", level = ApiAccessLevel.Granted),
                UserApiAccessMatrix.Entry(method = "DELETE", uri = "/events/{id}", level = ApiAccessLevel.Denied),
            )
        )
        val acl = ApiAcl(matrix)

        acl.hasAccessTo(getEvents) shouldBe true
        acl.hasAccessTo(createEvent) shouldBe true
        acl.hasAccessTo(deleteEvent) shouldBe false
    }
})
