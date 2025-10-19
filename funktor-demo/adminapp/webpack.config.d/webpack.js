let devServer = config.devServer

if (devServer) {
    config.mode = "development"
    // config.devtool = false
    config.devtool = "source-map"

    devServer.host = "127.0.0.1"
    devServer.port = 36588
    devServer.open = true
    devServer.hot = false

    // History fallback so that reloads work properly
    devServer.historyApiFallback = true

    // Remove static directories that should not be watched
    // Otherwise we get frontend reloads f.e. when log entries are written
    devServer.static = devServer.static
        .filter(it => it.includes("build"))
}

console.log(config)
