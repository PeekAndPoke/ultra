# ULTRA-00358: Maths: Ease.In.elastic NaN, Ease.In.bounce reversed, createExponential ignores lambda

> **Status (2026-11-25): DESIGNED.** Maintainer: "Fix all three"; implementation next.

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

## History

- 2026-11-25: created on the maintainer's decision.
