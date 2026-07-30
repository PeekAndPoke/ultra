package io.peekandpoke.funktor.codegen

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiRoute
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.ultra.codegen.sdk.TsSdkBuilder
import io.peekandpoke.ultra.datetime.MpInstant

class RestApiTsContributorSpec : FreeSpec() {

    private fun build(
        groups: List<ApiRoutes>,
        include: (ApiRoute<*>) -> Boolean = { true },
    ) = TsSdkBuilder
        .forTesting(listOf(RestApiTsContributor(lazyOf(listOf(FxDemoApiFeature(groups))), include)))
        .build()

    private fun clientOf(result: TsSdkBuilder.Result): String =
        result.output.entries().first { it.path == "fxDemoClient.ts" }.content

    init {
        "the emitted file set mirrors the feature" {
            val result = build(listOf(FxTalksApiRoutes(), FxSpeakersApiRoutes()))

            result.output.entries().map { it.path } shouldContainExactlyInAnyOrder listOf(
                "models.ts",
                "fxDemoClient.ts",
                // The client runtime and everything it imports — the closure, not just client.ts.
                "runtime/client.ts",
                "runtime/http.ts",
                "runtime/apiResponse.ts",
            )
        }

        "names come from the feature and group names, not from the Kotlin class names" {
            val out = clientOf(build(listOf(FxTalksApiRoutes(), FxSpeakersApiRoutes())))

            out shouldContain "export class FxTalksApi {"
            out shouldContain "export class FxSpeakersApi {"
            out shouldContain "export class FxDemoClient {"

            withClue("the aggregate exposes each group under a camelCased member") {
                out shouldContain "readonly fxTalks: FxTalksApi"
                out shouldContain "readonly fxSpeakers: FxSpeakersApi"
                out shouldContain "this.fxTalks = new FxTalksApi(config)"
            }
        }

        "an endpoint member uses codeGen funcName when it is set" {
            clientOf(build(listOf(FxSpeakersApiRoutes()))) shouldContain "readonly listSpeakers = () =>"
        }

        "an endpoint with no funcName gets a derived member naming its method and path" {
            val out = clientOf(build(listOf(FxTalksApiRoutes())))

            // Deliberately verbose: unambiguous, collision-resistant, and obvious in review so that
            // `funcName` gets set. A pretty guess would be quietly wrong instead.
            out shouldContain "readonly getApiFxTalksLatest = () =>"
        }

        "the request carries the route's method and pattern verbatim" {
            val out = clientOf(build(listOf(FxTalksApiRoutes())))

            out shouldContain "request(this.config, 'GET', '/api/fx/talks', z.array(FxTalkModel))"
        }

        "the ENVELOPE is unwrapped — the payload schema is passed, never ApiResponse" {
            // A route's responseType is `ApiResponse<T>`, because a handler returns ApiResponse.ok(x).
            // Rooting that unwrapped walks the hand-written envelope into models.ts AND makes `request`
            // wrap it a second time, so every parse fails against output no server produces.
            val result = build(listOf(FxTalksApiRoutes()))

            val models = result.output.entries().first { it.path == "models.ts" }.content

            withClue("models.ts must not declare the envelope — runtime/apiResponse.ts owns it") {
                models shouldNotContain "ApiResponse"
            }

            withClue("the payload schema reaches `request`, not the envelope's") {
                clientOf(result) shouldNotContain "ApiResponse"
            }

            result.model.decls.values.map { it.name } shouldContainExactlyInAnyOrder listOf("FxTalkModel")
        }

        "the include predicate narrows the root set" {
            val result = build(
                groups = listOf(FxTalksApiRoutes(), FxSpeakersApiRoutes()),
                include = { it.codeGen.tags.contains("public") },
            )

            val out = clientOf(result)

            out shouldContain "readonly listSpeakers = () =>"

            withClue("a filtered-out route contributes neither a member nor a group") {
                out shouldNotContain "listTalks"
                out shouldNotContain "FxTalksApi"
            }

            withClue("its payload type is not reached either, so models.ts never declares it") {
                result.model.decls.values.map { it.name } shouldContainExactlyInAnyOrder listOf("FxSpeakerModel")
            }
        }

        "a feature whose routes are all filtered out emits no client file at all" {
            val thrown = runCatching { build(listOf(FxTalksApiRoutes()), include = { false }) }
                .exceptionOrNull()

            // No roots at all means an empty SDK, which the builder refuses — the right failure, and
            // louder than silently writing a client class with no members.
            thrown!!.message!! shouldContain "supplied any root type"
        }

        "a route variant the generator cannot handle is refused, naming the route" {
            val thrown = runCatching { build(listOf(FxBodyApiRoutes())) }.exceptionOrNull()

            thrown!!.message!! shouldContain "/api/fx/talks"
            thrown.message!! shouldContain "WithBody"

            withClue("it must name the route rather than silently emitting a half client") {
                thrown.message!! shouldContain "createTalk"
            }
        }

        "url parameters" - {

            "path params fill the pattern, everything else becomes a query param" {
                val out = clientOf(build(listOf(FxParamApiRoutes())))

                withClue("the caller passes ONE object, mirroring the Kotlin PARAMS class") {
                    out shouldContain
                            "readonly getTalk = (params: { id: string; page?: number; " +
                            "search?: string | null; order?: 'ASC' | 'DESC'; exact?: boolean }) =>"
                }

                withClue("`id` is in the pattern so it fills the path; the rest go to the query") {
                    out shouldContain "path: { id: params.id },"
                    out shouldContain
                            "query: { page: params.page, search: params.search, " +
                            "order: params.order, exact: params.exact },"
                }
            }

            "a defaulted Kotlin parameter is optional in TypeScript, a required one is not" {
                val out = clientOf(build(listOf(FxParamApiRoutes())))

                withClue("`id` has no default, so it must not be optional") {
                    out shouldContain "id: string;"
                    out shouldNotContain "id?: string"
                }
            }

            "wire types are mapped, not Kotlin types" {
                // A value class travels as its underlying value, and an enum as its constant name.
                val out = clientOf(build(listOf(FxParamApiRoutes())))

                withClue("FxTalkId is a value class over String, so it is `string` on the wire") {
                    out shouldNotContain "FxTalkId"
                }

                withClue("an enum is the union of its constant names, needing no import") {
                    out shouldContain "'ASC' | 'DESC'"
                }
            }

            "a CLAIMED parameter type is mapped, and its format reaches the caller's TSDoc" {
                val result = TsSdkBuilder
                    .forTesting(
                        listOf(
                            RestApiTsContributor(
                                lazyOf(listOf(FxDemoApiFeature(listOf(FxClaimedParamApiRoutes()))))
                            ),
                            FunktorUrlParamsTsContributor(),
                        )
                    )
                    .build()

                val out = clientOf(result)

                withClue("MpInstant is `string` in a URL, not the object it is in a body") {
                    out shouldContain "readonly eventsSince = (params: { at: string; until?: string | null }) =>"
                }

                withClue("`string` says nothing about what the server can parse back") {
                    out shouldContain "@param params.at ISO-8601 instant"
                    out shouldContain "@param params.until ISO-8601 date, yyyy-MM-dd"
                }
            }

            "the same type is an object in a body and a string in a URL" {
                // The two claim registries are independent on purpose; this is the case that proves
                // deriving one from the other would be wrong.
                val result = TsSdkBuilder
                    .forTesting(
                        listOf(
                            RestApiTsContributor(
                                lazyOf(listOf(FxDemoApiFeature(listOf(FxClaimedParamApiRoutes()))))
                            ),
                            FunktorUrlParamsTsContributor(),
                        )
                    )
                    .build()

                val claim = claimsOf(FunktorUrlParamsTsContributor()).find(MpInstant::class)

                claim?.tsType shouldBe "string"

                withClue("as a URL param it is a bare string, with no import of the object type") {
                    clientOf(result) shouldNotContain "MpInstant"
                }
            }

            "a parameter whose wire form is not provable is refused, naming it and its type" {
                val thrown = runCatching { build(listOf(FxBadParamApiRoutes())) }.exceptionOrNull()

                thrown!!.message!! shouldContain "'model'"
                thrown.message!! shouldContain "FxTalkModel"

                withClue("the message must say what IS mappable, or the reader has to guess") {
                    thrown.message!! shouldContain "value classes"
                }
            }
        }

        "two routes in one group that produce the same member are refused, naming it" {
            val thrown = runCatching { build(listOf(FxClashApiRoutes())) }.exceptionOrNull()

            thrown!!.message!! shouldContain "same"
            thrown.message!! shouldContain "funcName"
        }

        "root labels are qualified, so two features cannot collide" {
            val result = TsSdkBuilder
                .forTesting(
                    listOf(
                        RestApiTsContributor(lazyOf(listOf(FxDemoApiFeature(listOf(FxSpeakersApiRoutes()))))),
                    )
                )
                .build()

            result.model.rootRefs.keys shouldBe setOf("funktor:rest:FxDemo:fx-speakers:listSpeakers")
        }
    }
}
