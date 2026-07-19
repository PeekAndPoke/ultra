# Auth API must not echo internal error messages on public routes

**Status:** TODO
**Plan:** none — from `.claude/tasks/20260720-error-response-disclosure-audit.md` (finding 7)
**Security-critical:** yes

## Spec

`funktor/auth/src/jvmMain/kotlin/api/AuthApi.kt:57,79,106,127,148,170,193,216,235`:

```kotlin
} catch (e: AuthError) {
    ApiResponse.forbidden<AuthSignInResponse>().withInfo(e.message ?: "")
}
```

This passes the raw `AuthError.message` to the client, unconditionally — not environment-gated. Some
of these routes are `public()`, so the audience is anonymous.

Most observed messages are generic (`"Invalid credentials"`, `"Weak password"`), but at least two
are account-enumeration oracles: `"User already exists"` on public sign-up, and the
`"User 'user-id' not found"` form seen in `EmailAndPasswordAuthSpec`. An attacker can test an email
list against sign-up/sign-in and learn which accounts exist.

- [ ] `AuthError` carries a closed set of client-safe codes; the API returns the code, not
      `e.message`
- [ ] Sign-up and sign-in return an identical response whether or not the account exists
- [ ] Full detail is still logged server-side for diagnosis
- [ ] Every `AuthError` construction site is reviewed and mapped to a code — enumerate them all,
      the audit did not confirm exhaustively
- [ ] Timing is considered: an early return on "user not found" is itself an oracle, so the
      not-found path should do comparable work (e.g. a dummy password hash verification)

## Implementation notes

- The generic messages currently in use are fine as *codes*; the work is mostly making the mapping
  explicit and closed rather than "whatever the exception said".
- Check the auth specs before changing messages — several assert on exact message text and will need
  updating to assert on codes instead.
- Rate limiting on these routes is a separate concern but pairs naturally; note if absent.

## Test evidence

- [ ] Test that sign-up with an existing email and with a fresh email return identical responses
- [ ] Test that sign-in for an unknown user and a wrong password return identical responses
- [ ] Test that no `AuthError` message text reaches the response body
- [ ] End-to-end via `AppSpec`/`AppUnderTest` against both DB backends (`MatrixTest2d`) — auth
      touches storage
- [ ] Full test command(s) run + green: `./gradlew :funktor:auth:jvmTest`

## Review record (filled by /feature-review)

| Reviewer | Verdict | Confirmed findings |
|---|---|---|
| 1. Implementation & code style | | |
| 2. Domain expert | | |
| 3. Security | | |

Fixes applied: ...

**Red-team follow-up**: `.claude/tasks/20260720-redteam-error-disclosure.md`
