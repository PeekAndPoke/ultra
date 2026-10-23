#!/usr/bin/env python3
"""Git hooks: block em/en dashes, raw control bytes and a missing Claude trailer, on added lines only.

  hooks.py install [--force]     write the pre-commit and commit-msg shims into the repo's hooks dir
  hooks.py pre-commit            check the staged files (git runs this)
  hooks.py commit-msg FILE       check the commit message (git runs this)
  hooks.py check [REV]           run the pre-commit checks against REV (default HEAD) without committing

What pre-commit checks, on lines the commit adds (old lines keep what they have until they are edited):
  dashes          em and en dashes in Markdown prose (fenced and inline code skipped)
  kdoc_dashes     em and en dashes inside KDoc (/** ... */) in .kt and .kts files
  control_bytes   C0 control bytes other than tab, newline and carriage return, in any text file

What commit-msg checks:
  message_dashes  em and en dashes in the message (comment lines skipped)
  trailer         the Co-Authored-By trailer, only in commits made from a Claude Code session
                  (CLAUDECODE is set) and never in merge commits Git writes itself

Escapes: a URL never counts, and a line containing `dash-ok` is skipped by the dash checks (for a quoted
title that carries a dash; in Markdown write it as <!-- dash-ok --> so it does not show).

Settings live in hooks.json next to this file (all optional):
  {"checks": {"dashes": true, "kdoc_dashes": true, "control_bytes": true, "message_dashes": true, "trailer": true},
   "dash_skip": "regex on the repo-relative path; matching files are not dash-checked (dated ledgers ...)",
   "trailer": "^Co-Authored-By: Claude"}

The hooks apply to everyone. `--no-verify` is denied for agents by the command baseline.
Standard library only. A first draft to copy and adapt.
"""
import json
import os
import re
import subprocess
import sys
from difflib import SequenceMatcher

HERE = os.path.dirname(os.path.abspath(__file__))
DASHES = {"\u2014": "em dash", "\u2013": "en dash"}  # escaped, so a dash scan of this file finds nothing
CONTROL = re.compile(r"[\x00-\x08\x0b\x0c\x0e-\x1f]")
URL = re.compile(r"\bhttps?://\S+")
INLINE_CODE = re.compile(r"`+[^`]*`+")
EMPTY_TREE = "4b825dc642cb6eb9a060e54bf8d69288fbee4904"
FENCE = re.compile(r"^\s{0,3}(```|~~~)")
TEXT_EXT = {".md", ".kt", ".kts", ".txt", ".json", ".yml", ".yaml", ".py", ".sh", ".ts", ".js", ".mjs",
            ".html", ".css", ".scss", ".xml", ".properties", ".gradle", ".toml", ".csv", ".sql", ".java"}
DEFAULTS = {"checks": {"dashes": True, "kdoc_dashes": True, "control_bytes": True,
                       "message_dashes": True, "trailer": True},
            "dash_skip": None, "trailer": r"^Co-Authored-By: Claude"}


def settings():
    s = json.loads(json.dumps(DEFAULTS))
    path = os.path.join(HERE, "hooks.json")
    if os.path.exists(path):
        with open(path) as f:
            mine = json.load(f)
        s["checks"].update(mine.get("checks", {}))
        s.update({k: v for k, v in mine.items() if k != "checks"})
    return s


def git(*args, check=True):
    r = subprocess.run(["git", *args], capture_output=True)
    if check and r.returncode != 0:
        raise SystemExit(f"hooks: git {' '.join(args)} failed: {r.stderr.decode(errors='replace').strip()}")
    return r


def blob(spec):
    """Bytes of `rev:path` (or `:path` for the index), or None if it does not exist."""
    r = git("show", spec, check=False)
    return r.stdout if r.returncode == 0 else None


def is_text(path, data):
    if os.path.splitext(path)[1].lower() in TEXT_EXT:
        return True  # git calls a text file with a NUL in it binary; we still want to see the NUL
    if b"\0" in data[:8000]:
        return False
    try:
        data.decode("utf-8")
        return True
    except UnicodeDecodeError:
        return False


def added_lines(new, olds):
    """Line numbers (1-based) of `new` that are added relative to every text in `olds`."""
    result = None
    for old in olds:
        sm = SequenceMatcher(None, old or [], new, autojunk=False)  # None: the file is new there
        mine = {j + 1 for tag, _, _, j1, j2 in sm.get_opcodes() if tag in ("replace", "insert")
                for j in range(j1, j2)}
        result = mine if result is None else result & mine
    return result or set()


def strip_escapes(line):
    return "" if "dash-ok" in line else URL.sub("", line)


def md_prose(lines):
    fenced = False
    for n, line in enumerate(lines, 1):
        if FENCE.match(line):
            fenced = not fenced
            continue
        if not fenced:
            yield n, INLINE_CODE.sub("", line)


def kdoc(lines):
    inside = False
    for n, line in enumerate(lines, 1):
        start = line.find("/**") if not inside else 0
        if start < 0:
            continue
        end = line.find("*/", start + 3 if not inside else 0)
        yield n, line[start:end if end >= 0 else len(line)]
        inside = end < 0


def dash_findings(path, numbered, added, what):
    for n, text in numbered:
        if n in added:
            text = strip_escapes(text)
            for ch, name in DASHES.items():
                if ch in text:
                    yield f"{path}:{n}: {name} in {what}"


def changed_paths(*diff_args):
    r = git("diff", "--name-only", "--diff-filter=ACMR", "-z", "--no-renames", *diff_args)
    return [p for p in r.stdout.decode().split("\0") if p]


def bases():
    """The commits a staged line must be new against: HEAD, plus MERGE_HEAD while merging (lines from the
    other branch were checked when they were committed there). [None] before the first commit."""
    if git("rev-parse", "--verify", "-q", "HEAD", check=False).returncode != 0:
        return [None]
    revs = ["HEAD"]
    if git("rev-parse", "--verify", "-q", "MERGE_HEAD", check=False).returncode == 0:
        revs.append("MERGE_HEAD")
    return revs


def findings(s, paths, new_rev, old_revs):
    """Problems on the added lines of `paths`. new_rev is "" for the index, else a commit."""
    c, out = s["checks"], []
    skip = re.compile(s["dash_skip"]) if s.get("dash_skip") else None
    for path in paths:
        data = blob(f"{new_rev}:{path}")
        if data is None or not is_text(path, data):
            continue
        new = data.decode("utf-8", errors="replace").splitlines()
        olds = [blob(f"{rev}:{path}") if rev else None for rev in old_revs]
        added = added_lines(new, [o.decode("utf-8", errors="replace").splitlines() if o is not None else None
                                  for o in olds])
        if c["control_bytes"]:
            out += [f"{path}:{n}: control byte 0x{ord(m.group()):02x}" for n in sorted(added)
                    if (m := CONTROL.search(new[n - 1]))]
        if skip and skip.search(path):
            continue
        if c["dashes"] and path.lower().endswith(".md"):
            out += dash_findings(path, md_prose(new), added, "Markdown")
        if c["kdoc_dashes"] and path.endswith((".kt", ".kts")):
            out += dash_findings(path, kdoc(new), added, "KDoc")
    return out


def report(problems, hook):
    if not problems:
        return 0
    print(f"{hook}: blocked ({len(problems)} problem(s)):", file=sys.stderr)
    for p in problems:
        print(f"  {p}", file=sys.stderr)
    text = " ".join(problems)
    if "dash" in text:
        print("Dashes: use a colon, comma, parentheses or a new sentence. A quoted title that carries one:\n"
              "  put dash-ok on that line (<!-- dash-ok --> in Markdown). Only added lines are checked.",
              file=sys.stderr)
    if "control byte" in text:
        print("Control bytes: remove them (often a NUL or an escape code pasted from a terminal).", file=sys.stderr)
    if "trailer" in text:
        print("Trailer: end the message with the Co-Authored-By line your session's instructions give.",
              file=sys.stderr)
    return 1


def pre_commit():
    revs = bases()
    paths = changed_paths("--cached", revs[0] or EMPTY_TREE)
    return report(findings(settings(), paths, "", revs), "pre-commit")


def check(rev="HEAD"):
    sha = git("rev-parse", rev).stdout.decode().strip()
    parent = f"{sha}^" if git("rev-parse", "--verify", "-q", f"{sha}^", check=False).returncode == 0 else None
    paths = changed_paths(parent or EMPTY_TREE, sha)
    return report(findings(settings(), paths, sha, [parent]), f"check {rev}")


def commit_msg(path):
    s = settings()
    c, out = s["checks"], []
    with open(path, encoding="utf-8", errors="replace") as f:
        text = f.read()
    text = text.split("# ------------------------ >8 ------------------------")[0]  # verbose commits
    lines = [l for l in text.splitlines() if not l.startswith("#")]
    if c["message_dashes"]:
        for n, line in enumerate(lines, 1):
            line = strip_escapes(line)
            for ch, name in DASHES.items():
                if ch in line:
                    out.append(f"message line {n}: {name}")
    merging = git("rev-parse", "--verify", "-q", "MERGE_HEAD", check=False).returncode == 0
    if c["trailer"] and os.environ.get("CLAUDECODE") and not merging:
        if not any(re.search(s["trailer"], l, re.I) for l in lines):
            out.append(f"message: no trailer matching {s['trailer']!r} (commits from a Claude Code session carry it)")
    return report(out, "commit-msg")


SHIM = """#!/bin/sh
# Installed by console/tools/hooks/hooks.py install. Edit the tool, not this file.
exec python3 "$(git rev-parse --show-toplevel)/console/tools/hooks/hooks.py" {hook} "$@"
"""


def install(force):
    hooks_dir = git("rev-parse", "--git-path", "hooks").stdout.decode().strip()
    os.makedirs(hooks_dir, exist_ok=True)
    for hook in ("pre-commit", "commit-msg"):
        dest = os.path.join(hooks_dir, hook)
        body = SHIM.format(hook=hook)
        if os.path.exists(dest) and not force:
            with open(dest) as f:
                if f.read() != body:
                    print(f"hooks: {dest} exists and is not ours; merge it by hand or pass --force", file=sys.stderr)
                    return 1
        with open(dest, "w") as f:
            f.write(body)
        os.chmod(dest, 0o755)
        print(f"installed {dest}")
    return 0


def main(argv):
    if not argv:
        print(__doc__, file=sys.stderr)
        return 2
    cmd, rest = argv[0], argv[1:]
    if cmd == "install":
        return install("--force" in rest)
    if cmd == "pre-commit":
        return pre_commit()
    if cmd == "commit-msg" and rest:
        return commit_msg(rest[0])
    if cmd == "check":
        return check(*rest[:1])
    print(__doc__, file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
