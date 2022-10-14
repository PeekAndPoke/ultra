package de.peekandpoke.ultra.meta.indexer

import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

annotation class IndexSubclasses()

class Indexer {
}

class FirstLevelProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        environment.logger.info("FirstLevelProcessor called!")

        val items = resolver.getSymbolsWithAnnotation(IndexSubclasses::class.qualifiedName!!)

        items.forEach {

            val isClass = it is KSClassDeclaration

            environment.logger.info(">>>>>> Found annotated $it $isClass")
        }

        return items.toList()
    }
}


class FirstLevelProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {

        environment.logger.info("FirstLevelProcessorProvider called!")

        return FirstLevelProcessor(environment)
    }
}
