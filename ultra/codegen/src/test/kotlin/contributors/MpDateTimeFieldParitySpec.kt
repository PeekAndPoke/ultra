package io.peekandpoke.ultra.codegen.contributors

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.ultra.common.TypedAttributes
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.datetime.MpLocalDate
import io.peekandpoke.ultra.datetime.MpLocalDateTime
import io.peekandpoke.ultra.datetime.MpLocalTime
import io.peekandpoke.ultra.datetime.MpTimezone
import io.peekandpoke.ultra.datetime.MpZonedDateTime
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.builtin.datetime.mp.MpDateTimeModule
import java.io.File
import java.util.zip.ZipFile
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.typeOf

/**
 * Guards the hand-written `runtime/datetime.ts` against the codecs it mirrors.
 *
 * This test is MANDATORY for any claim, not a nicety. A claimed type is never declared by the walker,
 * so `TsModelValidator`'s codec-parity check does not see it — a wrong claim produces confidently
 * wrong TypeScript and the generator stays silent. Slumbering a real value and comparing is the only
 * available check.
 *
 * It has already earned its place: the first draft of `datetime.ts` had `MpLocalTime` as
 * `{milliSeconds}` (it is a bare number) and omitted `MpTimezone` (a bare string) entirely.
 */
class MpDateTimeFieldParitySpec : FreeSpec() {

    /**
     * Every top-level class name in the `ultra/datetime` artifact.
     *
     * Read from the artifact the tests actually run against, so a type added there shows up here
     * without anyone editing a list.
     */
    private fun datetimeClassNames(): List<String> {
        val source = File(MpInstant::class.java.protectionDomain.codeSource.location.toURI())

        val entries = when {
            source.isDirectory -> source.walkTopDown()
                .filter { it.extension == "class" }
                .map { it.relativeTo(source).path.replace(File.separatorChar, '/') }
                .toList()

            else -> ZipFile(source).use { zip -> zip.entries().toList().map { it.name } }
        }

        return entries
            .filter { it.startsWith("io/peekandpoke/ultra/datetime/") && it.endsWith(".class") }
            .filter { !it.contains('$') }
            .map { it.removeSuffix(".class").replace('/', '.') }
    }

    private val codec = Codec.default

    private val runtimeTs: String by lazy {
        this::class.java.classLoader.getResourceAsStream("ts/runtime/datetime.ts")
            ?.bufferedReader()?.readText()
            ?: error("ts/runtime/datetime.ts is not on the classpath")
    }

    /** Slumbers [value] as [type] and returns its keys, or null when it is not an object. */
    private fun slumberedKeys(type: KType, value: Any?): Set<String>? {
        val map = codec.slumber(type, value) as? Map<*, *> ?: return null

        return map.keys.map { it.toString() }.toSet()
    }

    init {
        "types that slumber to an OBJECT expose exactly {ts, timezone, human}" - {

            val objectShaped: List<Pair<String, Pair<KType, Any>>> = listOf(
                "MpInstant" to (typeOf<MpInstant>() to MpInstant.fromEpochMillis(1_700_000_000_000)),
                "MpLocalDate" to (typeOf<MpLocalDate>() to MpLocalDate.of(2026, 7, 29)),
                "MpLocalDateTime" to (typeOf<MpLocalDateTime>() to MpLocalDateTime.of(2026, 7, 29, 12, 0)),
                "MpZonedDateTime" to (
                        typeOf<MpZonedDateTime>() to
                                MpInstant.fromEpochMillis(1_700_000_000_000).atZone(MpTimezone.UTC)
                        ),
            )

            objectShaped.forEach { (name, pair) ->
                "$name" {
                    val (type, value) = pair

                    val keys = slumberedKeys(type, value)

                    withClue("$name must slumber to a map — datetime.ts declares it as z.object") {
                        keys shouldBe setOf("ts", "timezone", "human")
                    }

                    withClue("datetime.ts must declare $name as an object over those keys") {
                        runtimeTs shouldContain "export const $name = z.object(timestamped)"
                    }
                }
            }
        }

        "MpLocalTime slumbers to a BARE NUMBER, not an object" {
            val slumbered = codec.slumber(typeOf<MpLocalTime>(), MpLocalTime.of(13, 45))

            withClue("if this ever becomes a map, datetime.ts must stop declaring it as z.number()") {
                (slumbered is Number) shouldBe true
            }

            runtimeTs shouldContain "export const MpLocalTime = z.number()"
        }

        "MpTimezone slumbers to a BARE STRING, not an object" {
            val slumbered = codec.slumber(typeOf<MpTimezone>(), MpTimezone.UTC)

            withClue("if this ever becomes a map, datetime.ts must stop declaring it as z.string()") {
                (slumbered is String) shouldBe true
            }

            runtimeTs shouldContain "export const MpTimezone = z.string()"
        }

        "every claimed type is actually present in the runtime module" {
            val declared = Regex("export const (\\w+) =").findAll(runtimeTs).map { it.groupValues[1] }.toList()

            withClue("a claim pointing at a name datetime.ts does not export would break the import") {
                MpDateTimeTsContributor.CLAIMED.values.forEach { tsName ->
                    withClue("'$tsName' must be exported by runtime/datetime.ts") {
                        declared.contains(tsName) shouldBe true
                    }
                }
            }
        }

        "the contributor claims exactly the Mp types MpDateTimeModule has a codec for" {
            // If ultra/datetime gains a type with a codec and nobody claims it, generation fails with
            // an unresolved-type error, and this is where that should be noticed.
            //
            // The previous version of this test compared CLAIMED.keys against a hand-written list of
            // the same six names, with no reference to Slumber at all — so it could not detect the
            // thing it names. Both sides here come from reality: the candidate set is enumerated from
            // the ultra/datetime artifact, and codec-existence is asked of the module itself.
            //
            // Scoped to MpDateTimeModule deliberately: that is the module this contributor mirrors.
            val candidates = datetimeClassNames()
                .filter { it.substringAfterLast('.').startsWith("Mp") }
                .mapNotNull { name -> runCatching { Class.forName(name).kotlin }.getOrNull() }
                .filter { !it.isAbstract && it.objectInstance == null }

            withClue("enumeration must actually find something, or this test is vacuous") {
                candidates.size shouldBeGreaterThan 6
            }

            val withCodec = candidates
                .filter { cls ->
                    runCatching {
                        MpDateTimeModule.getSlumberer(cls.createType(nullable = true), TypedAttributes.empty)
                    }.getOrNull() != null
                }
                .map { it.simpleName }

            withCodec shouldContainExactlyInAnyOrder MpDateTimeTsContributor.CLAIMED.keys.map { it.simpleName }
        }
    }
}
