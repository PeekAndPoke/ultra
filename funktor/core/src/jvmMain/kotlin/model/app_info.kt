package de.peekandpoke.funktor.core.model

import kotlinx.serialization.json.Json

fun AppInfo.Companion.default(
    loader: ClassLoader = Thread.currentThread().contextClassLoader,
): AppInfo {
    return AppInfo.of(
        version = AppVersion.default(loader = loader)
    )
}

fun AppVersion.Companion.default(
    loader: ClassLoader = Thread.currentThread().contextClassLoader,
): AppVersion {
    return fromResource(
        loader = loader,
    )
}

fun AppVersion.Companion.fromResource(
    path: String = "version.json",
    loader: ClassLoader = Thread.currentThread().contextClassLoader,
): AppVersion {
    val resource = loader.getResource(path) ?: return AppVersion()
    val content = resource.readText()

    return Json.decodeFromString(serializer(), content)
}
