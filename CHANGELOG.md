# Changelog

## Unreleased

### Breaking changes

- **Kraft modals: every modal answers exactly once (ULTRA-00233).** `ModalsManager.Handle` becomes
  `Handle<RESULT : Any>`, `FadingModal<P>` becomes `FadingModal<P, RESULT>`, `FadingModal.Props` becomes
  `FadingModal.Props<RESULT>`, and `ModalRenderer` takes the type parameter. The compiler finds these.
  - `handle.resolve(result)` decides the answer (the first call wins), `handle.onResult { result: RESULT? -> }` gets
    it once the modal is gone, `null` meaning dismissed. `onClose` runs after `onResult`.
  - `closeAll()` now runs the handlers (each modal answers what it resolved, else `null`); it used to run none.
  - `OkCancelModal`'s `onResult` takes `Result?`: `null` when dismissed by the backdrop, back navigation or
    `closeAll()`, which used to give no answer at all. Its default is `{}`.
  - `FadingModal.close(result)` answers and closes, `close()` dismisses. `close(onClose)` is deprecated; for this
    release `close()` and `close(result)` still call through it, so overrides keep running.
  - New: `ModalsManager.showAndAwait { }` returns the answer in a coroutine.
- **Upgrading with no compile error to warn you:**
  - `if (result == Cancel) ... else delete()`, `when (result) { Cancel -> ...; else -> delete() }` and string
    templates compile unchanged and now see `null` on a dismissal: treat `null` like Cancel;
  - handlers registered inside a modal's render function run once per render of the modal stage: register them on
    the handle `show` returns, or once in a component's `init`;
  - answers arrive after the fade-out (`fadeOutTimeMs`, 500 ms by default), where OkCancel's Cancel used to answer on
    the click and Ok after 150 ms; `<body>` scrolls again when the last modal unmounts.

- **Kraft testing (ULTRA-00240):**
  - `TestBed` now unmounts the app after each test, so components run their unmount hooks; `VDomEngine` gains
    `unmount(element)` (a custom engine must implement it).
  - `awaitCss` fails with an `AssertionError` naming the selector on timeout, where it returned an empty query.
  - `KQuery.first()`, `last()` and `nth()` are members: without an import they used to resolve to the stdlib's
    `List.first()` (an `Element`); now they always return a `KQuery`. For the element use `[0]` or `single()`.
    Imports of `io.peekandpoke.kraft.testing.first` / `last` / `nth` must go. These still compile and now see a
    KQuery: `.first()` followed by `as`, `unsafeCast`, `shouldBe`, `shouldBeInstanceOf`, `shouldNotBeNull()` or a
    string template.
  - New: `awaitGone(css)`, `eventually { }` (retries suspending assertions), `KQuery.hasFocus()`.

### Fixes

- **Kraft modals:** OkCancel's buttons are real `<button type="button">`s and Cancel has the focus on open; Ok then
  Cancel within the fade no longer answers twice; a modal opened from a modal's answer no longer reuses the closed
  one's component (the stage keys modals by id); `<body>` scrolls again after `closeAll()`; closing the top of two
  stacked modals no longer closes the one below (`BackNavigationTrap` ignores a step back onto its own entry).

## 0.114.0

### Breaking changes

- **Account activation hardening (funktor auth)** closes account pre-hijacking: an attacker who pre-registers someone
  else's address can no longer end up sharing that account.
  - The activation link opens a page with an **Activate** button; only the click activates. Opening the link changes
    nothing, so mail scanners no longer activate accounts.
  - A pending (never-activated) account expires after `RealmTokenConfig.pendingAccountLifetime` (7 days) and can be
    cancelled from the activation mail. Expiry or cancel **retires** it: its email becomes a key that keeps only a
    hash of the address, its auth records are deleted, and the address can sign up again. The data stays with the
    retired row.
  - A verified SSO sign-in on a pending password account activates it and deletes the sign-up password.
  - Resends are spaced by growing delays (`RealmTokenConfig.activationResendDelays`, replacing
    `activationResendCooldown`); `AuthResendActivationResponse.nextResendAt` says when.
  - **`AuthUserAdapter.retire(user, retirement)` is new and abstract.** Every realm implements it: rename every unique
    key with the given `AccountRetirement`, or throw `AuthError.notSupported()` in a realm without sign-up.
  - `AuthRealm.Messaging.sendAccountActivationEmail` and `AccountActivationEmailTemplate.Params` take a `cancelUrl`.
    `AuthProvider.resendActivation` takes the owner; `AuthProvider.cancelSignUp`, `AuthApiClient.CancelSignUp` and
    the route `AuthFrontendRoutes.cancelSignUp` are new.
  - Auth-record storage: `PendingActivation` gains `claimedBy`, `resends` and `lastSentAt`; there is a new
    `SignUpCancelToken` record. The storage adapter gains conditional writes (`replaceIfClaimedBy`,
    `removeIfClaimedBy`), `removeAllOfOwner` and `findCreatedBefore`, and loses `save`. There is a new index on
    (realm, type, createdAt) on both backends. Existing records need no migration.
- **Upgrading with no compile error to warn you:**
  - an app's own activation-mail renderer must render `cancelUrl`;
  - an app's own router must mount `cancel-signup`;
  - an app's own activation page must not POST on load;
  - an app's own SSO provider must route existing accounts through `AuthRealm.resolveSsoAccount`;
  - an app's own `AuthRealm.Messaging` lacks the retired-recipient guard;
  - `AuthRecordStorage.OnAfterSave` listeners do not see the conditional writes.
- **What changes for users:**
  - GitHub sign-in now asks for the `user:email` scope;
  - the first expiry sweep after upgrading retires every pending account older than the lifetime, and retired rows
    stay in the user collection;
  - an account activated by SSO has no password until a reset;
  - the expiry worker comes with `funktor/all`'s `Funktor` module.
- **Insights:** the v2 kontainer slice keeps only the instantiated services and drops `numOld` / `numTotal`.
- **Codegen:** `sdk:ts:generate` refuses packages the app's `package.json` does not list.

### Changes

- **Global locks expire by heartbeat**, not by creation time: a long-running holder keeps its lock, and a dead
  server's lock lapses after the ttl. Safe across a rolling deploy with old servers. The locks listing shows the last
  heartbeat.
- **Insights:** superuser endpoints and Vue views for the kontainer and database graphs. Monko query text is kept,
  now that it holds typed placeholders instead of values.

### Fixes

- **ultra log:** one dispatch coroutine per appender. An appender that suspends (e.g. a database appender) no longer
  delays or drops lines for the appenders after it; the insights per-request log slice lost lines this way.
- **ApiAcl:** the `canFullyAccess` / `canPartiallyAccess` KDoc says what they mean. `forCall` estimates `Granted`
  unless it is given an `estimateFn`.

## 0.113.0

### Breaking changes

- **The `macosX64` (Intel Mac) native target is gone** from `ultra:common`, `cache`, `datetime`, `maths` and
  `streams` — Kotlin deprecated it. `macosArm64`, `linuxX64`, `linuxArm64` and `mingwX64` stay.

### Build

- The build scripts no longer use what Gradle 9.6 deprecated in the Kotlin DSL (`by project` / `by extra` delegates,
  `tasks.registering`, `tasks.create`, the yarn-lock setters); the build configures without warnings.

Otherwise the same code as 0.112.0.

## 0.112.0

### Breaking changes

- **kotlinx-datetime 0.8.0** (was 0.6.2). `Instant` and `Clock` now come from `kotlin.time`;
  `Kronos.fromClock(...)` takes a `kotlin.time.Clock`. On the JVM, kotlinx `Month` / `DayOfWeek` are no longer
  aliases of the `java.time` types.
- **`installNativeTimezones()` is removed** (ultra and the Kraft datetime addon), and Kraft no longer installs a time
  zone provider by default. `Mp*` types need no setup in the browser any more: ultra resolves zones itself, through the
  browser's own IANA database (`Intl`), with no bundled data. The Kraft js-joda loaders (`fullTimezones()`, …) remain,
  for raw kotlinx `TimeZone.of(...)` only.
- **Zone ids are case-sensitive on JS too**: case variants of known ids (`europe/berlin`) throw
  `IllegalTimeZoneException`, as on the JVM.
- **`MpLocalTime` is `Int`-based**: `ofMilliSeconds(Int)`, `ofSecondOfDay(Int)`, `inWholeMilliSeconds(): Int`,
  `MpLocalTimeSlot.ofSecondsOfDay(Int, Int)`. Values outside a day now WRAP into it, also negative ones
  (`ofMilliSeconds(-3_600_000)` is 23:00). The stored value is unchanged (still a `Long` on the wire).
- `TimeZone.UTC` is written as `"UTC"` (was `"Z"`) by the slumber codec for raw kotlinx `LocalDateTime`; old `"Z"`
  records still read back.

### New

- Converters between `Mp*` types and kotlinx-datetime types (`.kotlinx` / `.mp`), next to the existing `java.time`
  ones.
- `MpTimezone` overloads where only kotlinx `TimeZone` ones existed (`MpInstantRange.plus/minus`,
  `MpZonedDateTimeRange.contains`, `MpZonedDateTime.parse/tryParse`); `MpTimezone.of("SYSTEM")` is the system zone.
- pixijs addon: `ApplicationOptions.backgroundAlpha`.

### Fixed

- **pixijs addon works with webpack 5.108** (Kotlin 2.4): webpack dropped pixi's side-effect imports, so `app.ticker`
  was undefined and a `Graphics` rendered nothing. The addon now registers pixi's extensions itself; consumers need no
  webpack rule (Klang can drop `webpack.config.d/04-pixi-side-effects.js`).
- Named zones gave wrong offsets for dates before the common era on JS.
- `MpLocalTime.of(...)` with large minute / second values.

### Performance (browser)

ultra's zone and date arithmetic no longer goes through `Intl` on every call nor through kotlinx 0.8's emulated-`Long`
conversion. In spross's browser profile, datetime fell from 8.7 % to 1.7 % of wall time against 0.111.0;
in ultra's headless-Chrome benchmark `instant.atZone(zone).toLocalDateTime()` went from ~33 µs to ~3 µs.

Zone math follows java.time semantics on every target and is checked against java.time for every JDK zone.
