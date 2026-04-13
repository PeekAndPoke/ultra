package io.peekandpoke.funktor.core.model

import kotlinx.serialization.json.Json

/** Creates an [AppInfo] from the version.json resource on the classpath. */
fun AppInfo.Companion.default(
    loader: ClassLoader = Thread.currentThread().contextClassLoader,
): AppInfo {
    return AppInfo.of(
        version = AppVersion.default(loader = loader)
    )
}

/** Creates an [AppVersion] from the version.json resource on the classpath. */
fun AppVersion.Companion.default(
    loader: ClassLoader = Thread.currentThread().contextClassLoader,
): AppVersion {
    return fromResource(
        loader = loader,
    )
}

/** Loads an [AppVersion] by parsing a JSON resource file. */
fun AppVersion.Companion.fromResource(
    path: String = "version.json",
    loader: ClassLoader = Thread.currentThread().contextClassLoader,
): AppVersion {
    val resource = loader.getResource(path) ?: return AppVersion()
    val content = resource.readText()

    return Json.decodeFromString(serializer(), content)
}
