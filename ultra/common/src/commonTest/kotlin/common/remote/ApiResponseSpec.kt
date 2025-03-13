package de.peekandpoke.ultra.common.remote

import de.peekandpoke.ultra.common.model.Message
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ApiResponseSpec : StringSpec() {

    init {
        "withInsights() should add insights to the ApiResponse" {
            val originalResponse = ApiResponse.ok("test data")
            val insights = ApiResponse.Insights(
                ts = 1234567890,
                method = "GET",
                url = "/api/test",
                server = "test-server",
                status = HttpStatusCode.OK,
                durationMs = 42.0,
                detailsUri = "/api/details"
            )

            val responseWithInsights = originalResponse.withInsights(insights)

            assertSoftly {
                responseWithInsights.status shouldBe HttpStatusCode.OK
                responseWithInsights.data shouldBe "test data"
                responseWithInsights.insights shouldBe insights
            }
        }

        "withInfo() should add an info message to the ApiResponse" {
            val originalResponse = ApiResponse.ok("test data")

            val responseWithInfo = originalResponse.withInfo("Test info message")

            assertSoftly {
                responseWithInfo.status shouldBe HttpStatusCode.OK
                responseWithInfo.data shouldBe "test data"
                responseWithInfo.messages shouldNotBe null
                responseWithInfo.messages.shouldNotBeNull()
                responseWithInfo.messages.size shouldBe 1
                responseWithInfo.messages[0].type shouldBe Message.Type.info
                responseWithInfo.messages[0].text shouldBe "Test info message"
            }

            // Test adding multiple info messages
            val responseWithMultipleInfo = responseWithInfo.withInfo("Second info message")

            assertSoftly {
                responseWithMultipleInfo.messages.shouldNotBeNull()
                responseWithMultipleInfo.messages.size shouldBe 2
                responseWithMultipleInfo.messages[1].type shouldBe Message.Type.info
                responseWithMultipleInfo.messages[1].text shouldBe "Second info message"
            }
        }

        "withWarning() should add a warning message to the ApiResponse" {
            val originalResponse = ApiResponse.ok("test data")

            val responseWithWarning = originalResponse.withWarning("Test warning message")

            assertSoftly {
                responseWithWarning.status shouldBe HttpStatusCode.OK
                responseWithWarning.data shouldBe "test data"
                responseWithWarning.messages.shouldNotBeNull()
                responseWithWarning.messages.size shouldBe 1
                responseWithWarning.messages[0].type shouldBe Message.Type.warning
                responseWithWarning.messages[0].text shouldBe "Test warning message"
            }

            // Test adding multiple warning messages
            val responseWithMultipleWarnings = responseWithWarning.withWarning("Second warning message")

            assertSoftly {
                responseWithMultipleWarnings.messages.shouldNotBeNull()
                responseWithMultipleWarnings.messages.size shouldBe 2
                responseWithMultipleWarnings.messages[1].type shouldBe Message.Type.warning
                responseWithMultipleWarnings.messages[1].text shouldBe "Second warning message"
            }
        }

        "withError() should add an error message to the ApiResponse" {
            val originalResponse = ApiResponse.ok("test data")

            val responseWithError = originalResponse.withError("Test error message")

            assertSoftly {
                responseWithError.status shouldBe HttpStatusCode.OK
                responseWithError.data shouldBe "test data"
                responseWithError.messages.shouldNotBeNull()
                responseWithError.messages.size shouldBe 1
                responseWithError.messages[0].type shouldBe Message.Type.error
                responseWithError.messages[0].text shouldBe "Test error message"
            }

            // Test adding multiple error messages
            val responseWithMultipleErrors = responseWithError.withError("Second error message")

            assertSoftly {
                responseWithMultipleErrors.messages.shouldNotBeNull()
                responseWithMultipleErrors.messages.size shouldBe 2
                responseWithMultipleErrors.messages[1].type shouldBe Message.Type.error
                responseWithMultipleErrors.messages[1].text shouldBe "Second error message"
            }
        }

        "isSuccess() should return true for 2xx status codes" {
            assertSoftly {
                ApiResponse.ok("test").isSuccess() shouldBe true
                ApiResponse.created("test").isSuccess() shouldBe true
                ApiResponse.accepted("test").isSuccess() shouldBe true
                ApiResponse.noContent<String>().isSuccess() shouldBe true
            }
        }

        "isSuccess() should return false for non-2xx status codes" {
            assertSoftly {
                ApiResponse.notFound<String>(null).isSuccess() shouldBe false
                ApiResponse.badRequest("error").isSuccess() shouldBe false
                ApiResponse.unauthorized<String>(null).isSuccess() shouldBe false
                ApiResponse.internalServerError("error").isSuccess() shouldBe false
            }
        }

        "isNotSuccess() should return false for 2xx status codes" {
            assertSoftly {
                ApiResponse.ok("test").isNotSuccess() shouldBe false
                ApiResponse.created("test").isNotSuccess() shouldBe false
                ApiResponse.accepted("test").isNotSuccess() shouldBe false
                ApiResponse.noContent<String>().isNotSuccess() shouldBe false
            }
        }

        "isNotSuccess() should return true for non-2xx status codes" {
            assertSoftly {
                ApiResponse.notFound<String>(null).isNotSuccess() shouldBe true
                ApiResponse.badRequest("error").isNotSuccess() shouldBe true
                ApiResponse.unauthorized<String>(null).isNotSuccess() shouldBe true
                ApiResponse.internalServerError("error").isNotSuccess() shouldBe true
            }
        }

        "map() should transform the data in the ApiResponse" {
            val originalResponse = ApiResponse.ok("test data")

            val mappedResponse = originalResponse.map { it?.uppercase() }

            assertSoftly {
                mappedResponse.status shouldBe HttpStatusCode.OK
                mappedResponse.data shouldBe "TEST DATA"
                mappedResponse.messages shouldBe null
                mappedResponse.insights shouldBe null
            }
        }

        "map() should preserve messages and insights" {
            val insights = ApiResponse.Insights(
                ts = 1234567890,
                method = "GET",
                url = "/api/test",
                server = "test-server",
                status = HttpStatusCode.OK,
                durationMs = 42.0,
                detailsUri = "/api/details"
            )

            val originalResponse = ApiResponse.ok("test data")
                .withInsights(insights)
                .withInfo("Info message")
                .withWarning("Warning message")

            val mappedResponse = originalResponse.map { it?.uppercase() }

            assertSoftly {
                mappedResponse.status shouldBe HttpStatusCode.OK
                mappedResponse.data shouldBe "TEST DATA"
                mappedResponse.insights shouldBe insights
                mappedResponse.messages.shouldNotBeNull()
                mappedResponse.messages.size shouldBe 2
                mappedResponse.messages[0].type shouldBe Message.Type.info
                mappedResponse.messages[0].text shouldBe "Info message"
                mappedResponse.messages[1].type shouldBe Message.Type.warning
                mappedResponse.messages[1].text shouldBe "Warning message"
            }
        }

        "mapNullable() should handle null data properly" {
            val originalResponse = ApiResponse.notFound<String>(null)

            val mappedResponse = originalResponse.map { it?.uppercase() ?: "NOT FOUND" }

            assertSoftly {
                mappedResponse.status shouldBe HttpStatusCode.NotFound
                mappedResponse.data shouldBe "NOT FOUND"
            }
        }

        // 1XX Creation methods /////////////////////////////////////////////////////////////////////////////////////

        "continueResponse() should create an ApiResponse with HttpStatusCode.Continue" {
            val response = ApiResponse.continueResponse("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.Continue
                response.status.value shouldBe 100
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.continueResponse<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.Continue
                nullResponse.status.value shouldBe 100
                nullResponse.data shouldBe null
            }
        }

        "switchingProtocols() should create an ApiResponse with HttpStatusCode.SwitchingProtocols" {
            val response = ApiResponse.switchingProtocols("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.SwitchingProtocols
                response.status.value shouldBe 101
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.switchingProtocols<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.SwitchingProtocols
                nullResponse.status.value shouldBe 101
                nullResponse.data shouldBe null
            }
        }

        "processing() should create an ApiResponse with HttpStatusCode.Processing" {
            val response = ApiResponse.processing("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.Processing
                response.status.value shouldBe 102
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.processing<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.Processing
                nullResponse.status.value shouldBe 102
                nullResponse.data shouldBe null
            }
        }

        "earlyHints() should create an ApiResponse with HttpStatusCode.EarlyHints" {
            // Assuming earlyHints() exists, based on the pattern of other methods
            val response = ApiResponse.earlyHints("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.EarlyHints
                response.status.value shouldBe 103
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.earlyHints<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.EarlyHints
                nullResponse.status.value shouldBe 103
                nullResponse.data shouldBe null
            }
        }

        // 2XX Creation methods /////////////////////////////////////////////////////////////////////////////////////

        "ok() should create an ApiResponse with HttpStatusCode.OK" {
            val response = ApiResponse.ok("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.OK
                response.status.value shouldBe 200
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.ok<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.OK
                nullResponse.status.value shouldBe 200
                nullResponse.data shouldBe null
            }
        }

        "okOrNotFound() should create correct ApiResponse based on data" {
            // With data -> OK
            val responseWithData = ApiResponse.okOrNotFound("test data")
            assertSoftly {
                responseWithData.status shouldBe HttpStatusCode.OK
                responseWithData.status.value shouldBe 200
                responseWithData.data shouldBe "test data"
            }

            // Without data -> NotFound
            val responseWithoutData = ApiResponse.okOrNotFound<String>(null)
            assertSoftly {
                responseWithoutData.status shouldBe HttpStatusCode.NotFound
                responseWithoutData.status.value shouldBe 404
                responseWithoutData.data shouldBe null
            }
        }

        "created() should create an ApiResponse with HttpStatusCode.Created" {
            val response = ApiResponse.created("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.Created
                response.status.value shouldBe 201
                response.data shouldBe "test data"
            }
        }

        "okOrCreated() should create correct ApiResponse based on created flag" {
            // created = true -> Created
            val createdResponse = ApiResponse.okOrCreated(true, "test data")
            assertSoftly {
                createdResponse.status shouldBe HttpStatusCode.Created
                createdResponse.status.value shouldBe 201
                createdResponse.data shouldBe "test data"
            }

            // created = false -> OK
            val okResponse = ApiResponse.okOrCreated(false, "test data")
            assertSoftly {
                okResponse.status shouldBe HttpStatusCode.OK
                okResponse.status.value shouldBe 200
                okResponse.data shouldBe "test data"
            }
        }

        "accepted() should create an ApiResponse with HttpStatusCode.Accepted" {
            val response = ApiResponse.accepted("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.Accepted
                response.status.value shouldBe 202
                response.data shouldBe "test data"
            }
        }

        "nonAuthoritativeInformation() should create an ApiResponse with HttpStatusCode.NonAuthoritativeInformation" {
            // Based on the pattern of other methods, I'm assuming this method exists
            // If it doesn't exist, this test would need to be removed
            val data = "test data"
            val response = ApiResponse.nonAuthoritativeInformation(data)

            assertSoftly {
                response.status shouldBe HttpStatusCode.NonAuthoritativeInformation
                response.status.value shouldBe 203
                response.data shouldBe data
            }
        }

        "noContent() should create an ApiResponse with HttpStatusCode.NoContent" {
            // Based on the pattern of other methods, I'm assuming this method exists
            // If it doesn't exist, this test would need to be removed
            val response = ApiResponse.noContent<String>()

            assertSoftly {
                response.status shouldBe HttpStatusCode.NoContent
                response.status.value shouldBe 204
                response.data shouldBe null
            }
        }

        "resetContent() should create an ApiResponse with HttpStatusCode.ResetContent" {
            // Based on the pattern of other methods, I'm assuming this method exists
            // If it doesn't exist, this test would need to be removed
            val response = ApiResponse.resetContent<String>()

            assertSoftly {
                response.status shouldBe HttpStatusCode.ResetContent
                response.status.value shouldBe 205
                response.data shouldBe null
            }
        }

        "partialContent() should create an ApiResponse with HttpStatusCode.PartialContent" {
            // Based on the pattern of other methods, I'm assuming this method exists
            // If it doesn't exist, this test would need to be removed
            val data = "test data"
            val response = ApiResponse.partialContent(data)

            assertSoftly {
                response.status shouldBe HttpStatusCode.PartialContent
                response.status.value shouldBe 206
                response.data shouldBe data
            }
        }

        // 2XX Creation methods (207-299) /////////////////////////////////////////////////////////////////////////////////////

        "multiStatus() should create an ApiResponse with HttpStatusCode.MultiStatus" {
            val response = ApiResponse.multiStatus("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.MultiStatus
                response.status.value shouldBe 207
                response.data shouldBe "test data"
            }
        }

        "alreadyReported() should create an ApiResponse with HttpStatusCode.AlreadyReported" {
            val response = ApiResponse.alreadyReported("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.AlreadyReported
                response.status.value shouldBe 208
                response.data shouldBe "test data"
            }
        }

        "imUsed() should create an ApiResponse with HttpStatusCode.IMUsed" {
            val response = ApiResponse.imUsed("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.IMUsed
                response.status.value shouldBe 226
                response.data shouldBe "test data"
            }

            // Test with null data if the method accepts null
            val nullResponse = ApiResponse.imUsed<String?>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.IMUsed
                nullResponse.status.value shouldBe 226
                nullResponse.data shouldBe null
            }
        }

        // 3XX Creation methods /////////////////////////////////////////////////////////////////////////////////////

        "multipleChoices() should create an ApiResponse with HttpStatusCode.MultipleChoices" {
            val response = ApiResponse.multipleChoices("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.MultipleChoices
                response.status.value shouldBe 300
                response.data shouldBe "test data"
            }

            // Test with null data if the method accepts null
            val nullResponse = ApiResponse.multipleChoices<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.MultipleChoices
                nullResponse.status.value shouldBe 300
                nullResponse.data shouldBe null
            }
        }

        "movedPermanently() should create an ApiResponse with HttpStatusCode.MovedPermanently" {
            val response = ApiResponse.movedPermanently("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.MovedPermanently
                response.status.value shouldBe 301
                response.data shouldBe "test data"
            }

            // Test with null data if the method accepts null
            val nullResponse = ApiResponse.movedPermanently<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.MovedPermanently
                nullResponse.status.value shouldBe 301
                nullResponse.data shouldBe null
            }
        }

        "found() should create an ApiResponse with HttpStatusCode.Found" {
            val response = ApiResponse.found("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.Found
                response.status.value shouldBe 302
                response.data shouldBe "test data"
            }

            // Test with null data if the method accepts null
            val nullResponse = ApiResponse.found<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.Found
                nullResponse.status.value shouldBe 302
                nullResponse.data shouldBe null
            }
        }

        "seeOther() should create an ApiResponse with HttpStatusCode.SeeOther" {
            val response = ApiResponse.seeOther("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.SeeOther
                response.status.value shouldBe 303
                response.data shouldBe "test data"
            }

            // Test with null data if the method accepts null
            val nullResponse = ApiResponse.seeOther<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.SeeOther
                nullResponse.status.value shouldBe 303
                nullResponse.data shouldBe null
            }
        }

        "notModified() should create an ApiResponse with HttpStatusCode.NotModified" {
            val response = ApiResponse.notModified<String>()

            assertSoftly {
                response.status shouldBe HttpStatusCode.NotModified
                response.status.value shouldBe 304
                response.data shouldBe null
            }
        }

        "useProxy() should create an ApiResponse with HttpStatusCode.UseProxy" {
            val response = ApiResponse.useProxy("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.UseProxy
                response.status.value shouldBe 305
                response.data shouldBe "test data"
            }

            // Test with null data if the method accepts null
            val nullResponse = ApiResponse.useProxy<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.UseProxy
                nullResponse.status.value shouldBe 305
                nullResponse.data shouldBe null
            }
        }

        "switchProxy() should create an ApiResponse with HttpStatusCode.SwitchProxy" {
            val response = ApiResponse.switchProxy("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.SwitchProxy
                response.status.value shouldBe 306
                response.data shouldBe "test data"
            }

            // Test with null data if the method accepts null
            val nullResponse = ApiResponse.switchProxy<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.SwitchProxy
                nullResponse.status.value shouldBe 306
                nullResponse.data shouldBe null
            }
        }

        "temporaryRedirect() should create an ApiResponse with HttpStatusCode.TemporaryRedirect" {
            val response = ApiResponse.temporaryRedirect("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.TemporaryRedirect
                response.status.value shouldBe 307
                response.data shouldBe "test data"
            }

            // Test with null data if the method accepts null
            val nullResponse = ApiResponse.temporaryRedirect<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.TemporaryRedirect
                nullResponse.status.value shouldBe 307
                nullResponse.data shouldBe null
            }
        }

        "permanentRedirect() should create an ApiResponse with HttpStatusCode.PermanentRedirect" {
            val response = ApiResponse.permanentRedirect("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.PermanentRedirect
                response.status.value shouldBe 308
                response.data shouldBe "test data"
            }

            // Test with null data if the method accepts null
            val nullResponse = ApiResponse.permanentRedirect<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.PermanentRedirect
                nullResponse.status.value shouldBe 308
                nullResponse.data shouldBe null
            }
        }

        // 4XX Creation methods /////////////////////////////////////////////////////////////////////////////////////

        // 4XX Creation methods /////////////////////////////////////////////////////////////////////////////////////

        "badRequest() should create an ApiResponse with HttpStatusCode.BadRequest" {
            val response = ApiResponse.badRequest("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.BadRequest
                response.status.value shouldBe 400
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.badRequest<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.BadRequest
                nullResponse.status.value shouldBe 400
                nullResponse.data shouldBe null
            }
        }

        "unauthorized() should create an ApiResponse with HttpStatusCode.Unauthorized" {
            val response = ApiResponse.unauthorized("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.Unauthorized
                response.status.value shouldBe 401
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.unauthorized<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.Unauthorized
                nullResponse.status.value shouldBe 401
                nullResponse.data shouldBe null
            }
        }

        "paymentRequired() should create an ApiResponse with HttpStatusCode.PaymentRequired" {
            val response = ApiResponse.paymentRequired("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.PaymentRequired
                response.status.value shouldBe 402
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.paymentRequired<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.PaymentRequired
                nullResponse.status.value shouldBe 402
                nullResponse.data shouldBe null
            }
        }

        "forbidden() should create an ApiResponse with HttpStatusCode.Forbidden" {
            val response = ApiResponse.forbidden("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.Forbidden
                response.status.value shouldBe 403
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.forbidden<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.Forbidden
                nullResponse.status.value shouldBe 403
                nullResponse.data shouldBe null
            }
        }

        "notFound() should create an ApiResponse with HttpStatusCode.NotFound" {
            val response = ApiResponse.notFound("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.NotFound
                response.status.value shouldBe 404
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.notFound<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.NotFound
                nullResponse.status.value shouldBe 404
                nullResponse.data shouldBe null
            }
        }

        "methodNotAllowed() should create an ApiResponse with HttpStatusCode.MethodNotAllowed" {
            val response = ApiResponse.methodNotAllowed("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.MethodNotAllowed
                response.status.value shouldBe 405
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.methodNotAllowed<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.MethodNotAllowed
                nullResponse.status.value shouldBe 405
                nullResponse.data shouldBe null
            }
        }

        "notAcceptable() should create an ApiResponse with HttpStatusCode.NotAcceptable" {
            val response = ApiResponse.notAcceptable("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.NotAcceptable
                response.status.value shouldBe 406
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.notAcceptable<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.NotAcceptable
                nullResponse.status.value shouldBe 406
                nullResponse.data shouldBe null
            }
        }

        "proxyAuthenticationRequired() should create an ApiResponse with HttpStatusCode.ProxyAuthenticationRequired" {
            val response = ApiResponse.proxyAuthenticationRequired("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.ProxyAuthenticationRequired
                response.status.value shouldBe 407
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.proxyAuthenticationRequired<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.ProxyAuthenticationRequired
                nullResponse.status.value shouldBe 407
                nullResponse.data shouldBe null
            }
        }

        "requestTimeout() should create an ApiResponse with HttpStatusCode.RequestTimeout" {
            val response = ApiResponse.requestTimeout("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.RequestTimeout
                response.status.value shouldBe 408
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.requestTimeout<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.RequestTimeout
                nullResponse.status.value shouldBe 408
                nullResponse.data shouldBe null
            }
        }

        "conflict() should create an ApiResponse with HttpStatusCode.Conflict" {
            val response = ApiResponse.conflict("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.Conflict
                response.status.value shouldBe 409
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.conflict<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.Conflict
                nullResponse.status.value shouldBe 409
                nullResponse.data shouldBe null
            }
        }

        "gone() should create an ApiResponse with HttpStatusCode.Gone" {
            val response = ApiResponse.gone("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.Gone
                response.status.value shouldBe 410
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.gone<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.Gone
                nullResponse.status.value shouldBe 410
                nullResponse.data shouldBe null
            }
        }

        "lengthRequired() should create an ApiResponse with HttpStatusCode.LengthRequired" {
            val response = ApiResponse.lengthRequired("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.LengthRequired
                response.status.value shouldBe 411
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.lengthRequired<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.LengthRequired
                nullResponse.status.value shouldBe 411
                nullResponse.data shouldBe null
            }
        }

        "preconditionFailed() should create an ApiResponse with HttpStatusCode.PreconditionFailed" {
            val response = ApiResponse.preconditionFailed("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.PreconditionFailed
                response.status.value shouldBe 412
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.preconditionFailed<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.PreconditionFailed
                nullResponse.status.value shouldBe 412
                nullResponse.data shouldBe null
            }
        }

        "contentTooLarge() should create an ApiResponse with HttpStatusCode.ContentTooLarge" {
            val response = ApiResponse.contentTooLarge("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.ContentTooLarge
                response.status.value shouldBe 413
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.contentTooLarge<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.ContentTooLarge
                nullResponse.status.value shouldBe 413
                nullResponse.data shouldBe null
            }
        }

        "uriTooLong() should create an ApiResponse with HttpStatusCode.URITooLong" {
            val response = ApiResponse.uriTooLong("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.URITooLong
                response.status.value shouldBe 414
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.uriTooLong<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.URITooLong
                nullResponse.status.value shouldBe 414
                nullResponse.data shouldBe null
            }
        }

        "unsupportedMediaType() should create an ApiResponse with HttpStatusCode.UnsupportedMediaType" {
            val response = ApiResponse.unsupportedMediaType("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.UnsupportedMediaType
                response.status.value shouldBe 415
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.unsupportedMediaType<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.UnsupportedMediaType
                nullResponse.status.value shouldBe 415
                nullResponse.data shouldBe null
            }
        }

        "rangeNotSatisfiable() should create an ApiResponse with HttpStatusCode.RangeNotSatisfiable" {
            val response = ApiResponse.rangeNotSatisfiable("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.RangeNotSatisfiable
                response.status.value shouldBe 416
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.rangeNotSatisfiable<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.RangeNotSatisfiable
                nullResponse.status.value shouldBe 416
                nullResponse.data shouldBe null
            }
        }

        "expectationFailed() should create an ApiResponse with HttpStatusCode.ExpectationFailed" {
            val response = ApiResponse.expectationFailed("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.ExpectationFailed
                response.status.value shouldBe 417
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.expectationFailed<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.ExpectationFailed
                nullResponse.status.value shouldBe 417
                nullResponse.data shouldBe null
            }
        }

        "imATeapot() should create an ApiResponse with HttpStatusCode.ImATeapot" {
            val response = ApiResponse.imATeapot("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.ImATeapot
                response.status.value shouldBe 418
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.imATeapot<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.ImATeapot
                nullResponse.status.value shouldBe 418
                nullResponse.data shouldBe null
            }
        }

        "misdirectedRequest() should create an ApiResponse with HttpStatusCode.MisdirectedRequest" {
            val response = ApiResponse.misdirectedRequest("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.MisdirectedRequest
                response.status.value shouldBe 421
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.misdirectedRequest<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.MisdirectedRequest
                nullResponse.status.value shouldBe 421
                nullResponse.data shouldBe null
            }
        }

        "unprocessableEntity() should create an ApiResponse with HttpStatusCode.UnprocessableEntity" {
            val response = ApiResponse.unprocessableEntity("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.UnprocessableEntity
                response.status.value shouldBe 422
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.unprocessableEntity<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.UnprocessableEntity
                nullResponse.status.value shouldBe 422
                nullResponse.data shouldBe null
            }
        }

        "locked() should create an ApiResponse with HttpStatusCode.Locked" {
            val response = ApiResponse.locked("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.Locked
                response.status.value shouldBe 423
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.locked<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.Locked
                nullResponse.status.value shouldBe 423
                nullResponse.data shouldBe null
            }
        }

        "failedDependency() should create an ApiResponse with HttpStatusCode.FailedDependency" {
            val response = ApiResponse.failedDependency("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.FailedDependency
                response.status.value shouldBe 424
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.failedDependency<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.FailedDependency
                nullResponse.status.value shouldBe 424
                nullResponse.data shouldBe null
            }
        }

        "tooEarly() should create an ApiResponse with HttpStatusCode.TooEarly" {
            val response = ApiResponse.tooEarly("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.TooEarly
                response.status.value shouldBe 425
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.tooEarly<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.TooEarly
                nullResponse.status.value shouldBe 425
                nullResponse.data shouldBe null
            }
        }

        "upgradeRequired() should create an ApiResponse with HttpStatusCode.UpgradeRequired" {
            val response = ApiResponse.upgradeRequired("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.UpgradeRequired
                response.status.value shouldBe 426
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.upgradeRequired<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.UpgradeRequired
                nullResponse.status.value shouldBe 426
                nullResponse.data shouldBe null
            }
        }

        "preconditionRequired() should create an ApiResponse with HttpStatusCode.PreconditionRequired" {
            val response = ApiResponse.preconditionRequired("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.PreconditionRequired
                response.status.value shouldBe 428
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.preconditionRequired<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.PreconditionRequired
                nullResponse.status.value shouldBe 428
                nullResponse.data shouldBe null
            }
        }

        "tooManyRequests() should create an ApiResponse with HttpStatusCode.TooManyRequests" {
            val response = ApiResponse.tooManyRequests("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.TooManyRequests
                response.status.value shouldBe 429
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.tooManyRequests<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.TooManyRequests
                nullResponse.status.value shouldBe 429
                nullResponse.data shouldBe null
            }
        }

        "requestHeaderFieldsTooLarge() should create an ApiResponse with HttpStatusCode.RequestHeaderFieldsTooLarge" {
            val response = ApiResponse.requestHeaderFieldsTooLarge("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.RequestHeaderFieldsTooLarge
                response.status.value shouldBe 431
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.requestHeaderFieldsTooLarge<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.RequestHeaderFieldsTooLarge
                nullResponse.status.value shouldBe 431
                nullResponse.data shouldBe null
            }
        }

        "unavailableForLegalReasons() should create an ApiResponse with HttpStatusCode.UnavailableForLegalReasons" {
            val response = ApiResponse.unavailableForLegalReasons("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.UnavailableForLegalReasons
                response.status.value shouldBe 451
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.unavailableForLegalReasons<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.UnavailableForLegalReasons
                nullResponse.status.value shouldBe 451
                nullResponse.data shouldBe null
            }
        }

        // 5XX Creation methods /////////////////////////////////////////////////////////////////////////////////////

        "internalServerError() should create an ApiResponse with HttpStatusCode.InternalServerError" {
            val response = ApiResponse.internalServerError("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.InternalServerError
                response.status.value shouldBe 500
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.internalServerError<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.InternalServerError
                nullResponse.status.value shouldBe 500
                nullResponse.data shouldBe null
            }
        }

        "notImplemented() should create an ApiResponse with HttpStatusCode.NotImplemented" {
            val response = ApiResponse.notImplemented("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.NotImplemented
                response.status.value shouldBe 501
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.notImplemented<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.NotImplemented
                nullResponse.status.value shouldBe 501
                nullResponse.data shouldBe null
            }
        }

        "badGateway() should create an ApiResponse with HttpStatusCode.BadGateway" {
            val response = ApiResponse.badGateway("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.BadGateway
                response.status.value shouldBe 502
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.badGateway<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.BadGateway
                nullResponse.status.value shouldBe 502
                nullResponse.data shouldBe null
            }
        }

        "serviceUnavailable() should create an ApiResponse with HttpStatusCode.ServiceUnavailable" {
            val response = ApiResponse.serviceUnavailable("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.ServiceUnavailable
                response.status.value shouldBe 503
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.serviceUnavailable<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.ServiceUnavailable
                nullResponse.status.value shouldBe 503
                nullResponse.data shouldBe null
            }
        }

        "gatewayTimeout() should create an ApiResponse with HttpStatusCode.GatewayTimeout" {
            val response = ApiResponse.gatewayTimeout("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.GatewayTimeout
                response.status.value shouldBe 504
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.gatewayTimeout<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.GatewayTimeout
                nullResponse.status.value shouldBe 504
                nullResponse.data shouldBe null
            }
        }

        "httpVersionNotSupported() should create an ApiResponse with HttpStatusCode.HTTPVersionNotSupported" {
            val response = ApiResponse.httpVersionNotSupported("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.HttpVersionNotSupported
                response.status.value shouldBe 505
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.httpVersionNotSupported<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.HttpVersionNotSupported
                nullResponse.status.value shouldBe 505
                nullResponse.data shouldBe null
            }
        }

        "variantAlsoNegotiates() should create an ApiResponse with HttpStatusCode.VariantAlsoNegotiates" {
            val response = ApiResponse.variantAlsoNegotiates("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.VariantAlsoNegotiates
                response.status.value shouldBe 506
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.variantAlsoNegotiates<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.VariantAlsoNegotiates
                nullResponse.status.value shouldBe 506
                nullResponse.data shouldBe null
            }
        }

        "insufficientStorage() should create an ApiResponse with HttpStatusCode.InsufficientStorage" {
            val response = ApiResponse.insufficientStorage("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.InsufficientStorage
                response.status.value shouldBe 507
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.insufficientStorage<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.InsufficientStorage
                nullResponse.status.value shouldBe 507
                nullResponse.data shouldBe null
            }
        }

        "loopDetected() should create an ApiResponse with HttpStatusCode.LoopDetected" {
            val response = ApiResponse.loopDetected("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.LoopDetected
                response.status.value shouldBe 508
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.loopDetected<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.LoopDetected
                nullResponse.status.value shouldBe 508
                nullResponse.data shouldBe null
            }
        }

        "notExtended() should create an ApiResponse with HttpStatusCode.NotExtended" {
            val response = ApiResponse.notExtended("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.NotExtended
                response.status.value shouldBe 510
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.notExtended<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.NotExtended
                nullResponse.status.value shouldBe 510
                nullResponse.data shouldBe null
            }
        }

        "networkAuthenticationRequired() should create an ApiResponse with HttpStatusCode.NetworkAuthenticationRequired" {
            val response = ApiResponse.networkAuthenticationRequired("test data")

            assertSoftly {
                response.status shouldBe HttpStatusCode.NetworkAuthenticationRequired
                response.status.value shouldBe 511
                response.data shouldBe "test data"
            }

            // Test with null data
            val nullResponse = ApiResponse.networkAuthenticationRequired<String>(null)
            assertSoftly {
                nullResponse.status shouldBe HttpStatusCode.NetworkAuthenticationRequired
                nullResponse.status.value shouldBe 511
                nullResponse.data shouldBe null
            }
        }
    }
}
