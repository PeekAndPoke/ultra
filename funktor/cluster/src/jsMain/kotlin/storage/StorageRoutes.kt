package de.peekandpoke.funktor.cluster.storage

import de.peekandpoke.funktor.cluster.FunktorClusterUi
import de.peekandpoke.kraft.addons.routing.RouterBuilder
import de.peekandpoke.kraft.routing.Route1
import de.peekandpoke.kraft.routing.Static

class StorageRoutes(mount: String) {

    class RandomData(mount: String) {
        val list = Static(mount)

        val view = Route1("$mount/{id}")

        fun view(id: String) = view.build(id)
    }

    class RandomCache(mount: String) {
        val list = Static(mount)

        val view = Route1("$mount/{id}")

        fun view(id: String) = view.build(id)
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
