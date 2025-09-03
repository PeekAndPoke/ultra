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
     * Returns true if the plugin vetoes the Mutator generation for the given type.
     */
    fun vetoesType(
        processor: MutatorKspProcessor,
        declaration: KSDeclaration,
    ): Boolean = false

    /**
     * Checks if the plugin can handle the given type.
     */
    fun generatesMutatorFor(
        processor: MutatorKspProcessor,
        declaration: KSDeclaration,
        plugins: MutatorKspPlugins,
    ): Boolean = false

    /**
     * Generates the code for the given type.
     */
    fun generateMutatorFor(
        processor: MutatorKspProcessor,
        ctx: MutatorGeneratorContext,
    ) {
    }

    /**
     * Checks if the plugin can handle the given property.
     */
    fun generatesMutatorFieldFor(
        processor: MutatorKspProcessor,
        property: KSPropertyDeclaration,
        plugins: MutatorKspPlugins,
    ): Boolean = false
}
