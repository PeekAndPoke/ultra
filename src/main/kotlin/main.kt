import de.peekandpoke.ultra.kontainer.kontainer


data class MyService(val conf: Int)

data class MyInjectInject(val inject: MyInjecting)

data class MyInjecting(val injected: MyInjected)

data class MyInjected(val conf: Int)

fun main() {

    println("Playground")

    val k = kontainer {

        config("conf", 100)

        singleton(MyService::class)
        singleton<MyInjectInject>()
        singleton<MyInjecting>()

        dynamic<MyInjected>()

    }.useWith(
        MyInjected(123)
    )

    println(k.dump())

}
