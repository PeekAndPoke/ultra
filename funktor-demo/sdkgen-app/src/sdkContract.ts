/**
 * Compile-time proof that the GENERATED auth types satisfy the HAND-WRITTEN runtime contracts.
 *
 * `runtime/auth.ts`, `login.ts` and `acl-loader.ts` are written structurally on purpose — they must
 * not import generated output, because the runtime ships in SDKs that never reached the auth feature.
 * The cost of that decision is that nothing inside `ultra:codegen` can check the claim its own KDoc
 * makes ("the generated `AuthSignInResponseSuccess` satisfies it"): the generator does not depend on
 * `funktor:auth`, and `ts-verify`'s fixtures are synthetic.
 *
 * This file is where the two halves finally meet, and it is why this app exists. It is checked by
 * `pnpm typecheck` and imported by nobody — `tsconfig.json` includes `src/**' + '/*.ts`, so it is
 * verified without being shipped.
 *
 * **It has already earned its place.** The auth model changed under the SDK twice in two days:
 * `AuthSignInResponse.Token` was deleted, then `Session.Cookie`. Both were silent here until someone
 * regenerated and read the diff.
 *
 * When one of these lines stops compiling, the server contract moved. Fix the runtime to match — do
 * not relax the assertion.
 */
import type {
    AuthSignInResponse,
    AuthSignInResponseSession,
    AuthSignInResponseSuccess,
    UserApiAccessMatrix,
    UserPermissions,
} from './funktorsdk/models.ts'
import type { AccessMatrix } from './funktorsdk/runtime/acl.ts'
import type { SessionCarrier, SignedIn } from './funktorsdk/runtime/auth.ts'
import type { SignInResult } from './funktorsdk/runtime/login.ts'

/**
 * Resolves to [Actual], and fails to compile unless [Actual] is assignable to [Contract].
 *
 * A plain `const x: Contract = {} as Actual` would do the same, but this reads as an assertion and
 * survives `noUnusedLocals` without a `void` or an underscore.
 */
type Satisfies<Contract, Actual extends Contract> = Actual

/**
 * The carrier, and with it the DISCRIMINATOR STRING.
 *
 * The sharpest of these. `SessionCarrier` matches on the literal `'bearer'`; if the Kotlin variant is
 * renamed, Slumber writes a different `_type` and this is the only thing that notices. Without it,
 * `restore()` and `stateOf()` both fall through to logged-out and the bug presents as "login does
 * nothing" — no error, no failed request, just a form that never advances.
 */
export type SessionCarrierParity = Satisfies<SessionCarrier, AuthSignInResponseSession>

/** The success payload the session is built from: `permissions`, `expiresAt.ts`, `userId`. */
export type SignedInParity = Satisfies<SignedIn<UserPermissions>, AuthSignInResponseSuccess>

/** All three sign-in branches, including the two that are NOT failures. */
export type SignInResultParity = Satisfies<SignInResult<UserPermissions>, AuthSignInResponse>

/** What `getMyApiAccess` returns, as `ApiAcl` and `AclLoader` expect to read it. */
export type AccessMatrixParity = Satisfies<AccessMatrix, UserApiAccessMatrix>
