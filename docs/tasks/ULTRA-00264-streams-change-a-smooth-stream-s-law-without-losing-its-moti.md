# ULTRA-00264: Streams: change a smooth() stream's law without losing its motion

> **Status (2026-10-29): DESIGNED.** Plan written and decided 2026-10-29. Request from Klang (Klang-Research, 2026-10-28), from its start-page lamp (klang 04af853dd). Not scheduled; Klang works with a local SwappableSmoothing. No epic fits: ULTRA-00255 (Ultra libraries) holds scans and defects, not wishes.

**Security-critical:** no

## Why this exists

Klang-Research, 2026-10-28: klang's start-page lamp (klang `04af853dd`, `src/jsMain/kotlin/comp/MotorBackground.kt`,
`BulbStutter.kt`) sends a dying bulb's brightness through `.smooth(smoothing)`, and each stutter picks its own law at
random (Exponential, AttackRelease, Spring, Linear). A new `.smooth()` per law starts the motion over (its first value
is taken as it is, position and velocity are lost), as the maintainer pointed out. Klang built, locally:

```kotlin
class SwappableSmoothing(var law: Smoothing) : Smoothing {
    override fun step(motion: Smoothing.Motion, goal: Double, elapsedMs: Double) = law.step(motion, goal, elapsedMs)
    override fun isSettled(motion: Smoothing.Motion, goal: Double, near: Double) = law.isSettled(motion, goal, near)
}
```

Checked against the code, 2026-10-28:

- **Klang's class is correct against today's operator:** `StreamSmoothOperator.frame` reads the law on every frame
  (`ultra/streams/src/commonMain/kotlin/ops/smooth.kt:160-163`) and keeps `Motion` (position and velocity) outside the
  law, so a swap takes effect on the next step from the current motion.
- **What a swap does not do:** wake a settled stream. With the goal reached and no frame pending, a new law does
  nothing until the next value, which is right: there is nothing to move.
- Both laws must agree on what `Motion.velocity` means: all four keep it in units per second (`Smoothing.kt`), so
  a Spring taking over from an Exponential swings on with the speed it had.

## Decide before implementing

1. **Which API.**
   - (a) `Smoothing.Swappable(initial)` in `ultra.maths`: Klang's class, five lines, mutable.
   - (b) `Stream<Double?>.smooth(smoothing: Stream<Smoothing?>)`: the law is a stream; a new law takes the next step
     from the current motion, `null` passes values at once (as `sharePer10Ms <= 0` does now).
   - Recommendation: (b). It fits how streams are composed (a law can be derived with `map` from a setting), keeps
     mutable state out of `maths`, and the operator already owns the lifecycle a second subscription needs. (a) stays
     a one-line user-side workaround.
2. **Under (b): a law change while moving.** Recommendation: no reset of `lastFrameAt`, the next frame steps with the
   new law over the real elapsed time.

## The work

1. The overload in `ultra/streams/src/commonMain/kotlin/ops/smooth.kt`: subscribe to the law stream with the source,
   unsubscribe together; the existing overloads delegate to it with a constant law.
2. Specs (commonTest, `HandClock`): a swap mid-motion keeps position and velocity (Spring after Exponential swings
   on), a swap to `null` lands at once, a swap while settled emits nothing, unsubscribing releases both sources.
3. Docs: the streams operators page, `maths/smoothing`, and the LLM mirrors; CHANGELOG.

## Acceptance

- The swap-keeps-motion spec is RED against an operator that restarts the motion on a new law.
- All three platforms green.

## History

- 2026-10-28: created from Klang's request; checked against the code the same day.
- 2026-10-28: the maintainer, in the Klang-Research session (relayed by Klang-Research, not yet confirmed in ultra's
  session): "The streams should be like patterns ... when there is something to be configured on a stream the config
  value itself needs to be a stream ..." ("like patterns": sprudel, where every argument of a pattern function is a
  pattern). It backs option (b), and reads wider: `rest` and `near` as streams too, and the other operators' config.
- 2026-10-29: DESIGNED.
- 2026-10-29: maintainer: "config is a stream" is a rule, with stream overloads beside the constant ones (answer "Rule + stream overloads"); option (b) decided.
