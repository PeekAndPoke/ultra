package de.peekandpoke.ultra.mutator.ksp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.Modifier
import de.peekandpoke.ultra.mutator.Mutable
import java.io.OutputStreamWriter

class MutatorKspProcessor(val codeGenerator: CodeGenerator, val logger: KSPLogger) : SymbolProcessor {
    var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {

        val resolvedClasses = resolver.getSymbolsWithAnnotation(Mutable::class.qualifiedName!!, inDepth = true)
            .filterIsInstance<KSClassDeclaration>()
            // Classes need to have a FQN
            .filter { it.qualifiedName != null }
            // Classes need to be data classes
            .filter { it.modifiers.contains(Modifier.DATA) }

        if (invoked) {
            return emptyList()
        }
        invoked = true

        fun KSClassDeclaration.getReferencedClasses(): List<KSClassDeclaration> {
            val typeArgs = typeParameters
                .flatMap { it.bounds }
                .filterIsInstance<KSClassDeclaration>()

            val params = primaryConstructor?.parameters
                ?.map { it.type.resolve().declaration }
                ?.filterIsInstance<KSClassDeclaration>()
                ?: emptyList()

            return typeArgs.plus(params)
        }

        codeGenerator.createNewFile(Dependencies.ALL_FILES, "", "Foo", "kt").use { output ->
            OutputStreamWriter(output).use { writer ->
                writer.write("package com.example\n\n")
                writer.write("class Foo {\n")

                resolvedClasses.forEach {
                    writer.write("    // ${it.qualifiedName?.asString()}")
                    writer.write("\n")
                    writer.write("    // -> ${it.qualifiedName?.getQualifier()} ${it.qualifiedName?.getShortName()}")
                    writer.write("\n")
                }

                writer.write("}\n")
            }
        }
        return emptyList()
    }
}
