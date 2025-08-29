package de.peekandpoke.karango.ksp

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

@Suppress("unused")
@AutoService(SymbolProcessorProvider::class)
class KarangoKspProcessorProvider : SymbolProcessorProvider {
    override fun create(
        environment: SymbolProcessorEnvironment,
    ): SymbolProcessor {
        return KarangoKspProcessor(environment)
    }
}
