package de.peekandpoke.mutator.ksp

import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class MutatorKspPlugins(
    val plugins: List<MutatorKspPlugin>,
) {
    fun findMutatorGenerator(cls: KSDeclaration): MutatorKspPlugin? {
        return plugins.firstOrNull {
            it.generatesMutatorFor(cls, this)
        }
    }

    fun hasMutatorGenerator(cls: KSDeclaration): Boolean {
        return findMutatorGenerator(cls) != null
    }

    fun findMutatorFieldGenerator(property: KSPropertyDeclaration): MutatorKspPlugin? {
        return plugins.firstOrNull {
            it.generatesMutatorFieldFor(property, this)
        }
    }

    fun hasMutatorFieldGenerator(property: KSPropertyDeclaration): Boolean {
        return findMutatorFieldGenerator(property) != null
    }
}
