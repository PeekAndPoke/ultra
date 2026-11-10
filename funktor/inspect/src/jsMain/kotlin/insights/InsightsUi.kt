package io.peekandpoke.funktor.inspect.insights

import io.peekandpoke.funktor.insights.api.InsightsApiClient
import io.peekandpoke.funktor.insights.api.InsightsRecordRef
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.routing.Route2
import io.peekandpoke.kraft.routing.RouterBuilder
import io.peekandpoke.kraft.routing.Static
import kotlinx.html.Tag

/**
 * The Kraft insights GUI: recorded requests and their details (ULTRA-00198), on par with the Vue one.
 *
 * **Superuser only**, on the server (`InsightsApi`'s auth floor); mounting or hiding these pages protects nothing.
 * The API answers only while the app's insights switch is on, in a development environment (ULTRA-00301).
 */
class InsightsUi(
    val api: InsightsApiClient,
    val routes: InsightsRoutes = InsightsRoutes(),
    /** Records per list page; the server caps it at 200. */
    val epp: Int = 20,
) {
    /** The graphs, fetched once per UI and shared by every record opened. */
    internal val graphs = InsightsGraphSource(api)

    operator fun invoke(block: InsightsUi.() -> Unit) {
        this.block()
    }

    @Suppress("FunctionName")
    fun Tag.InsightsListPage() = comp(InsightsListPage.Props(ui = this@InsightsUi)) { InsightsListPage(it) }

    @Suppress("FunctionName")
    fun Tag.InsightsDetailPage(bucket: String, file: String) =
        comp(InsightsDetailPage.Props(ui = this@InsightsUi, ref = InsightsRecordRef(bucket = bucket, file = file))) {
            InsightsDetailPage(it)
        }
}

class InsightsRoutes(mount: String = "/_/funktor/insights") {
    val list = Static(mount)
    val detail = Route2("$mount/{bucket}/{file}")
    fun detail(ref: InsightsRecordRef) = detail(ref.bucket, ref.file)
}

fun RouterBuilder.mountFunktorInsights(ui: InsightsUi) {
    mount(ui.routes.list) { ui { InsightsListPage() } }
    mount(ui.routes.detail) { ui { InsightsDetailPage(bucket = it["bucket"], file = it["file"]) } }
}
