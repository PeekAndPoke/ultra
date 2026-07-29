package io.peekandpoke.ultra.codegen.contributors

import io.peekandpoke.ultra.codegen.model.TsTypeClaims
import io.peekandpoke.ultra.codegen.sdk.TsSdkContributor
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Claims the kotlinx JSON types that Slumber has codecs for.
 *
 * These exist precisely for payloads a Kotlin data class cannot express, so a permissive TypeScript
 * type is the honest answer rather than a gap in the generator. Without these claims the walker
 * cannot classify them structurally and validation would (correctly) fail.
 *
 * All five are claimed, matching `BuiltInModule`'s dispatch — claiming only the common two would
 * leave the others failing validation the first time somebody used one.
 */
class KotlinxJsonTsContributor : TsSdkContributor {

    override val name: String = "ultra:codegen:kotlinx-json"

    override fun claimTypes(claims: TsTypeClaims.Scope) {
        // Any JSON value at all.
        claims.map<JsonElement>(tsName = "unknown", schema = "z.unknown()")

        // A JSON object with arbitrary keys.
        claims.map<JsonObject>(tsName = "Record<string, unknown>", schema = "z.record(z.string(), z.unknown())")

        // A JSON array with arbitrary elements.
        claims.map<JsonArray>(tsName = "unknown[]", schema = "z.array(z.unknown())")

        // KotlinXJsonPrimitiveCodec unwraps to a bare scalar, so the union is exact rather than opaque.
        claims.map<JsonPrimitive>(
            tsName = "string | number | boolean | null",
            schema = "z.union([z.string(), z.number(), z.boolean(), z.null()])",
        )

        claims.map<JsonNull>(tsName = "null", schema = "z.null()")
    }
}
