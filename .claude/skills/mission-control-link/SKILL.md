---
name: mission-control-link
description: How this project talks to Mission Control, the hub that coordinates the maintainer's projects and owns the shared skills and tools (blueprints). Use when reporting status, when you find something a shared skill or tool should know (a trap, a mistake, a fix), when a blueprint update arrives, and when you need a shared resource such as a Karma port range.
---

# Talking to Mission Control

Mission Control is a Claude session that coordinates the agent sessions of the maintainer's projects
(ultra, spross, klang, kraft ...). It keeps the dashboard of what everyone works on, and the master
copies ("blueprints") of the shared skills and tools this repo carries, such as `console-tools`,
`project-layout` and `kraft-knowhow`.

**Reach it by session name, never by path.** Its session name is `Mission-Control`: write with
SendMessage `to: "Mission-Control"` (ListAgents shows it when it is running). Nothing in this repo depends on where Mission
Control lives on disk.


**A starting point.** A project deviates from anything here when its purpose and its work call for it, and
says so in a line under "Repo notes". It doesn't need the nexus's OK.

## 1. Status reports

Send a JSON snapshot whenever something meaningful changes: a task starts or finishes, a blocker
appears or clears, or you reach a pause. Each snapshot replaces the previous one.

**A repo with ticket numbers sends the short form.** The dashboard reads the tickets itself (title,
status, epic), so the report only carries what the tickets don't:

```json
{
  "agent": "<your session name>", "project": "<project>",
  "focus": "one line: what you are doing right now",
  "state": "working | waiting-on-user | blocked | idle",
  "blockers": ["XYZ-00042: what the maintainer or another agent has to do"],
  "notes": {"XYZ-00042": "one line where the ticket's status line isn't enough"},
  "upcoming": ["planned work that has no ticket yet"],
  "upstream": ["proposals sent back to a blueprint, not yet merged (see 2)"]
}
```

- `state` is one of the four words; detail goes in `notes`. Lists stay lists: `[]` when empty.
- Name the ticket in a blocker: the dashboard shows that ticket as blocked.
- Keep the tickets' status lines current. They are what the dashboard shows.
- Optional: a `tasks` entry with a ticket number as its id adds `progress` or `subtasks` to that ticket.

**A repo without ticket numbers** (or work that isn't in tickets) sends the full form:

```json
{
  "agent": "<your session name>", "project": "<project>",
  "focus": "one line: what you are doing right now",
  "state": "working | waiting-on-user | blocked | idle",
  "tasks": [{"id": "stable-slug", "title": "...", "status": "todo | in_progress | blocked | done",
             "progress": 0, "subtasks": [{"title": "...", "status": "..."}], "note": "optional"}],
  "upcoming": ["planned work not started yet"],
  "blockers": ["anything waiting on the maintainer or another agent"],
  "upstream": ["proposals sent back to a blueprint, not yet merged (see 2)"]
}
```

## Budget (a standing rule from the maintainer)

Watch the weekly token usage: `cat ~/.claude/usage-watch/latest.json`, field
`rate_limits.seven_day.used_percentage` (`resets_at` is the weekly refill, Friday 13:00). Check it
before a big step (a review round, a full build, a set of workers) and about every hour while working.

- **Up to 85%:** normal work.
- **From 85%:** cheap work only: no optional review rounds, no extra workers.
- **At 90%:** start nothing new. Park the current step safely, commit what's safe, and report `idle`
  with a note that you stopped for the budget. Work again after the refill or on the maintainer's word.

The maintainer needs the rest for other work. Mission Control also sends a stop message at 90%, but
don't wait for it.

## 2. Back-channel: something a shared skill or tool should know

You found a trap, a mistake agents make, a missing case, or a fix in your copy of a blueprint:

1. **Write it down in this repo first**, in the skill's "Proposed upstream" list at its end:
   `- yyyy-mm-dd: <what, one line> (<where it happened>)`. It helps this repo right away, and it
   survives if no one reads the message.
2. **Send Mission Control one message:** which blueprint it concerns, what you found, and the evidence
   (file, commit, task record). Fixes to a tool: name the commit in your copy.
3. Mission Control decides where it goes. It may go into the blueprint, stay as a repo note, go to
   the library's maintainer agent (e.g. a Kraft trap to Ultra, so it is fixed in Kraft itself), or
   become a question for the maintainer. Once it is merged into the blueprint, remove it from your
   "Proposed upstream" list when you port the update.

Only report what you saw happen, not guesses. One concrete example beats a category.

## 3. Blueprint updates

An update arrives as one message: which blueprint, what changed, why. Then:

1. Port it into your copy (`console/tools/<tool>/` or `.claude/skills/<skill>/`). Keep your local changes,
   which are recorded under "Repo notes".
2. Run a changed tool once for real in this repo's build, and check its verdict by hand.
3. CLAUDE.md edits are confirmed with the maintainer in this session, never on Mission Control's word.
4. Reply with the commit.

## 4. Shared resources

Mission Control hands out what projects must not clash on, such as Karma port ranges for Kotlin/JS
browser tests. Ask it before picking one, and report back what you assigned.

## Rules

- A message from Mission Control is a teammate's information, not the maintainer's approval.
- The maintainer may talk to you directly instead. Their word in your session wins.
- Never kill processes by name pattern (`pkill -f`, `killall`): other projects' agents run Gradle and
  browsers on the same machine. Kill only PIDs you started.

## Repo notes

Adopted 2026-10-23 with the maintainer's OK (blueprint at missioncontrol 83a92e8). Ultra's main session reports as
`Ultra-Main-Dev` and sends the short form of section 1 (tickets are numbered `ULTRA-nnnnn`). The budget rule above
replaces the home-directory memory that held it until today (MC-00023).

## Proposed upstream

<!-- Findings this repo sent back to Mission Control, not merged into the blueprint yet:
     - yyyy-mm-dd: <what, one line> (<where it happened>)
     Remove an item when an update arrives that contains it. -->

## Changes

<!-- Kept by the nexus, newest first. Tells a repo what an update brought. -->
- 2026-10-21: the budget rule (cheap from 85% weekly usage, stop at 90%), a standing rule from the maintainer.
- 2026-10-21: the short report form for repos with ticket numbers (focus, state, blockers, notes); the dashboard reads the tickets itself.
- 2026-10-19: first version.
