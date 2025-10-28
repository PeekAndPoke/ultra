package de.peekandpoke.funktor.demo.adminapp

data class AdminAppConfig(
    val orgId: String = "",
    val lang: String = "en",
    val fallbackLang: String = "en",
    val title: String = "Funktor-Demo Admin | Local",
    val environment: String = "dev",
    val wwwBaseUrl: String = "http://www.funktor-demo.localhost:36587",
    val apiBaseUrl: String = "http://api.funktor-demo.localhost:36587",
    val insightsDetailsBaseUrl: String = "http://admin.funktor-demo.localhost:36587/_/insights/details/",
) {
    @Suppress("unused")
    @JsName("withOrgId")
    fun withOrgId(orgId: String) = copy(orgId = orgId)

    @Suppress("unused")
    @JsName("withLang")
    fun withLang(lang: String) = copy(lang = lang)

    @Suppress("unused")
    @JsName("withFallbackLang")
    fun withFallbackLang(fallbackLang: String) = copy(fallbackLang = fallbackLang)

    @Suppress("unused")
    @JsName("withTitle")
    fun withTitle(title: String) = copy(title = title)

    @Suppress("unused")
    @JsName("withEnvironment")
    fun withEnvironment(environment: String) = copy(environment = environment)

    @Suppress("unused")
    @JsName("withWwwBaseUrl")
    fun withWwwBaseUrl(url: String) = copy(wwwBaseUrl = url)

    @Suppress("unused")
    @JsName("withApiBaseUrl")
    fun withApiBaseUrl(url: String) = copy(apiBaseUrl = url)

    @Suppress("unused")
    @JsName("withInsightsDetailsBaseUrl")
    fun withInsightsDetailsBaseUrl(url: String) = copy(insightsDetailsBaseUrl = url)

    // Getters

    @Suppress("unused")
    @JsName("isLive")
    val isLive = environment == "live"

    @Suppress("unused")
    val isNotLive get() = !isLive
}
