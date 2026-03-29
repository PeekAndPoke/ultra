package io.peekandpoke.funktor.inspect.introspection

import io.peekandpoke.funktor.inspect.introspection.api.IntrospectionApiClient
import io.peekandpoke.kraft.components.comp
import kotlinx.html.Tag

class IntrospectionUi(
    val api: IntrospectionApiClient,
    val routes: IntrospectionRoutes = IntrospectionRoutes(),
) {
    /**
     * Small helper to get [IntrospectionUi] as this pointer into the scope
     */
    operator fun invoke(block: IntrospectionUi.() -> Unit) {
        this.block()
    }

    // // Overview /////////////////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.IntrospectionOverviewPage() = comp(
        IntrospectionOverviewPage.Props(ui = this@IntrospectionUi)
    ) {
        IntrospectionOverviewPage(it)
    }

    // // Lifecycle Hooks ///////////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.LifecycleHooksPage() = comp(
        LifecycleHooksPage.Props(ui = this@IntrospectionUi)
    ) {
        LifecycleHooksPage(it)
    }

    // // Config Info ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.ConfigInfoPage() = comp(
        ConfigInfoPage.Props(ui = this@IntrospectionUi)
    ) {
        ConfigInfoPage(it)
    }

    // // CLI Commands //////////////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.CliCommandsPage() = comp(
        CliCommandsPage.Props(ui = this@IntrospectionUi)
    ) {
        CliCommandsPage(it)
    }

    // // Fixtures /////////////////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.FixturesPage() = comp(
        FixturesPage.Props(ui = this@IntrospectionUi)
    ) {
        FixturesPage(it)
    }

    // // Repairs //////////////////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.RepairsPage() = comp(
        RepairsPage.Props(ui = this@IntrospectionUi)
    ) {
        RepairsPage(it)
    }

    // // Endpoints ////////////////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.EndpointsPage() = comp(
        EndpointsPage.Props(ui = this@IntrospectionUi)
    ) {
        EndpointsPage(it)
    }

    // // Auth Realms ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.AuthRealmsPage() = comp(
        AuthRealmsPage.Props(ui = this@IntrospectionUi)
    ) {
        AuthRealmsPage(it)
    }

    // // System Info ///////////////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("FunctionName")
    fun Tag.SystemInfoPage() = comp(
        SystemInfoPage.Props(ui = this@IntrospectionUi)
    ) {
        SystemInfoPage(it)
    }
}
