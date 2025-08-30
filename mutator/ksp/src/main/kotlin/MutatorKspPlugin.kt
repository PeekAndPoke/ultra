package de.peekandpoke.mutator.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

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
    fun generatesMutatorFor(declaration: KSDeclaration, plugins: MutatorKspPlugins): Boolean

    /**
     * Generates the code for the given type.
     */
    fun generateMutatorFor(ctx: MutatorGeneratorContext)

    /**
     * Checks if the plugin can handle the given property.
     */
    fun generatesMutatorFieldFor(property: KSPropertyDeclaration, plugins: MutatorKspPlugins): Boolean
}
