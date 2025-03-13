package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.lang.Printer
import de.peekandpoke.ultra.vault.lang.Statement
import de.peekandpoke.ultra.vault.lang.VaultDslMarker

@VaultDslMarker
val <T> Expression<T>.ASC: Sort
    get() = Sort(this, Direction.ASC)

@VaultDslMarker
val <T> Expression<T>.DESC: Sort
    get() = Sort(this, Direction.DESC)

@VaultDslMarker
fun <T> Expression<T>.sort(direction: Direction): Sort = Sort(this, direction)

enum class Direction(val op: String) {
    ASC("ASC"),
    DESC("DESC"),
}

data class Sort(
    val expression: Expression<*>,
    val direction: Direction,
)

internal data class SortBy(val sorts: List<Sort>) : Statement {
    override fun print(p: Printer): Printer {
        var printer = p.append("SORT ")
        sorts.forEachIndexed { idx, sort ->
            printer = printer.append(sort.expression).append(" ${sort.direction.op}")
            if (idx < sorts.lastIndex) {
                printer = printer.append(", ")
            }
        }

        return printer.appendLine()
    }
}
