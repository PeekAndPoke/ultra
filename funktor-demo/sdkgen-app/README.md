# sdkgen-app — the generated SDK, consumed for real

A minimal Vue 3 + Vite app whose only purpose is to **use** the TypeScript SDK that
`ultra:codegen` / `funktor:codegen` generate from the demo server's Kotlin route graph.

It exists because neither a Kotlin test nor an isolated `tsc` run can show what matters here: that
the emitted code is usable from a real frontend toolchain, with that toolchain's own tsconfig,
module resolution and type checker.

## Running it

```bash
# 1. generate the SDK. `--out` is the APP ROOT, not the SDK directory — `--sdkDir` defaults to
#    `src/funktorsdk` and is appended to it. Pointing --out at src/funktorsdk writes into
#    src/funktorsdk/src/funktorsdk and leaves the real one untouched; nothing errors, and the app
#    then type-checks against a stale SDK. Absolute, because --out resolves against the SERVER
#    module's working dir.
./gradlew :funktor-demo:server:run \
    --args="--cli sdk:ts:generate --out $PWD/funktor-demo/sdkgen-app"

# 2. install and check
cd funktor-demo/sdkgen-app
pnpm install
pnpm run typecheck     # vue-tsc over the app AND the generated SDK
pnpm run dev           # http://localhost:36591
```

Then start the demo server (`./gradlew :funktor-demo:server:run`) and open
<http://localhost:36591>.

**The API lives behind a host matcher.** `server.kt` mounts it under `host("api.*".toRegex())`, so it
is NOT served on plain `localhost:36587` — a request there 404s. The app uses
`http://api.funktor-demo.localhost:36587`, the same base the Kraft frontends use
(`AdminAppConfig.apiBaseUrl`), and the server's CORS list allows this app's origin on **36591** —
next in the series after adminapp 36588, www 36589, ops 36590.

`src/funktorsdk/` is **generated and git-ignored**. The generator owns that directory outright —
every run replaces it wholesale, so nothing hand-written may live there.

## Two things this app pinned down that nothing else did

**`allowImportingTsExtensions` is a real cost, not a predicted one.** Generated files import siblings
as `./runtime/client.ts`, because an extensionless specifier resolves only under
`moduleResolution: bundler` and is `ERR_MODULE_NOT_FOUND` everywhere else while `tsc` stays silent.
That pushes one flag onto every consuming app; `tsconfig.json` here is where that first bites.

**`vue-tsc` cannot run on TypeScript 7.** TS 7 is the native rewrite and no longer exports
`typescript/lib/tsc`, which `vue-tsc` resolves at startup:

```
Error [ERR_PACKAGE_PATH_NOT_EXPORTED]: Package subpath './lib/tsc' is not defined
```

`vue-tsc@3.3.9` is the newest release and its peer range still claims `>=5.0.0`, so the range is
stale rather than accurate. This app therefore pins **TypeScript 5.9.3** while `ultra/codegen`'s
`ts-verify` harness pins **7.0.2**. That divergence is worth keeping: the generated SDK is checked by
two different TypeScript majors, which is stronger evidence than either alone.
