#!/usr/bin/env python3
"""PreToolUse gate for mcp__idea__invoke_ide_action: listed IDE actions run without a prompt, every other
action asks the maintainer (interactive: a prompt; headless: refused).

Permission rules see only a tool's name, so "invoke_ide_action, but only Optimize Imports" needs this hook.
The allowlist is `idea-actions.allow` next to this file: one action id per line, `#` comments. A project adds
an action deliberately, and the diff shows it. The hook makes BOTH decisions: an explicit `ask` rule in the
permissions would outrank the hook's allow (measured 2026-10-24), so the baseline carries no rule for this
tool; the hook is the rule.

Settings (tracked .claude/settings.json, part of the command baseline from revision 5):
  "hooks": {"PreToolUse": [{"matcher": "mcp__idea__invoke_ide_action",
            "hooks": [{"type": "command", "command": "python3 \\"$CLAUDE_PROJECT_DIR\\"/.claude/hooks/idea-action-gate.py"}]}]}
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))


def allowed_actions():
    path = os.path.join(HERE, "idea-actions.allow")
    if not os.path.exists(path):
        return set()
    with open(path) as f:
        return {l.strip() for l in f if l.strip() and not l.strip().startswith("#")}


def main():
    try:
        event = json.load(sys.stdin)
    except json.JSONDecodeError:
        return 0  # no decision: the permission rules decide
    if event.get("tool_name") != "mcp__idea__invoke_ide_action":
        return 0
    action = (event.get("tool_input") or {}).get("actionId", "")
    listed = action in allowed_actions()
    json.dump({"hookSpecificOutput": {"hookEventName": "PreToolUse",
               "permissionDecision": "allow" if listed else "ask",
               "permissionDecisionReason": (f"IDE action {action} is on .claude/hooks/idea-actions.allow" if listed else
                                            f"IDE action {action!r} is not on .claude/hooks/idea-actions.allow: "
                                            "the maintainer decides")}}, sys.stdout)
    return 0


if __name__ == "__main__":
    sys.exit(main())
