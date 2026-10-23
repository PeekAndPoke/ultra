#!/usr/bin/env python3
"""Tests for hooks.py, on throwaway git repos with the hooks installed. Run: python3 test_hooks.py"""
import json
import os
import shutil
import subprocess
import tempfile
import unittest

HERE = os.path.dirname(os.path.abspath(__file__))
EM, EN = "\u2014", "\u2013"
TRAILER = "\n\nCo-Authored-By: Claude Test <noreply@anthropic.com>\n"


class Repo:
    def __init__(self, root, agent=False):
        self.root = root
        self.env = {k: v for k, v in os.environ.items() if not k.startswith(("GIT_", "CLAUDECODE"))}
        self.env.update(GIT_AUTHOR_NAME="t", GIT_AUTHOR_EMAIL="t@t", GIT_COMMITTER_NAME="t",
                        GIT_COMMITTER_EMAIL="t@t", HOME=root)
        if agent:
            self.env["CLAUDECODE"] = "1"

    def git(self, *args, ok=True):
        r = subprocess.run(["git", *args], cwd=self.root, env=self.env, capture_output=True, text=True)
        if ok and r.returncode != 0:
            raise AssertionError(f"git {args} failed:\n{r.stdout}{r.stderr}")
        return r

    def write(self, rel, text, mode="w"):
        path = os.path.join(self.root, rel)
        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, mode) as f:
            f.write(text)

    def commit(self, msg="change" + TRAILER, *paths):
        self.git("add", "--", *(paths or ["."]))
        return self.git("commit", "-q", "-m", msg, ok=False)


class HooksTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.mkdtemp(prefix="hooks-test-")
        self.repo = self.make(agent=False)

    def tearDown(self):
        shutil.rmtree(self.tmp)

    def make(self, agent, config=None, name="repo"):
        root = os.path.join(self.tmp, name)
        os.makedirs(root)
        r = Repo(root, agent)
        r.git("init", "-q", "-b", "main")
        tools = os.path.join(root, "console", "tools", "hooks")
        os.makedirs(tools)
        shutil.copy(os.path.join(HERE, "hooks.py"), tools)
        if config:
            r.write("console/tools/hooks/hooks.json", json.dumps(config))
        r.write("docs/old.md", f"# Old\n\nA dated line {EM} kept.\nSecond line.\n")
        r.write("src/A.kt", "/** Old KDoc. */\nclass A\n")
        self.assertEqual(r.commit("initial").returncode, 0)  # before install: old dashes are history
        res = subprocess.run(["python3", "console/tools/hooks/hooks.py", "install"], cwd=root, env=r.env,
                             capture_output=True, text=True)
        self.assertEqual(res.returncode, 0, res.stderr)
        return r

    def blocked(self, res, *needles):
        self.assertNotEqual(res.returncode, 0, "commit went through")
        for n in needles:
            self.assertIn(n, res.stderr)

    def passed(self, res):
        self.assertEqual(res.returncode, 0, res.stderr)

    # pre-commit: dashes in Markdown

    def test_added_dash_in_markdown_blocks(self):
        self.repo.write("docs/new.md", f"# New\n\nA claim {EM} with a dash.\n")
        self.blocked(self.repo.commit(), "docs/new.md:3: em dash in Markdown")

    def test_en_dash_blocks(self):
        self.repo.write("docs/new.md", f"pages 3{EN}5\n")
        self.blocked(self.repo.commit(), "en dash")

    def test_old_dash_line_untouched_passes(self):
        self.repo.write("docs/old.md", f"# Old\n\nA dated line {EM} kept.\nSecond line, edited.\n")
        self.passed(self.repo.commit())

    def test_editing_the_dash_line_blocks(self):
        self.repo.write("docs/old.md", f"# Old\n\nA dated line {EM} kept, edited.\nSecond line.\n")
        self.blocked(self.repo.commit(), "docs/old.md:3")

    def test_code_url_and_escape_pass(self):
        self.repo.write("docs/new.md", "\n".join([
            "```", f"a {EM} in a fence", "```",
            f"inline `a {EM} b` code",
            f"see https://example.org/a{EN}b for more",
            f'"A Title {EM} Quoted" <!-- dash-ok -->', ""]))
        self.passed(self.repo.commit())

    def test_dash_skip(self):
        r = self.make(agent=False, config={"dash_skip": r"ledger[^/]*\.md$"}, name="skip")
        r.write("docs/ledger-2026.md", f"row {EM} copied verbatim\n")
        self.passed(r.commit())

    def test_dashes_switched_off(self):
        r = self.make(agent=False, config={"checks": {"dashes": False}}, name="off")
        r.write("docs/new.md", f"a {EM} b\n")
        self.passed(r.commit())

    # pre-commit: KDoc

    def test_kdoc_dash_blocks(self):
        self.repo.write("src/B.kt", f"/**\n * Does a thing {EM} quickly.\n */\nfun b() = 1\n")
        self.blocked(self.repo.commit(), "src/B.kt:2: em dash in KDoc")

    def test_dash_outside_kdoc_passes(self):
        self.repo.write("src/B.kt", f'// line comment {EM}\nval s = "{EN}"\n/* block {EM} */\n')
        self.passed(self.repo.commit())

    # pre-commit: control bytes

    def test_nul_in_markdown_blocks(self):
        self.repo.write("docs/new.md", "text\x00more\n")
        self.blocked(self.repo.commit(), "docs/new.md:1: control byte 0x00")

    def test_escape_byte_in_kotlin_blocks(self):
        self.repo.write("src/C.kt", "val x = 1\x1b[0m\n")
        self.blocked(self.repo.commit(), "control byte 0x1b")

    def test_tab_and_crlf_pass(self):
        self.repo.write("docs/new.md", "a\tb\r\nc\r\n")
        self.passed(self.repo.commit())

    def test_binary_file_passes(self):
        self.repo.write("img/x.png", b"\x89PNG\r\n\x1a\n\x00\x00\x01\x02" * 50, mode="wb")
        self.passed(self.repo.commit())

    # commit-msg

    def test_message_dash_blocks(self):
        self.repo.write("docs/new.md", "fine\n")
        self.blocked(self.repo.commit(f"Add a thing {EM} now"), "message line 1: em dash")

    def test_message_comment_lines_skipped(self):
        self.repo.write("docs/new.md", "fine\n")
        path = os.path.join(self.tmp, "msg")
        with open(path, "w") as f:
            f.write(f"Add a thing\n# a comment {EM}\n")
        self.repo.git("add", ".")
        self.passed(self.repo.git("commit", "-q", "--cleanup=strip", "-F", path, ok=False))

    def test_trailer_not_required_for_the_maintainer(self):
        self.repo.write("docs/new.md", "fine\n")
        self.passed(self.repo.commit("No trailer"))

    def test_trailer_required_in_an_agent_session(self):
        r = self.make(agent=True, name="agent")
        r.write("docs/new.md", "fine\n")
        self.blocked(r.commit("No trailer"), "no trailer")
        self.passed(r.commit("With trailer" + TRAILER))

    def test_merge_commit_exempt_and_branch_lines_not_rechecked(self):
        r = self.make(agent=True, name="merge")
        r.git("checkout", "-q", "-b", "side")
        r.write("docs/side.md", "side line\n")
        self.passed(r.commit("side" + TRAILER))
        # a dash that reached the side branch before the hooks (or from a skip-listed copy)
        r.write("docs/side2.md", f"history {EM} line\n")
        r.git("add", ".")
        r.git("-c", "core.hooksPath=/dev/null", "commit", "-q", "-m", "pre-hook history")
        r.git("checkout", "-q", "main")
        r.write("docs/main.md", "main line\n")
        self.passed(r.commit("main" + TRAILER))
        res = r.git("merge", "--no-ff", "-q", "side", "-m", "Merge branch 'side'", ok=False)
        self.passed(res)

    # check

    def test_check_reports_on_a_commit(self):
        self.repo.write("docs/new.md", f"a {EM} b\n")
        self.repo.git("add", ".")
        self.repo.git("-c", "core.hooksPath=/dev/null", "commit", "-q", "-m", "unchecked")
        res = subprocess.run(["python3", "console/tools/hooks/hooks.py", "check"], cwd=self.repo.root,
                             env=self.repo.env, capture_output=True, text=True)
        self.blocked(res, "docs/new.md:1: em dash")

    def test_install_refuses_a_foreign_hook(self):
        hooks = self.repo.git("rev-parse", "--git-path", "hooks").stdout.strip()
        with open(os.path.join(self.repo.root, hooks, "pre-commit"), "w") as f:
            f.write("#!/bin/sh\necho mine\n")
        res = subprocess.run(["python3", "console/tools/hooks/hooks.py", "install"], cwd=self.repo.root,
                             env=self.repo.env, capture_output=True, text=True)
        self.assertEqual(res.returncode, 1)
        self.assertIn("not ours", res.stderr)


if __name__ == "__main__":
    unittest.main()
