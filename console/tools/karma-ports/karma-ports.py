#!/usr/bin/env python3
"""Writes <module>/karma.config.d/karma-port.js for every module in ports.txt (ULTRA-00230).

    console/tools/karma-ports/karma-ports.py           write the files
    console/tools/karma-ports/karma-ports.py --check   fail when a file is missing or stale, or a module with
                                                       browser tests has no port

A module has browser tests when its build file has a browser target and it has src/jsTest or src/commonTest.
"""
import os
import re
import sys

BASE = 9931
SLOTS = 3
BLOCK = 33
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", "..", ".."))
SKIP = ("/build/", "/node_modules/", "/.build-claude/", "/buildSrc/", "/.gradle/")

TEMPLATE = """// Written by console/tools/karma-ports/karma-ports.py from ports.txt (ULTRA-00230). Do not edit.
// A fixed Karma port for this module, in the block of this checkout (ULTRA_KARMA_SLOT, 0 to %(last_slot)d): two builds on
// this machine never share a Karma server. A port that is taken fails the run; Karma would move on to the next
// port, which belongs to another module.
;(function () {
    var slot = parseInt(process.env.ULTRA_KARMA_SLOT || '0', 10);

    if (!(slot >= 0 && slot <= %(last_slot)d)) {
        throw new Error('ULTRA_KARMA_SLOT must be 0 to %(last_slot)d, it is: ' + process.env.ULTRA_KARMA_SLOT);
    }

    var port = %(base)d + slot * %(block)d + %(index)d;
    var probe = "require('net').createServer().once('error', function () { process.exit(1); })" +
        ".listen(" + port + ", function () { process.exit(0); });";

    try {
        require('child_process').execFileSync(process.execPath, ['-e', probe]);
    } catch (e) {
        throw new Error('Karma port ' + port + ' of %(module)s is in use: another build on this machine runs these ' +
            'browser tests (or a Karma server was left behind). Wait for it, or set ULTRA_KARMA_SLOT.');
    }

    config.set({port: port});
})();
"""


def table():
    rows = {}
    for line in open(os.path.join(HERE, "ports.txt")):
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        index, module = line.split()
        index = int(index)
        assert 0 <= index < BLOCK, "index out of the block: %s" % line
        assert index not in rows.values() and module not in rows, "twice in ports.txt: %s" % line
        rows[module] = index
    return rows


def modules_with_browser_tests():
    found = []
    for folder, _, files in os.walk(ROOT):
        if any(s in folder + "/" for s in SKIP):
            continue
        for name in files:
            if name not in ("build.gradle.kts", "build.gradle"):
                continue
            text = open(os.path.join(folder, name)).read()
            if not re.search(r"browser\s*(\{|\(\))", text):
                continue
            if os.path.isdir(os.path.join(folder, "src", "jsTest")) or os.path.isdir(os.path.join(folder, "src", "commonTest")):
                rel = os.path.relpath(folder, ROOT)
                if rel != ".":
                    found.append(rel)
    return sorted(found)


def main():
    check = "--check" in sys.argv
    rows = table()
    problems = []

    for module in modules_with_browser_tests():
        if module not in rows:
            problems.append("no port in ports.txt: %s" % module)

    for module, index in sorted(rows.items(), key=lambda r: r[1]):
        path = os.path.join(ROOT, module, "karma.config.d", "karma-port.js")
        want = TEMPLATE % {"base": BASE, "block": BLOCK, "index": index, "module": module, "last_slot": SLOTS - 1}

        if not os.path.isdir(os.path.join(ROOT, module)):
            problems.append("in ports.txt, but no such module: %s" % module)
            continue

        have = open(path).read() if os.path.isfile(path) else None

        if have == want:
            continue
        if check:
            problems.append("%s: %s" % ("missing" if have is None else "stale", os.path.relpath(path, ROOT)))
        else:
            os.makedirs(os.path.dirname(path), exist_ok=True)
            open(path, "w").write(want)
            print("wrote %s (index %d, port %d in slot 0)" % (os.path.relpath(path, ROOT), index, BASE + index))

    for problem in problems:
        print("karma-ports: " + problem, file=sys.stderr)

    sys.exit(1 if problems else 0)


if __name__ == "__main__":
    main()
