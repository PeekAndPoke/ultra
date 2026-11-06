# Changelog

## Unreleased

### Breaking changes

- **Funktor auth, a security fix: SSO no longer signs in to an account through an address the provider did not
  verify** (ULTRA-00118). A Google or GitHub account that claimed someone's address without verifying it signed in to
  that person's active account, by sign-in or sign-up: an account takeover. `AuthRealm.resolveSsoAccount` now asks the
  provider for every existing account, not only a pending one, and an SSO sign-up creates a new account only for a
  verified address. Google needs `email_verified = true`. GitHub's identity stays the public profile address of
  `/user`, which must be a verified entry of `/user/emails`; it no longer has to be the primary, so a pending account is
  now also activated when that address is a verified secondary one.

  What changes with no compile signal:
  - Every GitHub sign-in now reads `/user/emails`. An app with its own authorize URL needs the `user:email` scope, a
    GitHub App needs the "Email addresses" permission. Without them every GitHub sign-in and sign-up is refused. (A
    custom `GithubSsoAuth.RemoteClient` does get a compile error: `getEmails` is abstract.) Funktor auth's `GithubSignInButton` asks for the scope,
    but under hash routing the scope lands in the URL's fragment and does not reach GitHub (ULTRA-00288).
  - A custom SSO provider that calls `resolveSsoAccount` is now asked `isEmailVerified` for active accounts too: a
    lambda answering `false` locks its users out, one answering `true` keeps the takeover. Its own `signUp` must check
    verification before `createForSignup`.
  - A user whose Google address is unverified can no longer sign in with Google, also to an account SSO created
    before the upgrade.
  - Sessions taken through the hole before the upgrade survive it: a refresh does not ask the provider again. A
    deployment that had Google or GitHub sign-up enabled can end every session by rotating the JWT signing key.

- **Funktor auth: email-first sign-up — the password is chosen when activating** (ULTRA-00231). A pending account no
  longer holds a password, so whoever signs up someone else's address holds nothing: the attacker's password that a
  victim's click on "Activate" used to bring to life no longer exists. `AuthSignUpRequest.EmailAndPassword` lost
  `password`; the mailed link opens "Choose your password", and `AuthActivateAccountRequest` carries it (the realm's
  password policy applies). A successful activation signs the user in: `AuthActivateAccountResponse.signIn`.
  Signing up again with a pending address re-sends the link under the growing delays, and answers exactly like a
  fresh sign-up. The password-reset completion now enforces the password policy on the server too.

  Removed: the resend flow — `AuthSignInResponse.ActivationRequired`, `AuthError.AccountNotActivated`,
  `AuthRecord.ActivationResendToken`, `POST /auth/{realm}/activate/resend` (`AuthApiClient.ResendActivation`,
  `AuthResendActivationRequest`/`Response`, `AuthProvider.resendActivation`, `AuthRealm`/`AuthSystem.resendActivation`),
  `RealmTokenConfig.activationResendTokenLifetime`, `AuthFrontendRoutes.resendActivation` and
  `AuthState.pendingActivation`. `AuthProvider.activateAccount` returns the activated user (or null) instead of a
  response, and `EmailAndPasswordAuth.Services` gained `removePasswordRecords`. The TypeScript SDK runtime's
  `LoginOutcome` lost `activation-required`.

  What changes with no compile signal:
  - Pending accounts from before the upgrade still hold their sign-up password. It no longer signs in (refused like
    a wrong one); their activation link opens "Choose your password", which replaces it, and SSO deletes it.
  - An app's own sign-up form that still asks for a password: the server ignores it and stores none, so the user
    chooses one again on the activation page. An app's own activation page that POSTs no password is refused. An
    app's own activation mail template: its link now leads to choosing the password, so its copy should say so.
  - A pending user who tries to sign in gets the plain "invalid credentials" answer, with no hint to activate; the
    way back is the mailed link, or signing up again.
  - Rows of type `activation-resend-token` may stay in the auth-record store (Mongo's TTL does not cull them). Nothing
    reads them any more; they can be deleted.
  - The activation link is now a credential: within `RealmTokenConfig.emailVerificationTokenLifetime` (24 h) it
    chooses the password and signs in. A deployment whose mail may be forwarded or scanned can shorten it. A provider
    without the `SignIn` capability still activates, but hands out no session.

- **Funktor auth: the display name is chosen at activation, older links overlap, refused input says why**
  (ULTRA-00289). Sign-up takes no profile fields any more, so whoever signs up someone else's address cannot
  choose what that account is called. A pre-registering attacker's "Call support at …" name used to survive into
  the victim's account. `AuthSignUpRequest.EmailAndPassword` lost `displayName`, and
  `AuthActivateAccountRequest.displayName` is new and required. `AuthDisplayName` (common) is the rule: 1 to 100
  UTF-16 chars after trimming, on one line, of characters that show. It refuses control characters, invisible ones
  (format controls in every plane, the tag block, default-ignorable code points, blank-rendering letters, a run of
  variation selectors, a joiner not between two visible characters), undisplayable ones (private use,
  noncharacters, broken UTF-16) and a leading combining mark. `AuthDisplayName.refusalOf` says why, naming the
  character and where it sits; the page shows that message. Names with the England, Scotland or Wales flag emoji are
  refused, as those are built from tag characters. `AuthUserAdapter.activate(user,
  ActivateUserParams)` is new and has no default: it writes the name once the activation has won, and a realm
  without sign-up throws `AuthError.notSupported()`.

  `AuthRealm.activate` now checks the password policy and the name before the provider touches the token, and
  answers a refusal with `AuthActivateAccountResponse.failure` (`WeakPassword`, `InvalidDisplayName`): a 200 with
  `success = false`, the same status as a dead link. The activation page keeps its form and says why. A refusal of
  the link itself still carries no reason. `RealmTokenConfig.activationLinkOverlap` (default 30 min) keeps the
  older links of a re-send usable for that long instead of killing them at once, so someone signing the address up
  again cannot kill the link a user is typing into; 0 restores the old rotation. In an org realm, an activation
  that needs an org choice hands `OrgSelectionRequired` to the login page, which opens on the picker, once
  (`AuthState.takeActivationOrgSelection()`). `AuthState.logout()` now also drops a pending org selection. A custom
  `EmailAndPasswordAuth.Services` must implement the new `capEmailVerificationTokens`.

  What changes with no compile signal:
  - An app's own activation page that POSTs no `displayName` is refused with a 400, and its link stays usable. An
    app's own sign-up form that still sends a name: the server ignores it.
  - An account activated through a password reset or SSO is never asked for a name and keeps the default from the
    address (its local part).
  - After a re-send, up to about three links can be live at once for the first half hour.

- **Funktor auth: an SSO sign-up's profile name meets `AuthDisplayName`, else the address's local part is the name**
  (ULTRA-00293). The provider's name was only trimmed, so control, invisible and bidi characters reached the
  account, and a GitHub profile without a name (`"name": null`) became the literal name `"null"`.
  `AuthUserAdapter.CreateUserForSignupParams.of` now keeps a given name only when the rule accepts it.

  What changes with no compile signal:
  - A custom provider that passes its own `displayName` to `CreateUserForSignupParams.of` gets the local part for a
    name the rule refuses.
  - Email-and-password sign-up goes the same way: a local part over 100 chars now gives a name cut to 100.

- **Funktor rest, a security fix: a request body that does not fit its type is a 400, and no answer in production
  quotes it** (ULTRA-00292). A body missing a required field, with a wrong-typed field, or not JSON at all was a 500:
  logged as an internal error with the whole input, and answered with Slumber's message, which quotes it, also in
  production. A sign-in or activation body missing one field put the cleartext password into the log. Now
  `SlumberRestCodec` turns these into `CouldNotConvertException` (400) whose message names the type and the failing
  path only; the original stays its cause, so outside production the stack trace still shows the input. A 400 is not
  logged as an internal error.

  What changes with no compile signal:
  - A client that sent malformed bodies and handled the 500 now gets a 400.
  - **A 500 in production no longer carries the exception's message**, only "The request '…' cause an internal
    server error": an unknown exception's message may quote the input. Outside production it carries the stack
    trace, as before (`ApiStatusPages.errorDetail`).

## 0.117.0

### New

- **Kraft testing: the test reaches the app (ULTRA-00243, from Klang).** The `TestBed` block runs in a
  `TestBedScope` with `app` and `router`, so a spec navigates the app's own router: `router.navToUri("/x")`, then
  `router.awaitUri("/x")`, which fails with where the router is. Existing `{ root -> }` blocks compile unchanged.
- **Kraft testing: `eventually` returns its block's value (ULTRA-00247, from Spross):**
  `val stage = eventually { findStage().shouldNotBeNull() }`.
- **Kraft: `Router.dispose()` and `KraftApp.dispose()` (ULTRA-00243).** The router's window listeners
  (`hashchange`, `popstate`, `click`) can now be removed; `KraftApp.dispose()` disposes the router and every app
  attribute that implements `KraftApp.Disposable`. TestBed disposes the app after each test: until now every TestBed
  render left its router listening to the window for the rest of the run. `RouterStrategy.dispose()` has an empty
  default, so custom strategies keep compiling.

- **Kraft: one `WindowController` for the page (ULTRA-00268).** `WindowController` is now an interface;
  `WindowController.default` is the browser-backed one, registered in every app and readable outside components
  (`WindowController.default.hasFocus`). New: `isVisible` (false while the tab is hidden). `TestWindowController` in
  kraft:testing is driven by hand (`blur()`, `focus()`, `visibility(false)`, `resize(size)`). Until now every app,
  so every TestBed render, created its own controller whose four listeners stayed on the window.

- **Streams: one `requestAnimationFrame` per frame (ULTRA-00269, from Klang).** `FrameClock.animationFrames` (the JS
  default clock of `smooth()` and `animTicker()`) batches its callers: one browser frame request runs them all, in
  order, with the same time. Klang measured about 9 rAF callbacks per frame before. A callback that throws is reported
  as uncaught on its own task, and the others still run.

- **Streams: `smooth()`'s law as a stream (ULTRA-00264, from Klang).** `smooth(smoothing: Stream<out Smoothing?>)`: a new
  law takes the next step from the current position and velocity, so the motion is not restarted; `null` passes
  values at once and lands a moving stream. The constant overloads delegate to it.

- **Streams: `sampled(every)` (ULTRA-00267, from Klang; named by the maintainer).** The latest value at every tick,
  also unchanged and `null`, so a graph's time axis is the clock's: `level.sampled(every = 250.milliseconds)
  .history(capacity = 480)`. `every` may be a `Stream<Duration>` that changes while listening (a new interval applies
  from the next tick) and `context` sets the coroutine context the ticks run in (JVM/native); `everyMs` and
  `sampled(clock: FrameClock)` too. `history()`'s KDoc names its copy per value.

- **Kraft three.js: pause the render loop (ULTRA-00270, from Klang).** `ThreeJs(paused = ...)` takes a
  `Stream<Boolean>`: while true no frame is asked for, and the scene, camera and WebGL context stay
  (`paused = windowCtrl.hasFocus.map { !it }`). After a resume the first `deltaMs` is 0 and `elapsedMs` counts
  running time only. The loop runs on `clock` (default `FrameClock.animationFrames`, shared with the streams); a new
  clock restarts it. The scene is drawn once after `onReady`, also when mounted paused.

- **Kraft three.js: the members the projects filled in themselves (ULTRA-00246, from Spross and Klang).** Constructors
  `createObject3D()`, `createGroup()`, `createSpotLight(...)`, `createRaycaster()`, `createVector2(x, y)`,
  `createCanvasTexture(canvas)`, and `create<T>(className, args...)` for any other class. New types `Group`,
  `SpotLight`, `Raycaster` (with `Intersection`), `Vector2`, `Texture`, `CanvasTexture`, `LightShadow` (`Light.shadow`,
  non-null on directional, point and spot lights), `ShadowMap`,
  and the constants `TextureWrapping` / `TextureFilter`. New members: `Object3D.renderOrder`, `parent`,
  `removeFromParent()`, `updateWorldMatrix(...)`, `localToWorld(v)`, `lookAt(Vector3)`; `Light.dispose()`, `shadow` on
  directional, point and spot lights; `HemisphereLight.groundColor`; `Material.needsUpdate`;
  `WebGLRenderer.shadowMap`. Spross's are taken from its Apache-2.0 workshop; Klang's AGPL files were rewritten from
  three.js's API, not copied.

- **Kraft pixi.js: the members the projects filled in themselves (ULTRA-00245 from Spross, ULTRA-00262 from
  Klang).** `raw`, `createContainer()`, `create<T>(className, args...)`; `Container.zIndex`, `sortableChildren`,
  `addChildAt`, `sortChildren()`, `scale: ObservablePoint`; `Text.anchor`; `Graphics.poly(points, close)`;
  `Application.resize()` and `queueResize()`. New: `app.followElementSize(element)` keeps the app the size of an
  element also when layout alone resizes it (a `ResizeObserver` through `queueResize()`); `resizeTo` follows the
  window's resize event only. Spross's code is taken from its Apache-2.0 workshop.

- **Kraft router: guard a page against leaving (ULTRA-00266, from Klang).** `guardLeaving { target -> ... }` in a
  component answers `LeaveResult.Proceed`, `Stop` or `Ask(confirm)`, asked before every navigation away while it is
  mounted: `navToUri`, intercepted links, `replaceUri`, Back/Forward and a typed hash (undone by putting the address
  back). Leaving means another path (before `?` and `#`), when RouterComponent remounts the page; a query-only change
  (every `urlParam`) never asks. `beforeUnload = true`
  arms the browser's prompt. A confirmed `Ask` on a Back replays the Back; a middleware redirect inside an approved
  navigation is not asked again. `router.withoutLeaveGuards { router.navToUri("/login") }` passes every guard (logout).
  `navBack()` and `clearHistoryAndNavTo()` change the router's history only once no guard holds them. A custom
  `RouterStrategy` calls `router.followBrowserNavigation()` from its listeners to take part; `RouterStrategy` gains
  `normalize(uri)` (the URI in the browser's encoding; a strategy whose `render` adds a prefix overrides it) and
  `restoreUri(uri): Boolean`, with defaults. Not covered: a hash typed into the address bar under hash
  routing is held, but leaves one extra history entry, and so does a held Forward; a held Back drops the forward
  history, as any new entry does.

- **Kraft: drag and drop rebuilt in Kraft core, `io.peekandpoke.kraft.dnd`, registered in every app (ULTRA-00265, from Klang's look editor; the maintainer's design).**
  A `DndManager` with an auto-mounted stage that draws only the drag preview (z-index above modals and popups), drags
  started from a component's `pointerdown` (`dnd.startDrag(event, payload, source, preview)`, a threshold before it
  begins), and drop zones components register while mounted (`dropZone<T> { onDragStart / onDragStop / onEnter /
  onOver / onLeave / onDrop }`). Mouse, touch and pen; Esc cancels; the drop knows its source.
  `DropZoneState` in kraft-semanticui is an optional helper.

- **Kraft: `WakeLockComponent` (ULTRA-00275, from gfx-playground).** Keeps the screen awake while mounted. Several
  share one lock (a counter: the last unmount releases it); the lock is taken again when the page becomes visible; no
  API, no error. `WakeLockController` (`wakeLockCtrl.acquire()`, `isActive`), registered in every app like
  `windowCtrl` (`kraftApp { wakeLockCtrl(...) }` to replace it; `WakeLockController.default` outside components). Hardened
  against the original: no leaked listener, no lock kept after an unmount during a pending request, one request at a
  time.

- **Kraft: `ResponsiveController` knows portrait and landscape (ULTRA-00276, for Klang's mobile version).** `State`
  has `orientation` (`Portrait` when height ≥ width, as the CSS media query; `isPortrait`, `isLandscape`), and each
  orientation has its own `Breakpoints`: a phone held sideways (up to 959 px wide) stays `Mobile`. It follows rotation
  (`screen.orientation`, the orientation media query) as well as resize, and measures again on the next frame for
  browsers that report the old size in the rotation event.

### Breaking changes

- **`ResponsiveController(breakpoints = ...)` is now `ResponsiveController(portrait = ..., landscape = ...)`**
  (ULTRA-00276); `Breakpoints.default` is now `Breakpoints.portrait` (768 / 1200, as before) and
  `Breakpoints.landscape` (960 / 1200). A desktop window 768 to 959 px wide is now `Mobile`, not `Tablet`, and a
  change of height alone can now change the display type (a viewport of that width that turns landscape: an on-screen
  keyboard that resizes the layout, a DevTools panel docked below).
  `State` has a new constructor parameter, `orientation`. A positional `ResponsiveController(Breakpoints(...))` still
  compiles and now sets the portrait breakpoints only: landscape viewports (most desktop windows) use the landscape
  default; pass both.

- **The old Semantic UI drag and drop is gone** (`DndDragHandle`, `DndDropTarget`, `greenHighlights`,
  `blueHighlights`): it had no users outside the examples. Port to the new drag and drop (see above and the docs page).

### Changed

- **Kraft router, hash strategy: plain clicks on route links (`href="#/..."`) now go through the router** (ULTRA-00266),
  as the path strategy's links already did, so a leave guard holds them before the browser writes the address. Other
  fragment links (`#section`) stay the browser's.
- **Kraft router: `replaceUri` keeps the history entry's state** (both strategies), so a `BackNavigationTrap`'s marker
  survives a `urlParam` change inside an open modal.
- **Kraft testing: the `TestBed` block's receiver** (ULTRA-00243) has `app` and `router`: inside the block they win
  over a file-level `val router`, so a spec that built its own router at file level and named it `router` now gets the
  app's. Rename it, or use the app's.
- **`ResponsiveController` is a `KraftApp.Disposable`:** disposing the app (TestBed does after each test) removes its
  window `resize` listener.
- **`WindowController()` is deprecated** (ULTRA-00268): it returns `WindowController.default`. Code that created its
  own controller (klang's `UiWork.window`) gets the shared one and can switch to `WindowController.default`.

### Fixes

- **Kraft: `ResponsiveController.DisplayType.isNotDesktop` / `isNotMobile` were `true` for `Desktop` / `Mobile`**
  (an initializer in the enum's constructor read entries not yet assigned; `State.isNotDesktop` was right).

- **Streams: one handler given twice is two subscriptions** (ULTRA-00274). A stream kept its subscribers in a set of
  handlers, so the same handler (and every lambda that captures nothing, such as the default of
  `subscribeToStream()`, which is one shared instance) subscribed twice held one entry: the first to leave ended the
  other, and a source computed only while listened to (fold, smooth, ticker) stopped under it. Each subscription now
  has an entry of its own; the same handler twice is notified twice. `StreamSource.subscriptions` therefore holds
  these entries, not the handlers given: unsubscribe with the returned `Unsubscribe`, not `subscriptions.remove(h)`.
- **Kraft routing: a malformed percent-escape no longer makes route matching throw** (ULTRA-00273): `#/edit/1?q=%`
  routes, with the text as written.
- **Kraft: `urlParam` keeps the path as the address has it** (ULTRA-00278): it rendered the whole address anew from
  the route, which could write the path differently (`@` as `%40`), and a leave guard took the param change for
  leaving the page.
- **Kraft Semantic UI: a link inside a FadingModal or a DropdownMenu reaches the router** (ULTRA-00277): their click
  handlers stopped every click, so route links were followed by the browser (a page reload under path routing); they
  now hand link clicks to the router (`Router.takeLinkClick(event)`, for any component that stops click propagation).
  A navigation to another page now dismisses an open FadingModal under both strategies (it answers `null`); a
  query-only `replaceUri` (a `urlParam`) does not, while under hash routing a pushed move, also a query-only one, is
  a Back for its trap and dismisses it, as before. A modal shown in the same handler as a navigation belongs to the
  new page. `RouterStrategy.linkClicked(event)` is the hook: a custom strategy with its own click listener overrides
  it, or links inside modals and menus are left to the browser.
- **Kraft: the popups stage removes its own document listeners** (ULTRA-00272); each app left two behind.

- **Streams: a subscribe that throws leaves nothing behind.** When `onSub()` throws, or the handler throws on its first
  value, `StreamSourceImpl` and `StreamWrapperBase` drop the handler (the caller got no unsubscribe) and release what
  the subscription started; `smooth()` and `sampled()` undo a half-made subscription, so the next subscriber starts
  afresh. A `smooth()` motion that a throwing law stalled goes on when the law changes, and lands on a `null` law.

- **Kraft: `ResponsiveController` measured the page, not the viewport** (ULTRA-00276): the height came from
  `document.body`, so on a long page it was the content's height. It now reads `document.documentElement`, and it
  emits only when the state changed (it emitted on every resize event).

- **Streams: `ticker(interval: Duration)` with a very long or infinite interval** wrapped to a negative delay and
  ticked back to back (`Duration.INFINITE` became -1 ms); it is clamped to `Int.MAX_VALUE` ms now.
- **Kraft router: `replaceUri` under hash routing (ULTRA-00263, from Klang).** It pushed the route as a path, which
  dropped the fragment, so the router never saw the new route: every `urlParam` change under hash routing was lost to
  the other subscribers of `router.current`. It now replaces the fragment in place. Under path routing `replaceUri`
  resolved the route twice and emitted twice; it now emits once. A `uri` given with its `#` counts as the same address.
- **Kraft core tests compile again:** two form specs called `FormController(this)`, private since 0.116.0; they use
  `FormController.controller(this)`.

## 0.116.0

### New

- **Streams: `filterIsInstanceOf(cls)` (ULTRA-00241).** Filters by a `KClass`, so the call needs no type arguments:
  `stream.filterIsInstanceOf(Diagnostics::class)` instead of `stream.filterIsInstance<Diagnostics, _>()`. The class
  must still be a subtype of the stream's type. With `initial`, it returns a non-null `Stream<I>`. It has a name of
  its own because a `KClass` also fits `filterIsInstance(initial)`: as an overload the call would be ambiguous on a
  `Stream<Any?>`. Watch for `filterIsInstance(X::class)` there today: it compiles as `initial` and filters for
  `KClass` values.

- **Streams: `smooth()`, `quantize()` and `FrameClock` (ULTRA-00242, from Klang's KLANG-00345).**
  `Stream<Double?>.smooth(sharePer10Ms)` approaches each new value a step per frame, exponentially, at the same pace at
  any frame rate; `smooth(smoothing)` takes any law. Within `near` it lands exactly and stops asking for frames.
  `quantize(size, offset)` snaps to a grid. `FrameClock` is the clock they run on: animation frames on JS, a 60 Hz
  coroutine ticker elsewhere, a hand-driven one in tests.
- **Maths: `Smoothing` laws and `Double.quantize` (ULTRA-00242).** `Exponential`, `Linear`, `AttackRelease` and
  `Spring`, each a pure `step(motion, goal, elapsedMs)` exact for any elapsed time, with `isSettled(motion, goal,
  near)`.

### Changed

- **`ultra:streams` depends on `ultra:maths`** (and so on `ultra:datetime`), for `smooth()`'s laws. Kraft apps get
  both through `kraft:core`.
- **`ticker()` and `animTicker()` run on `FrameClock`.** `TickerFrame` times are in milliseconds with fractions (were
  whole milliseconds on `ticker()`); `animTicker()`'s first frame is timed like the rest (`performance.now()`, was
  `Date.now()`). `ticker(clock)` is new. On the JVM and native, `ticker()` still calls back on `Dispatchers.Default`;
  `FrameClock.ticker(context = ...)` picks the thread. Streams are not thread-safe: subscribing from another thread
  while a frame emits can, rarely, start a second chain of frames, which the old coroutine loop could not.

### Docs

- The maths docs said "no dependencies"; maths depends on `ultra:datetime` and kotlinx.serialization.
- The streams `filter` examples read an operator nobody listened to and printed values it never had; they subscribe
  now, and say that a filter sees values only while someone listens. `filterNotNull`'s result is `Stream<T>`, not
  nullable.
- The streams `filterIsInstance` examples wrote one type argument, which does not compile; they now show both forms.

## 0.115.0

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
