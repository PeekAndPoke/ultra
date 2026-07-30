package io.peekandpoke.funktor.codegen

import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.TypedApiEndpoint
import io.peekandpoke.ultra.remote.api
import io.peekandpoke.ultra.remote.apiList
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

/** A request body — a variant the generator does not support yet. */
class FxBodyApiRoutes : ApiRoutes("fx-body", authFloor = { public() }) {

    val createTalk = TypedApiEndpoint
        .Post(
            uri = "/api/fx/talks",
            body = FxTalkModel.serializer(),
            response = FxTalkModel.serializer().api(),
        )
        .mount {
            codeGen { funcName = "createTalk" }.handle { ApiResponse.ok(it) }
        }
}

/** Two routes whose `funcName` collides, so the duplicate-member guard can be provoked. */
class FxClashApiRoutes : ApiRoutes("fx-clash", authFloor = { public() }) {

    val a = TypedApiEndpoint
        .Get(uri = "/api/fx/a", response = FxTalkModel.serializer().api())
        .mount {
            codeGen { funcName = "same" }.handle { ApiResponse.ok(FxTalkModel("a", "A")) }
        }

    val b = TypedApiEndpoint
        .Get(uri = "/api/fx/b", response = FxTalkModel.serializer().api())
        .mount {
            codeGen { funcName = "same" }.handle { ApiResponse.ok(FxTalkModel("b", "B")) }
        }
}

class FxDemoApiFeature(private val groups: List<ApiRoutes>) : ApiFeature {
    override val name: String = "FxDemo"
    override val description: String = "A demo feature for the TypeScript generator."
    override fun getRouteGroups(): List<ApiRoutes> = groups
}
