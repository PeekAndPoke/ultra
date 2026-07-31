package io.peekandpoke.funktor.codegen

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
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

        "two ApiRoutes sharing a group name merge into ONE class" {
            // Found by generating the demo's real API: funktor:auth declares ApiRoutes("login")
            // twice, and emitting a class per instance produced two `LoginApi` classes and two
            // `login` members in one file. Only tsc caught it (TS2300) — no Kotlin assertion did.
            val out = clientOf(build(listOf(FxSplitPublicRoutes(), FxSplitSecuredRoutes())))

            withClue("exactly one class, carrying both halves' members") {
                Regex("export class FxSplitApi \\{").findAll(out).count() shouldBe 1
                out shouldContain "readonly openPart = () =>"
                out shouldContain "readonly securedPart = () =>"
            }

            withClue("and one aggregate member, not two") {
                Regex("readonly fxSplit: FxSplitApi").findAll(out).count() shouldBe 1
            }
        }

        "merged groups still catch a member collision between the two halves" {
            val thrown = runCatching {
                build(listOf(FxSplitPublicRoutes(), FxSplitClashRoutes()))
            }.exceptionOrNull()

            thrown!!.message!! shouldContain "openPart"
        }

        "emitted identifiers and literals" - {

            "two GROUPS sharing a member name each keep their own schema" {
                // CRITICAL, review 2026-07-30. Schemas were keyed by member name across the whole
                // client, so `associate` (last-wins) gave one group the other's schema. Real in this
                // repo: FunktorClusterApiFeature has three groups declaring funcName = "list".
                // `z.array(X)` type-checks for any X, so tsc stayed silent and every call threw
                // ApiProtocolError against a fresh SDK.
                val out = clientOf(build(listOf(FxSharedNameTalksRoutes(), FxSharedNameSpeakersRoutes())))

                out shouldContain
                        "request(this.config, 'GET', '/api/fx/shared/talks', z.array(FxTalkModel))"
                out shouldContain
                        "request(this.config, 'GET', '/api/fx/shared/speakers', FxSpeakerModel)"
            }

            "a funcName that is not a TypeScript identifier is refused, naming the route" {
                // Verified against the pinned compiler: the emitted form below is a well-formed EXTRA
                // class field that runs on construction, and tsc exits 0 — arbitrary JavaScript in a
                // file that ships to every user's browser.
                val thrown = runCatching { build(listOf(FxHostileFuncNameRoutes())) }.exceptionOrNull()

                thrown!!.message!! shouldContain "/api/fx/evil"
                thrown.message!! shouldContain "not a valid TypeScript identifier"
            }

            "a parameter name that is not a TypeScript identifier is refused" {
                // Kotlin permits backticked property names, which would emit `params.a b`.
                val thrown = runCatching { build(listOf(FxHostileParamRoutes())) }.exceptionOrNull()

                thrown!!.message!! shouldContain "'a b'"
                thrown.message!! shouldContain "identifier position"
            }

            "enum constant names are ESCAPED, so a hostile one cannot widen the union" {
                val out = clientOf(build(listOf(FxHostileEnumRoutes())))

                withClue("the quote must be escaped rather than closing the literal early") {
                    out shouldContain "\\'"
                }

                withClue("the union must not degrade to `string`, which would accept anything") {
                    out shouldNotContain "order?: 'a' | string"
                }
            }
        }

        "server-sent events" - {

            "a stream member returns an AsyncGenerator and takes SseOptions" {
                val out = clientOf(build(listOf(FxTalksApiRoutes(), FxSseApiRoutes())))

                out shouldContain
                        "readonly watch = (params: { room: string }, options?: SseOptions)" +
                        ": AsyncGenerator<SseEvent> =>"

                withClue("path params fill the pattern exactly as they do for a request") {
                    out shouldContain "stream(this.config, '/api/fx/watch/{room}', {"
                    out shouldContain "path: { room: params.room },"
                    out shouldContain "}, options)"
                }
            }

            "a stream has NO response schema — the payload type is not on the route" {
                val out = clientOf(build(listOf(FxTalksApiRoutes(), FxSseApiRoutes())))

                withClue("ApiRoute.Sse.responseType is TypeRef<Unit>; nothing may be invented from it") {
                    out shouldNotContain "stream(this.config, '/api/fx/watch/{room}', z."
                }
            }

            "the SSE runtime ships only when a stream endpoint exists" {
                val withSse = build(listOf(FxTalksApiRoutes(), FxSseApiRoutes()))
                    .output.entries().map { it.path }

                withSse shouldContain "runtime/sse.ts"

                val withoutSse = build(listOf(FxTalksApiRoutes())).output.entries().map { it.path }

                withClue("an SDK with no streams must not carry the event-stream parser") {
                    withoutSse shouldNotContain "runtime/sse.ts"
                }
            }

            "a stream-free client does not IMPORT the sse runtime either" {
                // The file emission and the import are two separate conditions. Getting only the first
                // right leaves a client importing a module that was never written — a broken SDK that
                // no Kotlin assertion about emitted paths would notice, and that `tsc` cannot catch
                // here because ts-verify copies every runtime module into place regardless.
                val out = clientOf(build(listOf(FxTalksApiRoutes())))

                out shouldNotContain "runtime/sse.ts"

                withClue("and one WITH a stream must import it, or the emitted call is unresolved") {
                    clientOf(build(listOf(FxTalksApiRoutes(), FxSseApiRoutes()))) shouldContain
                            "from './runtime/sse.ts'"
                }
            }
        }

        "request bodies" - {

            "a body-only route takes the body as its single argument" {
                val out = clientOf(build(listOf(FxBodyApiRoutes())))

                out shouldContain "readonly createTalk = (body: FxSaveTalkRequest) =>"

                withClue("the body reaches `request` through its options, not the URL") {
                    out shouldContain "request(this.config, 'POST', '/api/fx/talks', FxTalkModel, {"
                    out shouldContain "body,"
                }
            }

            "a body WITH params takes both, params first, mirroring the Kotlin argument order" {
                val out = clientOf(build(listOf(FxBodyApiRoutes())))

                out shouldContain
                        "readonly updateTalk = (params: { id: string }, body: FxSaveTalkRequest) =>"

                out shouldContain "path: { id: params.id },"
            }

            "a type reachable only as a body is still declared and imported" {
                // The bug this guards: naming the body type without ROOTING it. The client would
                // reference FxSaveTalkRequest, models.ts would never declare it, and only tsc would
                // notice — in the consumer's project, not here.
                val result = build(listOf(FxBodyApiRoutes()))

                val models = result.output.entries().first { it.path == "models.ts" }.content

                models shouldContain "export const FxSaveTalkRequest"

                withClue("and the client must IMPORT it — the bare name also appears in the signature") {
                    clientOf(result) shouldContain "import { FxSaveTalkRequest, FxTalkModel } from './models.ts'"
                }

                result.model.decls.values.map { it.name } shouldContainExactlyInAnyOrder listOf(
                    "FxTalkModel",
                    "FxSaveTalkRequest",
                )
            }

            "the body is used in TYPE position, never as a schema" {
                // Nothing validates the request client-side: the server is the authority on what it
                // accepts. Emitting the schema here would be a different, and wrong, contract.
                val out = clientOf(build(listOf(FxBodyApiRoutes())))

                out shouldNotContain "body: z."
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

            // The token must be ABSENT from the message template, or the assertion passes on static
            // text: "produces the same TypeScript member name twice" already contains "same".
            thrown!!.message!! shouldContain "clashingMemberXyz"
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
