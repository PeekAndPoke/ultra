# Router click interception: respect anchors, route only relative same-host links

**Status:** DONE (archived 2026-09-28)
**Plan:** — (bug report, 2026-09-28)
**Security-critical:** no

## Spec

Bug report: `PathRoutingStrategy.clickListener` (`kraft/core/src/jsMain/kotlin/routing/Router.kt`)
intercepted every anchor click whose href failed `isUrlWithProtocol()`, ignoring `target`,
`download` and `rel`; the regex rejected `localhost`, bare hosts, IPs, and URLs containing
`, ; ! * ' $ [ ]`, so those were routed in-app (`/http://localhost:…`).

The router intercepts a click only when it is an in-app navigation, else the browser handles it:

- [x] Router enabled, event not already `defaultPrevented`
- [x] Primary button, no Ctrl / Meta / Shift / Alt
- [x] No `target` other than `_self` / empty (anchor's own, else `<base target>`; `_top` / `_parent`
      only unframed), no `download`, no `rel~=external`
- [x] href is not absolute (`scheme:` or `//host`, after the URL parser's own clean-up) — absolute URLs
      always go to the browser, even same-origin (maintainer, 2026-09-28: keep the old rule, detected by
      form, not by the regex)
- [x] relative href resolves (against `document.baseURI`, like the browser) to the page's own
      protocol AND host (not "http(s) only": round 2, keeps Capacitor / `file://` hosts routing)
- [x] Not a fragment navigation within the current path + query (`#section`) — browser scrolls;
      `popstate` with unchanged path + query does not re-resolve; route matching ignores the fragment
- [x] Relative hrefs follow browser resolution (`settings` on `/a/b` → `/a/settings`) — agreed with
      the maintainer 2026-09-28; previously `/settings`
- [x] `UrlWithProtocolRegex` accepts `localhost`, single-label hosts (with `_`), IPv4, bracketed IPv6,
      ports, userinfo, and all RFC 3986 path/query/fragment characters; http(s) and ASCII only

## Implementation notes

- Only `PathRoutingStrategy` installs a click listener, so `pathname + search + hash` is the right
  URI shape; the report's hash-strategy caveat does not apply.
- `willOpenNewTab` (public) is unchanged; the listener checks modifiers / button itself.
- Resolves against `document.baseURI`, not `window.location`, so `<base href>` behaves as in the browser.
- Regex host labels bounded `{0,126}`: the JVM engine recurses per group repetition (same trap as
  `EmailRegex`, see `20260729-common-scan-findings.md`).
- Found while reading, not in the report: `href="#section"` used to push `/#section`, i.e. jump to the
  root route; Shift/Alt clicks and clicks already `defaultPrevented` were intercepted too.
- Behaviour change worth knowing (round 2, R2): with the `defaultPrevented` check, only the FIRST enabled
  path router handles a link click — a second enabled path router (or a stale one never disabled) no
  longer re-resolves on that click. Matches react-router / SvelteKit.
- Docs written in this task at the maintainer's request (2026-09-28), so no follow-up DOCS task:
  `docs-site/src/pages/ultra/kraft/link-clicks.astro` (new), `routing.astro` (stale `onClick` pattern
  rewritten — it navigated twice), `docs-site/src/data/llms/kraft.md` (mirror section; the old comment
  claiming `navToUri(evt, …)` prevents default was false).

## Test evidence

- [x] `kraft/core-tests` jsTest `RouterClickInterceptionSpec`: 18 cases, real clicks through a real
      `PathRoutingStrategy` in ChromeHeadless (real fragment navigation + `history.back()`) — green
- [x] `ultra/common` `StringsExtSpec`: 29 — green on jvm, js, linuxX64
- [x] mutation-checked (each restored, green after):
  - old `clickListener` logic → 9 of the then 11 red (plain click, disabled router: regression guards)
  - round 1: protocol check, popstate guard, fragment strip in `resolveRouteForUri` → each red
  - maintainer rule: `isAbsoluteUrl` check removed → "absolute and scheme-relative" red
  - round 2: protocol check, host check (separate run), `isAbsoluteUrl` normalisation, popstate guard
    (Back direction), whitespace target → each red on its own test. The host mutant first SURVIVED:
    the cross-host case also differed by protocol, so the protocol check caught it — fixed the case
    to share the page's protocol, then red.
- [x] docs-site `pnpm run build`: 114 pages, 0 errors
- [x] Full test command(s) run + green: `./gradlew :ultra:common:jvmTest :ultra:common:jsTest
      :ultra:common:linuxX64Test :kraft:core-tests:jsTest`

## Review record — the LEDGER (filled by /feature-review)

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MAJOR | `Router.kt:196` | `blob:` passes the origin check (R1, R2; R3 as MINOR) | fix | node: `new URL("blob:http://localhost:9876/x").origin` = page origin. Now also requires `http:`/`https:`; test added, mutant red |
| 1 | MAJOR | `Router.kt:199` + `RouteBase.kt:50` | Browser fragment nav fires `popstate`; route matching keeps `#…` → catch-all / miss, fragment leaks into last param, `RouterComponent` remounts (R2) | fix | `RouteBase.match` splits on `?` only (verified). `resolveRouteForUri` strips the fragment; `popstateListener` ignores fragment-only changes; `RouterComponent` key strips `#`. Tests added; mutants red (confirms Chrome fires `popstate`) |
| 1 | MAJOR | `BackNavigationTrap.kt:56` | Active trap fires on an in-page anchor click (R2, part of the above) | out-of-scope → `20260928-backnavtrap-fragment-navigation.md` | Trap owns its own `popstate` listener; before this change the same click silently left the page for `/`, bypassing the trap entirely |
| 1 | MAJOR | `Router.kt:197,204` | Absolute same-origin hrefs (`https://app/api/export.csv`) were full navigations, now routed in-app (R2; R1 as MINOR) | maintainer-decision → fix | Maintainer 2026-09-28: keep the old rule — absolute URLs go to the browser. Detected by form (`scheme:` / `//`), not by the regex, so a URL the regex misses cannot slip into the router |
| 1 | MINOR | `Router.kt:196` | `javascript:` on plain click now runs; old code swallowed it by accident (R3) | maintainer-decision | Router is not a sanitizer; blocking also breaks intentional `javascript:` hrefs. Maintainer 2026-09-28: keep as is for now |
| 1 | MINOR | `Router.kt:213` | `_top` / `_parent` in an unframed page mean self (R1) | fix | `targetsSelf` checks `window.top` / `window.parent`; test added |
| 1 | MINOR | `regexes.kt:10` | "ASCII only" false: `IGNORE_CASE` folds U+017F / U+212A on JVM and JS (R1, R3) | fix | Dropped `IGNORE_CASE`, explicit ranges; tests with `Char(0x17F)` / `Char(0x212A)` |
| 1 | MINOR | `regexes.kt:21` | `_` in host rejected — validator regression for `http://my_service:8080` (R1) | fix | `_` allowed in labels; tests added |
| 1 | MINOR | `regexes.kt:24` | `'` accepted; consumers may treat "validated" as escaped (R3) | fix | KDoc: "A shape check: it neither escapes for output nor makes a URL safe to fetch." No such sink in repo (R3) |
| 1 | MINOR | `regexes.kt:15-26` | Accepts port > 65535, `999.1.1.1`, `[::::]`, bad `%` escapes, two `#` (R2) | fix (two `#`) / reject (rest) | Fragment now excludes `#`. Rest: documented shape check; a `%XX` group would recurse per character on the JVM (see label bound) |
| 1 | MINOR | `Router.kt:39-49` | `willOpenNewTab` ignores Shift/Alt; listener does not (R2) | maintainer-decision | Public API semantics ("new tab"); only matters for handlers that navigate without `preventDefault`. Maintainer 2026-09-28: keep as is for now |
| 1 | MINOR | `Router.kt:224` | `findClosestAnchor`: SVG `<a>`, interactive content inside `<a>`, shadow DOM (R1) | maintainer-decision | Pre-existing, not introduced here |
| 1 | MINOR | spec | No `<base href>`, `blob:`, `target=""` tests (R1) | fix | All three added |

Probed and CLEAN (round 1): ReDoS / stack depth on JVM, JS (1M chars, 8 adversarial shapes, < 50 ms),
Kotlin/JS `matches` anchoring, CR/LF/whitespace rejected, `/.//evil.com` collapsed by `cleanUri`,
cross-origin tricks (`//evil`, `/\evil`, `https://app@evil`) go to the browser, no server-side use
of the regex (no SSRF surface), keyboard Enter activation, `defaultPrevented` ordering vs Preact,
`HashRoutingStrategy` untouched, harness listener order / leaks.

| 2 | MINOR | `Router.kt:164` | Popstate guard one-sided: Back to the fragment-less entry re-resolves, duplicates router history (R1, R2 — reopened round-1 reason, factually false for that direction) | fix | Skip whenever path + query equal the active route's; Back test added, mutant red |
| 2 | MINOR | `Router.kt:236` | `isAbsoluteUrl` misses what the URL parser normalises: `/\host`, `\\host`, tab / CR / LF inside, leading C0 (R1, R2, R3 — round-1 reason "cannot slip" false) | fix | Strip C0 + space at ends, drop tab/LF/CR, read `\` as `/`; 5-case test, mutant red |
| 2 | MINOR | spec | Protocol / origin guards unreachable by any test since `isAbsoluteUrl` (R1, R3 — round-1 "mutant red" stale) | fix | `<base href>` cases: other host same protocol, same host other protocol; each guard's mutant red |
| 2 | MINOR | `Router.kt:210` | http(s)-only check stops all routing on Capacitor / Tauri / Electron / `file://` hosts (R2, new) | fix | Compare protocol + host with the page instead; restores pre-change behaviour there and still excludes `blob:` (host `""`) |
| 2 | MINOR | `Router.kt:230` | `==` on a cross-origin `window.top` reads `.equals` → `SecurityError` (R1) | fix | `===` |
| 2 | MINOR | `Router.kt:232` | Whitespace-only `target` treated as self; HTML makes it a name (R1) | fix | `else -> false`; test added, mutant red |
| 2 | MINOR | `Router.kt:182` | Only the first enabled path router handles a click (R2, new) | maintainer-decision | Recorded in Implementation notes; matches react-router / SvelteKit |
| 2 | MINOR | `regexes.kt:29` | `#` excluded from fragment rejects `https://app/#/docs#install` (R2) | withdrawn in reconcile | Round-1 reason not factually wrong (RFC 3986 forbids it); left to maintainer judgement |
| 2 | MINOR | `regexes.kt:24` | IPv6 branch accepts `[:]` (R3) | withdrawn in reconcile | Overlaps round-1 "documented shape check" rejection |
| 2 | MINOR | docs | `navToUri(evt, …)` + `onClick` never sees middle click; "Which links" fragment row wrong with a query / `<base href>` (R2 docs check) | fix | Both corrected in page and mirror |

Probed and CLEAN (round 2): popstate / fragment ordering; `resolveRouteForUri` strip safe for both
strategies (params are percent-encoded on render); `RouterComponent` key strips `#` before `?`;
`/.//evil.com` and `%2e%2e//` collapse to same-origin `pushState`; regex linear on 1M-char inputs
(JVM and Kotlin/JS), JVM recursion saturates at the label bound (< 256k stack); 300k-case fuzz of
Kotlin/JS `matches` vs true anchoring — no divergence; U+017F / U+212A rejected; no server-side
caller of the regex; relative `?q=` links now keep the current path; demo apps' `onClick` anchors
have no `href`, unaffected. All other docs claims match the code.

**Verdict: gate PASS.** Round 2 clean (no CRITICAL / MAJOR after reconcile); MINOR batch applied once
without re-review, tests green, new guards mutation-checked. Open for the maintainer (MINOR only):
multi-router click handling, `findClosestAnchor` gaps (SVG `<a>`, interactive content inside `<a>`,
shadow DOM), `#` in fragments. Follow-up: `20260928-backnavtrap-fragment-navigation.md`.
