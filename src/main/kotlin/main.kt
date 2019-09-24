import de.peekandpoke.ultra.kontainer.kontainer


data class MyService(val conf: Int)

fun main() {

    println("Playground")

    kontainer {

        singleton(MyService::class)

    }.useWith()
}
