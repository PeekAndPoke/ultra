package io.peekandpoke.funktor.inspect.introspection

import io.peekandpoke.kraft.routing.RouterBuilder
import io.peekandpoke.kraft.routing.Static

class IntrospectionRoutes(mount: String = "/_/funktor/introspection") {

    val lifecycleHooks = Static("$mount/lifecycle-hooks")

    val config = Static("$mount/config")

    val cliCommands = Static("$mount/cli-commands")

    val fixtures = Static("$mount/fixtures")

    val repairs = Static("$mount/repairs")

    val endpoints = Static("$mount/endpoints")

    val authRealms = Static("$mount/auth-realms")

    val system = Static("$mount/system")
}

fun RouterBuilder.mountFunktorIntrospection(ui: IntrospectionUi) {

    mount(ui.routes.lifecycleHooks) { ui { LifecycleHooksPage() } }

    mount(ui.routes.config) { ui { ConfigInfoPage() } }

    mount(ui.routes.cliCommands) { ui { CliCommandsPage() } }

    mount(ui.routes.fixtures) { ui { FixturesPage() } }

    mount(ui.routes.repairs) { ui { RepairsPage() } }

    mount(ui.routes.endpoints) { ui { EndpointsPage() } }

    mount(ui.routes.authRealms) { ui { AuthRealmsPage() } }

    mount(ui.routes.system) { ui { SystemInfoPage() } }
}
