# Maths — Easing, Smoothing & Distributions

Easing functions, smoothing laws and bucketed probability distributions for Kotlin Multiplatform. Depends on
`ultra:datetime` (for `Ease.timed`) and kotlinx.serialization; no global state.

- Package: `io.peekandpoke.ultra:maths`
- Version: {{ultraVersion}}
- Platforms: JVM, JS, Native (linuxX64, linuxArm64, macosArm64, mingwX64)
- Docs: https://peekandpoke.io/ultra/maths/

## Easing

`Ease.Fn` is a functional interface taking progress `0.0..1.0` and returning the eased value.
Curves are grouped as `Ease.In`, `Ease.Out`, `Ease.InOut` (sine, quad, cubic, quart, quint, expo,
circ, back, elastic, bounce), plus `Ease.linear`, `Ease.stuck` (always 0.0) and `Ease.immediate`
(always 1.0).

```kotlin
import io.peekandpoke.ultra.maths.Ease
import io.peekandpoke.ultra.maths.Ease.bindFromTo   // member extension — import required
import io.peekandpoke.ultra.maths.Ease.timed        // member extension — import required
import kotlin.time.Duration.Companion.milliseconds

Ease.Out.cubic(0.5)

// bind to a numeric range
val bound = Ease.InOut.sine.bindFromTo(0, 100)
bound(0.5)

// time-based, driven by Kronos (so it is testable)
val timed = Ease.Out.quad.timed(from = 0, to = 255, duration = 500.milliseconds)
while (!timed.isDone) { val v = timed() }
```

**`bindFromTo` and `timed` are member extensions of the `Ease` object.** They do not resolve without
the explicit `Ease.bindFromTo` / `Ease.timed` imports shown above.

`timed` takes an optional `kronos: Kronos = Kronos.systemUtc`, which is how you make an animation
deterministic in a test.

## Smoothing

Laws that move a value towards a goal that may move, a step at a time, exact for any elapsed time (the pace does not
depend on the frame rate). The streams operator `smooth()` runs them on frames.

```kotlin
import io.peekandpoke.ultra.maths.Smoothing
import io.peekandpoke.ultra.maths.Smoothing.Motion

val needle = Smoothing.Spring(stiffness = 200.0, damping = 10.0)
val next = needle.step(Motion(position = 0.0), goal = 1.0, elapsedMs = 16.7)  // position, velocity (units/s)
needle.isSettled(next, goal = 1.0, near = 0.001)
```

`Exponential(keepPer10Ms)` (no overshoot), `Linear(unitsPerSecond)` (slew limit), `AttackRelease(up, down)`
(exponential, one share rising, one falling), `Spring(stiffness, damping)` (overshoots below `2 * sqrt(stiffness)`).
`isSettled` is the minimum delta: within `near` the motion may land (keep `near` above 0); a spring only once its
energy can no longer carry it out of `near`. Parameters of 0,
negative or NaN land at once; an `Exponential` share of 1 or more counts as 0.999.

`Double.quantize(size, offset = 0.0)` snaps to the nearest `offset + k * size`; halfway goes up; a size of 0 or less,
or a non-finite size, offset or value, returns the value as it is.

## Distributions

`Distribution.sample(random): Double` returns a value in `0.0..1.0`. `BucketedDistribution`
implements it via inverse-CDF interpolation over buckets.

```kotlin
import io.peekandpoke.ultra.maths.stochastic.BucketedDistribution
import io.peekandpoke.ultra.maths.stochastic.sample
import kotlin.random.Random

val normal = BucketedDistribution.createTruncatedNormal(mean = 0.5, std = 0.15)

normal.sample(Random)                              // 0.0..1.0
normal.sample(Random, from = 10.0, to = 50.0)      // mapped to a range (extension)
```

Factories: `createUniform(count)`, `createTruncatedNormal(mean, std)`, `createExponential(lambda)`,
`createTriangular(...)`.

Combinators, all returning a new `BucketedDistribution`:

| Method | Effect |
|---|---|
| `inverse()` | mirrors the CDF |
| `reversed()` | reverses the bucket order |
| `blend(other, ratio = 0.5)` | weighted mix of two distributions |

## Not serializable

`BucketedDistribution` and `RandomRange` carry `@SerialName` but **not** `@Serializable` — there is
no `@Serializable` anywhere in this module, so kotlinx.serialization has no generated serializer for
them. Do not expect these types to round-trip through JSON as-is.
