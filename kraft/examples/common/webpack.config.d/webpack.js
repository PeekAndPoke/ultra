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

    devServer.port = 57238
    devServer.open = true
    devServer.hot = false
}

console.error(config)

// throw new Error(JSON.stringify(config))
