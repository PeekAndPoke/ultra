package io.peekandpoke.funktor.inspect

import io.peekandpoke.funktor.inspect.cluster.FunktorClusterRoutes
import io.peekandpoke.funktor.inspect.cluster.backgroundjobs.mountFunktorBackgroundJobs
import io.peekandpoke.funktor.inspect.cluster.depot.mountFunktorDepot
import io.peekandpoke.funktor.inspect.cluster.devtools.mountFunktorDevtools
import io.peekandpoke.funktor.inspect.cluster.locks.mountFunktorLocks
import io.peekandpoke.funktor.inspect.cluster.storage.mountFunktorStorage
import io.peekandpoke.funktor.inspect.cluster.vault.mountFunktorVault
import io.peekandpoke.funktor.inspect.cluster.workers.mountFunktorWorkers
import io.peekandpoke.funktor.inspect.introspection.IntrospectionRoutes
import io.peekandpoke.funktor.inspect.introspection.mountFunktorIntrospection
import io.peekandpoke.funktor.inspect.logging.LoggingRoutes
import io.peekandpoke.funktor.inspect.logging.mountFunktorLogging
import io.peekandpoke.kraft.routing.RouterBuilder
import io.peekandpoke.kraft.routing.Static

class FunktorInspectRoutes(val mount: String = "/_/funktor/inspect") {

    val overview = Static(mount)

    val logging = LoggingRoutes("$mount/logging")

    val cluster = FunktorClusterRoutes(mount)

    val app = IntrospectionRoutes("$mount/app")
}

fun RouterBuilder.mountFunktorInspect(ui: FunktorInspectUi) {

    mount(ui.routes.overview) { ui { FunktorInspectOverviewPage() } }

    // Logging
    mountFunktorLogging(ui.logging)

    // Cluster sub-sections (skip cluster overview — we have our own)
    mountFunktorLocks(ui.cluster)
    mountFunktorVault(ui.cluster)
    mountFunktorWorkers(ui.cluster)
    mountFunktorBackgroundJobs(ui.cluster)
    mountFunktorDepot(ui.cluster)
    mountFunktorStorage(ui.cluster)
    mountFunktorDevtools(ui.cluster)

    // App introspection
    mountFunktorIntrospection(ui.introspection)
}
