package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.vault.lang.Printer
import de.peekandpoke.ultra.vault.lang.Statement

internal data class OffsetAndLimit(val offset: Int, val limit: Int?) : Statement {

    override fun print(p: Printer) = p.append("LIMIT $offset, $limit").appendLine()
}
