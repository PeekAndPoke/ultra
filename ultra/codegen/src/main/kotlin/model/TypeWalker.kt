package io.peekandpoke.ultra.codegen.model

import io.peekandpoke.ultra.reflection.ReifiedKType
import io.peekandpoke.ultra.reflection.hasAnyAnnotationOnPropertyDefinedOnSuperTypes
import io.peekandpoke.ultra.reflection.hasAnyAnnotationRecursive
import io.peekandpoke.ultra.slumber.Slumber
import io.peekandpoke.ultra.slumber.builtin.polymorphism.PolymorphicChildUtil
import io.peekandpoke.ultra.slumber.builtin.polymorphism.PolymorphicParentUtil
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
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
    private val undetermined = mutableListOf<TypeModel.Undetermined>()
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
            undetermined = undetermined.toList(),
        )
    }

    /**
     * A reference to [cls] as declared, carrying [type]'s arguments.
     *
     * The declaration is enqueued under a class-keyed id so that `PageOf<Talk>` and `PageOf<Speaker>`
     * produce ONE declaration; the arguments are resolved here and travel on the reference. Resolving
     * them also keeps them reachable — `PageOf<Talk>` must still discover `Talk`.
     */
    private fun namedRef(type: KType, cls: KClass<*>, path: List<String>): TsTypeRef {
        val id = TypeId.declOf(cls, type)

        enqueue(id, path)

        val args = type.arguments.mapIndexedNotNull { index, arg ->
            arg.type?.let { resolveRef(it, path + "<$index>") }
        }

        return TsTypeRef.Named(id, args)
    }

    /**
     * The class viewed through its OWN type parameters, e.g. `PageOf<T>` rather than `PageOf<Talk>`.
     *
     * Reifying against this leaves every parameter as itself, which is exactly what a generic
     * declaration's body needs — `ReifiedKType` substitutes arguments for parameters, so handing it the
     * parameters as the arguments makes that substitution the identity.
     */
    private fun selfType(cls: KClass<*>): KType = cls.createType(
        arguments = cls.typeParameters.map { KTypeProjection.invariant(it.createType()) },
        nullable = false,
    )

    /**
     * Records a position whose type could not be determined and returns `unknown` for it.
     *
     * Returning rather than throwing keeps the walk going, so one run reports EVERY such position
     * instead of the first — the same reason validation collects problems rather than failing fast.
     */
    private fun undeterminable(path: List<String>, reason: String): TsTypeRef {
        undetermined.add(TypeModel.Undetermined(path = path, reason = reason))

        return TsTypeRef.TsUnknown
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
        // Inside a generic declaration's own body, a parameter stays a parameter: `items: T[]`.
        (type.classifier as? KTypeParameter)?.let { param ->
            val ref: TsTypeRef = TsTypeRef.TypeParam(param.name)

            return if (type.isMarkedNullable) ref.asNullable() else ref
        }

        val cls = type.classifier as? KClass<*>
            ?: return undeterminable(
                path = path,
                reason = "type parameter '$type' was never reified, so its shape is not knowable",
            )

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
            return if (claim.opaque) {
                TsTypeRef.TsUnknown
            } else {
                TsTypeRef.Named(TypeId.declOf(cls, type))
            }
        }

        // Nothing / Unit -> NullCodec
        if (cls == Unit::class || cls == Nothing::class) {
            return TsTypeRef.TsNull
        }

        // A user value class is checked BEFORE primitives and collections, exactly as Slumber does.
        if (cls.isUserValueClass()) {
            return namedRef(type, cls, path)
        }

        when (cls) {
            // Slumber writes whatever the RUNTIME value happens to be, so nothing about the wire
            // shape follows from the declared type. Loud, with `claims.opaque<Any>()` as the opt-in.
            Any::class -> return undeterminable(
                path = path,
                reason = "'Any' carries no static shape — Slumber serializes the runtime value",
            )

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
                value = value?.let { resolveRef(it, path + "*") }
                    ?: undeterminable(
                        path = path + "*",
                        reason = "map value type is not knowable — a star projection, or a subclass " +
                                "that fixes its type arguments",
                    )
            )
        }

        // Any Iterable or array slumbers to a JSON array. A primitive array (IntArray) carries no
        // type argument, so its element type comes from the lookup table instead.
        if (cls.isArrayLike()) {
            val item = cls.primitiveArrayElementType() ?: type.arguments.getOrNull(0)?.type

            return TsTypeRef.ArrayOf(
                item = item?.let { resolveRef(it, path + "*") }
                    ?: undeterminable(
                        path = path + "*",
                        reason = "element type is not knowable — a star projection, or a subclass " +
                                "that fixes its type arguments",
                    )
            )
        }

        // Anything else becomes a named reference and gets declared. The declaration is keyed by the
        // CLASS, so every instantiation shares it; the arguments ride on the reference instead.
        return namedRef(type, cls, path)
    }

    /** Classifies [pending] and records the resulting declaration (or an unresolved entry). */
    private fun declare(pending: Pending) {
        val id = pending.id
        val cls = id.cls
        val path = pending.path

        // A claimed type is referenced, never declared — its shape comes from the claim.
        //
        // Recording it in `usedClaims` is what `resolveNonNullRef` does for every ordinary reference.
        // It matters here because union variants are enqueued directly, so a CLAIMED polymorphic child
        // reaches this path without ever passing through `resolveNonNullRef` — and without the entry
        // its import is never emitted and the reference renders as the literal `unknown`.
        claims.find(cls)?.let { claim ->
            usedClaims[claim.qualifiedName] = claim
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
        val reified = ReifiedKType(selfType(cls))

        val underlying = reified.ctorFields2Types.firstOrNull()?.second

        return TsTypeDecl.Alias(
            id = id,
            name = TsNames.of(id),
            typeParams = cls.typeParameters.map { it.name },
            target = underlying
                ?.let { resolveRef(it, path + "value") }
                ?: undeterminable(
                    path = path + "value",
                    reason = "value class has no readable constructor property to alias",
                ),
        )
    }

    /** A polymorphic parent becomes a discriminated union over its concrete children. */
    private fun declareUnion(id: TypeId, cls: KClass<*>, path: List<String>): TsTypeDecl.Union {
        // The DISCRIMINATOR hops to the root, the CHILDREN do not — and the asymmetry is deliberate.
        //
        // The field name comes from whatever `createParentSlumberer` writes, and that resolves
        // `getParent(cls)` first (`builtin/polymorphism/Polymorphic.kt:122-124`). Skipping the hop made
        // the two halves of one union disagree: variants carried `kind` while the union discriminated
        // on `_type`, which throws at module evaluation.
        //
        // The variant SET is a different question. `createParentSlumberer` builds a wide map because it
        // looks up the runtime class of any value; but a field declared as an intermediate sealed class
        // can only ever hold that class's own subclasses, so widening to the root would emit
        // declarations for types the endpoint cannot return. `createParentAwaker` — the parsing
        // direction, which is what the generated schema performs — keys on `cls` for the same reason.
        val parent = PolymorphicParentUtil.getParent(cls) ?: cls

        val children = PolymorphicParentUtil.getChildren(cls)

        val ownParams = cls.typeParameters.map { it.name }

        val variants = children.map { child ->
            val childId = TypeId.declOf(child, child.createBareType())

            enqueue(childId, path + "<${child.simpleName}>")

            // Pass this parent's parameters down when the child re-declares the same ones, which is
            // the ordinary shape of a generic sealed hierarchy (`Storable<T>` -> `Stored<T>`). This is
            // what stops `Storable<Organisation>` and `Storable<Talk>` collapsing onto one type
            // carrying `unknown`. A child that changes arity is not expressible this way; it lands in
            // `undetermined` rather than being silently bound to Any.
            val args = when (child.typeParameters.size) {
                ownParams.size -> ownParams.map { TsTypeRef.TypeParam(it) }

                else -> {
                    undeterminable(
                        path = path + "<${child.simpleName}>",
                        reason = "variant '${child.simpleName}' declares ${child.typeParameters.size} " +
                                "type parameters but its parent declares ${ownParams.size}, so the " +
                                "parent's arguments cannot be passed down",
                    )
                    emptyList()
                }
            }

            TsTypeRef.Named(childId, args)
        }

        return TsTypeDecl.Union(
            id = id,
            name = TsNames.of(id),
            discriminatorField = PolymorphicParentUtil.getDiscriminator(parent),
            variants = variants,
            typeParams = ownParams,
        )
    }

    /**
     * A data class becomes an object type.
     *
     * Field selection mirrors `DataClassSlumberer` exactly: constructor properties, plus any property
     * carrying `@Slumber.Field` directly or inherited. The wire name is the property name, unmodified.
     */
    private fun declareObj(id: TypeId, cls: KClass<*>, path: List<String>): TsTypeDecl.Obj {
        // Reify against the class's OWN parameters, so the body keeps `T` instead of substituting an
        // instantiation's argument. This is the whole difference between generic emission and
        // monomorphization.
        val reified = ReifiedKType(selfType(cls))

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
            typeParams = cls.typeParameters.map { it.name },
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
