o# Funktor-Demo: Comprehensive Feature Showcase Plan

## Context

The funktor-demo currently only demonstrates a fraction of the funktor framework's capabilities: auth (
login/SSO/password), logging UI, cluster overview, and a dashboard with Kraft UI components (toasts/modals/popups). The
goal is to turn the demo into a comprehensive showcase that demonstrates **every funktor module and feature**
interactively where possible, and with live-data info pages for backend-only features.

## Decisions

- **Backend-only features** (CLI, fixtures, repair, lifecycle hooks): Exposed via API endpoints returning live data from
  the running server (registered hooks, commands, loaders). Shown as info pages with real tables.
- **Scope**: Start with **Wave 1 + 2** (infrastructure + Core Features + REST API Explorer). This establishes the
  pattern for all subsequent waves.
- **Access control**: All showcase pages visible to any authenticated user. Read-only endpoints use
  `authorize { public() }`. Destructive actions (queue job, send email, acquire lock) require `isSuperUser()`.

---

## Gap Analysis: What's Missing

| Module        | Currently Shown      | Not Shown                                                                   |
|---------------|----------------------|-----------------------------------------------------------------------------|
| **core**      | App startup, config  | Lifecycle hooks, CLI, fixtures, repair, retry                               |
| **rest**      | Basic API routes     | Typed routes showcase, authorization DSL, API listing, SSE                  |
| **auth**      | Login, SSO, password | Multi-realm info, password policy validator, provider listing               |
| **cluster**   | Overview UI          | Background jobs (interactive), depot, storage, locks, workers (interactive) |
| **insights**  | Mounted but hidden   | Needs nav link and info                                                     |
| **logging**   | UI works             | Already adequate                                                            |
| **messaging** | Configured           | Send test email, view sent messages                                         |
| **staticweb** | Not shown            | Flash sessions, template info                                               |
| **websocket** | Not shown            | Echo demo, typed messages                                                   |

---

## Proposed Navigation Structure

```
[Sidebar]
  Dashboard (home - enhanced overview with feature cards)
  Profile

  SHOWCASE
    Core Features          -- lifecycle, config, CLI, fixtures, repair, retry
    REST API Explorer      -- typed routes, endpoint listing, "try it"
    Authorization Demo     -- all auth rules, interactive matrix

  AUTH
    Auth Providers         -- realms, providers, capabilities
    Password Policy        -- live validator

  CLUSTER
    Background Jobs        -- queue jobs, live monitor, retry policies
    File Depot             -- upload, browse, download
    Key-Value Storage      -- random data + cache with TTL
    Distributed Locks      -- acquire/release, contention demo
    Workers                -- scheduled tasks, execution history

  MESSAGING
    Send Test Email        -- compose and send
    Sent Messages          -- list with details

  REAL-TIME
    SSE Demo               -- live clock, metrics stream
    WebSocket Demo         -- echo, typed messages, ack/nack

  SYSTEM
    Server Logs            -- (existing)
    Server Internals       -- (existing)
    Insights               -- link to insights GUI
    App Lifecycle          -- uptime, hooks, execution log
```

---

## Page Specifications

### Wave 1: Infrastructure (foundation for all pages)

**1.1 Grouped sidebar navigation**

- Modify `LoggedInLayout.kt` — collapsible sections using Semantic UI accordion/nested menus
- Expand `Nav` object in `nav.kt` with `object showcase { ... }` sub-routes
- All showcase routes registered in the router

**1.2 ShowcaseApiFeature + module wiring**

- New `server/.../api/showcase/ShowcaseApiFeature.kt` — aggregates all showcase `ApiRoutes`
- New `server/.../showcase/ShowcaseModule.kt` — kontainer module registering demo services
- Register in `kontainer.kt` (module wiring) and `ApiApp.kt` auto-discovers via `ApiFeature`

**1.3 ShowcaseApiClient**

- New client in `adminapp` or `common` with typed endpoint definitions matching server routes

---

### Wave 2: Core + REST Showcase

**2.1 Core Features Page** (`/showcase/core`)
| Feature | Endpoint | UI |
|---------|----------|-----|
| Lifecycle hooks | `GET /showcase/core/lifecycle-hooks` | Table: phase, class, execution order |
| Configuration | `GET /showcase/core/config-info` | Sanitized config tree (secrets masked) |
| CLI commands | `GET /showcase/core/cli-commands` | Table: command name, help text |
| Fixtures | `GET /showcase/core/fixtures-info` | Dependency graph visualization |
| Repairs | `GET /showcase/core/repairs-info` | List of registered repairs |
| Retry demo | `POST /showcase/core/retry-demo` | Interactive: configure attempts/delay, see timeline |

**2.2 REST API Explorer** (`/showcase/rest`)
| Feature | Endpoint | UI |
|---------|----------|-----|
| All endpoints listing | `GET /showcase/rest/all-endpoints` | Grouped by feature, shows method/URI/auth |
| Plain route | `GET /showcase/rest/plain` | Returns server time |
| Params route | `GET /showcase/rest/echo/{message}` | Echo typed param |
| Body route | `POST /showcase/rest/transform` | Text transformation |
| Params+Body route | `PUT /showcase/rest/items/{id}` | Combined demo |

Each route type has: code snippet showing definition, "Try It" form, response display.

---

### Wave 3: Auth + Authorization

**3.1 Auth Providers Page** (`/showcase/auth`)

- Lists all `AuthRealm` instances with providers and capabilities
- Password policy live validator (type password, see rules pass/fail)
- `GET /showcase/auth/realms`, `POST /showcase/auth/validate-password`

**3.2 Authorization Demo** (`/showcase/auth-rules`)

- Matrix of all rule types vs current user permissions
- Rules: `public()`, `isSuperUser()`, `forRole()`, `forGroup()`, `forPermission()`, `forAny()`, `forAll()`
- Interactive: toggle mock permissions, see access granted/denied
- `GET /showcase/auth-rules/check`

---

### Wave 4: Cluster Interactive Demos

**4.1 Background Jobs** (`/showcase/cluster/background-jobs`)

- New `DemoBackgroundJobHandler` — configurable delay, success/failure behavior
- Queue form: delay, retry policy (None/LinearDelay), succeed/fail toggle
- Live queue monitor via SSE: `GET /showcase/cluster/jobs/live`
- View archived jobs (completed/failed)
- `POST /showcase/cluster/jobs/queue`, existing cluster APIs for listing

**4.2 File Depot** (`/showcase/cluster/depot`)

- Upload form (base64-encoded in JSON body for simplicity)
- File browser with folder navigation
- Download links
- Shows backend info (FileSystem vs S3)
- `POST /showcase/depot/upload`, `GET /showcase/depot/list`, `GET /showcase/depot/download`

**4.3 Key-Value Storage** (`/showcase/cluster/storage`)

- **Random Data** tab: set category + key + JSON value, retrieve, list all
- **Random Cache** tab: same but with TTL policy, shows cache hit/miss/expiry
- `POST /showcase/storage/data/save`, `GET /showcase/storage/data/load/{category}/{id}`
- `POST /showcase/storage/cache/put`, `GET /showcase/storage/cache/get/{category}/{id}`

**4.4 Distributed Locks** (`/showcase/cluster/locks`)

- Acquire lock button with name and timeout
- Release button
- Active locks table (refreshes automatically)
- "Open in two tabs" instruction for contention demo
- `POST /showcase/locks/acquire`, `POST /showcase/locks/release`, `GET /showcase/locks/list`

**4.5 Workers (enhanced)**

- New `DemoWorker` — increments counter in RandomDataStorage every 30s
- Links to existing workers UI + shows DemoWorker output
- Registered in ShowcaseModule

---

### Wave 5: Messaging

**5.1 Send Test Email** (`/showcase/messaging`)

- Compose form: to (defaults to current user email), subject, body
- Send button, shows result (respects dev overrides)
- `POST /showcase/messaging/send`

**5.2 Sent Messages** (`/showcase/messaging/sent`)

- List of stored sent messages with details
- Shows email hooks chain info
- `GET /showcase/messaging/sent`

---

### Wave 6: Real-Time

**6.1 SSE Demo** (`/showcase/realtime/sse`)

- **Server Clock**: SSE endpoint streaming time every second
- **JVM Metrics**: SSE endpoint streaming heap/threads/CPU every 2s
- Shows `ApiRoute.Sse` definition code
- SSE `GET /showcase/sse/clock`, SSE `GET /showcase/sse/metrics`

**6.2 WebSocket Demo** (`/showcase/realtime/websocket`)

- Connection status indicator
- Echo service: send message, receive back
- Typed messages via `DemoWebsocketClientModule`
- Shows Ack/Nack protocol
- New `DemoWebsocketClientModule` in common

---

### Wave 7: System + Info Pages

**7.1 App Lifecycle** (`/showcase/system/lifecycle`)

- Server uptime, start time, app version
- Hooks table grouped by phase, sorted by execution order
- Custom `ShowcaseLifecycleHook` that records its execution

**7.2 Insights link**

- Nav entry opening insights GUI (`/_/insights/`) in new tab

**7.3 StaticWeb info card**

- Documentation card on Core Features page explaining flash sessions, templates, web resources

---

## File Structure

### Server

```
server/src/main/kotlin/
  api/showcase/
    ShowcaseApiFeature.kt         -- aggregates all showcase ApiRoutes
    CoreShowcaseApi.kt
    RestShowcaseApi.kt
    AuthShowcaseApi.kt
    AuthorizationShowcaseApi.kt
    BackgroundJobsShowcaseApi.kt
    DepotShowcaseApi.kt
    StorageShowcaseApi.kt
    LocksShowcaseApi.kt
    MessagingShowcaseApi.kt
    SseShowcaseApi.kt
    WebSocketShowcaseHandler.kt
  showcase/
    ShowcaseModule.kt             -- kontainer module
    DemoBackgroundJobHandler.kt
    DemoWorker.kt
    ShowcaseLifecycleHook.kt
    ShowcaseRepair.kt
```

### Common

```
common/src/commonMain/kotlin/
  showcase/
    CoreShowcaseModels.kt
    RestShowcaseModels.kt
    AuthShowcaseModels.kt
    StorageShowcaseModels.kt
    MessagingShowcaseModels.kt
    ShowcaseApiClient.kt          -- typed endpoint definitions
    DemoWebsocketClientModule.kt
```

### Admin App

```
adminapp/src/jsMain/kotlin/
  pages/showcase/
    CoreFeaturesPage.kt
    RestFeaturesPage.kt
    AuthorizationDemoPage.kt
    AuthShowcasePage.kt
    BackgroundJobsDemoPage.kt
    DepotDemoPage.kt
    StorageDemoPage.kt
    LocksDemoPage.kt
    MessagingDemoPage.kt
    SseDemoPage.kt
    WebSocketDemoPage.kt
    LifecycleStatusPage.kt
```

### Modified Files

- `server/.../kontainer.kt` — register ShowcaseModule
- `adminapp/.../nav.kt` — expanded routes
- `adminapp/.../layout/LoggedInLayout.kt` — grouped sidebar
- `adminapp/.../api.kt` — add ShowcaseApiClient

---

## Implementation Notes

1. **ApiFeature auto-discovery**: `ApiApp` already collects all `ApiFeature` from kontainer — just adding
   `ShowcaseApiFeature` to kontainer makes all routes available
2. **Auth for showcase endpoints**: All showcase pages visible to any authenticated user. Read-only endpoints use
   `authorize { public() }`. Destructive actions (queue job, send email, acquire lock) require
   `authorize { isSuperUser() }`
3. **Email safety**: Messaging demo respects `OverridingEmailSender` dev overrides — no real emails in dev
4. **File upload**: Use base64-encoded content in JSON body (avoids multipart complexity)
5. **SSE client**: adminapp already has Ktor HTTP Client with SSE plugin installed
6. **WebSocket**: May need Ktor WebSocket plugin installation in `server.kt`

---

## Verification

After each wave:

1. Run the server: `./gradlew :funktor-demo:server:run`
2. Open admin panel: `http://admin.funktor-demo.localhost:36588`
3. Verify new nav items appear and link to correct pages
4. Test each interactive feature (queue job, upload file, acquire lock, send email, etc.)
5. Verify SSE connections stay open and stream data
6. Check authorization: non-superuser should see restricted features as read-only info
