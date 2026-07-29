package io.peekandpoke.ultra.codegen.model

import io.peekandpoke.ultra.reflection.ReifiedKType
import io.peekandpoke.ultra.reflection.hasAnyAnnotationOnPropertyDefinedOnSuperTypes
import io.peekandpoke.ultra.reflection.hasAnyAnnotationRecursive
import io.peekandpoke.ultra.slumber.Slumber
import io.peekandpoke.ultra.slumber.builtin.polymorphism.PolymorphicChildUtil
import io.peekandpoke.ultra.slumber.builtin.polymorphism.PolymorphicParentUtil
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.withNullability

/**
 * Walks the transitive closure of types reachable from a set of roots and turns it into a [TypeModel].
 *
 * Classification mirrors Slumber's own dispatch order, because the generated types must describe what
 * Slumber *writes*, not what Kotlin declares. Where the two could drift, this walker defers to the
 * slumber-side utility (`PolymorphicParentUtil`, `PolymorphicChildUtil`, `DataClassSlumberer`'s field
 * selection) rather than re-deriving the rule.
 *
 * The walk is cycle-safe via a `seen` set, and every reference it emits is a symbolic [TypeId], so a
 * type may be referenced long before (or without ever) being declared.
 */
class TypeWalker(
    private val claims: TsTypeClaims,
) {
    /** A starting point for the walk, with a label used in the "reached via" trail. */
    data class Root(val type: KType, val label: String)

    private data class Pending(val id: TypeId, val path: List<String>)

    private val decls = LinkedHashMap<TypeId, TsTypeDecl>()
    private val usedClaims = LinkedHashMap<String, TsTypeClaim>()
    private val unresolved = mutableListOf<TypeModel.Unresolved>()
    private val longValued = mutableListOf<TypeModel.Reached>()
    private val seen = mutableSetOf<TypeId>()
    private val queue = ArrayDeque<Pending>()

    /** Walks from [roots] and returns the resulting model. */
    fun walk(roots: List<Root>): TypeModel {
        roots.forEach { root ->
            // Resolving the root registers whatever it references; the ref itself is discarded
            // because a root is a position, not a declaration.
            resolveRef(root.type, listOf(root.label))
        }

        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            declare(next)
        }

        return TypeModel(
            decls = decls.toMap(),
            usedClaims = usedClaims.toMap(),
            unresolved = unresolved.toList(),
            longValued = longValued.toList(),
        )
    }

    /** Enqueues [id] for declaration unless it is already known. */
    private fun enqueue(id: TypeId, path: List<String>) {
        if (seen.add(id)) {
            queue.addLast(Pending(id, path))
        }
    }

    /**
     * Turns a Kotlin type into a symbolic reference, enqueuing any named type it reaches.
     *
     * Order matters and mirrors Slumber: a claim wins over everything (a custom codec reshapes the
     * JSON, so the declared structure is irrelevant), then primitives, then collections.
     */
    private fun resolveRef(type: KType, path: List<String>): TsTypeRef {
        val cls = type.classifier as? KClass<*>
            ?: return TsTypeRef.TsUnknown

        val nullable = type.isMarkedNullable

        val ref = resolveNonNullRef(type, cls, path)

        return if (nullable) ref.asNullable() else ref
    }

    /**
     * Terminal and container cases, in the SAME ORDER as `BuiltInModule.getSlumberer`.
     *
     * Order is load-bearing there and therefore here: a user value class is resolved before
     * primitives and collections, so a `value class Ids(val v: List<String>)` aliases rather than
     * becoming an array.
     */
    private fun resolveNonNullRef(type: KType, cls: KClass<*>, path: List<String>): TsTypeRef {
        // A claim short-circuits everything below it — a custom codec reshapes the JSON, so the
        // declared structure is irrelevant.
        claims.find(cls)?.let { claim ->
            usedClaims[claim.qualifiedName] = claim
            return if (claim.opaque) TsTypeRef.TsUnknown else TsTypeRef.Named(TypeId.of(type))
        }

        // Nothing / Unit -> NullCodec
        if (cls == Unit::class || cls == Nothing::class) {
            return TsTypeRef.TsVoid
        }

        // A user value class is checked BEFORE primitives and collections, exactly as Slumber does.
        if (cls.isUserValueClass()) {
            val id = TypeId.of(type)
            enqueue(id, path)
            return TsTypeRef.Named(id)
        }

        when (cls) {
            Any::class -> return TsTypeRef.TsUnknown

            String::class, Char::class -> return TsTypeRef.TsString

            Boolean::class -> return TsTypeRef.TsBoolean

            Byte::class, Short::class, Int::class, Float::class, Double::class, Number::class ->
                return TsTypeRef.TsNumber

            Long::class -> {
                // Slumber writes a Long as a JSON number, so JSON.parse already truncates above 2^53.
                // Advisory, not an error: the loss is in the wire format, not in this generator.
                longValued.add(TypeModel.Reached(TypeId.of(type), path))
                return TsTypeRef.TsNumber
            }
        }

        // A Map slumbers to a JSON object; JSON keys are always strings whatever the Kotlin key type.
        if (cls.isMapLike()) {
            val value = type.arguments.getOrNull(1)?.type
            return TsTypeRef.RecordOf(
                value = value?.let { resolveRef(it, path + "*") } ?: TsTypeRef.TsUnknown
            )
        }

        // Any Iterable or array slumbers to a JSON array. A primitive array (IntArray) carries no
        // type argument, so its element type comes from the lookup table instead.
        if (cls.isArrayLike()) {
            val item = cls.primitiveArrayElementType() ?: type.arguments.getOrNull(0)?.type

            return TsTypeRef.ArrayOf(
                item = item?.let { resolveRef(it, path + "*") } ?: TsTypeRef.TsUnknown
            )
        }

        // Anything else becomes a named reference and gets declared.
        val id = TypeId.of(type)
        enqueue(id, path)
        return TsTypeRef.Named(id)
    }

    /** Classifies [pending] and records the resulting declaration (or an unresolved entry). */
    private fun declare(pending: Pending) {
        val id = pending.id
        val cls = id.cls
        val path = pending.path

        // A claimed type is referenced, never declared — its shape comes from the claim.
        if (claims.find(cls) != null) {
            return
        }

        // Mirrors the classification order of `BuiltInModule.getSlumberer`.
        when {
            cls.isUserValueClass() -> decls[id] = declareAlias(id, cls, path)

            cls.isEnumClass() -> decls[id] = declareEnum(id, cls)

            PolymorphicParentUtil.isPolymorphicParent(cls) -> decls[id] = declareUnion(id, cls, path)

            // A Kotlin `object` slumbers to an empty map via ObjectInstanceCodec — so it is an object
            // type with no properties. This branch is what makes the very common
            // `sealed class X { object A : X() }` variant work.
            cls.objectInstance != null -> decls[id] = declareSingleton(id, cls)

            cls.isData -> decls[id] = declareObj(id, cls, path)

            // Slumber hands a no-arg-constructor class to DataClassSlumberer even when it is not a
            // data class, so it must be typed the same way here.
            cls.hasNoArgPrimaryCtor() -> decls[id] = declareObj(id, cls, path)

            else -> unresolved.add(
                TypeModel.Unresolved(
                    id = id,
                    path = path,
                    reason = unresolvedReason(cls),
                )
            )
        }
    }

    /** A Kotlin `object`: `ObjectInstanceCodec` writes an empty map, plus any discriminator. */
    private fun declareSingleton(id: TypeId, cls: KClass<*>): TsTypeDecl.Obj = TsTypeDecl.Obj(
        id = id,
        name = TsNames.of(id),
        props = emptyList(),
        discriminator = discriminatorFor(cls),
    )

    private fun declareEnum(id: TypeId, cls: KClass<*>): TsTypeDecl.EnumDecl = TsTypeDecl.EnumDecl(
        id = id,
        name = TsNames.of(id),
        // EnumCodec serializes via Enum.name, so the constant names are the wire values.
        values = cls.java.enumConstants.filterIsInstance<Enum<*>>().map { it.name },
    )

    /** A value class aliases its single underlying property — `ValueClassSlumberer` writes the bare value. */
    private fun declareAlias(id: TypeId, cls: KClass<*>, path: List<String>): TsTypeDecl.Alias {
        val reified = ReifiedKType(id.type)

        val underlying = reified.ctorFields2Types.firstOrNull()?.second

        return TsTypeDecl.Alias(
            id = id,
            name = TsNames.of(id),
            target = underlying
                ?.let { resolveRef(it, path + "value") }
                ?: TsTypeRef.TsUnknown,
        )
    }

    /** A polymorphic parent becomes a discriminated union over its concrete children. */
    private fun declareUnion(id: TypeId, cls: KClass<*>, path: List<String>): TsTypeDecl.Union {
        val children = PolymorphicParentUtil.getChildren(cls)

        val variants = children.map { child ->
            val childId = TypeId.of(child.createBareType())
            enqueue(childId, path + "<${child.simpleName}>")
            childId
        }

        return TsTypeDecl.Union(
            id = id,
            name = TsNames.of(id),
            discriminatorField = PolymorphicParentUtil.getDiscriminator(cls),
            variants = variants,
        )
    }

    /**
     * A data class becomes an object type.
     *
     * Field selection mirrors `DataClassSlumberer` exactly: constructor properties, plus any property
     * carrying `@Slumber.Field` directly or inherited. The wire name is the property name, unmodified.
     */
    private fun declareObj(id: TypeId, cls: KClass<*>, path: List<String>): TsTypeDecl.Obj {
        val reified = ReifiedKType(id.type)

        val fields = reified.ctorFields2Types
            .plus(
                reified.allPropertiesToTypes.filter { (prop, _) ->
                    prop.hasAnyAnnotationRecursive { it is Slumber.Field } ||
                            prop.hasAnyAnnotationOnPropertyDefinedOnSuperTypes(reified.cls) { it is Slumber.Field }
                }
            )
            .distinctBy { (prop, _) -> prop }

        // A constructor parameter with a default may be omitted by a client sending this type as a
        // request body. Responses always carry every key, so treating it as optional only loosens
        // parsing, never tightens it.
        val optionalNames = reified.ctor?.parameters
            ?.filter { it.isOptional }
            ?.mapNotNull { it.name }
            ?.toSet()
            .orEmpty()

        val props = fields.map { (prop, propType) ->
            TsProp(
                name = prop.name,
                type = resolveRef(propType, path + prop.name),
                optional = prop.name in optionalNames,
            )
        }

        return TsTypeDecl.Obj(
            id = id,
            name = TsNames.of(id),
            props = props,
            discriminator = discriminatorFor(cls),
        )
    }

    /**
     * The discriminator this class writes, or `null` when it is not a polymorphic child.
     *
     * Mirrors `PolymorphicChildUtil.isPolymorphicChild` / `getIdentifier`, so a class picked up as a
     * child by Slumber gets the same literal here — including the `@SerialName`-only case.
     */
    private fun discriminatorFor(cls: KClass<*>): TsDiscriminator? {
        if (!PolymorphicChildUtil.isPolymorphicChild(cls)) {
            return null
        }

        val parent = PolymorphicParentUtil.getParent(cls) ?: cls

        return TsDiscriminator(
            field = PolymorphicParentUtil.getDiscriminator(parent),
            literal = PolymorphicChildUtil.getIdentifier(cls),
        )
    }

    private fun unresolvedReason(cls: KClass<*>): String = when {
        cls.isValue ->
            "kotlin stdlib value class — Slumber deliberately refuses these (their backing-field form " +
                    "would diverge from kotlinx). Claim it with a dedicated TypeScript mapping."

        cls.java.isInterface ->
            "plain interface — not a data class, enum, value class, polymorphic parent or object"

        cls.isAbstract ->
            "abstract class with no polymorphic marker (not sealed, no Polymorphic.Parent companion)"

        else ->
            "not a data class, enum, value class, polymorphic parent, object, or no-arg-constructor class"
    }
}

/** Convenience overload for walking a single root. */
fun TypeWalker.walk(type: KType, label: String): TypeModel = walk(listOf(TypeWalker.Root(type, label)))

/** Strips nullability, for callers that hold a nullable [KType]. */
internal fun KType.nonNull(): KType = withNullability(false)
