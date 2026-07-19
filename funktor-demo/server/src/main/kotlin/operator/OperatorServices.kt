package io.peekandpoke.funktor.demo.server.operator

class OperatorServices(
    operatorUsersRepo: Lazy<OperatorUsersRepo>,
) {
    val operatorUsersRepo by operatorUsersRepo
}
