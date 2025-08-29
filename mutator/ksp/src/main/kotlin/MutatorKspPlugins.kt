package de.peekandpoke.mutator.ksp

import com.google.devtools.ksp.symbol.KSDeclaration

class MutatorKspPlugins(
    val plugins: List<MutatorKspPlugin>,
) {
    fun findMutatorGenerator(cls: KSDeclaration): MutatorKspPlugin? {
        return plugins.firstOrNull {
            it.generatesMutatorFor(cls)
        }
    }

    fun hasMutatorGenerator(cls: KSDeclaration): Boolean {
        return findMutatorGenerator(cls) != null
    }
}
