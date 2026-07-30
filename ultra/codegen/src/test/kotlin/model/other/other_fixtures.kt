package io.peekandpoke.ultra.codegen.model.other

import io.peekandpoke.ultra.codegen.model.FxSpeaker as MainFxSpeaker

/**
 * A type whose simple name matches one in the parent fixture package.
 *
 * Monomorphized TypeScript names are built from simple names, so two same-named classes in different
 * packages collapse onto one. `TsNames` documents that as deliberate and delegates the failure to the
 * validator's collision check — this fixture is what lets that check be tested.
 */
data class FxSpeaker(val somethingElse: String)

data class FxHoldsBothSpeakers(
    val mine: FxSpeaker,
    val theirs: MainFxSpeaker,
)
