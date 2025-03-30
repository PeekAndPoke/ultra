let devServer = config.devServer

if (devServer) {

    config.mode = "development"

    // config.devtool = false
    config.devtool = "source-map"

    config.stats = {
        errors: true,
        warnings: true,
    };

    config.infrastructureLogging = {
        // Only warnings and errors
        // level: 'none' disable logging
        // Please read https://webpack.js.org/configuration/other-options/#infrastructurelogginglevel
        level: 'warn',
    }

    // config.plugins = []

    devServer.host = "127.0.0.1"
    devServer.port = 57202
    devServer.open = true
    devServer.hot = false
}

console.log(config)
