package io.peekandpoke.ultra.codegen.model

import io.peekandpoke.ultra.common.slumber.Slumber
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.datetime.MpLocalDate
import io.peekandpoke.ultra.datetime.MpTimezone
import io.peekandpoke.ultra.slumber.Polymorphic
import kotlin.reflect.KClass
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement

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

//  Positions whose type cannot be determined  //////////////////////////////////////////////////////

/** A star projection: the element type is genuinely absent, not merely unresolved. */
data class FxStarList(val rows: List<*>)

/** The same for a map value. */
data class FxStarMap(val meta: Map<String, *>)

/**
 * `Any` carries no static shape at all — Slumber serializes whatever the runtime value is, so nothing
 * about the wire follows from the declaration.
 */
data class FxHoldsAny(val payload: Any)

/** A subclass that FIXES its type arguments, so the property type carries none of its own. */
class FxRawHeaders : HashMap<String, String>()

data class FxHoldsRawHeaders(val headers: FxRawHeaders)

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

/** Three undeterminable positions in one type, to pin that the walk reports all of them. */
data class FxManyUndetermined(
    val a: List<*>,
    val b: Map<String, *>,
    val c: Any,
)

/** Reaches a kotlinx `JsonElement`, whose claim is deliberately opaque. */
data class FxHoldsJsonElement(val payload: JsonElement)

//  Generics — the full shape matrix  ////////////////////////////////////////////////////////////////

/** Three parameters, to prove arity is not special-cased at two. */
data class FxTriple<A, B, C>(val a: A, val b: B, val c: C)

/** A parameter in NULLABLE position inside the declaration: `value: T | null`. */
data class FxMaybe<T>(val value: T?)

/** A generic value class — aliases to its parameter rather than to a concrete scalar. */
@JvmInline
value class FxWrapped<T>(val unwrap: T)

/** A RECURSIVE generic: the factory must defer, exactly as a recursive concrete type does. */
data class FxTreeOf<T>(
    val value: T,
    val children: List<FxTreeOf<T>>,
)

/** A generic sealed hierarchy — the case monomorphization silently collapsed to `unknown`. */
sealed class FxStorable<T> {
    abstract val value: T

    data class New<T>(override val value: T) : FxStorable<T>()
    data class Stored<T>(override val value: T, val id: String) : FxStorable<T>()
}

/** Every generic shape reachable from one root, so one walk exercises the matrix. */
data class FxGenericMatrix(
    // parameter kinds
    val nullableArg: FxBox<String?>,
    val nonNullArg: FxBox<String>,
    val nullableProp: FxMaybe<FxSpeaker>,
    val enumArg: FxBox<FxStatus>,
    val valueClassArg: FxBox<FxTalkId>,
    val threeParams: FxTriple<FxSpeaker, FxStatus, FxTalkId>,
    // nesting
    val twoDeep: FxPageOf<FxBox<FxSpeaker>>,
    val threeDeep: FxPageOf<FxBox<FxPageOf<FxSpeaker>>>,
    val genericInList: List<FxBox<FxSpeaker>>,
    val genericInSet: Set<FxBox<FxStatus>>,
    val genericInMap: Map<String, FxBox<FxTalkId>>,
    val listInsideGeneric: FxBox<List<FxSpeaker>>,
    val mapInsideGeneric: FxBox<Map<String, FxSpeaker>>,
    val listOfListInsideGeneric: FxBox<List<List<FxSpeaker>>>,
    val nullableGeneric: FxBox<FxSpeaker>?,
    val listOfNullableGeneric: List<FxBox<FxSpeaker>?>,
    // value class and recursion
    val wrapped: FxWrapped<FxSpeaker>,
    val wrappedScalar: FxWrapped<String>,
    val tree: FxTreeOf<FxSpeaker>,
    val treeOfBoxes: FxTreeOf<FxBox<FxTalkId>>,
    // polymorphism
    val storable: FxStorable<FxSpeaker>,
    val storableOther: FxStorable<FxTalkId>,
)

//  Generics — the recursive and star-projected edges  ///////////////////////////////////////////////

/**
 * A RECURSIVE generic union whose variants sort BEFORE the parent, so `TsDeclOrder` marks the UNION
 * itself as the forward reference — the only arrangement that needs `z.lazy` on the union.
 *
 * The non-generic twin is [FxZeta]. Nesting the variants would take a different, working path.
 */
sealed class FxGZeta<T>

data class FxGAlphaBranch<T>(val kids: List<FxGZeta<T>>) : FxGZeta<T>()

data class FxGAlphaLeaf<T>(val v: T) : FxGZeta<T>()

/** A RECURSIVE generic ALIAS: the generic twin of [FxIds] / [FxIdHolder]. */
@JvmInline
value class FxGRefs<T>(val items: List<FxGHolder<T>>)

data class FxGHolder<T>(val refs: FxGRefs<T>)

data class FxGenericEdges(
    val union: FxGZeta<FxSpeaker>,
    val alias: FxGHolder<FxTalkId>,
)

/** A STAR-projected argument on a user generic — the element type is genuinely absent. */
data class FxStarGeneric(val boxed: FxBox<*>)

/** Partially star-projected: some arguments known, some not. */
data class FxPartialStarGeneric(val triple: FxTriple<String, *, *>)

//  Generic sealed children that REBIND their parent's parameters  ///////////////////////////////////

/** A child that REORDERS its parent's parameters. Legal Kotlin; positional binding inverts it. */
sealed class FxEither<L, R>

data class FxLeft<R, L>(val left: L) : FxEither<L, R>()

data class FxRight<R, L>(val right: R) : FxEither<L, R>()

data class FxHoldsEither(val either: FxEither<Int, String>)

/** A child that TRANSFORMS its parent's argument. Same arity, not expressible as a pass-through. */
sealed class FxFeed<T>

data class FxSingle<T>(val item: T) : FxFeed<T>()

data class FxBatched<U>(val items: List<U>) : FxFeed<List<U>>()

data class FxHoldsFeed(val feed: FxFeed<List<String>>)

//  Identifiers TypeScript will not accept  //////////////////////////////////////////////////////////

/** A Kotlin class named `Record` captures the TypeScript global the emitter uses for every Map. */
data class Record(val id: String)

data class FxUsesRecord<T>(val entries: Map<String, T>, val rec: Record)

data class FxHoldsRecordShadow(val cfg: FxUsesRecord<String>)

/** `infer` is not a Kotlin keyword; in TypeScript it is a SYNTAX error in type-parameter position. */
data class FxInferParam<infer>(val v: infer)

data class FxHoldsInferParam(val p: FxInferParam<String>)

/** A type parameter shadowing its own declaration's name. */
data class FxSelfShadow<FxSelfShadow>(val v: FxSelfShadow)

data class FxHoldsSelfShadow(val s: FxSelfShadow<String>)

/** A child binding ONE of its parameters to BOTH of its parent's — not a bijection either. */
sealed class FxSame<X, Y>

data class FxBoth<A>(val a: A) : FxSame<A, A>()

data class FxHoldsSame(val same: FxSame<String, String>)

//  Claimed types reached from a root  ///////////////////////////////////////////////////////////////

/**
 * Reaches CLAIMED types, so the emitted file carries a real `import ... from './runtime/datetime.ts'`.
 *
 * Every other fixture imports nothing but `zod`, which is why an extensionless module specifier
 * survived until 2026-07-30: `tsc` resolves it under `moduleResolution: bundler`, and nothing ever
 * asked Node to load a generated file that imports a runtime module.
 */
data class FxDated(
    val at: MpInstant,
    val day: MpLocalDate,
    val zone: MpTimezone,
    val optional: MpInstant?,
)
