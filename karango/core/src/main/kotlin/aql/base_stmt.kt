package de.peekandpoke.karango.aql

import de.peekandpoke.karango.AqlQueryOptionProvider
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.vault.lang.VaultDslMarker

/**
 * Base interface for all Statements.
 *
 * Statements return nothing. Statements cannot be nested within Expressions.
 */
interface AqlStatement : AqlExpression<Unit?> {
    override fun getType(): TypeRef<Unit?> = TypeRef.UnitNull
}

@VaultDslMarker
interface AqlStatementBuilder {
    val queryOptions: AqlQueryOptionProvider? get() = null

    val stmts: MutableList<AqlStatement>

    fun <T : AqlStatement> T.addStmt(): T = apply { stmts.add(this) }
}

@VaultDslMarker
class AqlStatementBuilderImpl internal constructor() : AqlStatementBuilder {

    private val _optionProviders = mutableListOf<AqlQueryOptionProvider>()

    override val queryOptions
        get(): AqlQueryOptionProvider = {
            _optionProviders.fold(it) { acc, next -> next(acc) }
        }

    override val stmts = mutableListOf<AqlStatement>()

    fun queryOptions(block: AqlQueryOptionProvider) {
        _optionProviders.add(block)
    }
}
