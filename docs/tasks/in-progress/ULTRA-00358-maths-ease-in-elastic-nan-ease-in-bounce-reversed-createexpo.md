# ULTRA-00358: Maths: Ease.In.elastic NaN, Ease.In.bounce reversed, createExponential ignores lambda

> **Status (2026-11-25): DONE.** Gate PASS in round 1 (no findings); merged into master.

## Why this exists

Found by the ULTRA-00356 truth audit, confirmed by its reviewer (ultra/maths/src/commonMain/kotlin/):
1. `Ease.In.elastic` is `(-2.0).pow(10.0 * x - 10.0) * sin(...)` (Ease.kt:172). A negative base to a non-whole power is
   NaN, so the curve is NaN for most x in (0, 1); at whole exponents the odd ones flip the sign. Out and InOut use
   `2.0.pow`. The test at 0.5 passes only because the exponent there is whole.
2. `Ease.In.bounce` is `Out.bounce(1 - x)` (Ease.kt:177): it runs from 1.0 down to 0.0. The standard form is
   `1 - Out.bounce(1 - x)`. `EasingSpec` pins the wrong values.
3. `BucketedDistribution.createExponential(lambda, tailCut, ...)` computes `xMax = -ln(tailCut) / lambda` and
   `pdf = exp(-lambda * x * xMax)`, which is `tailCut^x`: `lambda` cancels and only `tailCut` sets the steepness
   (stochastic/BucketedDistribution.kt:86-97). `BucketedDistributionSpec` passes regardless (fresh `Random(42)` per sample,
   compared with `<=`).

## Decisions

- Maintainer (2026-11-25): fix all three. Behaviour change for callers of the broken curves.
- createExponential's new meaning: to be settled with the maintainer before that part is implemented (the implementer
  proposes; samples stay in 0..1).
- Maintainer (2026-11-25), via the coordinator, verbatim: option A, "lambda only, default ln(1000)". pdf proportional
  to exp(-lambda * x) on [0, 1]; tailCut is removed (breaking); the default lambda = ln(1000) so the default curve
  stays exactly as today.

## The work

- Tests first, RED against the current code: elastic finite on a grid of x in [0, 1], matching the standard
  easings.net curve at a few points, In.elastic(0) = 0 and (1) = 1; In.bounce(0) = 0, (1) = 1 and
  In.bounce(x) = 1 - Out.bounce(1 - x); createExponential with a larger lambda has a steeper PDF (and a smaller mean),
  measured on the PDF or over many samples from one Random.
- Fix the two easings; correct the `EasingSpec` values that pin the wrong curve.
- createExponential per the decision above.
- Docs: drop the defect notes on docs-site/src/pages/ultra/maths/easing.astro, distributions.astro and the LLM mirror
  docs-site/src/data/llms/maths.md, and describe lambda as it then is.
- Mutation check; `:ultra:maths` tests green.

## Review ledger

Round 1, standard gate, one reviewer covering the three charters (implementation and code style, domain, security),
rung 1 reviewer-opus-high (Opus high while the Fable credits are out). Verdict: PASS, NO FINDINGS.

CLEAN: In.elastic and In.bounce equal the easings.net curves (7 and 5 points checked by hand, mirror identities exact);
the default createExponential equals the old default PDF up to one ulp of ln; every new test's expected value is
computed independently (constants, closed-form truncated mean, 8 sigma margin, one Random); extreme finite lambda gives
no NaN or loop; NaN, zero, negative and infinite lambda are rejected; docs true, no tailCut or defect text left.
Kept beyond the decision: the `isFinite()` check (fails at the call instead of later). The docs were updated in this
ticket, so no DOCS follow-up is needed.

## Acceptance

- In.elastic is finite and correct on [0, 1]; In.bounce runs 0 to 1; createExponential's lambda changes the
  distribution as documented; the docs say so.

## Implementation notes

Easings (items 1 and 2), branch wt/maths-fixes:
- ultra/maths/src/commonMain/kotlin/Ease.kt:172: `In.elastic` is now `-(2.0.pow(10.0 * x - 10.0)) * sin(...)`, the
  easings.net form. Identity: In.elastic(x) = 1 - Out.elastic(1 - x).
- ultra/maths/src/commonMain/kotlin/Ease.kt:177: `In.bounce` is now `1.0 - Out.bounce(1.0 - x)`.
- ultra/maths/src/commonTest/kotlin/EasingSpec.kt:187: the In.bounce table pinned the reversed curve; its values are
  now 1 minus the old ones (0.0 at 0, 1.0 at 1).
- EasingSpec.kt:207 (In.bounce endpoints and mirror of Out.bounce on a 101 point grid), :336 (In.elastic finite on a
  1001 point grid), :344 (In.elastic against easings.net values at 0.1, 0.25, 0.5, 0.7, 0.75, 0.9, 0.95), :363
  (In.elastic mirrors Out.elastic on a 101 point grid).
- ultra/maths/src/commonTest/kotlin/EaseCalcEdgeCasesSpec.kt:83: In.bounce joins the "0 at 0 and 1 at 1" list (it was
  excluded with a comment naming the bug).

Docs (easings only): docs-site/src/pages/ultra/maths/easing.astro:99,103 and docs-site/src/data/llms/maths.md:18
drop the In.elastic and In.bounce defect notes. `pnpm astro build`: 118 pages, clean. The createExponential notes on
distributions.astro and maths.md stay until item 3 lands.

createExponential (item 3), implemented as option A (see Decisions):
- ultra/maths/src/commonMain/kotlin/stochastic/BucketedDistribution.kt:77: `createExponential(lambda = ln(1000.0),
  count)`, pdf `exp(-lambda * xs[i])` on the [0, 1] grid; `tailCut` removed (breaking); `require(lambda > 0.0 &&
  lambda.isFinite())` (Infinity used to reach an IllegalStateException from the CDF).
- Tests: BucketedDistributionSpec.kt:183 (pdf equals exp(-lambda x) for lambda 0.5, 3, 12), :198 (default equals the
  former default curve 1e-3^x on 100 buckets; a regression guard, green before and after), :210 (replaces the vacuous
  test: larger lambda gives a strictly lower pdf at every x > 0, and over 50k samples from ONE Random(42) each mean
  is within 0.01 of the analytic truncated mean and the means differ by more than 0.1).
  BucketedDistributionEdgeCasesSpec.kt:63 (replaces the tailCut range checks: NaN and Infinity lambda throw
  IllegalArgumentException).
- Docs: distributions.astro:84-118 (lambda as the rate on [0, 1], default about 6.91, tailCut row replaced by count),
  maths.md:80-83. index.astro:72 and distributions.astro:146 (`lambda = 2.0`) stay as they are: valid code, and
  lambda now does what they suggest. `pnpm astro build`: 118 pages, clean.

The proposal as handed to the maintainer:
- The shape on 0..1 is exp(-k x), one degree of freedom. Today k = ln(1 / tailCut) (default 6.91, mean 0.144) and
  `lambda` cancels. Any two parameter form stays redundant.
- A (recommended): pdf proportional to exp(-lambda x) on [0, 1]; `lambda` is the rate on the unit interval; `tailCut`
  removed (the cut is always at 1, mass beyond it would be exp(-lambda)). Default lambda: ln(1000) keeps today's
  default curve; 1.0 would turn the default nearly flat (mean 0.418).
- B (signature kept): pdf = exp(-lambda * ln(1 / tailCut) * x); default unchanged, lambda multiplies steepness,
  tailCut stays as a second, redundant knob.
- Callers that change: none in production (grep over /opt/dev, kraft-examples and klang included; gfx-playground has
  its own copy). In ultra: docs examples with lambda = 2.0 (distributions.astro:146, index.astro:72; A: mean 0.144 to
  0.344, B: to 0.072), the tailCut example (distributions.astro:94), maths.md:85-87, tests
  BucketedDistributionSpec:155-193 and BucketedDistributionEdgeCasesSpec:63-69. Persisted distributions keep their
  stored pdf.
- The lambda test at BucketedDistributionSpec:181 is vacuous: a fresh Random(42) per sample makes every sample equal
  and `<=` accepts equal means. It gets rewritten either way.

## Test evidence

- RED before the fix (`:ultra:maths:jvmTest`, XML): EasingSpec 32 tests, 5 failures; EaseCalcEdgeCasesSpec 20 tests,
  1 failure. Reasons: In.elastic(0.001) not finite, In.elastic(0.25) NaN, In.bounce(0.0) is 1.0.
- Green after the fix, from the XML: jvmTest 133/133, linuxX64Test 133/133, jsBrowserTest 133/133 (plus the Kotest
  placeholder), via `console/wt-gradle.sh :ultra:maths:allTests`.
- Mutants (jvmTest, cp backup, restored, cmp identical):
  - In.elastic without the leading minus: killed (easings.net values, mirror test).
  - In.bounce as `Out.bounce(x)` (right endpoints, wrong curve): killed (table, mirror test).
  - In.bounce back to `Out.bounce(1.0 - x)`: killed (table, mirror test, EaseCalcEdgeCasesSpec endpoints).

- createExponential RED before the change (jvmTest, XML: 135 tests, 3 failures): pdf at x = 0.02 was 0.871 where
  exp(-0.5 * 0.02) = 0.990 (lambda ignored); lambda 1 and 5 gave identical pdfs; Infinity threw IllegalStateException.
- createExponential mutants (jvmTest, cp backup, restored, cmp identical), all killed:
  - lambda ignored (rate fixed at ln(1000)): 2 failures (pdf formula, steeper and smaller mean).
  - default 1.0: 1 failure (default curve).
  - no isFinite check: 1 failure (non-finite lambda).
  - sign flipped, exp(+lambda x): 4 failures.
- After item 3, from the XML (`console/wt-gradle.sh :ultra:maths:allTests`): jvmTest 135/135, linuxX64Test 135/135,
  jsBrowserTest 136/136 (135 plus the Kotest placeholder).

## History

- 2026-11-25: created on the maintainer's decision.
- 2026-11-25: IN PROGRESS. easing fixes landed; createExponential awaits the maintainer's decision
- 2026-11-25: IN PROGRESS. Easing fixes (In.elastic, In.bounce) with tests and mutants; createExponential proposal
  pending the maintainer.
- 2026-11-25: easing defect notes dropped from the docs and LLM mirror; createExponential proposal recorded.
- 2026-11-25: maintainer chose option A; createExponential is lambda only (default ln(1000)), tailCut removed, tests,
  mutants and docs done.
- 2026-11-25: gate PASS round 1, no findings; merged into master.
