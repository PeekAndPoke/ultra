# Tab specs — what each insights tab is given

The wire shape of every collector slice, **read from current records**, and what its tab shows.

**Re-derived 2026-10-19 (T1)** from real API responses, not from the Kotlin types:

```bash
INSIGHTS_WIRE_SAMPLES_DIR=/some/dir console/wt-gradle.sh :funktor:all:jvmTest
node funktor/codegen/vue-verify/fixtures/refresh.mjs /some/dir
```

The second line copies them into `funktor/codegen/vue-verify/fixtures/`, which the component tests read,
so **those files are the current samples**: `record-slim.json` (the request `InsightsRecordingSpec` records),
`record-vault.json` (queries on Arango and Mongo, profiling and explain on, two log lines), `record-fat.json`
(a record written before 2026-10-19), and the graph endpoints' responses. Regenerate rather than trust
this file once a collector moves.

**Every string is attacker-controlled.** Headers, query parameters, paths and user agents come from
unauthenticated requests. Render with `{{ }}`; **never `v-html`** — pinned by
`vue-verify/tests/escaping.spec.ts`.

## The envelope

`GET /_/funktor/insights/records/{bucket}/{file}` → `data`:

```
ref {bucket, file}   recordedAt {ts, timezone, human}   method   path   status: Int   durationMs
next / previous: ref | null                             collectors: [{key, data}]
```

- `recordedAt` is the depot file's time, `ts` in epoch millis. **The record FILE's own top-level `ts` is
  off by the server's UTC offset** (a `LocalDateTime` written as UTC); the API does not serve it — never read it.
- `status` is a bare number here; the reason phrase is in the `response` slice.
- A BRIEF record has `collectors: []`. An unknown `key` is an app-defined collector: show its raw tree.

## `request`

```json
{ "method": {"value": "GET"}, "scheme": "http", "host": "insights.funktor.local", "port": 80,
  "uri": "/_/funktor/introspection/lifecycle-hooks",
  "headers": {"Authorization": ["***redacted***"], "User-Agent": ["ktor-client"]}, "queryParams": {} }
```

`method` is an object. `uri` is the path only; the tab composes `scheme://host:port + uri`. Header names
keep the client's casing. `***redacted***` is a value to render; a DROPped header is absent.

## `response`

```json
{ "status": {"value": 200, "description": "OK"}, "headers": {"Content-Type": ["application/json; charset=UTF-8"]} }
```

## `user`

```json
{ "user": { "userId": "funktor_all_test_users/59138399", "clientIp": "localhost", "email": "…", "desc": "Super User",
            "type": "test-user", "_type": "logged-in" },
  "permissions": { "isSuperUser": true, "org": null, "accessibleOrgs": [], "branches": [], "groups": [],
                   "roles": [], "permissions": [] } }
```

`userId` is a flat string. **No `isSystem` / `isAnonymous`**: they are functions on `UserRecord`. `_type`
(`anonymous`, `system`, `logged-in`, `api-key`) is the discriminator the tab shows as "Kind".

## `routing`

`{ "trace": "Trace for [_, funktor, …]\n/, segment:0 -> SUCCESS @ /\n  …" }` — pre-formatted text, a `<pre>`.

## `runtime`

```json
{ "jvmVersion": "17.0.17", "kotlinVersion": "2.4.21", "cpus": 16, "maxMem": 536870912, "reservedMem": 408944640,
  "freeMem": 233964432, "openFileDescriptors": 306, "maxFileDescriptors": 1048576, "systemProperties": {"os.name": "Linux"} }
```

Memory in bytes (shown as whole MB). Both descriptor counts are `0` on non-Unix: "not measured", which
the cell itself says, so the Overview strip carries it too. `systemProperties` is the whole JVM set — paths
and user name of the server included; the fixtures keep six keys.

## `log`

```json
{ "entries": [ { "level": "INFO", "text": "2026-10-19 16:52:22.698 INFO - i.p.f.S1ProbeApi - S1 probe: …" } ] }
```

`level` is the `LogLevel` name. `text` is already formatted by `LogAppender.format`: render, do not parse.

## `template`

`{ "timeNs": null }` — null on every request that rendered no view, which is every API call. Normal, not an error.

## `vault`

```json
{ "entries": [ {
    "connection": "MongoDB(8.2.1)::ultra_wt_insights_vue", "queryLanguage": "json",
    "query": "{\n  \"filter\": {\n    \"token\": \"@p1\"\n  },\n  \"sort\": null,\n  \"limit\": 1,\n  \"skip\": null\n}",
    "varsCount": 1, "count": 1, "totalCount": null, "totalNs": 37765040,
    "measureQuery": {"totalNs": 1979916, "count": 1}, "measureIterator": {…}, "measureSerializer": {…},
    "measureDeserializer": {…}, "measureExplain": {…} } ] }
```

- `query` holds placeholders, never a value: Karango's `@v_1` (`aql`), Monko's `@p1` (`json`, since
  2026-10-19). `VaultCollector` keeps the text only for those two languages; for any other it is `null`.
- **No `vars`, no `queryExplained`**: values are counted (`varsCount`), and an EXPLAIN plan has the values
  substituted in by the database itself. An entry WITH either field predates `VaultCollector.Data`: its
  `query` may hold values and must not be shown (`slices.ts` marks it `legacy`).
- Arango's `connection` is `ArangoDB::<db>` (no version), Mongo's `MongoDB(<version>)::<db>`.
- Totals across entries are summed by the frontend; they are not in the record.

## `kontainer`

Two shapes. **The full contract** — v1 and v2, the join, the fingerprint, the caveats — is in
`.claude/tasks/20260824-insights-graphs-and-static-slices.md`, "Wire contract"; the reader is `readKontainer`.

- **v2 (since 2026-10-19)** `{ numOld, numTotal, graphFingerprint, services: [{cls, type, instances: [{cls, createdAt}]}] }`
  — marked by the `graphFingerprint` KEY (its value may be `null`); only services with instances; class names
  flat strings; definitions come from `GET /_/funktor/insights/graphs/kontainer`, joined on `cls`.
- **v1 (before)** `{ numOld, numTotal, info: { services: [...] } }` — every service with its definition inline;
  class names `{"fqn"}`, `codeLocation` `{"location"}`.
- `createdAt` is `{ts, timezone, human}`, `ts` in millis (Jackson-era records: epoch seconds as a double).
- `type == "Singleton"` is process state shared by every request; any other type is this request's own.
  `createdAt` only says whether this request created the instance.
- `numOld` / `numTotal` are always 0 in funktor (tracking cannot be switched on): shown as "not measured".

## `app-config`

```json
{ "info": { "version": {"project": "n/a", "gitBranch": "n/a", "gitDesc": "n/a", …}, "versionName": "…", "versionHash": "…" },
  "config": { "ktor": {…}, "funktor": {…}, "keys": {…} } }
```

Both are free-form (`Any` in Kotlin): a JSON tree. Fields declared `Redacted<T>` arrive as `***redacted***`;
**a secret an app keeps in a plain `String` is recorded and displayed verbatim** — the tab says so in the UI.

## The graph endpoints

`GET …/graphs/kontainer` (every service, its definition, the `fingerprint`) and `GET …/graphs/database`
(`{repos: [{id: {name, connection}, storedClasses: [{fqn, references: [{repo: id | null, fqn}]}]}]}`). Both
superuser-only (`401` otherwise), both static per deployed build; the Vue page fetches each at most once, on the
first click. Contract: the task file named under `kontainer` above.

## Shell — the deleted bar's items

| Old bar item | Source now |
|---|---|
| status code | the envelope's `status` |
| processing time | the envelope's `durationMs` |
| git branch + describe | `app-config` → `info.version` (shown only inside the Config tab's tree) |
| environment id | `app-config` → `config.ktor.application.id` (likewise) |
