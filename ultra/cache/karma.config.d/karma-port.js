// Written by console/tools/karma-ports/karma-ports.py from ports.txt (ULTRA-00230). Do not edit.
// A fixed Karma port for this module, in the block of this checkout (ULTRA_KARMA_SLOT, 0 to 2): two builds on
// this machine never share a Karma server. A port that is taken fails the run; Karma would move on to the next
// port, which belongs to another module.
;(function () {
    var slot = parseInt(process.env.ULTRA_KARMA_SLOT || '0', 10);

    if (!(slot >= 0 && slot <= 2)) {
        throw new Error('ULTRA_KARMA_SLOT must be 0 to 2, it is: ' + process.env.ULTRA_KARMA_SLOT);
    }

    var port = 9931 + slot * 33 + 2;
    var probe = "require('net').createServer().once('error', function () { process.exit(1); })" +
        ".listen(" + port + ", function () { process.exit(0); });";

    try {
        require('child_process').execFileSync(process.execPath, ['-e', probe]);
    } catch (e) {
        throw new Error('Karma port ' + port + ' of ultra/cache is in use: another build on this machine runs these ' +
            'browser tests (or a Karma server was left behind). Wait for it, or set ULTRA_KARMA_SLOT.');
    }

    config.set({port: port});
})();
