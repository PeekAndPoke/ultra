@file:Suppress("detekt:ReturnCount")

package de.peekandpoke.ultra.vault.tools

import de.peekandpoke.ultra.common.reflection.ReifiedKType
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.common.startsWithAny
import de.peekandpoke.ultra.log.Log
import de.peekandpoke.ultra.vault.Database
import de.peekandpoke.ultra.vault.LazyRef
import de.peekandpoke.ultra.vault.Ref
import de.peekandpoke.ultra.vault.domain.DatabaseGraphModel
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor

class DatabaseGraphBuilder(
    private val database: Database,
    private val log: Log,
) {
    private inner class RefFinder(type: ReifiedKType) {

        private val packageBlackList = listOf(
            // exclude java std lib
            "java.",
            // exclude javax std lib
            "javax.",
            // exclude javafx
            "javafx.",
            // exclude kotlin std lib
            "kotlin.",
            // exclude google guava
            "com.google.common."
        )

        private val visitedTypes = mutableSetOf<KType>()

        private val references = mutableListOf<DatabaseGraphModel.Reference>()

        init {
            type.ctorParams2Types.forEach { (_, type) ->
                visit(type)
            }
        }

        fun getReferencesTypes(): List<DatabaseGraphModel.Reference> {
            return references.toList()
        }

        private fun visit(type: KType) {

            if (visitedTypes.contains(type)) {
                return
            }

            visitedTypes.add(type)

            // No class? Stop!
            val cls = type.classifier as? KClass<*> ?: return

            // Primitive type? Stop!
            if (cls.java.isPrimitive || cls == String::class) {
                return
            }

            // Is this a direct ref?
            if (type.classifier == Ref::class) {
                (type.arguments[0].type?.classifier as? KClass<*>)?.let { refClass ->
                    // TODO: what if there is not repo ?
                    database.getRepositoryStoringOrNull(refClass.java).let { repo ->
                        references.add(
                            DatabaseGraphModel.Reference(
                                repo = repo?.let {
                                    DatabaseGraphModel.Repo.Id(name = it.name, connection = it.connection)
                                },
                                fqn = refClass.java.name,
                                type = DatabaseGraphModel.Reference.Type.Direct,
                            )
                        )
                    }
                }
                return
            }

            // Is this a lazy ref?
            if (type.classifier == LazyRef::class) {
                (type.arguments[0].type?.classifier as? KClass<*>)?.let { refClass ->
                    // TODO: what if there is not repo ?
                    database.getRepositoryStoringOrNull(refClass.java).let { repo ->
                        references.add(
                            DatabaseGraphModel.Reference(
                                repo = repo?.let {
                                    DatabaseGraphModel.Repo.Id(name = it.name, connection = it.connection)
                                },
                                fqn = refClass.java.name,
                                type = DatabaseGraphModel.Reference.Type.Lazy,
                            )
                        )
                    }
                }
                return
            }

            // Blacklisted package? Stop!
            if (cls.java.`package`.name.startsWithAny(packageBlackList)) {
                return
            }

            // Look at all the type arguments
            type.arguments.forEach { typeArg ->
                typeArg.type?.let {
                    visit(it)
                }
            }

            // Look into data classes
            if (cls.isData) {
                cls.primaryConstructor?.let { ctor ->
                    ctor.parameters.forEach { param ->
                        visit(param.type)
                    }
                }
                return
            }
        }
    }

    private val model: DatabaseGraphModel by lazy {
        buildInternal()
    }

    fun getGraph(): DatabaseGraphModel = model

    private fun buildInternal(): DatabaseGraphModel {

        val repos = database.getRepositories().map { repo ->

            log.trace("===============================================")
            log.trace("REPO: ${repo.name}")

            val storedClasses = repo.getAllStoredClasses().map { cls ->

                val type = cls.kType().reified

                log.trace("  -----------------------------------------------")
                log.trace("  Class: ${cls.java.name}")

                type.ctorParams2Types.forEach { (param, type) ->
                    log.trace("    ${param.name}: $type")
                }

                val references = RefFinder(type).getReferencesTypes()

                log.trace("  => REFS:")
                references.forEach { ref -> log.trace("    $ref") }

                DatabaseGraphModel.StoredClass(
                    fqn = cls.java.name,
                    references = references,
                )
            }

            DatabaseGraphModel.Repo(
                id = DatabaseGraphModel.Repo.Id(name = repo.name, connection = repo.connection),
                connection = "",
                storedClasses = storedClasses,
            )
        }

        return DatabaseGraphModel(
            repos = repos,
        )
    }
}
