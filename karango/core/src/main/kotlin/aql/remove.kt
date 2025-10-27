@file:Suppress("FunctionName")

package de.peekandpoke.karango.aql

import de.peekandpoke.karango.vault.KarangoRepository
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.ensureKey
import de.peekandpoke.ultra.vault.lang.VaultDslMarker
import de.peekandpoke.ultra.vault.lang.VaultTerminalExpressionMarker

@Suppress("unused")
@VaultTerminalExpressionMarker
fun REMOVE(what: String): AqlRemovePreStage<String> = REMOVE(what.aql)

@Suppress("unused")
@VaultTerminalExpressionMarker
fun <E> REMOVE(what: AqlExpression<E>): AqlRemovePreStage<E> = AqlRemovePreStage(what)

@Suppress("unused")
@VaultTerminalExpressionMarker
fun <T> REMOVE(entity: Stored<T>): AqlRemovePreStage<String> = AqlRemovePreStage(entity._id.ensureKey.aql)

@VaultDslMarker
class AqlRemovePreStage<E> internal constructor(private val what: AqlExpression<E>) {

    @VaultTerminalExpressionMarker
    infix fun <T : Any> IN(repo: KarangoRepository<T>): AqlTerminalExpr<T> =
        IN(repo = repo, options = {})

    @VaultTerminalExpressionMarker
    fun <T : Any> IN(repo: KarangoRepository<T>, options: AqlRemoveOptions.() -> Unit): AqlTerminalExpr<T> =
        AqlRemoveIn(repo = repo, expression = what, options = AqlRemoveOptions().apply(options))
}

class AqlRemoveOptions internal constructor() {
    var ignoreErrors: Boolean? = null
    var waitForSync: Boolean? = null
    var ignoreRevs: Boolean? = null

    fun getParams(): List<Pair<String, Boolean>> = listOfNotNull(
        ignoreErrors?.let { "ignoreErrors" to it },
        waitForSync?.let { "waitForSync" to it },
        ignoreRevs?.let { "ignoreRevs" to it },
    )
}

internal class AqlRemoveIn<E, T : Any> internal constructor(
    private val repo: KarangoRepository<T>,
    private val expression: AqlExpression<E>,
    private val options: AqlRemoveOptions,
) : AqlTerminalExpr<T> {

    override fun innerType(): TypeRef<T> = repo.storedType

    override fun getType() = repo.getType()

    override fun print(p: AqlPrinter) {
        p.append("REMOVE ").append(expression).append(" IN ").append(repo)

        options.getParams().takeIf { it.isNotEmpty() }?.let { params ->
            p.append(" OPTIONS { ")
            p.append(params.joinToString(", ") { (name, value) -> "$name: $value" })
            p.append(" } ")
        }

        p.nl()

        p.append("RETURN OLD")
        p.appendLine()
    }
}
