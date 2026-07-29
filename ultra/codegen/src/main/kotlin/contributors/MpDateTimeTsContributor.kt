package io.peekandpoke.ultra.codegen.contributors

import io.peekandpoke.ultra.codegen.model.TsTypeClaims
import io.peekandpoke.ultra.codegen.sdk.TsSdkContributor
import io.peekandpoke.ultra.codegen.sdk.TsSdkEmitContext
import io.peekandpoke.ultra.codegen.ts.TsRuntime
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.datetime.MpLocalDate
import io.peekandpoke.ultra.datetime.MpLocalDateTime
import io.peekandpoke.ultra.datetime.MpLocalTime
import io.peekandpoke.ultra.datetime.MpTimezone
import io.peekandpoke.ultra.datetime.MpZonedDateTime

/**
 * Claims the ultra/datetime types and ships their hand-written TypeScript.
 *
 * These all go through custom Slumber codecs, so their JSON shape cannot be derived — `MpInstant` is a
 * data class over a single value yet writes `{ts, timezone, human}`. Two of the six are not objects at
 * all: `MpLocalTime` is a bare number and `MpTimezone` a bare string.
 *
 * The TypeScript lives in `resources/ts/runtime/datetime.ts` as a checked-in resource, maintained by
 * hand next to the codecs it mirrors. `MpDateTimeFieldParitySpec` fails if the two drift — mandatory,
 * because a claim is trusted and never verified by the generator itself.
 */
class MpDateTimeTsContributor : TsSdkContributor {

    companion object {
        /** How generated code imports the runtime module. */
        val MODULE: String = TsRuntime.Module.DateTime.moduleSpecifier

        /** The claimed types, paired with the TypeScript name each maps to. */
        val CLAIMED: Map<kotlin.reflect.KClass<*>, String> = mapOf(
            MpInstant::class to "MpInstant",
            MpLocalDate::class to "MpLocalDate",
            MpLocalDateTime::class to "MpLocalDateTime",
            MpZonedDateTime::class to "MpZonedDateTime",
            MpLocalTime::class to "MpLocalTime",
            MpTimezone::class to "MpTimezone",
        )
    }

    override val name: String = "ultra:codegen:datetime"

    override fun claimTypes(claims: TsTypeClaims.Scope) {
        CLAIMED.forEach { (cls, tsName) ->
            // Type and schema share a name: datetime.ts exports both, the zod pattern.
            claims.map(cls = cls, tsName = tsName, importFrom = MODULE, schema = tsName)
        }
    }

    override fun emit(context: TsSdkEmitContext) {
        // Only ship the runtime when something actually reaches it — no dead code in the SDK.
        val used = CLAIMED.keys.any { cls ->
            context.model.usedClaims.containsKey(cls.qualifiedName)
        }

        if (used) {
            TsRuntime.emit(context.out, setOf(TsRuntime.Module.DateTime))
        }
    }
}
