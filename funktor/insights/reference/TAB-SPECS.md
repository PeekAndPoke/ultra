# Tab specs — what each insights tab has to show

The input for the Vue rewrite. For each collector: **the wire shape the API actually serves**, what the
old SemanticUI tab displayed, and what the old tab used that the record no longer carries.

Wire shapes were read out of a real record (`InsightsRecordingSpec`'s depot), not inferred from the
Kotlin types — several differ from what the `Data` class suggests.

**Every string below is attacker-controlled.** Headers, query parameters, paths and user agents come from
unauthenticated requests. Render with `{{ }}` or `v-text`; **never `v-html`**. See
`InsightsCollectorSlice`'s KDoc.

## The envelope

`GET /_/funktor/insights/records/{bucket}/{file}` → `InsightsRecord`:

```
ref { bucket, file }   recordedAt: MpInstant?   durationMs: Double?
next / previous: InsightsRecordRef?            collectors: [{ key, data }]
```

`data` is opaque JSON. A tab looks up its own `key` and parses only that. An unknown key is a record from
an app-defined collector — see D-M6 in the task file; there is no fallback tab yet.

## Shell — page chrome, not a tab

The old GUI had a **bar** (an overlay on every page, per-request) and a **details page** (tabs). Only the
details page survives; the bar's job moves to the list view.

| Old bar item | Source now |
|---|---|
| status code, green/red | `InsightsRecordSummary.status` |
| processing time, red >300ms / yellow >150 / olive >75 / green | `InsightsRecordSummary.durationMs` |
| git branch + describe | `app-config` slice → `info.version` |
| environment id | `app-config` slice → `config.ktor.application.id` |

Old **Overview** tab — keep as the detail page's landing section: request `method + url`, status
(green on 2xx, red otherwise, with the reason phrase), response time, timestamp. Then the Database and
Runtime stat strips inlined from their tabs.

---

## `request`

```json
{ "method": {"value": "GET"}, "scheme": "http", "host": "…", "port": 80,
  "uri": "/path/only", "headers": {"Name": ["v", …]}, "queryParams": {"k": ["v"]} }
```

- **`method` is an object, not a string** — ktor `HttpMethod` serialises as `{value}`. Same for
  `HttpStatusCode` in `response`.
- `uri` is **path only**. The old `Data` had a computed `fullUrl`; it was removed (it skewed the stored
  shape). Vue composes `scheme://host:port + uri` itself.
- `headers` and `queryParams` are already redacted server-side — `***redacted***` is a real value to
  render, not an error. A `DROP`ped header is absent entirely.

Old tab: icon `cloud_upload_alternate`, title "Request", body was a raw JSON dump. A table of
name → values is the obvious improvement; the JSON dump is worth keeping behind a toggle.

## `response`

```json
{ "status": {"value": 200, "description": "OK"}, "headers": {"Name": ["v"]} }
```

Old tab: icon `cloud_download_alternate`, title "Response", raw JSON dump. Same treatment as `request`.
`Set-Cookie` arrives redacted.

## `user`

```json
{ "user": { "userId", "clientIp", "email", "desc", "type", "isSystem", "isAnonymous" },
  "permissions": { "isSuperUser", "org", "accessibleOrgs", "branches", "groups", "roles", "permissions" } }
```

`userId` is a flat string — the value class does not survive as an object. Old tab: icon `user`, two
`H4` sections "User" and "Permissions", each a JSON dump. Old bar showed `userId` alone.

## `routing`

```json
{ "trace": "…multi-line ktor RoutingResolveTrace…" }
```

A pre-formatted text tree. Old tab: icon `compass_outline`, `<pre>` of the trace, then a JSON dump.
Keep the `<pre>`; the JSON dump adds nothing here.

## `runtime`

```json
{ "jvmVersion", "kotlinVersion", "cpus", "maxMem", "reservedMem", "freeMem",
  "openFileDescriptors", "maxFileDescriptors", "systemProperties": {"k": "v"} }
```

Old tab: icon `microchip`, title "Runtime". A horizontal stat strip of seven cells — **JVM**, **Kotlin**,
**CPUs**, **Free Heap**, **Reserved Heap**, **Max Heap** (all three as `%d MB`, dividing by 1024·1024),
**File descriptors** (`open:` / `max:` on two lines) — then a plain list of `systemProperties`.

The same stat strip appeared inside Overview. Old bar showed `$cpus CPUs` and free/reserved/max in GB to
two decimals (dividing by 1e9 — deliberately different units from the tab's MB).

`openFileDescriptors`/`maxFileDescriptors` are `0` on non-Unix.

## `log`

```json
{ "entries": [ { "level": "INFO", "text": "…preformatted…" } ] }
```

`level` serialises as the enum name: `ALL TRACE DEBUG INFO WARNING ERROR OFF`. `text` is already
formatted by `LogAppender.format` — render as `<pre>`, do not re-parse.

Old tab: icon `list`, title "Logs". Empty → a single "No log entries" message. Otherwise one message
block per entry, coloured by level: INFO → positive/green, WARNING → warning/yellow, ERROR and OFF →
error/red, TRACE/DEBUG/ALL → neutral. Old bar showed the entry count.

## `template`

```json
{ "timeNs": null }
```

View rendering time, `null` when nothing rendered — which is every API request, so the tab must handle
null as the normal case, not an error.

Old tab: icon `tv`, title "View", JSON dump. Old bar: `%.2f ms` with a `tv` icon, red >30ms,
yellow >10ms, green otherwise. Those thresholds are the only content this collector really has.

## `vault` — database queries

```json
{ "entries": [ QueryProfiler.Entry.Impl ] }
```

Empty in the sample; the shape comes from `ultra/vault/.../profiling/QueryProfiler.kt`. Each entry has
`connection`, `count`, `totalCount?`, `query?`, `queryLanguage`, `vars?`, `queryExplained?`, `totalNs`,
and five sub-measures — `measureSerializer`, `measureQuery`, `measureIterator`, `measureDeserializer`,
`measureExplain` — each `{ totalNs, count }`.

**Every total was a private `lazy` property and is therefore NOT in the record.** Vue sums them:
`totalTimeNs = Σ entries.totalNs`, and one per sub-measure.

Old tab: icon `database`, title "Database". A seven-cell stat strip — Queries (count), Total, Serializer,
Query, Iterator, Deserializer, Explain, each as ms — then one segment per query:

- header `Query #n took X ms - <connection>`
- a six-cell strip: `Results: count of total totalCount|n/a`, then each sub-measure as
  `X ms (Nx)`
- the query itself, syntax-highlighted by `queryLanguage` (Prism)
- `vars` as JSON when non-empty
- `queryExplained` inside a collapsed `<details>Explained</details>`

The same stat strip appeared inside Overview.

> **Gap — the database graph cannot be rebuilt from a record.** The old tab also drew a vis.js network of
> repositories and their references, built from `DatabaseGraphBuilder`, a **live kontainer service**
> queried at render time. It is not in the stored data. Either the graph moves to its own live endpoint
> (it describes the schema, not the request — arguably where it belongs), or `VaultCollector` starts
> storing `DatabaseGraphModel`, which is static per boot and would bloat every record. **Decide before
> promising this tab.**

## `kontainer`

```json
{ "numOld": 0, "numTotal": 0,
  "info": { "services": [ { "cls": {"fqn"}, "type": "Singleton",
      "definition": { "creates": {"fqn"}, "injectionType", "injects": [{"name", "classes": [{"fqn"}], "provisionType"}],
                      "codeLocation": {"location"}, "overwrites": null },
      "instances": [ {"createdAt": 1785496992.70, "cls": {"fqn"}} ] } ] } }
```

`createdAt` is epoch **seconds** as a double. `type` and `injectionType` are enum names:
`Singleton Prototype Dynamic SemiDynamic DynamicOverride`. `provisionType` is `Direct` or `Lazy`.
`overwrites` is a nested definition of the same shape, or null — a chain.

The biggest slice by far: **137.8 KB average, 73.6% of a record.**

Old tab: icon `cubes`, title "Kontainer". A sortable table sorted by *services with instances first, then
FQN*, with columns **Service** (fqn), **Type**, **Instances** (fqn + createdAt per instance), **Injects**
(param name, then the classes it may receive), **Definition & Overwrites** (walk the `overwrites` chain;
the first row plain, later rows prefixed `Overwrites -> `, each showing `injectionType - creates.fqn` and
the code location).

Two vis.js graph buttons, differing only in which services are included:

| Button | Nodes |
|---|---|
| "Show instances Graph" (green) | services with `instances` non-empty |
| "Show Full Graph" (red) | all services |

Node = a service: label `simpleName\ntype`, size = number of injected classes, colour by type —
Singleton `#1E90FF`, Prototype `#FFD700`, Dynamic `#8B0000`, SemiDynamic `#9400D3`, DynamicOverride
`#DC143C`. Edge = an injection, `from` service `to` injected class, arrow at the target, **dashed when
`provisionType == Lazy`**, weight 2 for Direct / 1 for Lazy.

All of that is derivable from the record — unlike the vault graph. The layout physics is vis.js tuning,
not spec; pick whatever the Vue graph library does well.

Old bar: `young / old / total` as `${numTotal - numOld} / $numOld / $numTotal`, icon green <10 old,
yellow <50, red otherwise. `numOld` is "alive longer than 15 minutes" — the threshold is hardcoded.

## `app-config`

```json
{ "info": { "version": {…}, "versionName", "versionHash" }, "config": { …the whole AppConfig… } }
```

`info` is `AppInfo`; `config` is the entire application config. Both are `Any` in Kotlin and arrive as
free-form JSON — this is the collector that cannot be typed (see the task file), so a generic JSON tree
view is the honest rendering.

Old tab: icon `cog`, title "Config", two `H4` sections "AppInfo" and "Config", each a JSON dump.

> **The STOP is lifted — but read this before building the tab.** The four known leaks (JWT signing key,
> CSRF secret, Arango password, Mongo connection string) were closed on 2026-08-01 by `Redacted<T>`, which
> redacts at the *serializer*, not just in `toString()`. Verified 2026-08-02: all four are declared
> `Redacted<String>` — `JwtSigningKey.secret:36`, `UltraSecurityConfig.csrfSecret:6`,
> `ArangoDbConfig.password:13`, `MongoDbConfig.connectionString:7`. Task archived at
> `.claude/tasks-archive/2026-07/20260731-config-secrets-in-insights.md`.
>
> **What is NOT closed:** `Redacted<T>` protects a *declared* field, and this collector serialises the
> whole `AppConfig` as `Any`. An application config that holds a secret in a plain `String` still writes
> it into every record, and this tab would put it on a screen. That is a property of the app's own config,
> which the framework cannot see — so the tab ships, and the docs for it must say plainly that anything
> not wrapped in `Redacted` is rendered verbatim.

---

## Deleting from here

Per `README.md`: when a tab ships, delete its `collectors/*.kt` reference file **and its section above**.
When both are empty, delete the directory.
