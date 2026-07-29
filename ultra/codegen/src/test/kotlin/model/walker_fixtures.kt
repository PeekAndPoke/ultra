package io.peekandpoke.ultra.codegen.model

import io.peekandpoke.ultra.slumber.Polymorphic
import io.peekandpoke.ultra.slumber.Slumber
import kotlinx.serialization.SerialName

//  Simple shapes  //////////////////////////////////////////////////////////////////////////////////

enum class FxStatus { ACTIVE, ARCHIVED }

@JvmInline
value class FxTalkId(val value: String)

@JvmInline
value class FxSeatCount(val value: Int)

data class FxSpeaker(
    val name: String,
    val bio: String?,
)

data class FxTalk(
    val id: FxTalkId,
    val title: String,
    val status: FxStatus,
    val speakers: List<FxSpeaker>,
    val tags: Set<String>,
    val meta: Map<String, String>,
    val seats: FxSeatCount,
    val durationMs: Long,
    val rating: Double?,
    val featured: Boolean = false,
)

//  Cycles  /////////////////////////////////////////////////////////////////////////////////////////

data class FxNode(
    val name: String,
    val children: List<FxNode>,
    val parent: FxNode?,
)

data class FxMutualA(val b: FxMutualB?)

data class FxMutualB(val a: FxMutualA?)

//  Generics  ///////////////////////////////////////////////////////////////////////////////////////

data class FxPageOf<T>(
    val items: List<T>,
    val total: Int,
)

//  Polymorphism — plain sealed  ////////////////////////////////////////////////////////////////////

sealed class FxShape {
    data class Circle(val radius: Double) : FxShape()
    data class Square(val side: Double) : FxShape()
}

//  Polymorphism — custom discriminator + identifiers  //////////////////////////////////////////////

sealed class FxEvent {
    companion object : Polymorphic.Parent {
        override val discriminator: String = "kind"
        override val childTypes: Set<kotlin.reflect.KClass<*>> = setOf(Created::class, Deleted::class)
    }

    data class Created(val at: String) : FxEvent() {
        companion object : Polymorphic.Child {
            override val identifier: String = "created"
        }
    }

    @SerialName("deleted")
    data class Deleted(val at: String) : FxEvent()
}

//  @Slumber.Field on a non-constructor property  ///////////////////////////////////////////////////

data class FxWithExtraField(val a: String) {
    @Slumber.Field
    val computed: String = "$a!"
}

//  Slumber-dispatch parity cases  //////////////////////////////////////////////////////////////////

/**
 * A sealed hierarchy covering all three variant shapes.
 *
 * [Pending] is a PLAIN object, which only `ObjectInstanceCodec` handles — a `data object` would also
 * satisfy `isData` and so cannot distinguish the object branch from the data-class branch.
 */
sealed class FxResult {
    object Pending : FxResult()
    data object Skipped : FxResult()
    data class Done(val value: String) : FxResult()
}

/** Not a data class, but has a no-arg primary constructor — Slumber routes it to DataClassSlumberer. */
class FxNoArgCtor {
    val ignored: String = "not a ctor property"
}

/** A value class wrapping a list — Slumber resolves value classes BEFORE collections. */
@JvmInline
value class FxIdList(val values: List<String>)

/** Holds kotlin stdlib value classes, which Slumber refuses rather than serializing generically. */
data class FxHoldsStdlibValueClass(val count: UInt)

/** Holds an array, which Slumber has no slumberer for. */
data class FxHoldsArray(val items: Array<String>) {
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = items.contentHashCode()
}

data class FxHoldsNoArgCtor(val thing: FxNoArgCtor)

data class FxHoldsIdList(val ids: FxIdList)

//  Unresolvable  ///////////////////////////////////////////////////////////////////////////////////

interface FxPlainInterface {
    val name: String
}

data class FxHoldsInterface(val thing: FxPlainInterface)

//  Claimed type stand-in  //////////////////////////////////////////////////////////////////////////

class FxCustomCodecType(val whatever: String)

data class FxHoldsClaimed(val stamp: FxCustomCodecType)

/**
 * A sealed hierarchy where one child is handled by a custom codec.
 *
 * Union variants are enqueued directly by the walker rather than going through reference
 * resolution, so this is the one path on which the claim guard in `declare` is load-bearing.
 */
sealed class FxPartlyClaimed {
    data class Plain(val a: String) : FxPartlyClaimed()
    data class Custom(val b: String) : FxPartlyClaimed()
}

//  Nested classes that share a simple name  ////////////////////////////////////////////////////////

object FxOuterA {
    data class Inner(val a: String)
}

object FxOuterB {
    data class Inner(val b: String)
}

data class FxHoldsBothInners(val a: FxOuterA.Inner, val b: FxOuterB.Inner)
