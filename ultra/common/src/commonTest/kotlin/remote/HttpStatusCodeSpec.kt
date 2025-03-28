package de.peekandpoke.ultra.common.remote

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class HttpStatusCodeSpec : StringSpec({

    "Constructor should properly set value and description" {
        val code = HttpStatusCode(418, "Test Description")
        code.value shouldBe 418
        code.description shouldBe "Test Description"
    }

    "equals() should consider two HttpStatusCode objects with the same value as equal regardless of description" {
        val code1 = HttpStatusCode(404, "Not Found")
        val code2 = HttpStatusCode(404, "Resource Not Found") // Different description

        code1 shouldBe code2
        (code1 == code2) shouldBe true
    }

    "equals() should consider two HttpStatusCode objects with different values as not equal" {
        val code1 = HttpStatusCode(200, "Success")
        val code2 = HttpStatusCode(201, "Success")

        code1 shouldNotBe code2
    }

    "hashCode() should only depend on value, not description" {
        val code1 = HttpStatusCode(200, "OK")
        val code2 = HttpStatusCode(200, "Success") // Same value, different description

        code1.hashCode() shouldBe code2.hashCode()
    }

    "HttpStatusCode objects with the same value should be deduplicated in sets" {
        val set = setOf(
            HttpStatusCode(200, "OK"),
            HttpStatusCode(200, "Modified OK"), // Same value, different description
            HttpStatusCode(404, "Not Found")
        )

        set.size shouldBe 2 // Should only have 200 and 404
    }

    "HashMap should treat HttpStatusCode objects with the same value as the same key" {
        val map = mutableMapOf<HttpStatusCode, String>()

        // Add with one description
        map[HttpStatusCode(200, "OK")] = "First"

        // Update with a different description
        map[HttpStatusCode(200, "Success")] = "Second"

        // Get with yet another description
        val result = map[HttpStatusCode(200, "Different")]

        map.size shouldBe 1
        result shouldBe "Second"
    }

    "isInformational() should correctly identify 1xx status codes" {
        // True cases
        assertSoftly {
            HttpStatusCode.Continue.isInformational() shouldBe true
            HttpStatusCode.SwitchingProtocols.isInformational() shouldBe true
            HttpStatusCode.Processing.isInformational() shouldBe true
            HttpStatusCode.EarlyHints.isInformational() shouldBe true
            HttpStatusCode(150, "Custom Informational").isInformational() shouldBe true
        }

        // False cases
        assertSoftly {
            HttpStatusCode.OK.isInformational() shouldBe false
            HttpStatusCode.NotFound.isInformational() shouldBe false
            HttpStatusCode(99, "Too Low").isInformational() shouldBe false
            HttpStatusCode(200, "Too High").isInformational() shouldBe false
        }
    }

    "isSuccess() should correctly identify 2xx status codes" {
        // True cases
        assertSoftly {
            HttpStatusCode.OK.isSuccess() shouldBe true
            HttpStatusCode.Created.isSuccess() shouldBe true
            HttpStatusCode.Accepted.isSuccess() shouldBe true
            HttpStatusCode.NoContent.isSuccess() shouldBe true
            HttpStatusCode(250, "Custom Success").isSuccess() shouldBe true
        }

        // False cases
        assertSoftly {
            HttpStatusCode.Continue.isSuccess() shouldBe false
            HttpStatusCode.NotFound.isSuccess() shouldBe false
            HttpStatusCode(199, "Too Low").isSuccess() shouldBe false
            HttpStatusCode(300, "Too High").isSuccess() shouldBe false
        }
    }

    "isRedirect() should correctly identify 3xx status codes" {
        // True cases
        assertSoftly {
            HttpStatusCode.MovedPermanently.isRedirect() shouldBe true
            HttpStatusCode.Found.isRedirect() shouldBe true
            HttpStatusCode.SeeOther.isRedirect() shouldBe true
            HttpStatusCode.TemporaryRedirect.isRedirect() shouldBe true
            HttpStatusCode(350, "Custom Redirect").isRedirect() shouldBe true
        }

        // False cases
        assertSoftly {
            HttpStatusCode.OK.isRedirect() shouldBe false
            HttpStatusCode.BadRequest.isRedirect() shouldBe false
            HttpStatusCode(299, "Too Low").isRedirect() shouldBe false
            HttpStatusCode(400, "Too High").isRedirect() shouldBe false
        }
    }

    "isClientError() should correctly identify 4xx status codes" {
        // True cases
        assertSoftly {
            HttpStatusCode.BadRequest.isClientError() shouldBe true
            HttpStatusCode.Unauthorized.isClientError() shouldBe true
            HttpStatusCode.Forbidden.isClientError() shouldBe true
            HttpStatusCode.NotFound.isClientError() shouldBe true
            HttpStatusCode(450, "Custom Client Error").isClientError() shouldBe true
        }

        // False cases
        assertSoftly {
            HttpStatusCode.OK.isClientError() shouldBe false
            HttpStatusCode.MovedPermanently.isClientError() shouldBe false
            HttpStatusCode(399, "Too Low").isClientError() shouldBe false
            HttpStatusCode(500, "Too High").isClientError() shouldBe false
        }
    }

    "isServerError() should correctly identify 5xx status codes" {
        // Need to create server error codes as they aren't visible in the provided code
        val internalServerError = HttpStatusCode(500, "Internal Server Error")
        val serviceUnavailable = HttpStatusCode(503, "Service Unavailable")

        // True cases
        assertSoftly {
            internalServerError.isServerError() shouldBe true
            serviceUnavailable.isServerError() shouldBe true
            HttpStatusCode(550, "Custom Server Error").isServerError() shouldBe true
        }

        // False cases
        assertSoftly {
            HttpStatusCode.OK.isServerError() shouldBe false
            HttpStatusCode.NotFound.isServerError() shouldBe false
            HttpStatusCode(499, "Too Low").isServerError() shouldBe false
            HttpStatusCode(600, "Too High").isServerError() shouldBe false
        }
    }

    "Edge and boundary cases should be handled correctly" {
        assertSoftly {
            // Edge cases
            HttpStatusCode(100, "Min informational").isInformational() shouldBe true
            HttpStatusCode(199, "Max informational").isInformational() shouldBe true

            HttpStatusCode(200, "Min success").isSuccess() shouldBe true
            HttpStatusCode(299, "Max success").isSuccess() shouldBe true

            HttpStatusCode(300, "Min redirect").isRedirect() shouldBe true
            HttpStatusCode(399, "Max redirect").isRedirect() shouldBe true

            HttpStatusCode(400, "Min client error").isClientError() shouldBe true
            HttpStatusCode(499, "Max client error").isClientError() shouldBe true

            HttpStatusCode(500, "Min server error").isServerError() shouldBe true
            HttpStatusCode(599, "Max server error").isServerError() shouldBe true
        }
    }
})
