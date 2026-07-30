package io.peekandpoke.ultra.codegen.ts

import io.peekandpoke.ultra.codegen.contributors.MpDateTimeTsContributor
import io.peekandpoke.ultra.codegen.model.FxBox
import io.peekandpoke.ultra.codegen.model.FxDated
import io.peekandpoke.ultra.codegen.model.FxEvent
import io.peekandpoke.ultra.codegen.model.FxGAlphaBranch
import io.peekandpoke.ultra.codegen.model.FxGAlphaLeaf
import io.peekandpoke.ultra.codegen.model.FxGHolder
import io.peekandpoke.ultra.codegen.model.FxGRefs
import io.peekandpoke.ultra.codegen.model.FxGenericEdges
import io.peekandpoke.ultra.codegen.model.FxGenericMatrix
import io.peekandpoke.ultra.codegen.model.FxIdHolder
import io.peekandpoke.ultra.codegen.model.FxIds
import io.peekandpoke.ultra.codegen.model.FxMaybe
import io.peekandpoke.ultra.codegen.model.FxNode
import io.peekandpoke.ultra.codegen.model.FxPageOf
import io.peekandpoke.ultra.codegen.model.FxQuoted
import io.peekandpoke.ultra.codegen.model.FxResult
import io.peekandpoke.ultra.codegen.model.FxSeatCount
import io.peekandpoke.ultra.codegen.model.FxShape
import io.peekandpoke.ultra.codegen.model.FxSpeaker
import io.peekandpoke.ultra.codegen.model.FxStatus
import io.peekandpoke.ultra.codegen.model.FxStorable
import io.peekandpoke.ultra.codegen.model.FxTalk
import io.peekandpoke.ultra.codegen.model.FxTalkId
import io.peekandpoke.ultra.codegen.model.FxTreeOf
import io.peekandpoke.ultra.codegen.model.FxTriple
import io.peekandpoke.ultra.codegen.model.FxWrapped
import io.peekandpoke.ultra.codegen.model.TsTypeClaims
import io.peekandpoke.ultra.codegen.model.TsTypeDecl
import io.peekandpoke.ultra.codegen.model.TypeId
import io.peekandpoke.ultra.codegen.model.TypeModel
import io.peekandpoke.ultra.codegen.model.TypeWalker
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.datetime.MpLocalDate
import io.peekandpoke.ultra.datetime.MpTimezone
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.slumber.Codec
import java.io.File
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Writes the inputs the `ts-verify` harness consumes: emitted TypeScript, the JSON Slumber really
 * produces for each fixture, and a manifest tying the two together.
 *
 * Run from Gradle (`:ultra:codegen:generateTsFixtures`), not as a test — the assertions live on the
 * TypeScript side, where `tsc` and `zod` can actually be applied.
 */
object TsFixtureGenerator {

    /** One fixture: a root type plus a real instance of the type to be checked. */
    private data class Fixture(
        val name: String,
        val root: KType,
        val instance: Any?,
        /** The type the sample is slumbered AS, when it differs from the root (polymorphic children). */
        val sampleType: KType = root,
        /** The declaration the schema is taken from, when it differs from the root. */
        val schemaType: KType = root,
    )

    private val fixtures: List<Fixture> = listOf(
        Fixture(
            name = "talk",
            root = typeOf<FxTalk>(),
            instance = FxTalk(
                id = FxTalkId("t-1"),
                title = "Hello",
                status = FxStatus.ACTIVE,
                speakers = listOf(FxSpeaker("Ada", null)),
                tags = setOf("a", "b"),
                meta = mapOf("k" to "v"),
                seats = FxSeatCount(42),
                durationMs = 1234L,
                rating = null,
                featured = true,
            ),
        ),
        Fixture(
            name = "node",
            root = typeOf<FxNode>(),
            instance = FxNode("root", listOf(FxNode("kid", emptyList(), null)), null),
        ),
        Fixture(
            name = "event",
            root = typeOf<FxEvent>(),
            instance = FxEvent.Created("now"),
            schemaType = typeOf<FxEvent.Created>(),
        ),
        Fixture(
            name = "shape",
            root = typeOf<FxShape>(),
            instance = FxShape.Circle(1.5),
            schemaType = typeOf<FxShape.Circle>(),
        ),
        Fixture(
            name = "result",
            root = typeOf<FxResult>(),
            instance = FxResult.Done("done"),
            schemaType = typeOf<FxResult.Done>(),
        ),
        // The generic shape matrix: nesting, nullable arguments, three parameters, a generic value
        // class, a recursive generic and a generic sealed hierarchy. `tsc` is the only thing that can
        // confirm the emitted factories actually compose — in particular that a factory CALL is still
        // accepted as a z.discriminatedUnion option.
        Fixture(
            name = "genericMatrix",
            root = typeOf<FxGenericMatrix>(),
            instance = FxGenericMatrix(
                nullableArg = FxBox(null, "a"),
                nonNullArg = FxBox("v", "b"),
                nullableProp = FxMaybe(null),
                enumArg = FxBox(FxStatus.ACTIVE, "c"),
                valueClassArg = FxBox(FxTalkId("t"), "d"),
                threeParams = FxTriple(FxSpeaker("Ada", null), FxStatus.ARCHIVED, FxTalkId("t2")),
                twoDeep = FxPageOf(listOf(FxBox(FxSpeaker("A", null), "e")), 1),
                threeDeep = FxPageOf(listOf(FxBox(FxPageOf(listOf(FxSpeaker("B", null)), 1), "f")), 1),
                genericInList = listOf(FxBox(FxSpeaker("C", null), "g")),
                genericInSet = setOf(FxBox(FxStatus.ACTIVE, "h")),
                genericInMap = mapOf("k" to FxBox(FxTalkId("t3"), "i")),
                listInsideGeneric = FxBox(listOf(FxSpeaker("D", null)), "j"),
                mapInsideGeneric = FxBox(mapOf("m" to FxSpeaker("E", null)), "k"),
                listOfListInsideGeneric = FxBox(listOf(listOf(FxSpeaker("F", null))), "l"),
                nullableGeneric = null,
                listOfNullableGeneric = listOf(null, FxBox(FxSpeaker("G", null), "m")),
                wrapped = FxWrapped(FxSpeaker("H", null)),
                wrappedScalar = FxWrapped("plain"),
                tree = FxTreeOf(FxSpeaker("I", null), listOf(FxTreeOf(FxSpeaker("J", null), emptyList()))),
                treeOfBoxes = FxTreeOf(FxBox(FxTalkId("t4"), "n"), emptyList()),
                storable = FxStorable.Stored(FxSpeaker("K", null), "id-1"),
                storableOther = FxStorable.New(FxTalkId("t5")),
            ),
        ),
        // Recursive generic UNION and recursive generic ALIAS. Both emitted without `z.lazy` until
        // 2026-07-30: tsc was clean and the first parse blew the stack, so only running it catches them.
        Fixture(
            name = "genericEdges",
            root = typeOf<FxGenericEdges>(),
            instance = FxGenericEdges(
                union = FxGAlphaBranch(listOf(FxGAlphaLeaf(FxSpeaker("A", null)))),
                alias = FxGHolder(FxGRefs(emptyList())),
            ),
        ),
        // A value class ON a cycle. `tsc` is what proves the deferral is needed: emitted eagerly this
        // is TS2448, "block-scoped variable used before its declaration", which no Kotlin assertion
        // about the emitted text can demonstrate.
        Fixture(
            name = "idHolder",
            root = typeOf<FxIdHolder>(),
            instance = FxIdHolder(FxIds(emptyList())),
        ),
        // Emitted TypeScript carrying a quote, a backslash and non-identifier keys. This is the only
        // check that the escaping produces something a real parser accepts: `tsc` compiles the file and
        // `zod` parses the sample against it. A Kotlin assertion can only compare it to a string I also
        // wrote, and I would make the same mistake twice.
        Fixture(
            name = "quoted",
            root = typeOf<FxQuoted>(),
            instance = FxQuoted.Apostrophe(plain = "ok", `it's` = "apostrophe", `dashed-name` = "dashed"),
            schemaType = typeOf<FxQuoted.Apostrophe>(),
        ),
        // The ONLY fixture whose emitted file imports a runtime module rather than just `zod`. Until
        // 2026-07-30 every generated import was extensionless, which `tsc` accepts under
        // `moduleResolution: bundler` and Node refuses with ERR_MODULE_NOT_FOUND — so the emitted SDK
        // type-checked and then failed to load. Nothing caught it because nothing ever asked Node to
        // load a generated file that imports one. `verify.ts` importing this fixture is that ask.
        Fixture(
            name = "dated",
            root = typeOf<FxDated>(),
            instance = FxDated(
                at = MpInstant.parse("2026-07-30T10:15:30Z"),
                day = MpLocalDate.of(2026, 7, 30),
                zone = MpTimezone.of("Europe/Berlin"),
                optional = null,
            ),
        ),
    )

    fun generate(targetDir: File) {
        targetDir.deleteRecursively()
        targetDir.mkdirs()

        val codec = Codec.default

        copyRuntime(targetDir)

        // A real envelope around a real payload. The generic `ApiResponse<T>` is hand-written rather
        // than generated, so this is the only place its schema meets output a server actually produced.
        File(targetDir, "apiResponse.sample.json").writeText(
            codec.slumber(
                typeOf<ApiResponse<FxSpeaker>>(),
                ApiResponse.ok(FxSpeaker("Ada", null)).withWarning("careful"),
            ).toJson()
        )

        // The datetime claims are registered for every fixture, not just the one that reaches them:
        // a claim only takes effect when its type is actually walked to, so this cannot change any
        // other fixture's output.
        val claims = TsTypeClaims().also { MpDateTimeTsContributor().claimTypes(it.scopeFor("fixtures")) }

        val entries = fixtures.map { fixture ->
            val model: TypeModel = TypeWalker(claims)
                .walk(listOf(TypeWalker.Root(fixture.root, fixture.name)))

            File(targetDir, "${fixture.name}.ts").writeText(TsModelEmitter(model).emit())

            val slumbered = codec.slumber(fixture.sampleType, fixture.instance)

            File(targetDir, "${fixture.name}.sample.json").writeText(slumbered.toJson())

            // declFor, not decls[TypeId.of(...)]: a declaration is keyed by its CLASS, so an id built
            // from an instantiation never matches one. Harmless while every fixture root is
            // non-generic, and an immediate error the moment one is not.
            val decl = model.declFor(fixture.schemaType) as? TsTypeDecl.Obj
                ?: error("fixture '${fixture.name}': no object declaration for ${fixture.schemaType}")

            // Required = everything the schema must insist on. A defaulted constructor parameter is
            // emitted `.optional()`, so dropping it is legal and must NOT be a negative case.
            val required = decl.props.filter { !it.optional }.map { it.name } +
                    listOfNotNull(decl.discriminator?.field)

            ManifestEntry(name = fixture.name, schema = decl.name, requiredFields = required)
        }

        generateClient(targetDir, claims)

        File(targetDir, "manifest.json").writeText(entries.toManifestJson())

        println("[ts-fixtures] wrote ${entries.size} fixtures to $targetDir")
    }

    /**
     * Emits a generated API CLIENT, plus the `models.ts` it imports from.
     *
     * This is the only place emitted client code meets a real compiler and a real runtime.
     * `verifyRuntime.ts` constructs it against a stub transport and calls a member, which exercises
     * the whole chain at once: the emitted class shape, its imports resolving, the schema expression
     * it passes, `request`, and the envelope parse. A Kotlin assertion over the emitted string can
     * only compare it to a string written by the same hand that emitted it.
     *
     * The client is built by hand from a [TsClientSpec] rather than by running the funktor REST
     * contributor: that lives in `funktor/codegen`, which depends on this module, so reaching for it
     * here would invert the dependency. The contributor's own tests cover the route walk that
     * produces the spec.
     */
    private fun generateClient(targetDir: File, claims: TsTypeClaims) {
        val model = TypeWalker(claims).walk(
            listOf(
                TypeWalker.Root(typeOf<List<FxSpeaker>>(), "client:listSpeakers"),
                TypeWalker.Root(typeOf<FxTalk>(), "client:getTalk"),
                TypeWalker.Root(typeOf<FxDated?>(), "client:latest"),
                // A CLAIMED type as the payload itself. Claimed types are not exported by models.ts —
                // they are imported into it from the module that owns them — so a client returning one
                // must import it from there instead. Without this root that branch never runs, and the
                // emitter would happily import a name models.ts does not export.
                TypeWalker.Root(typeOf<MpInstant>(), "client:serverTime"),
            )
        )

        File(targetDir, "models.ts").writeText(TsModelEmitter(model).emit())

        val spec = TsClientSpec(
            className = "FxDemoClient",
            fileName = "fxDemoClient.ts",
            doc = "A demo feature.\nSecond line, which must not break out of the comment: */",
            groups = listOf(
                TsClientSpec.Group(
                    className = "FxTalksApi",
                    member = "talks",
                    doc = "Routes of the `fx-talks` group.",
                    endpoints = listOf(
                        // A LIST payload — the case that needs `z.array(...)`, i.e. the one where the
                        // schema expression is not simply the declaration's own name.
                        TsClientSpec.Endpoint(
                            member = "listSpeakers",
                            httpMethod = "GET",
                            pattern = "/api/fx/speakers",
                            responseRef = model.refForRoot("client:listSpeakers"),
                            doc = "List all speakers",
                        ),
                        TsClientSpec.Endpoint(
                            member = "getTalk",
                            httpMethod = "GET",
                            pattern = "/api/fx/talks/{id}",
                            responseRef = model.refForRoot("client:getTalk"),
                            doc = null,
                        ),
                    ),
                ),
                // A second group, so the aggregate really aggregates — and one whose payload reaches
                // CLAIMED types, which are imported from the runtime module rather than from models.ts.
                TsClientSpec.Group(
                    className = "FxStatusApi",
                    member = "status",
                    doc = null,
                    endpoints = listOf(
                        TsClientSpec.Endpoint(
                            member = "latest",
                            httpMethod = "POST",
                            pattern = "/api/fx/status",
                            responseRef = model.refForRoot("client:latest"),
                            doc = "Nullable payload",
                        ),
                        TsClientSpec.Endpoint(
                            member = "serverTime",
                            httpMethod = "GET",
                            pattern = "/api/fx/time",
                            responseRef = model.refForRoot("client:serverTime"),
                            doc = "A claimed type as the payload",
                        ),
                    ),
                ),
            ),
        )

        File(targetDir, spec.fileName).writeText(TsClientEmitter(model).emit(spec))
    }

    /**
     * Copies the hand-written runtime next to the generated fixtures.
     *
     * Without this the runtime is the one part of the SDK no compiler ever looks at: it is a classpath
     * resource, so neither the Kotlin build nor `tsc` sees it. Copying it into the verified directory
     * puts it under `tsc --noEmit` and makes it importable by the node harness.
     */
    private fun copyRuntime(targetDir: File) {
        TsRuntime.Module.entries.forEach { module ->
            val content = TsFixtureGenerator::class.java.classLoader.getResourceAsStream(module.resource)
                ?.bufferedReader()?.readText()
                ?: error("runtime resource '${module.resource}' is not on the classpath")

            File(targetDir, module.path)
                .also { it.parentFile.mkdirs() }
                .writeText(content)
        }
    }

    private data class ManifestEntry(
        val name: String,
        val schema: String,
        val requiredFields: List<String>,
    )

    private fun List<ManifestEntry>.toManifestJson(): String =
        joinToString(",", """{"fixtures":[""", "]}") { entry ->
            """{"name":${entry.name.jsonString()},""" +
                    """"schema":${entry.schema.jsonString()},""" +
                    """"requiredFields":${entry.requiredFields.joinToString(",", "[", "]") { it.jsonString() }}}"""
        }
}

/**
 * Minimal JSON writer.
 *
 * Deliberately dependency-free: pulling a JSON library in here would mean the verification harness
 * shares tooling with the code it verifies.
 */
private fun Any?.toJson(): String = when (this) {
    null -> "null"
    is String -> jsonString()
    is Boolean, is Number -> toString()
    is Char -> toString().jsonString()
    is Map<*, *> -> entries.joinToString(",", "{", "}") { "${it.key.toString().jsonString()}:${it.value.toJson()}" }
    is Iterable<*> -> joinToString(",", "[", "]") { it.toJson() }
    else -> toString().jsonString()
}

private fun String.jsonString(): String = buildString {
    append('"')
    this@jsonString.forEach { ch ->
        when {
            ch == '"' -> append("\\\"")
            ch == '\\' -> append("\\\\")
            ch.code < 0x20 -> append("\\u%04x".format(ch.code))
            else -> append(ch)
        }
    }
    append('"')
}

fun main(args: Array<String>) {
    val target = args.firstOrNull()
        ?: error("usage: TsFixtureGenerator <target-dir>")

    TsFixtureGenerator.generate(File(target))
}
