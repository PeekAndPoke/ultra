package de.peekandpoke.mutator.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration

interface MutatorKspPlugin {

    class MutatorGeneratorContext(
        val codeBlocks: MutatorCodeBlocks,
        val cls: KSClassDeclaration,
        val plugins: MutatorKspPlugins,
    )

    /**
     * The name of the plugin.
     */
    val name: String

    /**
     * Checks if the plugin can handle the given type.
     */
    fun generatesMutatorFor(type: KSDeclaration): Boolean

    /**
     * Generates the code for the given type.
     */
    fun generateForClass(ctx: MutatorGeneratorContext)
}
