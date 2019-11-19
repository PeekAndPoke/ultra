package de.peekandpoke.ultra.mutator.meta

import com.squareup.kotlinpoet.ParameterizedTypeName
import de.peekandpoke.ultra.meta.model.MType

/**
 * Helper for rendering code for the given [type]
 */
data class RenderHelper(val type: MType) {

    /**
     * Generic type params, like: "<P1>" or "<P1, P2>"
     */
    val generics: String = when (val typeName = type.typeName) {

        is ParameterizedTypeName ->
            "<" + typeName.typeArguments.mapIndexed { idx, _ -> "P${idx}" }.joinToString(", ") + "> "

        else -> ""
    }

    /**
     * Receiver name for extension properties like
     *
     * MtType.mutate()
     *
     * or
     *
     * MyGeneric<String>.mutate()
     */
    val receiverName: String = "${type.nestedName}$generics"

    /**
     * Mutator class name (including generic parameters, when the type is generic) like
     *
     * MyTypeMutator
     *
     * or
     *
     * MuGenericMutator<P1, P2>
     *
     * @see generics
     */
    val mutatorClassName: String = "${type.nestedFileName}Mutator$generics"

    /**
     * Return a fully parameterized Mutator class name e.g. MyTypeMutator<String>
     */
    fun mutatorClass(typeParams: ParameterizedTypeName): String {
        return "${type.nestedName}Mutator<${typeParams.typeArguments.joinToString(", ")}>"
    }
}
