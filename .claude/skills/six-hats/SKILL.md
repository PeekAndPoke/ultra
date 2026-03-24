---
name: six-hats
description: "Use when someone asks for a six thinking hats analysis, six hats method, thinking hats brainstorm, or multi-perspective analysis on a topic."
argument-hint: "[ topic or brief to analyze ]"
---

## What This Skill Does

Runs Edward de Bono's Six Thinking Hats method on any topic as a **3-round discussion.** You (Claude) act as the
**White Hat** — the fact-gatherer and coordinator. You launch 5 parallel agents (Red, Black, Yellow, Green, Blue),
let them hear each other's positions, and facilitate 3 rounds of debate before synthesizing.

## The Six Hats

| Hat    | Role                  | Focus                                             |
|--------|-----------------------|---------------------------------------------------|
| White  | **You (coordinator)** | Facts, data, what we know and don't know          |
| Red    | Agent                 | Gut feelings, emotions, user empathy, intuition   |
| Black  | Agent                 | Risks, problems, obstacles, what could go wrong   |
| Yellow | Agent                 | Benefits, opportunities, value, what's exciting   |
| Green  | Agent                 | Creative ideas, alternatives, wild angles         |
| Blue   | Agent                 | Process, priorities, next steps, what to do first |

## Steps

### Step 1: Determine the topic

Use `$*` as the topic if provided. If no argument is given, ask the user what topic they want to analyze.

### Step 2: Gather White Hat facts

Before launching the agents, briefly state the **known facts and context** about the topic. This is your White Hat
contribution. Keep it to 3-5 bullet points. If the topic relates to the codebase, do a quick search to ground your
facts in reality.

### Step 3: Round 1 — Opening Positions (5 agents in parallel)

Launch all 5 agents **in a single message** so they run concurrently. Use `subagent_type: "general-purpose"` for each.

Each agent gets this brief structure:

```
TOPIC: [the topic]

CONTEXT (White Hat facts):
[your bullet points from Step 2]

YOUR ROLE: You are the [COLOR] Hat in a Six Thinking Hats discussion (Round 1 of 3).
[hat-specific persona below]

ROUND 1 TASK: State your opening position on this topic.

RULES:
- Stay strictly in your hat's perspective. Do not cover other hats' territory.
- Be specific and concrete, not generic. Relate everything to THIS topic.
- Keep your response to 150-250 words.
- Use bullet points for clarity.
- End with your single strongest point, marked as "KEY TAKEAWAY:"
```

**Hat-specific personas (used in all rounds):**

**Red Hat (Emotions & Intuition):**
> You represent gut feelings, emotions, and intuition. No justification needed — just honest reactions.
> Think about: How does this feel to use? What would excite or frustrate users? What is your instinctive
> reaction? What would users LOVE? What would make them quit in frustration? Channel the voice of someone
> encountering this for the first time.

**Black Hat (Risks & Problems):**
> You are the critical thinker. Find problems, risks, and obstacles.
> Think about: What could go wrong? What are the technical risks? Where will users get confused or stuck?
> What assumptions are we making that might be wrong? What has failed when others tried this? Be specific
> about failure modes, not vaguely negative.

**Yellow Hat (Benefits & Value):**
> You are the optimist. Find the value, benefits, and opportunities.
> Think about: What's the best outcome if this works? Who benefits and how? What doors does this open?
> What competitive advantage does this create? What existing strengths does this build on? Be genuinely
> enthusiastic but grounded — no empty hype.

**Green Hat (Creativity & Alternatives):**
> You are the creative thinker. Generate novel ideas, alternatives, and unexpected angles.
> Think about: What if we approached this completely differently? What would a 10x version look like?
> What ideas can we steal from other domains? What's the weirdest approach that might work? What
> combinations haven't been considered? Push beyond the obvious. At least one idea should be surprising.

**Blue Hat (Process & Priorities):**
> You are the organizer. Think about execution, sequencing, and priorities.
> Think about: What should be built first? What are the dependencies? What's the MVP vs the full vision?
> How do we validate this works before going all-in? What milestones would show progress? What decisions
> need to be made before building? Create actionable structure.

### Step 4: Round 2 — Reactions & Debate (5 agents in parallel)

After Round 1 completes, launch all 5 agents again **in a single message.** Each agent now receives
**all Round 1 outputs** from every hat, and must react to the discussion.

Each agent gets this brief structure:

```
TOPIC: [the topic]

CONTEXT (White Hat facts):
[your bullet points from Step 2]

YOUR ROLE: You are the [COLOR] Hat in a Six Thinking Hats discussion (Round 2 of 3).
[same hat-specific persona as Round 1]

## Round 1 Positions (all hats):

### Red Hat said:
[paste Red's Round 1 output]

### Black Hat said:
[paste Black's Round 1 output]

### Yellow Hat said:
[paste Yellow's Round 1 output]

### Green Hat said:
[paste Green's Round 1 output]

### Blue Hat said:
[paste Blue's Round 1 output]

ROUND 2 TASK: React to what the other hats said. From YOUR hat's perspective:
- What do you agree with? What do you push back on?
- Which ideas from other hats change or strengthen your position?
- Where do you see conflicts that need resolving?
- Refine your position based on the discussion so far.

RULES:
- Stay in your hat's perspective. React to others through YOUR lens.
- Reference specific points from other hats by name (e.g., "Black raises a valid concern about X, but...").
- Keep your response to 150-250 words.
- End with "REFINED POSITION:" — a single sentence capturing your updated stance.
```

### Step 5: Round 3 — Convergence (5 agents in parallel)

After Round 2 completes, launch all 5 agents one final time **in a single message.** Each agent receives
**all Round 2 outputs** and must converge toward actionable conclusions.

Each agent gets this brief structure:

```
TOPIC: [the topic]

CONTEXT (White Hat facts):
[your bullet points from Step 2]

YOUR ROLE: You are the [COLOR] Hat in a Six Thinking Hats discussion (Round 3 of 3 — Final Round).
[same hat-specific persona as Round 1]

## Round 2 Reactions (all hats):

### Red Hat said:
[paste Red's Round 2 output]

### Black Hat said:
[paste Black's Round 2 output]

### Yellow Hat said:
[paste Yellow's Round 2 output]

### Green Hat said:
[paste Green's Round 2 output]

### Blue Hat said:
[paste Blue's Round 2 output]

ROUND 3 TASK: This is the final round. Deliver your concluding position:
- What is your hat's definitive recommendation?
- What one thing MUST happen based on this discussion?
- What was the most important insight that emerged from the debate?

RULES:
- Stay in your hat's perspective.
- Be concise and decisive — this is your final word.
- Keep your response to 100-150 words.
- End with "FINAL VERDICT:" — your single most important recommendation in one sentence.
```

### Step 6: Synthesize

After all 3 rounds complete, write the final synthesis. Structure it as follows:

```markdown
# Six Hats Analysis: [Topic]

**Date:** [current date]
**Rounds:** 3

---

## White Hat — Facts & Context

[Your bullet points from Step 2]

---

## Discussion Summary

### Round 1 — Opening Positions

#### Red Hat — Feelings & Intuition

[3-4 key bullets from Round 1]

#### Black Hat — Risks & Problems

[3-4 key bullets from Round 1]

#### Yellow Hat — Benefits & Value

[3-4 key bullets from Round 1]

#### Green Hat — Creative Ideas

[3-4 key bullets from Round 1]

#### Blue Hat — Process & Next Steps

[3-4 key bullets from Round 1]

### Round 2 — Key Debates

Summarize the most interesting exchanges between hats. Focus on:

- Where hats challenged each other
- Where positions shifted
- Where creative tension produced new insights

Write this as a narrative (2-3 paragraphs), not bullet points. Name the hats as speakers.

### Round 3 — Final Verdicts

| Hat | Final Verdict |
|-----|---------------|
| Red | [one sentence] |
| Black | [one sentence] |
| Yellow | [one sentence] |
| Green | [one sentence] |
| Blue | [one sentence] |

---

## Synthesis

[2-3 paragraphs weaving together the strongest insights from all 3 rounds.
Highlight how positions evolved through debate.
Identify the consensus that emerged AND the unresolved tensions.
End with a clear recommendation or set of options for the user.]

## Recommended Actions

[3-5 concrete, prioritized next steps that emerged from the discussion]
```

### Step 7: Save results

If the user specified an output location, save the synthesis there. Otherwise, present it in the conversation.

## Notes

- All 5 agents in each round MUST be launched in a single message for parallel execution.
- Rounds are sequential — Round 2 depends on Round 1, Round 3 depends on Round 2.
- Brief the user between rounds: "Round 1 complete, launching Round 2..." so they know progress is happening.
- Do NOT summarize what you're about to do at the start — just do it. The user knows what six hats means.
- If the topic is about the Klang project or codebase, ground your White Hat facts in actual code/architecture.
- The Round 2 debate summary is the most valuable part — this is where hats challenge each other and ideas evolve. Spend
  effort making it insightful, not just a recap.
- Keep agent prompts focused. Include only the previous round's outputs, not the full history of all rounds (to avoid
  context bloat).
