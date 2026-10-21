#!/usr/bin/env python3
"""Ticket numbers for task files: take a number, keep each file in the folder its status says, and
migrate a repo from file-name references to ticket numbers.

  ticket.py init PREFIX                    create docs/tasks/_next-ticket.txt ("next ticket: PREFIX-00001")
  ticket.py new "Title" [--status WORD] [--part-of ID]
                                           take the next number, create the file in its folder (default IDEA)
  ticket.py move ID WORD [--note TEXT]     set the status line, move the file to that status's folder
  ticket.py find ID|OLD-NAME               print the path (an old file name or code works after a migration)
  ticket.py list [--status WORD] [--epic ID [--deep]] [--json]
                                           ID, status, path, title (--epic: that epic's tickets and counts;
                                           --json: id, title, status, date, note, epic, path per ticket)
  ticket.py epics [--json]                 every epic with its children's counts (--json for a dashboard snapshot)
  ticket.py check                          folders, numbers, counter, references and Part of lines
                                           (exit 1 on a problem)
  ticket.py migrate PREFIX [--apply]       number every task and plan by its date (dry run by default)
  ticket.py adopt FILE [--apply] [--commit]
                                           or one file at a time: number it, keep its old name behind the
                                           number (20260802-foo.md -> X-00042-20260802-foo.md), replace its
                                           references, verify none is left, and (--commit) commit it alone,
                                           only the files it changed (--trailer LINE adds a commit trailer)
  ticket.py sweep [--apply] [--show] [--exclude GLOB ...]
                                           replace references to the old paths with ticket numbers;
                                           .json files get the new path instead (tools read it back)

Common options: --root DIR (default: the repo containing the current directory).

Folders, by the status word in the line `> **Status (yyyy-mm-dd): WORD.**`:
  docs/tasks/future/              IDEA, NOT SCHEDULED
  docs/tasks/                     DESIGNED, SCHEDULED, NOT STARTED, PAUSED
  docs/tasks/in-progress/         IN PROGRESS (PAUSED allowed too, while the whole project pauses)
  docs/tasks-archive/<yyyy-mm>/   DONE, WON'T IMPLEMENT (the month of the status date)

Not tickets: README.md, NEXT-STEPS.md, TEMPLATE.md, every _*.md, and every path matching a
`skip: <glob>` line in the counter file (e.g. `skip: docs/tasks/in-progress/*-checklist.md`).
PAUSED in in-progress/ is accepted only while in-progress/NEXT-STEPS.md (the project's pause note) exists.

Epics (optional): a ticket names its one parent in the line `> **Part of:** XYZ-00042 (short title)`
in the status block, after a bare `>` line (so it renders as its own paragraph). The epic is an ordinary ticket; it keeps no list of its children (`list
--epic` derives it), and its own status never counts: its progress is its children's statuses. An
epic may have a parent of its own; counts are of direct children unless --deep. "Follow-up of" and
"split out of" say where a ticket came from, not what it belongs to: they stay in History.

References: write the number, with a short title on first mention: "KLANG-00042 (engine follow-ups)",
"KLANG-00042 item 30". `check` refuses references that name a ticket file instead of its number.

Race conditions: `new` and `migrate` hold a lock in the git common dir, so every worktree of the repo
shares it, and take the next number as one above the highest of the counter and every ticket number
found in any worktree on disk (committed or not), any local branch, or any fetched remote branch.
Within one clone a number is never handed out twice. Separate clones (another machine) share neither
the lock nor the files: there, `check` finds a duplicate after the merge (and the counter file conflicts).

Exit codes: 0 ok, 1 a problem found (check, sweep leftovers) or a refused action, 2 usage error.
Standard library only; needs git.
"""
import argparse
import datetime
import fcntl
import fnmatch
import os
import re
import subprocess
import sys

COUNTER = "docs/tasks/_next-ticket.txt"
TASKS, FUTURE, IN_PROGRESS, ARCHIVE = "docs/tasks", "docs/tasks/future", "docs/tasks/in-progress", "docs/tasks-archive"
PLANS = "docs/plans"
HOUSEKEEPING = "docs/housekeeping"
NOT_TICKETS = {"README.md", "NEXT-STEPS.md", "TEMPLATE.md"}  # plus every _*.md
STATUS_LINE = re.compile(r"^> \*\*Status \((\d{4}-\d{2}-\d{2})\):\s*(.*)$")
TASK_SEGMENT = re.compile(r"(^|/)(tasks|tasks-archive|plans)(/|$)")
ELIDED = r"(?:\u2026/|\.\.\./)"  # "…/name.md" or ".../name.md": a path shortened in prose
PART_OF = re.compile(r"^> \*\*Part of:\*\*\s*([A-Z][A-Z0-9]*-\d{5})\b\s*(?:\(([^)]*)\))?")
BUCKET = {"DONE": "closed", "WON'T IMPLEMENT": "closed", "IDEA": "ideas", "NOT SCHEDULED": "ideas",
          "PAUSED": "paused"}  # everything else is open
STATUS_WORD = re.compile(r"[A-Z][A-Z']*(?: [A-Z][A-Z']*)*(?![a-z])")
FOLDER_OF = {
    "IDEA": FUTURE, "NOT SCHEDULED": FUTURE,
    "DESIGNED": TASKS, "SCHEDULED": TASKS, "NOT STARTED": TASKS, "PAUSED": TASKS,
    "IN PROGRESS": IN_PROGRESS,
    "DONE": ARCHIVE, "WON'T IMPLEMENT": ARCHIVE,
}
MD_LINK = re.compile(r"(!?)\[((?:[^\]\\]|\\.)*)\]\(\s*<?([^)\s>]+)>?(\s+\"[^\"]*\")?\s*\)")
WAS = re.compile(r"\(was `[^`]+`\)")
CODE_PATH = re.compile(r"`((?:\u2026/|\.\.\./)?(?:\.{1,2}/)*[\w.-]+(?:/[\w.-]+)*\.md)(#[\w-]+)?`")
MD_PATH = re.compile(r"(?<![\w/.-])((?:\u2026/|\.\.\./)?(?:\.{1,2}/)*[\w.-]+(?:/[\w.-]+)*\.md)(#[\w-]+)?(?![\w/-])")


class Refused(Exception):
    """Print the message and exit 1."""


def readf(path):
    with open(path, encoding="utf-8") as f:
        return f.read()


def writef(path, text):
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


def git(root, *args, ok=(0,)):
    r = subprocess.run(["git", "-C", root, *args], capture_output=True, text=True)
    if r.returncode not in ok:
        raise Refused(f"git {' '.join(args)}: {r.stderr.strip()}")
    return r.stdout


def repo_root(start):
    try:
        return git(start, "rev-parse", "--show-toplevel").strip()
    except Refused:
        raise Refused(f"{start} is not inside a git repository")


def today():
    return datetime.date.today().isoformat()


# ---------------------------------------------------------------- the counter and the lock

def read_counter(root):
    path = os.path.join(root, COUNTER)
    if not os.path.exists(path):
        raise Refused(f"no {COUNTER}; run `ticket.py init PREFIX` (or `migrate PREFIX`) first")
    m = re.search(r"next ticket:\s*([A-Z][A-Z0-9]*)-(\d{5})", readf(path))
    if not m:
        raise Refused(f"{COUNTER} must hold a line like 'next ticket: KLANG-00001'")
    return m.group(1), int(m.group(2))


def write_counter(root, prefix, n):
    os.makedirs(os.path.join(root, TASKS), exist_ok=True)
    path = os.path.join(root, COUNTER)
    rest = ""
    if os.path.exists(path):
        rest = "".join(l for l in readf(path).splitlines(True) if not l.startswith("next ticket:"))
    tmp = f"{path}.{os.getpid()}.tmp"
    with open(tmp, "w") as f:
        f.write(f"next ticket: {prefix}-{n:05d}\n{rest}")
    os.replace(tmp, path)  # atomic: a reader outside the lock never sees a half-written counter


class Lock:
    """An exclusive lock in the git common dir, shared by every worktree of the repo."""

    def __init__(self, root):
        common = git(root, "rev-parse", "--path-format=absolute", "--git-common-dir").strip()
        self.path = os.path.join(common, "ticket.lock")

    def __enter__(self):
        self.f = open(self.path, "w")
        fcntl.flock(self.f, fcntl.LOCK_EX)
        return self

    def __exit__(self, *exc):
        fcntl.flock(self.f, fcntl.LOCK_UN)
        self.f.close()


def highest_in_use(root, prefix):
    """The highest ticket number in any worktree on disk, any local branch or any fetched remote branch."""
    pat = re.compile(rf"(?:^|/){re.escape(prefix)}-(\d{{5}})-[^/]*\.md$")
    best = 0
    trees = [l[len("worktree "):] for l in git(root, "worktree", "list", "--porcelain").splitlines()
             if l.startswith("worktree ")]
    for tree in trees:
        for base in (TASKS, ARCHIVE):
            for dirpath, _, files in os.walk(os.path.join(tree, base)):
                for name in files:
                    m = pat.search(name)
                    if m:
                        best = max(best, int(m.group(1)))
    for ref in git(root, "for-each-ref", "--format=%(refname)", "refs/heads", "refs/remotes").split():
        if ref.endswith("/HEAD"):
            continue
        for name in git(root, "ls-tree", "-r", "--name-only", ref, "--", TASKS, ARCHIVE).splitlines():
            m = pat.search(name)
            if m:
                best = max(best, int(m.group(1)))
    return best


def take_numbers(root, prefix, count):
    """Take `count` consecutive numbers under the lock; returns the first."""
    with Lock(root):
        try:
            p, n = read_counter(root)
            if p != prefix:
                raise Refused(f"{COUNTER} has prefix {p}, not {prefix}")
        except Refused:
            if os.path.exists(os.path.join(root, COUNTER)):
                raise
            n = 1
        first = max(n, highest_in_use(root, prefix) + 1)
        write_counter(root, prefix, first + count)
        return first


# ---------------------------------------------------------------- reading tickets

def slug(title):
    s = re.sub(r"[^a-z0-9]+", "-", title.lower()).strip("-")
    return s[:60].rstrip("-") or "untitled"


def is_ticket_dir_file(name):
    return name.endswith(".md") and name not in NOT_TICKETS and not name.startswith("_")


def skip_globs(root):
    path = os.path.join(root, COUNTER)
    if not os.path.exists(path):
        return []
    return re.findall(r"^skip:\s*(\S+)\s*$", readf(path), re.M)


def skipped(root, rel):
    return any(fnmatch.fnmatch(rel, g) for g in skip_globs(root))


def ticket_files(root):
    """Every .md in the ticket folders (README, NEXT-STEPS, TEMPLATE and _*.md excepted), relative paths."""
    out = []
    for d in (TASKS, FUTURE, IN_PROGRESS):
        full = os.path.join(root, d)
        if os.path.isdir(full):
            out += [f"{d}/{n}" for n in sorted(os.listdir(full))
                    if os.path.isfile(os.path.join(full, n)) and is_ticket_dir_file(n)]
    full = os.path.join(root, ARCHIVE)
    if os.path.isdir(full):
        for dirpath, _, files in sorted(os.walk(full)):
            rel = os.path.relpath(dirpath, root)
            out += [f"{rel}/{n}" for n in sorted(files) if is_ticket_dir_file(n)]
    return [f for f in out if not skipped(root, f)]


def parse_word(rest):
    """The status word at the start of what follows the date: the longest known word, so
    'IDEA, the maintainer's' and 'DESIGNED with the maintainer' read as IDEA and DESIGNED; else the
    leading upper-case words (an unknown word such as AGREED), else None."""
    rest = rest.lstrip("*").lstrip()
    for word in sorted(FOLDER_OF, key=len, reverse=True):
        if rest.startswith(word) and not rest[len(word):len(word) + 1].isalpha():
            return word
    m = STATUS_WORD.match(rest)
    return m.group(0) if m else None


def status_of(path):
    """(date, word or None, rest of the line, line index), or None when there is no status line."""
    with open(path, encoding="utf-8") as f:
        for i, line in enumerate(f):
            m = STATUS_LINE.match(line.rstrip("\n"))
            if m:
                return m.group(1), parse_word(m.group(2)), m.group(2), i
            if i > 20:
                break
    return None


def part_of(path):
    """(parent ID, short title or '', line index) from the Part of line in the header, or None."""
    with open(path, encoding="utf-8") as f:
        for i, line in enumerate(f):
            m = PART_OF.match(line.rstrip("\n"))
            if m:
                return m.group(1), (m.group(2) or "").strip(), i
            if i > 30 or line.startswith("## "):
                break
    return None


def words(text):
    return {w for w in re.findall(r"[a-z0-9]+", text.lower()) if len(w) >= 4}


def bare_title(path, tid):
    t = title_of(path)
    return t[len(tid) + 1:].strip() if tid and t.startswith(tid + ":") else t


def tree(root, prefix):
    """{id: {rel, word, title, parent, short}} for every numbered ticket."""
    out = {}
    for rel in ticket_files(root):
        tid = id_of(rel, prefix)
        if not tid:
            continue
        path = os.path.join(root, rel)
        st, po = status_of(path), part_of(path)
        out[tid] = {"rel": rel, "word": (st[1] if st else None) or "?", "title": bare_title(path, tid),
                    "parent": po[0] if po else None, "short": po[1] if po else ""}
    return out


def children(t, epic, deep=False):
    kids = sorted(k for k, v in t.items() if v["parent"] == epic and k != epic)
    if deep:
        seen, todo = set(kids), list(kids)
        while todo:
            for k in children(t, todo.pop()):
                if k not in seen:
                    seen.add(k); todo.append(k)
        kids = sorted(seen)
    return kids


def counts(t, kids):
    c = {"total": len(kids), "closed": 0, "open": 0, "paused": 0, "ideas": 0}
    for k in kids:
        c[BUCKET.get(t[k]["word"], "open")] += 1
    return c


def count_line(c):
    return f"{c['closed']}/{c['total']} closed, {c['open']} open, {c['paused']} paused, {c['ideas']} ideas"


def title_of(path):
    with open(path, encoding="utf-8") as f:
        for line in f:
            if line.startswith("# "):
                return line[2:].strip()
    return ""


def folder_for(word, date):
    folder = FOLDER_OF.get(word)
    if folder == ARCHIVE:
        return f"{ARCHIVE}/{date[:7]}"
    return folder


def id_of(rel, prefix):
    m = re.match(rf"{re.escape(prefix)}-(\d{{5}})-", os.path.basename(rel))
    return f"{prefix}-{m.group(1)}" if m else None


def find(root, key):
    prefix, _ = read_counter(root)
    files = ticket_files(root)
    if re.fullmatch(rf"{re.escape(prefix)}-\d{{5}}", key):
        hits = [f for f in files if os.path.basename(f).startswith(key + "-")]
    else:
        hits = []
        for oldpath, tid in read_migrations(root).items():
            if key in oldpath:
                hits += [f for f in files if os.path.basename(f).startswith(tid + "-")]
        for f in files:
            text = readf(os.path.join(root, f))
            if any(key in old for old in re.findall(r"\(was `([^`]+)`\)", text)) or key in os.path.basename(f):
                hits.append(f)
    return sorted(set(hits))


def add_history(text, line):
    if re.search(r"^## History\s*$", text, re.M):
        return text.rstrip("\n") + "\n" + line + "\n"
    return text.rstrip("\n") + "\n\n## History\n\n" + line + "\n"


def move_file(root, src, dst):
    if src == dst:
        return
    os.makedirs(os.path.dirname(os.path.join(root, dst)), exist_ok=True)
    tracked = git(root, "ls-files", "--error-unmatch", src, ok=(0, 1)).strip()
    if tracked:
        git(root, "mv", src, dst)
    else:
        os.rename(os.path.join(root, src), os.path.join(root, dst))


# ---------------------------------------------------------------- commands

def cmd_init(a):
    if os.path.exists(os.path.join(a.root, COUNTER)):
        raise Refused(f"{COUNTER} already exists")
    if not re.fullmatch(r"[A-Z][A-Z0-9]*", a.prefix):
        raise Refused("PREFIX is upper-case letters and digits, e.g. KLANG")
    with Lock(a.root):
        write_counter(a.root, a.prefix, highest_in_use(a.root, a.prefix) + 1)
    print(readf(os.path.join(a.root, COUNTER)).strip())


def cmd_new(a):
    word = a.status.upper()
    if word not in FOLDER_OF or FOLDER_OF[word] == ARCHIVE:
        raise Refused(f"a new ticket starts as one of: {', '.join(w for w, f in FOLDER_OF.items() if f != ARCHIVE)}")
    prefix, _ = read_counter(a.root)
    part = ""
    if a.part_of:
        hits = [f for f in ticket_files(a.root) if os.path.basename(f).startswith(a.part_of + "-")]
        if len(hits) != 1:
            raise Refused(f"--part-of {a.part_of}: {len(hits)} matching tickets")
        part = f">\n> **Part of:** {a.part_of} ({bare_title(os.path.join(a.root, hits[0]), a.part_of)})\n"
    n = take_numbers(a.root, prefix, 1)
    tid = f"{prefix}-{n:05d}"
    rel = f"{folder_for(word, today())}/{tid}-{slug(a.title)}.md"
    body = (f"# {tid}: {a.title}\n\n> **Status ({today()}): {word}.** {a.note or ''}\n{part}\n"
            "## Why this exists\n\n## Decide before implementing\n\n## The work\n\n## Acceptance\n\n"
            f"## History\n\n- {today()}: created.\n")
    os.makedirs(os.path.dirname(os.path.join(a.root, rel)), exist_ok=True)
    with open(os.path.join(a.root, rel), "x", encoding="utf-8") as f:
        f.write(body)
    print(f"{tid} {rel}")


def status_block_end(lines, st):
    """The index after the status line's blockquote: it goes on until a blank, bare '>' or new bold line."""
    end = st[3] + 1
    while end < len(lines) and lines[end].startswith("> ") and lines[end][2:].strip() \
            and not lines[end].startswith("> **"):
        end += 1
    return end


def status_prose(lines, st):
    """The text after the status word, all its lines joined."""
    note = " ".join([st[2]] + [l[2:].strip() for l in lines[st[3] + 1:status_block_end(lines, st)]]).lstrip("*").lstrip()
    if st[1] and note.startswith(st[1]):
        note = note[len(st[1]):]
    return note.lstrip(" .,;:*").strip()


def cmd_move(a):
    word = a.status.upper()
    if word not in FOLDER_OF:
        raise Refused(f"unknown status {word}; one of: {', '.join(FOLDER_OF)}")
    hits = find(a.root, a.id)
    if len(hits) != 1:
        raise Refused(f"{a.id}: {len(hits)} matching files {hits}")
    src = hits[0]
    path = os.path.join(a.root, src)
    lines = readf(path).split("\n")
    st = status_of(path)
    end = status_block_end(lines, st) if st else 0
    if a.note:
        note = f" {a.note}"
    elif st:  # keep the prose after the old word, all its lines
        note = status_prose(lines, st)
        note = f" {note}" if note else ""
    else:
        note = ""
    new_line = f"> **Status ({today()}): {word}.**{note}"
    if st:
        lines[st[3]:end] = [new_line]
    else:
        at = next((i + 1 for i, l in enumerate(lines) if l.startswith("# ")), 0)
        lines[at:at] = ["", new_line]
    text = add_history("\n".join(lines), f"- {today()}: {word}.{(' ' + a.note) if a.note else ''}")
    writef(path, text)
    dst = f"{folder_for(word, today())}/{os.path.basename(src)}"
    move_file(a.root, src, dst)
    if git(a.root, "ls-files", "--error-unmatch", dst, ok=(0, 1)).strip():
        git(a.root, "add", dst)  # git mv staged the old content; stage the new status and History too
    print(dst)


def cmd_find(a):
    hits = find(a.root, a.key)
    for h in hits:
        print(h)
    return 0 if hits else 1


def cmd_list(a):
    prefix, _ = read_counter(a.root)
    if a.json:
        import json
        t = tree(a.root, prefix)
        keep = set(children(t, a.epic, a.deep)) if a.epic else set(t)
        out = []
        for tid in sorted(keep):
            v = t[tid]
            if a.status and v["word"] != a.status.upper():
                continue
            path = os.path.join(a.root, v["rel"])
            st = status_of(path)
            note = status_prose(readf(path).split("\n"), st) if st else ""
            out.append({"id": tid, "title": v["title"], "status": v["word"], "date": st[0] if st else None,
                        "note": note, "epic": v["parent"], "path": v["rel"]})
        print(json.dumps(out, indent=1, ensure_ascii=False))
        return 0
    if a.epic:
        t = tree(a.root, prefix)
        if a.epic not in t:
            raise Refused(f"{a.epic}: no such ticket")
        kids = children(t, a.epic, a.deep)
        for k in kids:
            if not a.status or t[k]["word"] == a.status.upper():
                print(f"{k:14} {t[k]['word']:16} {t[k]['rel']}  {t[k]['title']}")
        print(f"{a.epic} ({t[a.epic]['title']}): {count_line(counts(t, kids))}")
        return 0
    for rel in ticket_files(a.root):
        st = status_of(os.path.join(a.root, rel))
        word = st[1] if st else "?"
        if a.status and word != a.status.upper():
            continue
        print(f"{id_of(rel, prefix) or '(no number)':14} {word:16} {rel}  {title_of(os.path.join(a.root, rel))}")


def cmd_epics(a):
    prefix, _ = read_counter(a.root)
    t = tree(a.root, prefix)
    epics = sorted({v["parent"] for v in t.values() if v["parent"] in t})
    out = []
    for e in epics:
        kids = children(t, e)
        out.append({"id": e, "title": t[e]["title"], "status": t[e]["word"], "parent": t[e]["parent"],
                    "counts": counts(t, kids),
                    "children": [{"id": k, "title": t[k]["title"], "status": t[k]["word"]} for k in kids]})
    if a.json:
        import json
        print(json.dumps(out, indent=1, ensure_ascii=False))
    else:
        for e in out:
            up = f"  (part of {e['parent']})" if e["parent"] else ""
            print(f"{e['id']:14} {e['title']}: {count_line(e['counts'])}{up}")
    return 0


def text_files(root):
    for rel in git(root, "ls-files", "-z").split("\0"):
        full = os.path.join(root, rel)
        if not rel or not os.path.isfile(full) or os.path.getsize(full) > 2_000_000:
            continue
        if rel.endswith(".log"):
            continue  # dated logs keep their words
        try:
            with open(full, encoding="utf-8") as f:
                yield rel, f.read()
        except (UnicodeDecodeError, OSError):
            continue


def cmd_check(a):
    prefix, nxt = read_counter(a.root)
    problems = []
    seen = {}
    for rel in ticket_files(a.root):
        tid = id_of(rel, prefix)
        if not tid:
            problems.append(f"{rel}: not named {prefix}-nnnnn-<title>.md")
            continue
        if tid in seen:
            problems.append(f"{rel}: {tid} is also {seen[tid]}")
        seen[tid] = rel
        st = status_of(os.path.join(a.root, rel))
        if not st:
            problems.append(f"{rel}: no status line")
            continue
        date, word, _, _ = st
        if not word:
            problems.append(f"{rel}: the status line has no status word")
            continue
        want = folder_for(word, date)
        here = os.path.dirname(rel)
        if want is None:
            problems.append(f"{rel}: unknown status word {word}")
        elif word == "PAUSED" and here == IN_PROGRESS:
            if not os.path.exists(os.path.join(a.root, IN_PROGRESS, "NEXT-STEPS.md")):
                problems.append(f"{rel}: PAUSED in in-progress/ needs the pause note in-progress/NEXT-STEPS.md")
        elif (FOLDER_OF[word] == ARCHIVE and not here.startswith(ARCHIVE + "/")) or \
                (FOLDER_OF[word] != ARCHIVE and here != want):
            problems.append(f"{rel}: status {word} belongs in {want}/")
    t = tree(a.root, prefix)
    notes = []
    for tid, v in sorted(t.items()):
        par = v["parent"]
        if not par:
            continue
        if par == tid:
            problems.append(f"{v['rel']}: Part of names the ticket itself")
        elif par not in t:
            problems.append(f"{v['rel']}: Part of {par}, which has no ticket file")
        else:
            if v["short"] and not words(v["short"]) & words(t[par]["title"]):  # a paraphrase is fine
                problems.append(f"{v['rel']}: Part of {par} ({v['short']}), but {par} is titled \"{t[par]['title']}\"")
            chain, up = [tid], par
            while up in t and up not in chain:
                chain.append(up)
                up = t[up]["parent"]
            if up in chain and up == tid:
                problems.append(f"{v['rel']}: Part of loops: {' > '.join(chain + [up])}")
    for e in sorted({v["parent"] for v in t.values() if v["parent"] in t}):
        c = counts(t, children(t, e))
        if t[e]["word"] in ("DONE", "WON'T IMPLEMENT") and c["closed"] < c["total"]:
            notes.append(f"note: {e} is {t[e]['word']} with {c['total'] - c['closed']} open child ticket(s)")
    if seen:
        top = max(int(t.split("-")[-1]) for t in seen)
        if nxt <= top:
            problems.append(f"{COUNTER}: next is {prefix}-{nxt:05d}, but {prefix}-{top:05d} exists")
    if any(files for _, _, files in os.walk(os.path.join(a.root, PLANS))):
        problems.append(f"{PLANS}/ exists: plans are tickets in {FUTURE}/ or {TASKS}/")
    ref = re.compile(rf"\b{re.escape(prefix)}-\d{{5}}\b")
    named = re.compile(rf"{re.escape(prefix)}-\d{{5}}-[\w.-]*\.md")
    for rel, text in text_files(a.root):
        if rel.startswith(HOUSEKEEPING + "/") or rel == COUNTER:
            continue
        for no, line in enumerate(text.split("\n"), 1):
            for m in ref.finditer(line):
                if m.group(0) not in seen:
                    problems.append(f"{rel}:{no}: {m.group(0)} has no ticket file")
            if named.search(line) and not rel.endswith(".json"):  # tools may store paths in json
                problems.append(f"{rel}:{no}: names a ticket file; reference the number instead")
    for p in problems + notes:
        print(p)
    print(f"{len(seen)} tickets, next {prefix}-{nxt:05d}, {len(problems)} problems")
    return 1 if problems else 0


# ---------------------------------------------------------------- migrate and sweep

def migration_candidates(root):
    out = []
    for base in (PLANS, TASKS, ARCHIVE):
        full = os.path.join(root, base)
        for dirpath, dirs, files in os.walk(full):
            rel = os.path.relpath(dirpath, root)
            if base == TASKS and rel not in (TASKS, FUTURE, IN_PROGRESS):
                dirs[:] = []
                continue
            out += [f"{rel}/{n}" for n in sorted(files) if is_ticket_dir_file(n)]
    return sorted(f for f in set(out) if not skipped(root, f))


def other_task_subfolders(root):
    full = os.path.join(root, TASKS)
    if not os.path.isdir(full):
        return []
    return [f"{TASKS}/{n}/" for n in sorted(os.listdir(full))
            if os.path.isdir(os.path.join(full, n)) and n not in ("future", "in-progress")]


DATE_PREFIX = re.compile(r"^(\d{4})-?(\d{2})-?(\d{2})[-_]")


def created_key(root, rel):
    """(date, first-add time, name): the date from a dated name (20260802-, 2026-03-31-, 2026-03-27_),
    else from the first commit that added the file; ties broken by that commit's time, then the name."""
    log = git(root, "log", "--follow", "--diff-filter=A", "--format=%as %at", "--", rel).split("\n")
    log = [l for l in log if l.strip()]
    first_day, first_ts = (log[-1].split() if log else
                           (datetime.date.fromtimestamp(os.path.getmtime(os.path.join(root, rel))).isoformat(), "0"))
    m = DATE_PREFIX.match(os.path.basename(rel))
    day = "-".join(m.groups()) if m else first_day
    return day, int(first_ts), os.path.basename(rel)


def migration_target(rel, st):
    """The folder a migrated file goes to: its status's folder, else the matching new folder."""
    here = os.path.dirname(rel)
    if here == FUTURE or here.startswith(PLANS + "/"):
        return FUTURE  # the folder says "later"; a status word that disagrees is noted, not obeyed
    if st and st[1] in FOLDER_OF:
        if FOLDER_OF[st[1]] == ARCHIVE:
            return here if here.startswith(ARCHIVE + "/") else f"{ARCHIVE}/{st[0][:7]}"
        if st[1] == "PAUSED" and here == IN_PROGRESS:
            return here
        return FOLDER_OF[st[1]]
    if here.startswith(ARCHIVE):
        return here
    if here == PLANS:
        return TASKS
    if here.startswith(PLANS):
        return FUTURE
    return here


def cmd_migrate(a):
    if not re.fullmatch(r"[A-Z][A-Z0-9]*", a.prefix):
        raise Refused("PREFIX is upper-case letters and digits, e.g. KLANG")
    files = [f for f in migration_candidates(a.root) if not id_of(f, a.prefix)]
    dated = [(k[0], f) for k, f in sorted((created_key(a.root, f), f) for f in files)]
    if not dated:
        print("nothing to migrate")
        return 0
    if a.apply:
        first = take_numbers(a.root, a.prefix, len(dated))
    else:
        with Lock(a.root):
            first = max(1, highest_in_use(a.root, a.prefix) + 1)
            if os.path.exists(os.path.join(a.root, COUNTER)):
                first = max(first, read_counter(a.root)[1])
    rows, notes = [], []
    for i, (date, rel) in enumerate(dated):
        tid = f"{a.prefix}-{first + i:05d}"
        st = status_of(os.path.join(a.root, rel))
        if not st or not st[1]:
            notes.append(f"{rel}: no status {'line' if not st else 'word'}; placed by its folder")
        elif st[1] not in FOLDER_OF:
            notes.append(f"{rel}: status word {st[1]} is not a known one; placed by its folder")
        elif migration_target(rel, st) == FUTURE and FOLDER_OF[st[1]] != FUTURE:
            notes.append(f"{rel}: kept in future/, but its status {st[1]} belongs elsewhere; "
                         "set IDEA or NOT SCHEDULED, or `move` it")
        name = DATE_PREFIX.sub("", os.path.basename(rel))
        dst = f"{migration_target(rel, st)}/{tid}-{name}"
        rows.append((tid, date, rel, dst))
    for tid, date, rel, dst in rows:
        print(f"{tid}  {date}  {rel} -> {dst}")
    for n in notes:
        print(f"note: {n}")
    others = []
    if os.path.isdir(os.path.join(a.root, PLANS)):
        for dirpath, _, fs in os.walk(os.path.join(a.root, PLANS)):
            others += [os.path.relpath(os.path.join(dirpath, n), a.root) for n in fs if not n.endswith(".md")]
    for o in others:
        print(f"note: {o} is not a task file; move it by hand")
    for d in other_task_subfolders(a.root):
        print(f"note: {d} is not a ticket folder; decide per file (ticket, or a skip: line), then migrate again")
    if not a.apply:
        print(f"dry run: {len(rows)} files would be numbered {rows[0][0]} to {rows[-1][0]}; --apply to do it")
        return 0
    stamp = today()
    for tid, date, rel, dst in rows:
        path = os.path.join(a.root, rel)
        text = readf(path)
        text = re.sub(r"^# (?!" + re.escape(tid) + ")", f"# {tid}: ", text, count=1, flags=re.M)
        text = add_history(text, f"- {stamp}: numbered {tid} (was `{rel}`).")
        writef(path, text)
        move_file(a.root, rel, dst)
    for dirpath, _, _ in sorted(os.walk(os.path.join(a.root, PLANS)), reverse=True):
        if not os.listdir(dirpath):
            os.rmdir(dirpath)  # an emptied plans/ folder goes; one still holding files stays and is noted
    record = write_record(a.root, rows)
    print(f"numbered {len(rows)} files; record: {record}")
    print("next: `ticket.py sweep` (dry run), then `ticket.py sweep --apply`, then `ticket.py check`")
    return 0


def former_paths(root, rel):
    """Where the file lived before (git's rename history), newest first, without rel itself. Only .md
    files in a task or plan folder count: --follow pairs a small file with unrelated ones (a .gitkeep,
    a gradle.properties), and such a path would send references to the wrong ticket."""
    out = []
    for line in git(root, "log", "--follow", "--name-only", "--format=", "--", rel).splitlines():
        line = line.strip()
        if line and line != rel and line not in out and line.endswith(".md") \
                and TASK_SEGMENT.search(os.path.dirname(line)):
            out.append(line)
    return out


def write_record(root, rows):
    """Append rows (ticket, created, old path, new path) to today's record, with a row for each former
    path of a file too, so a reference to where a file used to live resolves. Returns the record's path."""
    os.makedirs(os.path.join(root, HOUSEKEEPING), exist_ok=True)
    stamp = today()
    rel = f"{HOUSEKEEPING}/{stamp.replace('-', '')}-ticket-migration.md"
    path = os.path.join(root, rel)
    known = set(read_migrations(root))
    current = {r[2] for r in rows}
    lines = []
    for tid, date, old, new in rows:
        if old not in known:
            lines.append(f"| {tid} | {date} | `{old}` | `{new}` |")
            known.add(old)
        # history lives under the OLD path: a bulk migration's new paths have none until it is committed
        for former in former_paths(root, old) or former_paths(root, new):
            if former not in known and former not in current:
                lines.append(f"| {tid} | {date} | `{former}` | `{new}` |")
                known.add(former)
    if not os.path.exists(path):
        writef(path, f"# Ticket migration ({stamp})\n\nOld path to ticket; `ticket.py sweep` and `find` read "
                     "this table. Former locations of a file are rows too.\n\n"
                     "| Ticket | Created | Old path | New path |\n|---|---|---|---|\n")
    with open(path, "a", encoding="utf-8") as f:
        f.write("".join(l + "\n" for l in lines))
    return rel


def read_migrations(root, full=False):
    """old path -> ticket id (or, with full, -> (ticket id, new path)), from every migration record."""
    old = {}
    d = os.path.join(root, HOUSEKEEPING)
    if os.path.isdir(d):
        for n in sorted(os.listdir(d)):
            if n.endswith("-ticket-migration.md"):
                for m in re.finditer(r"^\| ([A-Z][A-Z0-9]*-\d{5}) \| [^|]+ \| `([^`]+)` \| `([^`]+)` \|$",
                                     readf(os.path.join(d, n)), re.M):
                    old[m.group(2)] = (m.group(1), m.group(3)) if full else m.group(1)
    return old


def short_title(old_path):
    """A short title from the old file name: '20261020-04o-control-loops.md' -> '04o control loops'."""
    return DATE_PREFIX.sub("", os.path.basename(old_path))[:-3].replace("-", " ").replace("_", " ")


def resolver(old):
    """resolve(file, target) -> (ticket id, new path, short title) for a reference to a migrated file."""
    by_suffix, by_name = {}, {}
    for path in old:
        by_name.setdefault(os.path.basename(path), set()).add(old[path][0])
        parts = path.split("/")
        for i in range(len(parts)):
            by_suffix.setdefault("/".join(parts[i:]), set()).add(path)

    def resolve(file_rel, target):
        target = re.sub(r"^" + ELIDED, "", target.split("#")[0])
        if not target.endswith(".md") or "://" in target:
            return None
        here = os.path.dirname(file_rel)
        hit = None
        for cand in (os.path.normpath(os.path.join(here, target)), os.path.normpath(target)):
            if cand in old:
                hit = cand
                break
        if not hit:
            hits = by_suffix.get(re.sub(r"^(\.{1,2}/)+", "", target), set())
            # several candidates are fine when they all are one ticket (a file and its former locations)
            hit = sorted(hits)[0] if hits and len({old[h][0] for h in hits}) == 1 else None
        if not hit:  # a path the file never had (a stale folder): the file name, if it is one ticket's
            tids = by_name.get(os.path.basename(target), set())
            if len(tids) == 1:
                tid = next(iter(tids))
                hit = sorted(p for p in old if old[p][0] == tid)[0]
        if not hit:
            return None
        tid, new = old[hit]
        return tid, new, short_title(hit)
    return resolve


def sweep_text(rel, text, resolve):
    """Returns (new text, [(line number, old, new)]). In .json, a path becomes the new path, so tools
    that read it back keep working; elsewhere it becomes the number, with a short title for bare paths."""
    changes = []
    as_path = rel.endswith(".json")

    def lineno(pos):
        return text.count("\n", 0, pos) + 1

    def link(m):
        bang, label, target, title = m.groups()
        r = resolve(rel, target)
        if not r or bang:
            return m.group(0)
        tid, new, short = r
        anchor = target.split("#")[1] if "#" in target else ""
        ref = f"{tid}#{anchor}" if anchor else tid
        plain = label.strip("`")
        if plain.endswith(".md") or "/" in plain or plain == ref:
            out = f"{ref} ({short})"
        else:
            out = f"{label} ({ref})"
        changes.append((lineno(m.start()), m.group(0), out))
        return out

    def bare(m):
        r = resolve(rel, m.group(1))
        if not r:
            return m.group(0)
        tid, new, short = r
        if as_path:
            out = new + (m.group(2) or "")
        else:
            out = f"{tid}{m.group(2) or ''} ({short})"
        changes.append((lineno(m.start()), m.group(0), out))
        return out

    # "(was `old path`)" lines are the record find reads; never rewrite them
    kept = []

    def keep(m):
        kept.append(m.group(0))
        return f"\0WAS{len(kept) - 1}\0"
    text = WAS.sub(keep, text)
    out = text if as_path else MD_LINK.sub(link, text)
    if not as_path:
        text = out  # line numbers for later passes refer to the text after the link pass
        out = CODE_PATH.sub(bare, out)  # a code span that is just the path loses its backticks
        text = out
    out = MD_PATH.sub(bare, out)
    out = re.sub(r"\0WAS(\d+)\0", lambda m: kept[int(m.group(1))], out)
    return out, changes


def apply_sweep(root, old, exclude=(), apply=True, show=False):
    """Sweep the references to the paths in `old` (old path -> (ticket, new path)). Returns the count."""
    resolve = resolver(old)
    total = 0
    for rel, text in text_files(root):
        if rel.startswith(HOUSEKEEPING + "/") or any(fnmatch.fnmatch(rel, g) for g in exclude):
            continue
        new, changes = sweep_text(rel, text, resolve)
        if changes:
            total += len(changes)
            print(f"{rel}: {len(changes)} references")
            if show:
                for no, o, n in changes:
                    print(f"    {no}: {o}  ->  {n}")
            if apply:
                writef(os.path.join(root, rel), new)
    return total


def stem_hits(root, stem, exclude=()):
    """Lines that still name the old file stem (with or without .md), outside the records."""
    pat = re.compile(rf"(?<![\w-]){re.escape(stem)}(?![\w-])")
    hits = []
    for rel, text in text_files(root):
        if rel.startswith(HOUSEKEEPING + "/") or any(fnmatch.fnmatch(rel, g) for g in exclude):
            continue
        for no, line in enumerate(WAS.sub("", text).split("\n"), 1):
            if pat.search(line):
                hits.append(f"{rel}:{no}: {line.strip()[:120]}")
    return hits


def changed_files(root):
    """Paths that differ from HEAD (modified, added, deleted, untracked and not ignored)."""
    out, parts = set(), git(root, "status", "--porcelain", "-z", "--untracked-files=all").split("\0")
    i = 0
    while i < len(parts):
        entry = parts[i]
        if len(entry) > 3:
            out.add(entry[3:])
            if entry[0] in "RC":  # a rename's old path follows as its own entry
                i += 1
                out.add(parts[i])
        i += 1
    return out


def cmd_adopt(a):
    prefix, _ = read_counter(a.root)
    rel = os.path.relpath(os.path.abspath(a.file), a.root) if os.path.isabs(a.file) or os.path.exists(a.file) \
        else a.file
    if rel not in migration_candidates(a.root):
        raise Refused(f"{rel}: not a task or plan file (or skipped)")
    if id_of(rel, prefix):
        raise Refused(f"{rel} already has a number")
    stem = os.path.basename(rel)[:-3]
    st = status_of(os.path.join(a.root, rel))
    if not st or not st[1]:
        print(f"note: {rel}: no status word; placed by its folder")
    elif st[1] not in FOLDER_OF:
        print(f"note: {rel}: status word {st[1]} is not a known one; placed by its folder")
    folder = migration_target(rel, st)
    if not a.apply:
        print(f"would number {rel} as the next {prefix} ticket, in {folder}/, as {prefix}-nnnnn-{stem}.md; "
              "--apply to do it")
        return 0
    before = changed_files(a.root)  # what was already changed is not this ticket's to commit
    tid = f"{prefix}-{take_numbers(a.root, prefix, 1):05d}"
    dst = f"{folder}/{tid}-{stem}.md"
    date = created_key(a.root, rel)[0]
    record = write_record(a.root, [(tid, date, rel, dst)])
    path = os.path.join(a.root, rel)
    text = re.sub(r"^# (?!" + re.escape(tid) + ")", f"# {tid}: ", readf(path), count=1, flags=re.M)
    writef(path, add_history(text, f"- {today()}: numbered {tid} (was `{rel}`)."))
    move_file(a.root, rel, dst)
    old = {k: v for k, v in read_migrations(a.root, full=True).items() if v[0] == tid}
    n = apply_sweep(a.root, old, a.exclude)
    if "-" in stem:  # a distinctive stem named without .md ("see 20260802-foo") becomes the number too
        pat = re.compile(rf"(?<![\w/.-]){re.escape(stem)}(?![\w-]|\.md)")
        for frel, ftext in text_files(a.root):
            if frel.startswith(HOUSEKEEPING + "/") or any(fnmatch.fnmatch(frel, g) for g in a.exclude):
                continue
            kept = []
            body = WAS.sub(lambda m: kept.append(m.group(0)) or f"\0WAS{len(kept) - 1}\0", ftext)
            body, k = pat.subn(tid, body)
            if k:
                n += k
                print(f"{frel}: {k} mentions of {stem}")
                writef(os.path.join(a.root, frel), re.sub(r"\0WAS(\d+)\0", lambda m: kept[int(m.group(1))], body))
    print(f"{tid} {dst} ({n} references; record {record})")
    left = stem_hits(a.root, stem, a.exclude)
    for h in left:
        print(f"left: {h}")
    if left:
        print(f"stopped: {len(left)} lines still name {stem}; fix them (or --exclude), then commit by hand")
        return 1
    if a.commit:
        mine = sorted((changed_files(a.root) - before) | {rel, dst, COUNTER})
        git(a.root, "add", "-A", "--", *[f for f in mine if os.path.exists(os.path.join(a.root, f)) or
                                         git(a.root, "ls-files", "--", f).strip()])
        msg = f"{tid}: numbered (was {rel})" + ("\n\n" + "\n".join(a.trailer) if a.trailer else "")
        git(a.root, "commit", "-q", "-m", msg)
        print(f"committed {git(a.root, 'rev-parse', '--short', 'HEAD').strip()}")
    return 0


def cmd_sweep(a):
    old = read_migrations(a.root, full=True)
    if not old:
        raise Refused(f"no migration record in {HOUSEKEEPING}/; run `ticket.py migrate PREFIX --apply` first")
    resolve = resolver(old)
    total, changed, leftovers = 0, 0, []
    old_names = {os.path.basename(p) for p in old}
    for rel, text in text_files(a.root):
        if rel.startswith(HOUSEKEEPING + "/") or any(fnmatch.fnmatch(rel, g) for g in a.exclude):
            continue
        new, changes = sweep_text(rel, text, resolve)
        if changes:
            total += len(changes)
            changed += 1
            print(f"{rel}: {len(changes)} references")
            if a.show:
                for no, o, n in changes:
                    print(f"    {no}: {o}  ->  {n}")
            if a.apply:
                with open(os.path.join(a.root, rel), "w", encoding="utf-8") as f:
                    f.write(new)
        for no, line in enumerate(WAS.sub("", new).split("\n"), 1):
            for m in MD_PATH.finditer(line):
                path = m.group(1)
                token = (line[:m.start()].split() or [""])[-1] if not line[:m.start()].endswith((" ", "\t")) else ""
                if os.path.basename(path) not in old_names or "://" in token:
                    continue  # not a migrated file's name, or part of a URL (e.g. a link pinned to a tag)
                here = os.path.normpath(os.path.join(a.root, os.path.dirname(rel), path))
                if not os.path.exists(here) and not os.path.exists(os.path.join(a.root, path)):
                    leftovers.append(f"{rel}:{no}: {path} (a migrated name, but which file is ambiguous)")
    for l in leftovers:
        print(f"left: {l}")
    verb = "replaced" if a.apply else "would replace"
    print(f"{verb} {total} references in {changed} files; {len(leftovers)} unresolved")
    if not a.apply:
        print("dry run; --show lists each change, --apply writes")
    return 1 if leftovers else 0


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__.split("\n")[0])
    p.add_argument("--root", default=None)
    common = argparse.ArgumentParser(add_help=False)
    common.add_argument("--root", default=argparse.SUPPRESS)  # also accepted after the subcommand
    sub = p.add_subparsers(dest="cmd", required=True)
    _add = sub.add_parser
    sub.add_parser = lambda *a, **k: _add(*a, parents=[common], **k)
    s = sub.add_parser("init"); s.add_argument("prefix")
    s = sub.add_parser("new"); s.add_argument("title"); s.add_argument("--status", default="IDEA"); s.add_argument("--note")
    s.add_argument("--part-of")
    s = sub.add_parser("move"); s.add_argument("id"); s.add_argument("status", nargs="+"); s.add_argument("--note")
    s = sub.add_parser("find"); s.add_argument("key")
    s = sub.add_parser("list"); s.add_argument("--status"); s.add_argument("--epic"); s.add_argument("--deep", action="store_true")
    s.add_argument("--json", action="store_true")
    s = sub.add_parser("epics"); s.add_argument("--json", action="store_true")
    sub.add_parser("check")
    s = sub.add_parser("migrate"); s.add_argument("prefix"); s.add_argument("--apply", action="store_true")
    s = sub.add_parser("adopt"); s.add_argument("file"); s.add_argument("--apply", action="store_true")
    s.add_argument("--commit", action="store_true")
    s.add_argument("--trailer", action="append", default=[], metavar="LINE",
                   help='a commit trailer for --commit, e.g. "Co-Authored-By: ..." (repeatable)')
    s.add_argument("--exclude", nargs="+", action="extend", default=[], metavar="GLOB")
    s = sub.add_parser("sweep"); s.add_argument("--apply", action="store_true"); s.add_argument("--show", action="store_true")
    s.add_argument("--exclude", nargs="+", action="extend", default=[], metavar="GLOB")
    a = p.parse_args(argv)
    if a.cmd == "move":
        a.status = " ".join(a.status)
    try:
        a.root = repo_root(os.path.abspath(a.root or "."))
        rc = {"init": cmd_init, "new": cmd_new, "move": cmd_move, "find": cmd_find, "list": cmd_list,
              "check": cmd_check, "epics": cmd_epics, "migrate": cmd_migrate, "adopt": cmd_adopt, "sweep": cmd_sweep}[a.cmd](a)
        return rc or 0
    except Refused as e:
        print(f"ticket: {e}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
