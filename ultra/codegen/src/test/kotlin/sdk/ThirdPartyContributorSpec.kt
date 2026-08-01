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
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

//  A downstream project's own type, with its own Slumber codec  ////////////////////////////////////

/** Looks like a plain data class, but its codec writes a different shape entirely. */
data class Money(val amount: Double, val currencyCode: String)

data class Invoice(val total: Money, val label: String)

/** Writes `{cents, currency}` — nothing in Money's Kotlin type reveals this. */
internal object MoneyCodec : Awaker, Slumberer {
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

internal object MoneyModule : SlumberModule {
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

//  A contributor whose resources ONLY its own classloader can see  /////////////////////////////////

/** Rooted by [IsolatedResourceContributor]; deliberately plain, so no claim is needed. */
data class IsolatedPayload(val a: String)

/**
 * Loaded through [IsolatedLoader] in the test below, so `this::class.java.classLoader` is that loader
 * rather than the one running the suite. Public and top-level so its bytes can be read back and
 * redefined.
 */
class IsolatedResourceContributor : TsSdkContributor {
    override val name: String = "acme:isolated"

    override fun contribute(roots: TsSdkRoots) {
        roots.root(typeOf<IsolatedPayload>(), "IsolatedApi.get")
    }

    override fun emit(context: TsSdkEmitContext) {
        context.out.resource("ts/runtime/isolated.ts", to = "runtime/isolated.ts")
    }
}

/**
 * Serves resources from [resourceDir] and can redefine a class so it belongs to THIS loader.
 *
 * Both halves are needed for the test to be able to fail. Serving the resource from a directory that
 * is not on the suite's classpath means only this loader can find it; redefining the contributor means
 * `contributor::class.java.classLoader` is this loader rather than the suite's. Without either, the
 * test passes whichever loader `resource()` happens to use — which is exactly the flaw it replaces.
 */
private class IsolatedLoader(private val resourceDir: File) :
    ClassLoader(IsolatedLoader::class.java.classLoader) {

    fun redefine(cls: Class<*>): Class<*> {
        val bytes = parent.getResourceAsStream(cls.name.replace('.', '/') + ".class")!!.readBytes()

        // Supertypes still resolve through the parent, so the redefined class implements the SAME
        // TsSdkContributor interface and stays castable.
        return defineClass(cls.name, bytes, 0, bytes.size)
    }

    override fun getResourceAsStream(name: String): InputStream? =
        File(resourceDir, name).takeIf { it.isFile }?.inputStream() ?: super.getResourceAsStream(name)
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
                        listOf("models.ts", "runtime/money.ts", "index.ts")
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

            withClue("the emitted resource is stamped with its contributor") {
                emitted.content shouldContain "export const Money = z.object({"
                emitted.writtenBy shouldBe "acme:money"
            }
        }

        "a resource is read through the CONTRIBUTOR's classloader, not this module's" {
            // The previous version of this test loaded its fixture from ultra:codegen's own test
            // resources — the very loader the bug used — so it passed either way and could not
            // regression-cover anything. Here the resource exists ONLY on the contributor's loader.
            val dir = Files.createTempDirectory("isolated-contributor").toFile()

            try {
                File(dir, "ts/runtime").mkdirs()
                File(dir, "ts/runtime/isolated.ts").writeText("export const Isolated = 'only-here'\n")

                val loader = IsolatedLoader(dir)

                withClue("precondition: the suite's own loader must NOT see it, or nothing is proven") {
                    javaClass.classLoader.getResourceAsStream("ts/runtime/isolated.ts") shouldBe null
                }

                val contributor = loader.redefine(IsolatedResourceContributor::class.java)
                    .getDeclaredConstructor()
                    .newInstance() as TsSdkContributor

                withClue("the redefined class must really belong to the isolated loader") {
                    contributor::class.java.classLoader shouldBe loader
                }

                val result = TsSdkBuilder.forTesting(listOf(contributor)).build()

                result.output.entries().first { it.path == "runtime/isolated.ts" }
                    .content shouldContain "only-here"
            } finally {
                dir.deleteRecursively()
            }
        }
    }
}
