import de.peekandpoke.common.datetime.PortableDate
import de.peekandpoke.ultra.slumber.Codec
import kotlin.time.ExperimentalTime

@ExperimentalTime
fun main() {

    val d = PortableDate(282828282_000)

    val codec = Codec.default

    val encoded = codec.slumber(d)

    println("Encoded")
    println(encoded)

    val decoded = codec.awake(PortableDate::class, encoded)

    println("decoded")
    println(decoded)
}

