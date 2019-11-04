import java.time.LocalDate
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType


data class MyService(val conf: Int)

data class MyInjectInject(val inject: MyInjecting)

data class MyInjecting(val injected: MyInjected)

data class MyInjected(val conf: Int)

fun main() {

    val type = List::class.createType(
        listOf(
            KTypeProjection.invariant(LocalDate::class.createType())
        )
    )

    println(type)

}
