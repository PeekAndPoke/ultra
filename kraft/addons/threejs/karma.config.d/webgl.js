// Enable software WebGL (SwiftShader) in headless Chrome so three.js can create a GL context.
config.customLaunchers = config.customLaunchers || {};
config.customLaunchers.ChromeHeadlessWebGL = {
    base: 'ChromeHeadless',
    flags: [
        '--use-gl=angle',
        '--use-angle=swiftshader',
        '--enable-unsafe-swiftshader',
        '--ignore-gpu-blocklist',
        '--enable-webgl',
    ],
};
config.browsers = ['ChromeHeadlessWebGL'];
