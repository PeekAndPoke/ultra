@file:Suppress("detekt:ReturnCount")

package io.peekandpoke.ultra.vault.tools

import io.peekandpoke.ultra.common.startsWithAny
import io.peekandpoke.ultra.log.Log
import io.peekandpoke.ultra.reflection.ReifiedKType
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Database
import io.peekandpoke.ultra.vault.Ref
import io.peekandpoke.ultra.vault.domain.DatabaseGraphModel
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor

/**
 * Builds a [DatabaseGraphModel] from the repositories registered in [database].
 *
 * References are discovered through primary-constructor parameters: those of each stored class,
 * and recursively those of every `data` class reached from them.
 *
 * The graph is computed once per instance and then cached. The builder is registered as `dynamic`
 * in `Ultra_Vault`, so each request gets its own instance and therefore its own snapshot.
 *
 * @param database Supplies the repositories to walk and resolves reference targets to repositories.
 * @param log Receives a trace of every repository, stored class and reference that is visited.
 */
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
                    database.getRepositoryStoringOrNull(refClass).let { repo ->
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

            // LazyRef was merged into Ref, so every Ref is lazy and the Direct/Lazy distinction is
            // obsolete: all references are emitted above as Direct, and Lazy is never constructed.

            // Type arguments first: a Ref inside List<Ref<X>> would otherwise be lost to the
            // package blacklist below, because List itself is blacklisted.
            type.arguments.forEach { typeArg ->
                typeArg.type?.let {
                    visit(it)
                }
            }

            // Blacklisted package? Stop! Arrays have no package and hold nothing to walk into.
            val pkg = cls.java.`package`?.name

            if (pkg == null || pkg.startsWithAny(packageBlackList)) {
                return
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

    /** The graph, built on the first call and cached for the lifetime of this builder. */
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
                connection = repo.connection,
                storedClasses = storedClasses,
            )
        }

        return DatabaseGraphModel(
            repos = repos,
        )
    }
}
