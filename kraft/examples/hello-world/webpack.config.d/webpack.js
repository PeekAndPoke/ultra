// Content-hash async chunks so CDNs serve the correct version after deploys
config.output.chunkFilename = "[id].[contenthash].js"

// ES2015 target generates .mjs files (strict ESM) where imports must be fully specified.
// This rule relaxes that requirement so dynamic import() calls without extensions keep working.
config.module.rules.push({
    test: /\.m?js$/,
    resolve: {fullySpecified: false},
})

let devServer = config.devServer

if (devServer) {
    config.mode = "development"
    // config.devtool = false
    config.devtool = "source-map"

    devServer.host = "127.0.0.1"
    devServer.port = 57201
    devServer.open = true
    devServer.hot = false

    // History fallback so that reloads work properly
    devServer.historyApiFallback = true
}

console.log(config)
