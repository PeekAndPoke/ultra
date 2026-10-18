package io.peekandpoke.ultra.common.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A value that can be READ IN but never written out — serializing always yields [PLACEHOLDER].
 *
 * ```kotlin
 * data class SigningKey(val id: String, val secret: Redacted<String>)
 * data class DemoConfig(val aws: Redacted<AwsConfig>)   // a whole SUBTREE
 *
 * Mac.getInstance("HmacSHA512").init(SecretKeySpec(key.secret.value.toByteArray(), …))
 * ```
 *
 * **The round trip is broken on purpose.** Deserializing yields the real value; serializing yields the
 * placeholder, so a value that goes out cannot come back. That is the point: it is how a signing key can
 * be loaded from configuration and still never reach a log line, an insights record or an HTTP response.
 *
 * ### THE SHARP EDGE — read this before using the type
 *
 * A value that is serialized and then read back is **gone**, and both codecs now say so **loudly**:
 * reading [PLACEHOLDER] throws, on the Slumber side and the kotlinx side alike. Earlier it threw only
 * for a structured [T] and silently produced `Redacted("***redacted***")` for a `Redacted<String>` —
 * a real object holding a publicly known constant where a secret belongs. An application that rebuilt
 * its configuration from an insights record or an `app:config` dump would have booted happily and
 * signed every JWT with a value anyone can read off this file.
 *
 * **The obligation this puts on the caller:** do not put a [Redacted] where something will read it back.
 *
 * - **Configuration — yes.** Loaded once from HOCON, never written back. This is what the type is for.
 * - **A persisted entity — no.** Insert writes the placeholder; the next read throws, and the stored
 *   secret is already lost.
 * - **A request/response DTO — no.** A UI that GETs an object, edits an unrelated field and PUTs it back
 *   sends the placeholder, and the write fails. Split the type instead: return a redacted view, accept
 *   the secret on a dedicated endpoint that only ever receives it.
 *
 * Failing loudly does not make those shapes work — it makes them fail at the first read instead of
 * destroying a credential quietly. Choosing where the type goes is still the caller's job.
 *
 * ### Why a type instead of an annotation
 *
 * This replaces `@JsonIgnore`, which is opt-in and had **silently failed four times** — `JwtConfig`'s
 * signing key, the CSRF secret and two database passwords were all written verbatim into every insights
 * record. An annotation depends on someone remembering it on every new field forever; a type is carried
 * by the compiler and stated where the field is declared.
 *
 * ### Why a plain class and not a `value class`
 *
 * Measured, not assumed: a `@JvmInline value class` is *inlined*, and both Slumber's value-class
 * slumberer and Jackson unwrap straight past any custom codec — the secret came out in the clear while
 * kotlinx honoured the codec. A plain class has a real runtime type for a codec to match on. For the
 * same reason this is not a `data class`: the generated `toString()`, `copy()` and `component1()` would
 * each be a way to leak the value by accident.
 *
 * A second reason, found by mutation: because it is **not** a data class, `DataClassSlumberer` does not
 * claim it either. Remove the Slumber codec and Slumber throws *"There is no known way to slumber the
 * type Redacted"* rather than quietly emitting `{"value": …}`. It fails **closed**.
 *
 * ### It is only as good as its codecs
 *
 * The type alone protects nothing. Each serializer must be taught about it, or it falls through to the
 * generic object machinery and emits `{"value": …}`. kotlinx is handled here; Slumber has
 * `RedactedCodec`. **Adding a serializer to the codebase means adding a codec for this type.**
 */
@Serializable(with = RedactedSerializer::class)
class Redacted<T>(
    /** The real value. Reading it is deliberately unremarkable; keeping it in is the type's job. */
    val value: T,
) {
    companion object {
        /** What every serializer must emit. Fixed, so it cannot be mistaken for content. */
        const val PLACEHOLDER = "***redacted***"
    }

    /** Redacts. Not optional — a hand-written redacting `toString()` on `JwtConfig` was what made the
     * original leak look protected while Jackson serialized the field in full. */
    override fun toString(): String = PLACEHOLDER

    override fun equals(other: Any?): Boolean =
        this === other || (other is Redacted<*> && other.value == value)

    /**
     * Constant — deliberately NOT the inner value's hash.
     *
     * `String.hashCode()` is a cheap, well-known, non-cryptographic digest, so returning it would hand
     * out a 32-bit oracle over the secret that is offline-invertible for anything short or low-entropy.
     * The one reachable sink found in review was `BackgroundJobQueued.calcHash`, whose fallback branch
     * hashes the raw object and persists the result as an admin-readable `dedupeKey`.
     *
     * The equals/hashCode contract still holds — equal values still hash equally — at the cost of
     * collisions if `Redacted` is ever used as a hash key, which nothing does.
     */
    override fun hashCode(): Int = 0
}

/**
 * Writes [Redacted.PLACEHOLDER]; reads the inner value with its own serializer, but REJECTS the
 * placeholder itself.
 *
 * The descriptor claims a string because that is what this ever *writes*. Reading delegates to [inner],
 * so a payload carrying the real shape still awakens — the asymmetry is the contract, not a defect.
 *
 * **Kotlinx-json only.** A format that drives decoding from the outer descriptor — protobuf, cbor,
 * properties — would encode and decode against a `STRING` shape and break on a structured `T`. JSON
 * reads whatever token is present, which is why the mismatch is safe here and nowhere else.
 *
 * Deliberately equivalent to Slumber's `RedactedAwaker`: both reject [Redacted.PLACEHOLDER] on read.
 * A divergence between the two would mean a secret survives one path and is destroyed on the other.
 */
class RedactedSerializer<T>(private val inner: KSerializer<T>) : KSerializer<Redacted<T>> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("io.peekandpoke.ultra.common.model.Redacted", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Redacted<T>) {
        encoder.encodeString(Redacted.PLACEHOLDER)
    }

    override fun deserialize(decoder: Decoder): Redacted<T> {
        val value = inner.deserialize(decoder)

        // See RedactedAwaker for the reasoning. Reading our own output back would hand out a Redacted
        // holding a publicly known constant where a secret belongs.
        if (value == Redacted.PLACEHOLDER) {
            throw SerializationException(
                "The redaction placeholder was read back as a value. The original is gone and must be " +
                        "supplied again — a redacted value cannot be round-tripped."
            )
        }

        return Redacted(value)
    }
}
