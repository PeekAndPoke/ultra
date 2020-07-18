const devServer = config.devServer

// noinspection JSUnresolvedVariable
if (devServer) {

    config.mode = "development"

    // devServer.hot = true
    devServer.port = 11111
    devServer.overlay = true
    devServer.watchOptions = {
        aggregateTimeout: 100,
        poll: 100
    };
    devServer.stats = {
        warnings: true
    };
    // config.devServer.clientLogLevel = 'error';

    // config.devtool = "eval-cheap-source-map"
}

console.log(config)
