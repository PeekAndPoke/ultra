import de.peekandpoke.ultra.common.datetime.PortableDate

fun main() {
    println(PortableDate(1000) < PortableDate(2000))
    println(PortableDate(1000) == PortableDate(1000))
    println(PortableDate(1001) > PortableDate(1000))
}

