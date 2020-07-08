import de.peekandpoke.common.datetime.PortableDate
import kotlin.time.ExperimentalTime

@ExperimentalTime
fun main() {

    println(PortableDate(1000) < PortableDate(2000))
    println(PortableDate(1000) == PortableDate(1000))
    println(PortableDate(1001) > PortableDate(1000))

}

