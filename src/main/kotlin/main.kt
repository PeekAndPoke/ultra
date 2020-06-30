import com.soywiz.klock.DateTime
import de.peekandpoke.ultra.slumber.Codec
import kotlin.time.ExperimentalTime

@ExperimentalTime
fun main() {

    val d = DateTime(282828282_000)

    val codec = Codec.default

    val encoded = codec.slumber(d)

    println("Encoded")
    println(encoded)

    val decoded = codec.awake(DateTime::class, encoded)

    println("decoded")
    println(decoded)
}

