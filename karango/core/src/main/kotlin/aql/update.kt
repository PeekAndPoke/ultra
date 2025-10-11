package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.lang.NameExpr
import de.peekandpoke.ultra.vault.lang.Printer
import de.peekandpoke.ultra.vault.lang.PropertyPath
import de.peekandpoke.ultra.vault.lang.Statement

@Suppress("FunctionName")
fun <T : Any> StatementBuilder.UPDATE(
    expr: Expression<T>,
    repo: Repository<T>,
    with: UpdateWith.UpdateWithBuilder<T>.() -> Unit,
): UpdateWith<T> {
    return UpdateWith(
        expr = expr,
        repo = repo,
        assignments = UpdateWith.UpdateWithBuilder(expr).apply(with).build(),
    ).addStmt()
}

class UpdateWith<T : Any>(
    private val expr: Expression<T>,
    private val repo: Repository<T>,
    private val assignments: List<Pair<PropertyPath<*, *>, Expression<*>>>,
) : Statement {

    class UpdateWithBuilder<T>(private val root: Expression<T>) {
        /**
         * The fields included in the index.
         */
        private val _assignments = mutableListOf<Pair<PropertyPath<*, *>, Expression<*>>>()

        fun <X> put(field: Expression<T>.() -> PropertyPath<X, *>, value: Expression<T>.() -> Expression<X>) {
            _assignments.add(
                root.field() to root.value()
            )
        }

        internal fun build() = _assignments.toList()
    }

    override fun print(p: Printer): Any = p.apply {
        append("UPDATE ").append(expr).append(" WITH { ").nl()
        indent {

            val converted = convertAssignments(assignments)

            append(converted)
        }
        append("} IN ").append(repo).nl()
    }

    private fun Printer.append(map: Map<String, Any?>) {

        map.forEach { (name, value) ->
            val nameExpr = NameExpr(name, kType<String>())

            append(nameExpr).append(": ")

            when (value) {
                is Expression<*> -> append(value)
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

    private fun convertAssignments(items: List<Pair<PropertyPath<*, *>, Expression<*>>>): Map<String, Any?> {

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
