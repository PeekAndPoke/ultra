package de.peekandpoke.mutator.ksp

import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class MutatorKspPlugins(
    val processor: MutatorKspProcessor,
    val plugins: List<MutatorKspPlugin>,
) {
    private val vetoCache = mutableMapOf<KSDeclaration, Boolean>()
    private val mutatorGeneratorCache = mutableMapOf<KSDeclaration, MutatorKspPlugin?>()
    private val mutatorFieldGeneratorCache = mutableMapOf<KSPropertyDeclaration, MutatorKspPlugin?>()

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
