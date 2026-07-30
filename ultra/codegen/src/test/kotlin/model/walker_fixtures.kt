package io.peekandpoke.ultra.codegen.model

import io.peekandpoke.ultra.slumber.Polymorphic
import io.peekandpoke.ultra.slumber.Slumber
import kotlinx.serialization.SerialName
import kotlin.reflect.KClass

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
        override val childTypes: Set<KClass<*>> = setOf(Created::class, Deleted::class)
    }

    data class Created(val at: String) : FxEvent() {
        companion object : Polymorphic.Child {
            override val identifier: String = "created"
        }
    }

    @SerialName("deleted")
    data class Deleted(val at: String) : FxEvent()
}

//  A RECURSIVE polymorphic hierarchy  //////////////////////////////////////////////////////////////

/**
 * A union that is ITSELF emitted lazily, which is the only branch that writes `export type X = A | B`
 * — and therefore the only place a variant's TYPE name is used rather than its schema.
 *
 * Getting there needs care. `TsDeclOrder` seeds a DFS from `decls.keys.sortedBy { it.key }` and a
 * declaration is lazy iff it references something at or after its own index, so a union whose variants
 * are NESTED classes is never lazy: `Parent.Child` sorts after `Parent`, the walk starts at the parent,
 * and post-order puts every child first. The variants here are TOP-LEVEL and named to sort BEFORE the
 * parent, so the walk starts at a variant and the union lands mid-order with a forward reference.
 */
sealed class FxZeta

data class FxAlphaBranch(val kids: List<FxZeta>) : FxZeta()

data class FxAlphaLeaf(val v: String) : FxZeta()

//  A value class ON a cycle  ///////////////////////////////////////////////////////////////////////

/**
 * A value class whose underlying type reaches back to the class holding it.
 *
 * Aliases are the one declaration kind that can be recursive without being an object or a union —
 * `TsTypeDecl.Alias.referencedIds()` delegates to its target — so the emitter has to defer this one
 * exactly as it defers a recursive object. A zod schema is a `const`, so an eager forward reference is
 * a temporal-dead-zone error at module evaluation rather than a compile warning.
 */
@JvmInline
value class FxIds(val items: List<FxIdHolder>)

data class FxIdHolder(val ids: FxIds)

//  Polymorphism — the Polymorphic.Parent companion sits on the ROOT  ///////////////////////////////

/**
 * A three-level hierarchy whose `Polymorphic.Parent` companion is on the root.
 *
 * Walking the INTERMEDIATE class is what discriminates here: for the root, the declared class already
 * is the companion holder, so correct and incorrect resolution agree and the test proves nothing.
 * `createParentSlumberer` resolves `getParent(cls)` before reading the discriminator, so anything
 * typed as [FxDeepRoot.Middle] is still written with the root's `kind`.
 */
sealed class FxDeepRoot {
    companion object : Polymorphic.Parent {
        override val discriminator: String = "kind"

        // Left empty on purpose: `getChildren` unions this with `sealedSubclasses` and recurses, so a
        // sealed hierarchy is discovered without listing anything here.
        override val childTypes: Set<KClass<*>> = emptySet()
    }

    sealed class Middle : FxDeepRoot()

    data class Leaf(val v: String) : Middle()

    data class Direct(val w: String) : FxDeepRoot()
}

//  Wire strings that are not identifier-shaped  ////////////////////////////////////////////////////

/**
 * A hierarchy whose wire strings contain characters that end a TypeScript literal.
 *
 * `Polymorphic.Parent.discriminator` and `Polymorphic.Child.identifier` are ordinary string constants,
 * and a property name can be any backtick identifier — none of the three is constrained to look like a
 * TypeScript identifier. Emitting them raw produced a `models.ts` that does not parse.
 */
sealed class FxQuoted {
    companion object : Polymorphic.Parent {
        override val discriminator: String = "@type"
        override val childTypes: Set<KClass<*>> = setOf(Apostrophe::class)
    }

    data class Apostrophe(
        val plain: String,
        /** Needs quoting AND escaping — the quoted form would otherwise close on the apostrophe. */
        val `it's`: String,
        /** Needs quoting but no escaping. */
        val `dashed-name`: String,
    ) : FxQuoted() {
        companion object : Polymorphic.Child {
            override val identifier: String = """O'Brien\Co"""
        }
    }
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

/** Holds an object array — supported by Slumber since 20260729-slumber-array-support. */
data class FxHoldsArray(val items: Array<String>) {
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = items.contentHashCode()
}

/** Primitive arrays carry no type argument, so their element type comes from a lookup table. */
data class FxHoldsPrimitiveArrays(
    val ints: IntArray,
    val flags: BooleanArray,
    val chars: CharArray,
) {
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = ints.contentHashCode()
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

//  Generics probe  /////////////////////////////////////////////////////////////////////////////////

data class FxBox<T>(val item: T, val label: String)

data class FxPair<A, B>(val first: A, val second: B)

/**
 * The same generic instantiated with a nullable and a non-nullable argument.
 *
 * `ReifiedKType` preserves the argument's nullability, so these reify to different props — `item` is
 * `string` in one and `string | null` in the other. They must not collapse onto one declaration.
 */
data class FxNullableArgs(
    val required: FxBox<String>,
    val optional: FxBox<String?>,
)

/** Generic in a property position, nested in containers, and doubly nested. */
data class FxGenericHolder(
    val page: FxPageOf<FxSpeaker>,
    val boxes: List<FxBox<FxSpeaker>>,
    val nested: FxBox<List<FxSpeaker>>,
    val deep: FxPageOf<FxBox<FxSpeaker>>,
    val pair: FxPair<FxSpeaker, FxStatus>,
    val mapped: Map<String, FxBox<FxTalkId>>,
)

/** The same generic at two different instantiations in one model. */
data class FxTwoInstantiations(
    val a: FxBox<FxSpeaker>,
    val b: FxBox<FxStatus>,
)
