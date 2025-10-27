package de.peekandpoke.karango.aql

import de.peekandpoke.karango.aql.AqlPrinter.Companion.printQuery
import de.peekandpoke.karango.vault.KarangoRepository
import de.peekandpoke.ultra.common.reflection.kType

@Suppress("FunctionName")
fun <T : Any> AqlStatementBuilder.UPDATE(
    expr: AqlExpression<T>,
    repo: KarangoRepository<T>,
    with: AqlUpdateWithStmt.UpdateWithBuilder<T>.() -> Unit,
): AqlUpdateWithStmt<T> {
    return AqlUpdateWithStmt(
        expr = expr,
        repo = repo,
        assignments = AqlUpdateWithStmt.UpdateWithBuilder(expr).apply(with).build(),
    ).addStmt()
}

class AqlUpdateWithStmt<T : Any>(
    private val expr: AqlExpression<T>,
    private val repo: KarangoRepository<T>,
    private val assignments: List<Pair<AqlPropertyPath<*, *>, AqlExpression<*>>>,
) : AqlStatement {

    class UpdateWithBuilder<T>(private val root: AqlExpression<T>) {
        /**
         * The fields included in the index.
         */
        private val _assignments = mutableListOf<Pair<AqlPropertyPath<*, *>, AqlExpression<*>>>()

        fun <X> put(
            field: AqlExpression<T>.() -> AqlPropertyPath<X, *>,
            value: AqlExpression<T>.() -> AqlExpression<X>,
        ) {
            _assignments.add(
                root.field() to root.value()
            )
        }

        internal fun build() = _assignments.toList()
    }

    override fun print(p: AqlPrinter) {
        p.append("UPDATE ").append(expr).append(" WITH { ").nl()

        p.indent {
            val converted = convertAssignments(assignments)
            append(converted)
        }

        p.append("} IN ").append(repo)
        p.nl()
    }

    private fun AqlPrinter.append(map: Map<String, Any?>) {

        map.forEach { (name, value) ->
            val nameExpr = AqlNameExpr(name, kType<String>())

            append(nameExpr).append(": ")

            when (value) {
                is AqlExpression<*> -> append(value)
                is Map<*, *> -> {
                    append("{").nl()
                    indent {
                        @Suppress("UNCHECKED_CAST")
                        append(value as Map<String, Any?>)
                    }
                    append("}")
                }

                else -> {
                    /** noop */
                }
            }
            append(",").nl()
        }
    }

    private fun convertAssignments(items: List<Pair<AqlPropertyPath<*, *>, AqlExpression<*>>>): Map<String, Any?> {

        val result = mutableMapOf<String, Any?>()

        items.forEach { (field, value) ->
            field.dropRoot()?.let { fieldPath ->

                val list = fieldPath.getAsList().toMutableList()

                var currentItem = list.removeFirstOrNull()
                var currentMap = result

                while (currentItem != null) {
                    val currentName = currentItem.printQuery().replace("`", "")

                    if (list.isEmpty()) {
                        currentMap[currentName] = value
                    } else {
                        @Suppress("UNCHECKED_CAST")
                        currentMap =
                            currentMap.getOrPut(currentName) { mutableMapOf<String, Any?>() } as MutableMap<String, Any?>
                    }

                    currentItem = list.removeFirstOrNull()
                }
            }
        }

        return result.toMap()
    }
}
