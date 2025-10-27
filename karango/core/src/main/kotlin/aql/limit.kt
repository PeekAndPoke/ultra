package de.peekandpoke.karango.aql

internal data class AqlOffsetAndLimitStmt(val offset: Int, val limit: Int?) : AqlStatement {

    override fun print(p: AqlPrinter) {
        p.append("LIMIT $offset, $limit")
        p.nl()
    }
}
