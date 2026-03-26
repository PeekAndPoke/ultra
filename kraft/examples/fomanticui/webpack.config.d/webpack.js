let devServer = config.devServer

if (devServer) {
    config.mode = "development"
    // config.devtool = false
    config.devtool = "source-map"

    devServer.host = "127.0.0.1"
    devServer.port = 57203
    devServer.open = true
    devServer.hot = false

    // History fallback so that reloads work properly
    devServer.historyApiFallback = true
}

console.log(config)
