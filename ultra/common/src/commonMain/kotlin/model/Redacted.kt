package io.peekandpoke.ultra.common.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * A value that can be READ IN but never written out — serializing always yields [PLACEHOLDER].
 *
 * ```kotlin
 * data class JwtConfig(val signingKey: Redacted<String>, val issuer: String)
 * data class DemoConfig(val aws: Redacted<AwsConfig>)   // a whole SUBTREE
 *
 * JwtGenerator(config.signingKey.value)
 * ```
 *
 * **The round trip is broken on purpose.** Deserializing yields the real value; serializing yields the
 * placeholder, so a value that goes out and comes back has been destroyed. That is the point: it is how
 * a signing key can be loaded from configuration and still never reach a log line, an insights record or
 * an HTTP response.
 *
 * Concretely, re-reading this type's own output differs by [T]: a `Redacted<String>` comes back holding
 * [PLACEHOLDER], while a `Redacted<SomeObject>` **throws**, because a string is not that object's shape.
 * Throwing is the better of the two and is pinned by a test — the alternative would be quietly handing
 * back an object whose fields were invented.
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

    override fun hashCode(): Int = value?.hashCode() ?: 0
}

/**
 * Writes [Redacted.PLACEHOLDER]; reads the inner value with its own serializer.
 *
 * The descriptor claims a string because that is what this ever *writes*. Reading delegates to [inner],
 * so a payload carrying the real shape still awakens — the asymmetry is the contract, not a defect.
 */
class RedactedSerializer<T>(private val inner: KSerializer<T>) : KSerializer<Redacted<T>> {

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("io.peekandpoke.ultra.common.model.Redacted", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Redacted<T>) {
        encoder.encodeString(Redacted.PLACEHOLDER)
    }

    override fun deserialize(decoder: Decoder): Redacted<T> = Redacted(inner.deserialize(decoder))
}
