package de.peekandpoke.mutator.ksp.builtin

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import de.peekandpoke.mutator.ksp.MutatorKspPlugin
import de.peekandpoke.mutator.ksp.MutatorKspProcessor
import de.peekandpoke.mutator.ksp.isPrimitiveOrString

class BuiltInPlatformTypesVetoingPlugin : MutatorKspPlugin {
    companion object {
        val blackListedPackages = listOf(
            "java.*".toRegex(),
            "javax.*".toRegex(),
            "kotlin.*".toRegex(),
            "kotlinx.*".toRegex(),
        )
    }

    override val name: String = "BuiltIn: Platform Types Vetoing Plugin"

    override fun vetoesType(
        processor: MutatorKspProcessor,
        declaration: KSDeclaration,
    ): Boolean {
        val pkg = declaration.packageName.asString()

        val isNullQualifiedName = declaration.qualifiedName == null
        val isBlacklistedPackage = blackListedPackages.any { it.matches(pkg) }
        val isPrimitiveOrString = declaration.isPrimitiveOrString()
        val isCompanionObject = ((declaration as? KSClassDeclaration)?.isCompanionObject ?: false)

        // Blacklist packages
        val isVetoed = isBlacklistedPackage ||
                // Blacklist all primitive types and String
                isPrimitiveOrString ||
                // Blacklist companion objects
                isCompanionObject ||
                // Blacklist types with no qualified name (e.g. TypeAliases)
                isNullQualifiedName

        if (isVetoed) {
            processor.logger.warn("Type $declaration is vetoed from being generated as a mutator")
        }

        return isVetoed
    }
}
