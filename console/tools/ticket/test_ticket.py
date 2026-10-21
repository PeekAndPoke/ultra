#!/usr/bin/env python3
"""Tests for ticket.py, on throwaway git repos. Run: python3 test_ticket.py"""
import concurrent.futures
import io
import json
import re
import os
import subprocess
import sys
import tempfile
import unittest
from contextlib import redirect_stdout, redirect_stderr

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import ticket  # noqa: E402


def sh(cwd, *args):
    subprocess.run(args, cwd=cwd, check=True, capture_output=True)


def write(root, rel, text):
    path = os.path.join(root, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


def read(root, rel):
    with open(os.path.join(root, rel), encoding="utf-8") as f:
        return f.read()


def run(root, *argv):
    out, err = io.StringIO(), io.StringIO()
    with redirect_stdout(out), redirect_stderr(err):
        rc = ticket.main(["--root", root, *argv])
    return rc, out.getvalue() + err.getvalue()


def status(word, date="2026-10-19"):
    return f"> **Status ({date}): {word}.** Something.\n"


def commit(root, msg, date):
    env = dict(os.environ, GIT_AUTHOR_DATE=date, GIT_COMMITTER_DATE=date)
    subprocess.run(["git", "add", "-A"], cwd=root, check=True, capture_output=True)
    subprocess.run(["git", "commit", "-q", "-m", msg], cwd=root, check=True, capture_output=True, env=env)


class Repo(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = os.path.join(self.tmp.name, "proj")
        os.makedirs(self.root)
        sh(self.root, "git", "init", "-q", "-b", "main")
        sh(self.root, "git", "config", "user.email", "t@t")
        sh(self.root, "git", "config", "user.name", "t")

    def tearDown(self):
        self.tmp.cleanup()


class MigrateAndSweep(Repo):
    def setUp(self):
        super().setUp()
        r = self.root
        write(r, "docs/tasks-archive/2026-10/20261017-01-skeleton.md", "# 01: Skeleton\n\n" + status("DONE", "2026-10-17"))
        write(r, "docs/tasks-archive/2026-10/20261017-02-sun.md", "# 02: Sun\n\n" + status("DONE", "2026-10-17"))
        write(r, "docs/tasks-archive/2026-03/2026-03-31-foo.md", "# Foo\n\n" + status("DONE", "2026-03-31"))
        write(r, "docs/tasks-archive/2026-03/2026-03-27_bar.md", "# Bar\n\n" + status("DONE", "2026-03-27"))
        commit(r, "old", "2026-03-27T10:00:00")
        write(r, "docs/plans/tunnel.md", "# Plan: tunnel\n\n" + status("AGREED"))
        write(r, "docs/plans/future/far.md", "# Far plan\n\n" + status("IDEA"))
        write(r, "docs/tasks/04d-chatter.md", "# 04d: Chatter\n\n" + status("DESIGNED"))
        write(r, "docs/tasks/in-progress/04i-one-ui.md",
              "# 04i: One UI\n\n" + status("IN PROGRESS") + "\nDecisions in [the plan](../../plans/tunnel.md#part-2).\n"
              "Before: [01](../../tasks-archive/2026-10/20261017-01-skeleton.md). Later: [future/zz.md](../future/zz.md).\n")
        write(r, "docs/tasks/in-progress/README.md", "# How tasks look\n")
        write(r, "docs/tasks/future/zz.md", "# ZZ\n\n" + status("IDEA"))
        write(r, "docs/tasks/future/later.md", "# Later\n\n" + status("DESIGNED"))
        write(r, "docs/tasks/_maintainer-questions.md", "1. Q1 about `docs/tasks/04d-chatter.md`\n")
        write(r, "docs/blog/post.md", "See https://github.com/x/y/blob/v0.3.3/docs/plans/tunnel.md#L7 and zz.md.\n")
        write(r, "core/src/Thing.kt", "// see docs/tasks/04d-chatter.md, step 3\nclass Thing\n")
        write(r, ".claude/reviews/04i.json", json.dumps({"task": "docs/tasks/in-progress/04i-one-ui.md"}) + "\n")
        commit(r, "new", "2026-10-19T10:00:00")

    def test_dry_run_changes_nothing(self):
        rc, out = run(self.root, "migrate", "SP")
        self.assertEqual(rc, 0, out)
        self.assertIn("dry run", out)
        self.assertTrue(os.path.exists(os.path.join(self.root, "docs/plans/tunnel.md")))
        self.assertFalse(os.path.exists(os.path.join(self.root, ticket.COUNTER)))

    def test_numbering_order_and_places(self):
        rc, out = run(self.root, "migrate", "SP", "--apply")
        self.assertEqual(rc, 0, out)
        rows = ticket.read_migrations(self.root, full=True)
        ids = {old: tid for old, (tid, new) in rows.items()}
        # by date from the name (three formats), then first-add time, then name
        self.assertEqual(ids["docs/tasks-archive/2026-03/2026-03-27_bar.md"], "SP-00001")
        self.assertEqual(ids["docs/tasks-archive/2026-03/2026-03-31-foo.md"], "SP-00002")
        self.assertEqual(ids["docs/tasks-archive/2026-10/20261017-01-skeleton.md"], "SP-00003")
        self.assertEqual(ids["docs/tasks-archive/2026-10/20261017-02-sun.md"], "SP-00004")
        self.assertEqual(len(ids), 10)  # README and _maintainer-questions are not tickets
        new = {old: n for old, (tid, n) in rows.items()}
        self.assertEqual(new["docs/tasks-archive/2026-03/2026-03-27_bar.md"], "docs/tasks-archive/2026-03/SP-00001-bar.md")
        self.assertTrue(new["docs/plans/tunnel.md"].startswith("docs/tasks/SP-"))  # unknown word AGREED: by folder
        self.assertTrue(new["docs/plans/future/far.md"].startswith("docs/tasks/future/SP-"))
        self.assertTrue(new["docs/tasks/in-progress/04i-one-ui.md"].startswith("docs/tasks/in-progress/SP-"))
        self.assertIn("status word AGREED", out)
        self.assertEqual(ticket.read_counter(self.root), ("SP", 11))
        self.assertTrue(new["docs/tasks/future/later.md"].startswith("docs/tasks/future/SP-"))  # future/ is kept
        self.assertIn("kept in future/, but its status DESIGNED", out)
        moved = read(self.root, new["docs/tasks/in-progress/04i-one-ui.md"])
        self.assertIn("(was `docs/tasks/in-progress/04i-one-ui.md`)", moved)
        self.assertRegex(moved, r"^# SP-\d{5}: 04i: One UI")

    def test_sweep_and_check(self):
        run(self.root, "migrate", "SP", "--apply")
        rows = ticket.read_migrations(self.root, full=True)
        tid = {old: t for old, (t, n) in rows.items()}
        rc, out = run(self.root, "sweep")
        self.assertIn("would replace", out)
        rc, out = run(self.root, "sweep", "--apply", "--show")
        self.assertEqual(rc, 0, out)
        ui = read(self.root, rows["docs/tasks/in-progress/04i-one-ui.md"][1])
        self.assertIn(f"Decisions in the plan ({tid['docs/plans/tunnel.md']}#part-2).", ui)
        self.assertIn(f"Before: 01 ({tid['docs/tasks-archive/2026-10/20261017-01-skeleton.md']}).", ui)
        self.assertIn("(was `docs/tasks/in-progress/04i-one-ui.md`)", ui)  # the record line is never swept
        self.assertIn(f"{tid['docs/tasks/future/zz.md']} (zz)", ui)  # a path-like label becomes number + title
        kt = read(self.root, "core/src/Thing.kt")
        self.assertIn(f"// see {tid['docs/tasks/04d-chatter.md']} (04d chatter), step 3", kt)
        j = json.loads(read(self.root, ".claude/reviews/04i.json"))
        self.assertEqual(j["task"], rows["docs/tasks/in-progress/04i-one-ui.md"][1])  # json keeps a path
        blog = read(self.root, "docs/blog/post.md")
        self.assertIn("https://github.com/x/y/blob/v0.3.3/docs/plans/tunnel.md#L7", blog)  # pinned URL untouched
        self.assertIn(f"and {tid['docs/tasks/future/zz.md']} (zz).", blog)  # a bare, unique file name
        q = read(self.root, "docs/tasks/_maintainer-questions.md")
        self.assertIn(f"Q1 about {tid['docs/tasks/04d-chatter.md']} (04d chatter)\n", q)  # backticks dropped
        # the plan's unknown word and the leftover plans/ folder are what check reports
        sh(self.root, "git", "add", "-A")
        rc, out = run(self.root, "check")
        self.assertEqual(rc, 1)
        self.assertIn("unknown status word AGREED", out)
        self.assertNotIn("has no ticket file", out)
        self.assertNotIn("names a ticket file", out)
        self.assertIn("status DESIGNED belongs in docs/tasks/", out)
        plan = rows["docs/plans/tunnel.md"][1]
        write(self.root, plan, read(self.root, plan).replace("AGREED", "DESIGNED"))
        later = rows["docs/tasks/future/later.md"][1]
        write(self.root, later, read(self.root, later).replace("DESIGNED", "NOT SCHEDULED"))
        self.assertFalse(os.path.exists(os.path.join(self.root, "docs/plans")))  # emptied by migrate
        rc, out = run(self.root, "check")
        self.assertEqual(rc, 0, out)

    def test_find_by_old_name_and_code(self):
        run(self.root, "migrate", "SP", "--apply")
        rc, out = run(self.root, "find", "04i")
        self.assertEqual(rc, 0, out)
        self.assertIn("docs/tasks/in-progress/SP-", out)
        self.assertEqual(len(out.strip().split("\n")), 1)
        rc, out = run(self.root, "find", "20261017-02-sun.md")
        self.assertIn("SP-00004-02-sun.md", out)


class StatusWords(unittest.TestCase):
    def test_words_with_prose_after_them(self):
        cases = {
            "IDEA, the maintainer's; not scheduled.**": "IDEA",
            "DESIGNED with the maintainer; not scheduled.**": "DESIGNED",
            "IN PROGRESS.** Part 1 done.": "IN PROGRESS",
            "NOT SCHEDULED.**": "NOT SCHEDULED",
            "WON'T IMPLEMENT.** Reason.": "WON'T IMPLEMENT",
            "AGREED.** An ordering.": "AGREED",
            "OUT OF SCOPE.**": "OUT OF SCOPE",
            "** decided by the maintainer": None,
        }
        for rest, word in cases.items():
            self.assertEqual(ticket.parse_word(rest), word, rest)


class NewMoveCheck(Repo):
    def test_new_move_and_folders(self):
        rc, out = run(self.root, "init", "KL")
        self.assertEqual(rc, 0, out)
        rc, out = run(self.root, "new", "Engine follow-ups")
        self.assertEqual(out.split()[1], "docs/tasks/future/KL-00001-engine-follow-ups.md")
        rc, out = run(self.root, "move", "KL-00001", "IN", "PROGRESS", "--note", "Started.")
        path = out.strip()
        self.assertEqual(path, "docs/tasks/in-progress/KL-00001-engine-follow-ups.md")
        text = read(self.root, path)
        self.assertIn(f"> **Status ({ticket.today()}): IN PROGRESS.** Started.", text)
        self.assertIn(f"- {ticket.today()}: IN PROGRESS. Started.", text)
        rc, out = run(self.root, "move", "KL-00001", "DONE")
        self.assertEqual(out.strip(), f"docs/tasks-archive/{ticket.today()[:7]}/KL-00001-engine-follow-ups.md")
        self.assertEqual(run(self.root, "check")[0], 0)

    def test_move_replaces_a_two_line_status_and_stages_its_edit(self):
        run(self.root, "init", "KL")
        write(self.root, "docs/tasks/in-progress/KL-00001-shop.md",
              "# KL-00001: Shop\n\n> **Status (2026-10-19): IN PROGRESS.** Waits for the\n"
              "> checkout; it comes before task 05.\n> **Owner:** klang.\n\nBody.\n")
        commit(self.root, "add", "2026-10-19T10:00:00")
        rc, out = run(self.root, "move", "KL-00001", "DONE", "--note", "Shipped.")
        self.assertEqual(rc, 0, out)
        path = out.strip()
        text = read(self.root, path)
        self.assertIn(f"> **Status ({ticket.today()}): DONE.** Shipped.\n> **Owner:** klang.\n", text)
        self.assertNotIn("checkout", text)
        staged = subprocess.run(["git", "show", f":{path}"], cwd=self.root, check=True,
                                capture_output=True, text=True).stdout
        self.assertEqual(staged, text)
        write(self.root, "docs/tasks/in-progress/KL-00002-cart.md",
              "# KL-00002: Cart\n\n> **Status (2026-10-19): IN PROGRESS.** Waits for the\n"
              "> checkout.\n\nBody.\n")
        rc, out = run(self.root, "move", "KL-00002", "PAUSED")
        self.assertIn(f"> **Status ({ticket.today()}): PAUSED.** Waits for the checkout.\n\nBody.",
                      read(self.root, out.strip()))

    def test_epics(self):
        run(self.root, "init", "KL")
        run(self.root, "new", "Editor", "--status", "SCHEDULED")
        run(self.root, "new", "Bookmarks", "--status", "IN PROGRESS", "--part-of", "KL-00001")
        run(self.root, "new", "Key map", "--part-of", "KL-00001")
        rc, out = run(self.root, "new", "Old idea", "--part-of", "KL-00001")
        text = read(self.root, "docs/tasks/in-progress/KL-00002-bookmarks.md")
        self.assertIn("IN PROGRESS.** \n>\n> **Part of:** KL-00001 (Editor)\n", text)
        run(self.root, "move", "KL-00004", "WON'T", "IMPLEMENT", "--note", "Dropped.")
        rc, out = run(self.root, "move", "KL-00002", "DONE")
        self.assertIn("> **Part of:** KL-00001 (Editor)", read(self.root, out.strip()))  # move keeps it
        rc, out = run(self.root, "list", "--epic", "KL-00001")
        self.assertIn("KL-00001 (Editor): 2/3 closed, 0 open, 0 paused, 1 ideas", out)
        rows = {t["id"]: t for t in json.loads(run(self.root, "list", "--json")[1])}
        self.assertEqual((rows["KL-00003"]["status"], rows["KL-00003"]["epic"], rows["KL-00004"]["note"]),
                         ("IDEA", "KL-00001", "Dropped."))
        rc, out = run(self.root, "epics", "--json")
        epic = json.loads(out)[0]
        self.assertEqual((epic["id"], epic["counts"]["total"], len(epic["children"])), ("KL-00001", 3, 3))
        self.assertEqual(run(self.root, "check")[0], 0)
        run(self.root, "move", "KL-00001", "DONE")
        rc, out = run(self.root, "check")
        self.assertEqual(rc, 0, out)  # a DONE epic with open children is a note, not a problem
        self.assertIn("note: KL-00001 is DONE with 1 open child ticket(s)", out)
        # problems: itself, a missing parent, a drifted title, a loop
        write(self.root, "docs/tasks/future/KL-00005-a.md",
              "# KL-00005: A\n\n> **Status (2026-10-21): IDEA.**\n> **Part of:** KL-00005 (A)\n")
        write(self.root, "docs/tasks/future/KL-00006-b.md",
              "# KL-00006: B\n\n> **Status (2026-10-21): IDEA.**\n> **Part of:** KL-00099 (gone)\n")
        write(self.root, "docs/tasks/future/KL-00007-c.md",
              "# KL-00007: C\n\n> **Status (2026-10-21): IDEA.**\n> **Part of:** KL-00001 (Editr)\n")
        write(self.root, "docs/tasks/future/KL-00008-d.md",
              "# KL-00008: D\n\n> **Status (2026-10-21): IDEA.**\n> **Part of:** KL-00009 (E)\n")
        write(self.root, "docs/tasks/future/KL-00009-e.md",
              "# KL-00009: E\n\n> **Status (2026-10-21): IDEA.**\n> **Part of:** KL-00008 (D)\n")
        write(self.root, ticket.COUNTER, "next ticket: KL-00010\n")
        rc, out = run(self.root, "check")
        self.assertEqual(rc, 1)
        self.assertIn("KL-00005-a.md: Part of names the ticket itself", out)
        self.assertIn("KL-00006-b.md: Part of KL-00099, which has no ticket file", out)
        self.assertIn('Part of KL-00001 (Editr), but KL-00001 is titled "Editor"', out)
        self.assertIn("KL-00008-d.md: Part of loops: KL-00008 > KL-00009 > KL-00008", out)

    def test_check_finds_problems(self):
        run(self.root, "init", "KL")
        run(self.root, "new", "One")
        write(self.root, "docs/tasks/future/KL-00001-one.md",
              read(self.root, "docs/tasks/future/KL-00001-one.md").replace("IDEA", "IN PROGRESS"))
        write(self.root, "docs/tasks/in-progress/KL-00002-two.md", "# Two\n\n" + status("PAUSED"))
        write(self.root, "docs/tasks/loose.md", "# Loose\n")
        write(self.root, "src/A.kt", "// KL-00099 and docs/tasks/future/KL-00001-one.md\n")
        sh(self.root, "git", "add", "-A")
        rc, out = run(self.root, "check")
        self.assertEqual(rc, 1)
        self.assertIn("status IN PROGRESS belongs in docs/tasks/in-progress/", out)
        self.assertIn("PAUSED in in-progress/ needs the pause note", out)
        self.assertIn("loose.md: not named KL-nnnnn", out)
        self.assertIn("KL-00099 has no ticket file", out)
        self.assertIn("names a ticket file", out)
        self.assertIn("next is KL-00002, but KL-00002 exists", out)
        write(self.root, "docs/tasks/in-progress/NEXT-STEPS.md", "# Paused\n")
        self.assertNotIn("PAUSED in in-progress", run(self.root, "check")[1])

    def test_skip_glob(self):
        run(self.root, "init", "KL")
        with open(os.path.join(self.root, ticket.COUNTER), "a") as f:
            f.write("skip: docs/tasks/in-progress/*-checklist.md\n")
        write(self.root, "docs/tasks/in-progress/bump-checklist.md", "# Checklist\n")
        self.assertNotIn("checklist", run(self.root, "check")[1])
        run(self.root, "new", "One")
        self.assertIn("skip:", read(self.root, ticket.COUNTER))  # kept when the counter moves

    def test_parallel_new_never_repeats(self):
        run(self.root, "init", "KL")
        cmd = [sys.executable, os.path.join(HERE, "ticket.py"), "--root", self.root, "new"]
        with concurrent.futures.ThreadPoolExecutor(8) as ex:
            outs = list(ex.map(lambda i: (lambda r: r.stdout or r.stderr)(subprocess.run(cmd + [f"t{i}"], capture_output=True, text=True)),
                               range(16)))
        self.assertTrue(all(re.match(r"KL-\d{5} ", o) for o in outs), outs)  # every run succeeded
        ids = [o.split()[0] for o in outs]
        self.assertEqual(len(set(ids)), 16, outs)
        self.assertEqual(ticket.read_counter(self.root), ("KL", 17))

    def test_two_worktrees_never_take_the_same_number(self):
        run(self.root, "init", "KL")
        commit(self.root, "base", "2026-10-19T10:00:00")
        other = os.path.join(self.tmp.name, "proj-worktrees", "measure")
        sh(self.root, "git", "worktree", "add", "-q", "-b", "wt", other)
        rc, a = run(self.root, "new", "On main, not committed")
        rc, b = run(other, "new", "In the worktree, its own counter copy")
        self.assertTrue(a.startswith("KL-00001 "), a)
        self.assertTrue(b.startswith("KL-00002 "), b)  # it saw main's uncommitted file

    def test_number_taken_on_another_branch_is_not_reused(self):
        run(self.root, "init", "KL")
        write(self.root, "README.md", "x\n")
        commit(self.root, "base", "2026-10-19T10:00:00")
        sh(self.root, "git", "checkout", "-q", "-b", "other")
        run(self.root, "new", "On other")
        commit(self.root, "other", "2026-10-19T11:00:00")
        sh(self.root, "git", "checkout", "-q", "main")
        self.assertEqual(ticket.read_counter(self.root)[1], 1)  # main's counter never saw it
        rc, out = run(self.root, "new", "On main")
        self.assertTrue(out.startswith("KL-00002 "), out)


class AdoptAndFixes(Repo):
    def setUp(self):
        super().setUp()
        r = self.root
        body = "".join(f"Line {i} of the weather task, long enough for git to see the rename.\n" for i in range(30))
        write(r, "docs/tasks/04e-weather.md", "# 04e: Weather\n\n" + status("DESIGNED") + body)
        commit(r, "first", "2026-10-18T10:00:00")
        os.makedirs(os.path.join(r, "docs/tasks/in-progress"))
        sh(r, "git", "mv", "docs/tasks/04e-weather.md", "docs/tasks/in-progress/04e-weather.md")
        write(r, "docs/tasks/in-progress/04e-weather.md", "# 04e: Weather\n\n" + status("IN PROGRESS") + body)
        write(r, "docs/research/dossier.md", "Old place: docs/tasks/04e-weather.md. Now `docs/tasks/in-progress/04e-weather.md`.\n")
        write(r, "docs/tasks-archive/2026-08/20260802-foo-array.md", "# Foo\n\n" + status("DONE", "2026-08-02"))
        write(r, "docs/tasks-archive/2026-08/20260802-foo-array-support.md", "# Foo support\n\n" + status("DONE", "2026-08-02"))
        write(r, "src/A.kt", "// see 20260802-foo-array and 20260802-foo-array-support\n")
        commit(r, "second", "2026-10-19T10:00:00")
        run(r, "init", "UL")

    def test_adopt_keeps_the_old_name_and_commits(self):
        rc, out = run(self.root, "adopt", "docs/tasks-archive/2026-08/20260802-foo-array.md", "--apply", "--commit")
        self.assertEqual(rc, 0, out)
        self.assertTrue(os.path.exists(os.path.join(self.root, "docs/tasks-archive/2026-08/UL-00001-20260802-foo-array.md")))
        kt = read(self.root, "src/A.kt")
        self.assertEqual(kt, "// see UL-00001 and 20260802-foo-array-support\n")  # the longer stem is untouched
        self.assertIn("committed", out)
        self.assertEqual(subprocess.run(["git", "status", "--porcelain"], cwd=self.root, capture_output=True,
                                        text=True).stdout, "")

    def test_former_location_and_code_span_resolve(self):
        rc, out = run(self.root, "adopt", "docs/tasks/in-progress/04e-weather.md", "--apply")
        self.assertEqual(rc, 0, out)
        d = read(self.root, "docs/research/dossier.md")
        self.assertEqual(d, "Old place: UL-00001 (04e weather). Now UL-00001 (04e weather).\n")

    def test_records_of_one_day_accumulate(self):
        run(self.root, "adopt", "docs/tasks-archive/2026-08/20260802-foo-array.md", "--apply")
        run(self.root, "adopt", "docs/tasks-archive/2026-08/20260802-foo-array-support.md", "--apply")
        rows = ticket.read_migrations(self.root)
        self.assertEqual(rows["docs/tasks-archive/2026-08/20260802-foo-array.md"], "UL-00001")
        self.assertEqual(rows["docs/tasks-archive/2026-08/20260802-foo-array-support.md"], "UL-00002")

    def test_bulk_migration_records_former_locations(self):
        # 04e-weather.md moved from docs/tasks/ to in-progress/ in history; a bulk migrate must know both
        write(self.root, "docs/research/old.md", "See docs/tasks/04e-weather.md and 04e-weather.md.\n")
        commit(self.root, "ref", "2026-10-19T12:00:00")
        rc, out = run(self.root, "migrate", "UL", "--apply")
        self.assertEqual(rc, 0, out)
        rows = ticket.read_migrations(self.root)
        tid = rows["docs/tasks/in-progress/04e-weather.md"]
        self.assertEqual(rows.get("docs/tasks/04e-weather.md"), tid)  # the former location is a row
        rc, out = run(self.root, "sweep", "--apply")
        self.assertEqual(rc, 0, out)
        # both the old full path and the bare name resolve: every candidate is the same ticket
        self.assertEqual(read(self.root, "docs/research/old.md"),
                         f"See {tid} (04e weather) and {tid} (04e weather).\n")

    def test_adopt_resolves_stale_and_elided_paths_and_commits_only_its_files(self):
        write(self.root, "docs/tasks-archive/2026-10/20261018-docs-kronos.md",
              "# Kronos docs\n\n> **Status (2026-10-18): DONE.** Shipped.\n\nBody of the kronos ticket.\n")
        write(self.root, "README.md", "See `docs/tasks/20261018-docs-kronos.md` and `\u2026/20261018-docs-kronos.md`.\n")
        write(self.root, ".gitignore", "*.ignored\n")
        commit(self.root, "add", "2026-10-18T10:00:00")  # with the counter setUp made
        write(self.root, "notes/scratch.txt", "not this ticket's\n")  # untracked before adopt
        rc, out = run(self.root, "adopt", "docs/tasks-archive/2026-10/20261018-docs-kronos.md", "--apply", "--commit",
                      "--trailer", "Co-Authored-By: Test <t@t>")
        self.assertEqual(rc, 0, out)
        self.assertEqual(read(self.root, "README.md"), "See UL-00001 (docs kronos) and UL-00001 (docs kronos).\n")
        msg = subprocess.run(["git", "log", "-1", "--format=%B"], cwd=self.root, capture_output=True, text=True).stdout
        self.assertIn("Co-Authored-By: Test <t@t>", msg)
        files = subprocess.run(["git", "show", "--name-only", "--format=", "HEAD"], cwd=self.root,
                               capture_output=True, text=True).stdout
        self.assertNotIn("notes/scratch.txt", files)
        self.assertIn("README.md", files)

    def test_root_after_the_subcommand(self):
        out = io.StringIO()
        with redirect_stdout(out):
            rc = ticket.main(["list", "--root", self.root])
        self.assertEqual(rc, 0)


if __name__ == "__main__":
    unittest.main(verbosity=1)
