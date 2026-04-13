package io.peekandpoke.mutator.ksp

import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class MutatorKspPlugins(
    val processor: MutatorKspProcessor,
    val plugins: List<MutatorKspPlugin>,
) {
    private val vetoCache = mutableMapOf<KSDeclaration, Boolean>()
    private val mutatorGeneratorCache = mutableMapOf<KSDeclaration, MutatorKspPlugin?>()
    private val mutatorFieldGeneratorCache = mutableMapOf<KSPropertyDeclaration, MutatorKspPlugin?>()

    private var processingSetNames: Set<String> = emptySet()

    /**
     * Populates the processing set — the definitive set of declarations that will get mutators generated.
     *
     * Must be called after discovery/filtering and before code generation.
     * Clears caches that depend on the processing set.
     *
     * Uses qualified names for reliable lookup — KSP may produce different object instances
     * for the same type depending on the resolution path.
     */
    fun setProcessingSet(declarations: Set<KSDeclaration>) {
        processingSetNames = declarations.mapNotNull { it.qualifiedName?.asString() }.toSet()
        vetoCache.clear()
        mutatorGeneratorCache.clear()
        mutatorFieldGeneratorCache.clear()
    }

    /**
     * Returns true if a mutator WILL be generated for this declaration.
     *
     * This is the definitive answer to "does class X get a mutator?"
     * Plugin authors should use this to decide whether a property should get
     * a mutator accessor or a simple get/set accessor.
     *
     * Respects [io.peekandpoke.mutator.NotMutable], vetoing plugins, and
     * [io.peekandpoke.mutator.Mutable.deep] settings.
     */
    fun isInProcessingSet(declaration: KSDeclaration): Boolean {
        return declaration.qualifiedName?.asString() in processingSetNames
    }

    fun isVetoed(declaration: KSDeclaration): Boolean {
        return vetoCache.getOrPut(declaration) {
            plugins.any {
                it.vetoesType(processor = processor, declaration = declaration)
            }
        }
    }

    fun isNotVetoed(declaration: KSDeclaration): Boolean {
        return !isVetoed(declaration)
    }

    fun findMutatorGenerator(declaration: KSDeclaration): MutatorKspPlugin? {
        return mutatorGeneratorCache.getOrPut(declaration) {
            isNotVetoed(declaration) || return@getOrPut null

            plugins.firstOrNull {
                it.generatesMutatorFor(processor = processor, declaration = declaration, plugins = this)
            }
        }
    }

    fun hasMutatorGenerator(declaration: KSDeclaration): Boolean {
        return findMutatorGenerator(declaration) != null
    }

    fun findMutatorFieldGenerator(property: KSPropertyDeclaration): MutatorKspPlugin? {
        return mutatorFieldGeneratorCache.getOrPut(property) {
            plugins.firstOrNull {
                it.generatesMutatorFieldFor(processor = processor, property = property, plugins = this)
            }
        }
    }

    fun hasMutatorFieldGenerator(property: KSPropertyDeclaration): Boolean {
        return findMutatorFieldGenerator(property) != null
    }
}
