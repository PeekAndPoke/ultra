package io.peekandpoke.ultra.codegen.sdk

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.ultra.codegen.model.TsTypeClaims
import io.peekandpoke.ultra.common.TypedAttributes
import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.SlumberConfig
import io.peekandpoke.ultra.slumber.SlumberModule
import io.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

//  A downstream project's own type, with its own Slumber codec  ////////////////////////////////////

/** Looks like a plain data class, but its codec writes a different shape entirely. */
data class Money(val amount: Double, val currencyCode: String)

data class Invoice(val total: Money, val label: String)

/** Writes `{cents, currency}` — nothing in Money's Kotlin type reveals this. */
private object MoneyCodec : Awaker, Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context): Any? = when (data) {
        is Money -> mapOf("cents" to (data.amount * 100).toLong(), "currency" to data.currencyCode)
        else -> null
    }

    override fun awake(data: Any?, context: Awaker.Context): Any? = when (data) {
        is Map<*, *> -> Money(
            amount = (data["cents"] as Number).toDouble() / 100,
            currencyCode = data["currency"] as String,
        )

        else -> null
    }
}

private object MoneyModule : SlumberModule {
    override fun getAwaker(type: KType, attributes: TypedAttributes): Awaker? =
        MoneyCodec.takeIf { (type.classifier as? KClass<*>) == Money::class }

    override fun getSlumberer(type: KType, attributes: TypedAttributes): Slumberer? =
        MoneyCodec.takeIf { (type.classifier as? KClass<*>) == Money::class }
}

//  The downstream project's contributor  ///////////////////////////////////////////////////////////

private class MoneyTsContributor : TsSdkContributor {
    override val name: String = "acme:money"

    override fun claimTypes(claims: TsTypeClaims.Scope) {
        claims.map<Money>(tsName = "Money", importFrom = "./runtime/money", schema = "Money")
    }

    override fun contribute(roots: TsSdkRoots) {
        roots.root(typeOf<Invoice>(), "InvoiceApi.get")
    }

    override fun emit(context: TsSdkEmitContext) {
        context.out.resource("ts/runtime/money.ts", to = "runtime/money.ts")
    }
}

/**
 * Proves the extension point is genuinely open: a contributor defined entirely outside this module's
 * production code can claim its own type, ship its own runtime and be treated exactly like the
 * built-in datetime contributor.
 *
 * If any of this ever required privileged access, the built-ins would be special cases rather than a
 * mechanism, and downstream projects could not add their own emitters.
 */
class ThirdPartyContributorSpec : FreeSpec() {

    private val config = SlumberConfig.default.prependModules(MoneyModule)

    init {
        "a custom codec WITHOUT a claim fails validation, naming the codec" {
            val silent = object : TsSdkContributor {
                override val name = "roots-only"
                override fun contribute(roots: TsSdkRoots) = roots.root(typeOf<Invoice>(), "InvoiceApi.get")
            }

            val thrown = runCatching {
                TsSdkBuilder(contributors = listOf(silent), slumberConfig = config).build()
            }.exceptionOrNull()

            withClue("this is the whole point — an unclaimed custom shape must not be guessed at") {
                thrown!!.message!! shouldContain "MoneyCodec"
                thrown.message!! shouldContain "claims.map"
            }
        }

        "the same model succeeds once a third-party contributor claims it" {
            val result = TsSdkBuilder(
                contributors = listOf(MoneyTsContributor()),
                slumberConfig = config,
            ).build()

            withClue("the contributor's own runtime must be emitted alongside the models") {
                result.output.entries().map { it.path } shouldContainExactly
                        listOf("models.ts", "runtime/money.ts")
            }

            val models = result.output.entries().first { it.path == "models.ts" }.content

            withClue("the claimed type is imported from the contributor's module, not redeclared") {
                models shouldContain "import { Money } from './runtime/money'"
                models shouldContain "total: Money,"
            }

            withClue("Money must NOT be declared — its shape comes from the claim") {
                models.contains("export const Money = z.object") shouldBe false
            }
        }

        "a third-party contributor is indistinguishable from a built-in one" {
            val result = TsSdkBuilder(
                contributors = listOf(MoneyTsContributor()),
                slumberConfig = config,
            ).build()

            val emitted = result.output.entries().first { it.path == "runtime/money.ts" }

            withClue("resources resolve from the contributor's own classpath, not a privileged location") {
                emitted.content shouldContain "export const Money = z.object({"
                emitted.writtenBy shouldBe "acme:money"
            }
        }
    }
}
