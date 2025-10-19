package io.peekandpoke.funktor.demo.server.admin

class AdminUserServices(
    adminUsersRepo: Lazy<AdminUsersRepo>,
) {
    val adminUsersRepo by adminUsersRepo
}
