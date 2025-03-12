@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.ensureKey
import de.peekandpoke.ultra.vault.lang.Expression
import de.peekandpoke.ultra.vault.lang.Printer
import de.peekandpoke.ultra.vault.lang.TerminalExpr
import de.peekandpoke.ultra.vault.lang.VaultDslMarker
import de.peekandpoke.ultra.vault.lang.VaultTerminalExpressionMarker

@Suppress("unused")
@VaultTerminalExpressionMarker
fun REMOVE(what: String) = REMOVE(what.aql)

@Suppress("unused")
@VaultTerminalExpressionMarker
fun <E> REMOVE(what: Expression<E>) = RemovePreStage(what)

@Suppress("unused")
@VaultTerminalExpressionMarker
fun <T> REMOVE(entity: Stored<T>) = RemovePreStage(entity._id.ensureKey.aql)

@VaultDslMarker
class RemovePreStage<E> internal constructor(private val what: Expression<E>) {

    @VaultTerminalExpressionMarker
    infix fun <T : Any> IN(repo: Repository<T>): TerminalExpr<T> = IN(repo = repo, options = {})

    @VaultTerminalExpressionMarker
    fun <T : Any> IN(repo: Repository<T>, options: RemoveOptions.() -> Unit): TerminalExpr<T> =
        RemoveIn(repo = repo, expression = what, options = RemoveOptions().apply(options))
}

class RemoveOptions internal constructor() {
    var ignoreErrors: Boolean? = null
    var waitForSync: Boolean? = null
    var ignoreRevs: Boolean? = null

    fun getParams(): List<Pair<String, Boolean>> = listOfNotNull(
        ignoreErrors?.let { "ignoreErrors" to it },
        waitForSync?.let { "waitForSync" to it },
        ignoreRevs?.let { "ignoreRevs" to it },
    )
}

internal class RemoveIn<E, T : Any> internal constructor(
    private val repo: Repository<T>,
    private val expression: Expression<E>,
    private val options: RemoveOptions,
) : TerminalExpr<T> {


    override fun innerType(): TypeRef<T> = repo.storedType

    override fun getType() = repo.getType()

    override fun print(p: Printer) = with(p) {
        append("REMOVE ").append(expression).append(" IN ").append(repo)

        options.getParams().takeIf { it.isNotEmpty() }?.let { params ->
            append(" OPTIONS { ")
            append(params.joinToString(", ") { (name, value) -> "$name: $value" })
            append(" } ")
        }

        appendLine()
        append("RETURN OLD").appendLine()
    }
}
