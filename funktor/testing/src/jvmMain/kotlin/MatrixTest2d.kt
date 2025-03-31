package de.peekandpoke.funktor.testing

import io.kotest.core.spec.style.scopes.FreeSpecContainerScope

@JvmName("matrixTest1d")
suspend fun <D1> FreeSpecContainerScope.matrixTest1d(builder: MatrixTest1d.Builder<D1>.() -> Unit) {
    de.peekandpoke.funktor.testing.MatrixTest1d.Builder<D1>(context = this)
        .apply { builder() }
        .build()
        .run()
}

@JvmName("matrixTest2d")
suspend fun <D1, D2> FreeSpecContainerScope.matrixTest2d(builder: MatrixTest2d.Builder<D1, D2>.() -> Unit) {
    de.peekandpoke.funktor.testing.MatrixTest2d.Builder<D1, D2>(context = this)
        .apply { builder() }
        .build()
        .run()
}

@JvmName("matrixTest3d")
suspend fun <D1, D2, D3> FreeSpecContainerScope.matrixTest3d(builder: MatrixTest3d.Builder<D1, D2, D3>.() -> Unit) {
    de.peekandpoke.funktor.testing.MatrixTest3d.Builder<D1, D2, D3>(context = this)
        .apply { builder() }
        .build()
        .run()
}

class MatrixTest1d<D1>(
    private val context: FreeSpecContainerScope,
    private val dim1: List<D1>,
    private val cases: List<Case<D1>>,
    private val default: Case<D1>?,
) {
    class Builder<D1>(private val context: FreeSpecContainerScope) {
        private val dim1: MutableList<D1> = mutableListOf()
        private val cases: MutableList<Case<D1>> = mutableListOf()

        private var default: Case<D1>? = null

        internal fun build() = MatrixTest1d(
            context = context,
            dim1 = dim1,
            cases = cases.toList(),
            default = default,
        )

        fun dim1(vararg items: D1) {
            dim1(items.toList())
        }

        fun dim1(items: List<D1>) {
            dim1.addAll(items)
        }

        fun case(
            description: String,
            predicate: (d1: D1) -> Boolean,
            execute: suspend FreeSpecContainerScope.(d1: D1) -> Unit,
        ) {
            cases.add(
                Case(description, predicate, execute)
            )
        }

        fun default(
            description: String,
            execute: suspend FreeSpecContainerScope.(d1: D1) -> Unit,
        ) {
            default = Case(description, { _ -> true }, execute)
        }
    }

    class Case<D1>(
        val description: String,
        val predicate: (d1: D1) -> Boolean,
        val execute: suspend FreeSpecContainerScope.(d1: D1) -> Unit,
    )

    suspend fun run() {

        val flattened: List<Pair<Case<D1>, D1>> =

            dim1.mapNotNull { d1 ->
                (cases.firstOrNull { it.predicate(d1) } ?: default)?.let { case ->
                    Pair(
                        case,
                        d1,
                    )
                }
            }

        val groupedByCase = flattened.groupBy { it.first }

        groupedByCase.forEach { (case, entries) ->

            context.apply {
                case.description - {
                    val innerContext = this

                    entries.forEach { entry ->
                        case.execute(
                            innerContext,
                            entry.second,
                        )
                    }
                }
            }
        }
    }
}

class MatrixTest2d<D1, D2>(
    private val context: FreeSpecContainerScope,
    private val dim1: List<D1>,
    private val dim2: List<D2>,
    private val cases: List<Case<D1, D2>>,
    private val default: Case<D1, D2>?,
) {
    class Builder<D1, D2>(private val context: FreeSpecContainerScope) {
        private val dim1: MutableList<D1> = mutableListOf()
        private val dim2: MutableList<D2> = mutableListOf()
        private val cases: MutableList<Case<D1, D2>> = mutableListOf()

        private var default: Case<D1, D2>? = null

        internal fun build() = MatrixTest2d(
            context = context,
            dim1 = dim1,
            dim2 = dim2,
            cases = cases.toList(),
            default = default,
        )

        fun dim1(vararg items: D1) {
            dim1(items.toList())
        }

        fun dim1(items: List<D1>) {
            dim1.addAll(items)
        }

        fun dim2(vararg items: D2) {
            dim2(items.toList())
        }

        fun dim2(items: List<D2>) {
            dim2.addAll(items)
        }

        fun case(
            description: String,
            predicate: (d1: D1, d2: D2) -> Boolean,
            execute: suspend FreeSpecContainerScope.(d1: D1, d2: D2) -> Unit,
        ) {
            cases.add(
                Case(description, predicate, execute)
            )
        }

        fun default(
            description: String,
            execute: suspend FreeSpecContainerScope.(d1: D1, d2: D2) -> Unit,
        ) {
            default = Case(description, { _, _ -> true }, execute)
        }
    }

    class Case<D1, D2>(
        val description: String,
        val predicate: (d1: D1, d2: D2) -> Boolean,
        val execute: suspend FreeSpecContainerScope.(d1: D1, d2: D2) -> Unit,
    )

    suspend fun run() {

        val flattened: List<Pair<Case<D1, D2>, Pair<D1, D2>>> =

            dim1.flatMap { d1 ->
                dim2.mapNotNull { d2 ->
                    (cases.firstOrNull { it.predicate(d1, d2) } ?: default)?.let { case ->
                        Pair(
                            case,
                            Pair(d1, d2),
                        )
                    }
                }
            }

        val groupedByCase = flattened.groupBy { it.first }

        groupedByCase.forEach { (case, entries) ->

            context.apply {
                case.description - {
                    val innerContext = this

                    entries.forEach { entry ->
                        case.execute(
                            innerContext,
                            entry.second.first,
                            entry.second.second,
                        )
                    }
                }
            }
        }
    }
}

class MatrixTest3d<D1, D2, D3>(
    private val context: FreeSpecContainerScope,
    private val dim1: List<D1>,
    private val dim2: List<D2>,
    private val dim3: List<D3>,
    private val selector: (d1: D1, d2: D2, d3: D3) -> Boolean,
    private val cases: List<Case<D1, D2, D3>>,
    private val default: Case<D1, D2, D3>?,
) {
    class Builder<D1, D2, D3>(private val context: FreeSpecContainerScope) {
        private val dim1: MutableList<D1> = mutableListOf()
        private val dim2: MutableList<D2> = mutableListOf()
        private val dim3: MutableList<D3> = mutableListOf()
        private var selector: (d1: D1, d2: D2, d3: D3) -> Boolean = { _, _, _ -> true }
        private val cases: MutableList<Case<D1, D2, D3>> = mutableListOf()
        private var default: Case<D1, D2, D3>? = null

        internal fun build() = MatrixTest3d(
            context = context,
            dim1 = dim1,
            dim2 = dim2,
            dim3 = dim3,
            selector = selector,
            cases = cases.toList(),
            default = default,
        )

        fun dim1(vararg items: D1) {
            dim1(items.toList())
        }

        fun dim1(items: List<D1>) {
            dim1.addAll(items)
        }

        fun dim2(vararg items: D2) {
            dim2(items.toList())
        }

        fun dim2(items: List<D2>) {
            dim2.addAll(items)
        }

        fun dim3(vararg items: D3) {
            dim3(items.toList())
        }

        fun dim3(items: List<D3>) {
            dim3.addAll(items)
        }

        fun select(predicate: (d1: D1, d2: D2, d3: D3) -> Boolean) {
            selector = predicate
        }

        fun case(
            description: String,
            predicate: (d1: D1, d2: D2, d3: D3) -> Boolean,
            execute: suspend FreeSpecContainerScope.(d1: D1, d2: D2, d3: D3) -> Unit,
        ) {
            cases.add(
                Case(description, predicate, execute)
            )
        }

        fun default(
            description: String,
            execute: suspend FreeSpecContainerScope.(d1: D1, d2: D2, d3: D3) -> Unit,
        ) {
            default = Case(description, { _, _, _ -> true }, execute)
        }
    }

    class Case<D1, D2, D3>(
        val description: String,
        val predicate: (d1: D1, d2: D2, d3: D3) -> Boolean,
        val execute: suspend FreeSpecContainerScope.(d1: D1, d2: D2, d3: D3) -> Unit,
    )

    suspend fun run() {

        val flattened: List<Pair<Case<D1, D2, D3>, Triple<D1, D2, D3>>> =

            dim1.flatMap { d1 ->
                dim2.flatMap { d2 ->
                    dim3.filter { d3 ->
                        selector(d1, d2, d3)
                    }
                        .mapNotNull { d3 ->
                            (cases.firstOrNull { it.predicate(d1, d2, d3) } ?: default)?.let { case ->
                                Pair(
                                    case,
                                    Triple(d1, d2, d3),
                                )
                            }
                        }
                }
            }

        val groupedByCase = flattened.groupBy { it.first }

        groupedByCase.forEach { (case, entries) ->

            context.apply {
                case.description - {
                    val innerContext = this

                    entries.forEach { entry ->
                        case.execute(
                            innerContext,
                            entry.second.first,
                            entry.second.second,
                            entry.second.third,
                        )
                    }
                }
            }
        }
    }
}
