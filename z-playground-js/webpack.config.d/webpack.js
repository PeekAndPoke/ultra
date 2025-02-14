const devServer = config.devServer

// noinspection JSUnresolvedVariable
if (devServer) {

    config.mode = "development"

    // devServer.hot = true
    devServer.port = 11111
}

console.log(config)
