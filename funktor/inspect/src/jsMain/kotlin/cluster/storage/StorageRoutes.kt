package io.peekandpoke.funktor.cluster.storage

import io.peekandpoke.funktor.cluster.FunktorClusterUi
import io.peekandpoke.kraft.routing.Route1
import io.peekandpoke.kraft.routing.RouterBuilder
import io.peekandpoke.kraft.routing.Static

class StorageRoutes(mount: String) {

    class RandomData(mount: String) {
        val list = Static(mount)

        val view = Route1("$mount/{id}")
        fun view(id: String) = view.bind(id)
    }

    class RandomCache(mount: String) {
        val list = Static(mount)

        val view = Route1("$mount/{id}")
        fun view(id: String) = view.bind(id)
    }

    val randomData = RandomData("$mount/random-data")
    val randomCache = RandomCache("$mount/random-cache")
}

internal fun RouterBuilder.mountFunktorStorage(
    ui: FunktorClusterUi,
) {
    mount(ui.routes.storage.randomData.list) { ui { RandomDataStorageListPage() } }
    mount(ui.routes.storage.randomData.view) { ui { RandomDataStorageViewPage(it["id"]) } }

    mount(ui.routes.storage.randomCache.list) { ui { RandomCacheStorageListPage() } }
    mount(ui.routes.storage.randomCache.view) { ui { RandomCacheStorageViewPage(it["id"]) } }
}
