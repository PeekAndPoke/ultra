package io.peekandpoke.karango.aql

val <T> AqlExpression<T>.ASC: AqlSorting
    get() = AqlSorting(this, AqlSortDirection.ASC)

val <T> AqlExpression<T>.DESC: AqlSorting
    get() = AqlSorting(this, AqlSortDirection.DESC)

fun <T> AqlExpression<T>.sort(direction: AqlSortDirection): AqlSorting =
    AqlSorting(expression = this, direction = direction)

enum class AqlSortDirection(val op: String) {
    ASC("ASC"),
    DESC("DESC"),
}

data class AqlSorting(
    val expression: AqlExpression<*>,
    val direction: AqlSortDirection,
)

internal data class AqlSortByStmt(val sorts: List<AqlSorting>) : AqlStatement {
    override fun print(p: AqlPrinter) {
        p.append("SORT ")

        sorts.forEachIndexed { idx, sort ->
            p.append(sort.expression).append(" ${sort.direction.op}")

            if (idx < sorts.lastIndex) p.append(", ")
        }

        p.nl()
    }
}
