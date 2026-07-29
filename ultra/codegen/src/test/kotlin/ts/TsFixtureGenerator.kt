package io.peekandpoke.ultra.codegen.ts

import io.peekandpoke.ultra.codegen.model.FxEvent
import io.peekandpoke.ultra.codegen.model.FxNode
import io.peekandpoke.ultra.codegen.model.FxResult
import io.peekandpoke.ultra.codegen.model.FxSeatCount
import io.peekandpoke.ultra.codegen.model.FxShape
import io.peekandpoke.ultra.codegen.model.FxSpeaker
import io.peekandpoke.ultra.codegen.model.FxStatus
import io.peekandpoke.ultra.codegen.model.FxTalk
import io.peekandpoke.ultra.codegen.model.FxTalkId
import io.peekandpoke.ultra.codegen.model.TsTypeClaims
import io.peekandpoke.ultra.codegen.model.TsTypeDecl
import io.peekandpoke.ultra.codegen.model.TypeId
import io.peekandpoke.ultra.codegen.model.TypeModel
import io.peekandpoke.ultra.codegen.model.TypeWalker
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
    )

    fun generate(targetDir: File) {
        targetDir.deleteRecursively()
        targetDir.mkdirs()

        val codec = Codec.default

        val entries = fixtures.map { fixture ->
            val model: TypeModel = TypeWalker(TsTypeClaims())
                .walk(listOf(TypeWalker.Root(fixture.root, fixture.name)))

            File(targetDir, "${fixture.name}.ts").writeText(TsModelEmitter(model).emit())

            val slumbered = codec.slumber(fixture.sampleType, fixture.instance)

            File(targetDir, "${fixture.name}.sample.json").writeText(slumbered.toJson())

            val decl = model.decls[TypeId.of(fixture.schemaType)] as? TsTypeDecl.Obj
                ?: error("fixture '${fixture.name}': no object declaration for ${fixture.schemaType}")

            // Required = everything the schema must insist on. A defaulted constructor parameter is
            // emitted `.optional()`, so dropping it is legal and must NOT be a negative case.
            val required = decl.props.filter { !it.optional }.map { it.name } +
                    listOfNotNull(decl.discriminator?.field)

            ManifestEntry(name = fixture.name, schema = decl.name, requiredFields = required)
        }

        File(targetDir, "manifest.json").writeText(entries.toManifestJson())

        println("[ts-fixtures] wrote ${entries.size} fixtures to $targetDir")
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
