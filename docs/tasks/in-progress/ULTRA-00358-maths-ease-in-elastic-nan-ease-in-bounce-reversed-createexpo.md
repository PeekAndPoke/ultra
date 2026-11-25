# ULTRA-00358: Maths: Ease.In.elastic NaN, Ease.In.bounce reversed, createExponential ignores lambda

> **Status (2026-11-25): IN PROGRESS.** easing fixes landed; createExponential awaits the maintainer's decision

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

createExponential (item 3): not implemented; proposal for the maintainer:
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

## History

- 2026-11-25: created on the maintainer's decision.
- 2026-11-25: IN PROGRESS. easing fixes landed; createExponential awaits the maintainer's decision
- 2026-11-25: IN PROGRESS. Easing fixes (In.elastic, In.bounce) with tests and mutants; createExponential proposal
  pending the maintainer.
- 2026-11-25: easing defect notes dropped from the docs and LLM mirror; createExponential proposal recorded.
