## Kraft — Build SPAs in Pure Kotlin

### Overview {#kraft-overview}

# Kraft — Build SPAs in Pure Kotlin

A component-based framework for building single-page applications.
Kotlin all the way down — no JavaScript, no template engines, no compromises.

## What is Kraft?

Kraft lets you build reactive web applications using **only Kotlin**. It compiles to JavaScript and
uses [Preact](https://preactjs.com) (a 3KB React alternative) as its rendering engine — but you never touch Preact
directly.

Instead, you write components like this:

```kotlin
class Counter(ctx: Ctx<Props>) : Component<Counter.Props>(ctx) {
    data class Props(val start: Int)

    private var count by value(props.start)

    override fun VDom.render() {
        div {
            h2 { +"Count: $count" }
            ui.blue.button {
                onClick { count++ }
                +"Increment"
            }
        }
    }
}
```

That's a complete, working component. State management (`value()`), event handling (`onClick`), and type-safe HTML — all
in one place.

## Why Kraft?

**Not another React wrapper.** Kotlin/JS React wrappers try to map React's API to Kotlin. Kraft takes a different
approach: it provides its own component model designed for Kotlin, then uses Preact as a lightweight rendering layer
underneath.

This means:

- **Zero JavaScript dependencies** beyond Preact (which itself has zero dependencies)
- **Kotlin idioms** everywhere — data classes for props, delegated properties for state, extension functions for the DSL
- **Type safety** from props to HTML to CSS — your IDE catches errors before the browser does
- **No template engine** — kotlinx.html gives you type-safe markup as code

## What you get

- **Components** — Class-based and functional components with props, state, and lifecycle hooks.
- **Reactive State** — `var x by value(0)` — change the value, the UI updates. Built-in stream support.
- **Routing** — Type-safe routes with params, middleware for auth guards, and nested layouts.
- **Forms & Validation** — Two-way binding, field validation, draft/commit pattern — all type-safe.
- **SemanticUI DSL** — `ui.blue.button { +"Click" }` — a beautiful, fluent API for FomanticUI.
- **12 Addons** — ChartJS, PrismJS, PDF viewer, PixiJS (2D WebGL), Three.js (3D WebGL), markdown, signature pad, and
  more — loaded on demand
  via the AddonRegistry.

## Quick taste

Here's a complete app with routing:

```kotlin
fun main() {
    val app = kraftApp {
        semanticUI()
        routing {
            usePathStrategy()
            mount(Static("/")) { HomePage() }
            mount(Route1("/users/{id}")) { route ->
                UserPage(route["id"])
            }
        }
    }

    app.mount("#app", PreactVDomEngine()) {
        RouterComponent()
    }
}
```

## Example Projects

Clone and run these standalone example repos to see Kraft in action:

- **Hello World** — Minimal project — mount a component, see it
  render. https://github.com/PeekAndPoke/kraft-example-helloworld
- **Router Demo** — Multi-page app with parameterized routes and form handling with
  Mutator. https://github.com/PeekAndPoke/kraft-example-router
- **Remote API** — Consume REST APIs and display results with routing and
  SemanticUI. https://github.com/PeekAndPoke/kraft-example-remote

---

### Getting Started {#kraft-getting-started}

# Getting Started

From zero to a working Kraft app in five minutes.

## Prerequisites

- **JDK 17+** — Kraft compiles Kotlin to JavaScript, but the build runs on the JVM
- **Gradle** — the build system (wrapper included in most projects)

## 1. Set up your Gradle project

In your `build.gradle.kts`:

```kotlin
plugins {
    kotlin("multiplatform") version "2.3.10"
    kotlin("plugin.serialization") version "2.3.10"
}

kotlin {
    js {
        browser {
            testTask { }
        }
        binaries.executable()
    }

    sourceSets {
        jsMain {
            dependencies {
                // Kraft core
                implementation("io.peekandpoke.kraft:core:0.102.0")

                // SemanticUI integration (optional but recommended)
                implementation("io.peekandpoke.kraft:semanticui:0.102.0")
            }
        }
    }
}
```

## 2. Create your entry point

Create `src/jsMain/resources/index.html`:

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>My Kraft App</title>
    <link rel="stylesheet"
          href="https://cdn.jsdelivr.net/npm/fomantic-ui@2.8.8/dist/semantic.min.css">
</head>
<body>
    <div id="app"></div>
    <script src="your-project-name.js"></script>
</body>
</html>
```

## 3. Write your first app

Create `src/jsMain/kotlin/main.kt`:

```kotlin
import io.peekandpoke.kraft.components.*
import io.peekandpoke.kraft.routing.Static
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.kraft.vdom.preact.PreactVDomEngine
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.*

// Define the app and its routes
val app = kraftApp {
    semanticUI()
    routing {
        mount(Static("")) { HomePage() }
    }
}

// Entry point
fun main() {
    app.mount("#app", PreactVDomEngine()) {
        RouterComponent()
    }
}
```

## 4. Create your first component

Create `src/jsMain/kotlin/HomePage.kt`:

```kotlin
import io.peekandpoke.kraft.components.*
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.*

// Factory function — this is how other components use yours
@Suppress("FunctionName")
fun Tag.HomePage() = comp {
    HomePage(it)
}

// The component class
class HomePage(ctx: NoProps) : PureComponent(ctx) {

    // Reactive state — changing this redraws the component
    private var count by value(0)

    override fun VDom.render() {
        ui.container {
            ui.segment {
                ui.header H1 { +"Hello from Kraft!" }

                p { +"You clicked the button $count times." }

                ui.blue.button {
                    onClick { count++ }
                    +"Click me"
                }
            }
        }
    }
}
```

## 5. Run it

```bash
./gradlew -t --parallel jsRun
```

The `-t` flag enables continuous build — save a file and the browser refreshes.

## What just happened?

Let's break down the patterns you just used:

### The factory function pattern

```kotlin
@Suppress("FunctionName")
fun Tag.HomePage() = comp {
    HomePage(it)
}
```

Every component has a **factory function** — an extension on `Tag` that creates the component via `comp { }`. This is
how you compose components in the HTML DSL:

```kotlin
override fun VDom.render() {
    div {
        HomePage()           // Just call the factory function
        Counter(start = 5)   // With props
    }
}
```

### Reactive state with `value()`

```kotlin
private var count by value(0)
```

`value()` creates a **delegated property** that automatically triggers a redraw when changed. No `setState()`, no
actions, no dispatchers — just assign a new value.

### The HTML DSL

```kotlin
ui.container {
    ui.segment {
        ui.header H1 { +"Hello" }
    }
}
```

This is [kotlinx.html](https://github.com/Kotlin/kotlinx.html) with the SemanticUI DSL layered on top. Standard HTML
tags (`div`, `p`, `button`) work alongside SemanticUI helpers (`ui.segment`, `ui.button`).

---

### Components {#kraft-components}

# Components

Everything in Kraft is a component. Here's how they work.

## The component model

Every Kraft component is a Kotlin class that extends `Component<PROPS>` and implements one method: `render()`.

```kotlin
class Greeting(ctx: Ctx<Props>) : Component<Greeting.Props>(ctx) {
    data class Props(val name: String)

    override fun VDom.render() {
        h2 { +"Hello, ${props.name}!" }
    }
}
```

Alongside the class, you define a **factory function** so other components can use it in the HTML DSL:

```kotlin
@Suppress("FunctionName")
fun Tag.Greeting(name: String) = comp(
    Greeting.Props(name = name)
) { Greeting(it) }
```

Now any parent component can write:

```kotlin
override fun VDom.render() {
    Greeting(name = "World")
}
```

## Component types

### Component with props

The most common type. Props are a `data class` — immutable, type-safe, and IDE-friendly.

```kotlin
class UserCard(ctx: Ctx<Props>) : Component<UserCard.Props>(ctx) {
    data class Props(
        val name: String,
        val email: String,
        val role: String = "member",  // Default values work
    )

    override fun VDom.render() {
        ui.card {
            noui.content {
                ui.header { +props.name }
                noui.description { +props.email }
            }
            noui.extra.content {
                ui.label { +props.role }
            }
        }
    }
}
```

### PureComponent (no props)

For components that don't need external data — pages, layouts, root components:

```kotlin
class DashboardPage(ctx: NoProps) : PureComponent(ctx) {
    override fun VDom.render() {
        ui.container {
            ui.header H1 { +"Dashboard" }
            // ...
        }
    }
}
```

`PureComponent` is a convenience alias for `Component<Any?>` with `NoProps` context.

### Functional components

For simple, stateless rendering — no class needed:

```kotlin
val Badge = component { name: String, color: String ->
    ui.with(color).label { +name }
}

// Usage
override fun VDom.render() {
    Badge("Admin", "red")
    Badge("Active", "green")
}
```

Functional components support up to 10 parameters.

## Lifecycle hooks

Components have four lifecycle hooks:

```kotlin
class LiveSearch(ctx: Ctx<Props>) : Component<LiveSearch.Props>(ctx) {
    data class Props(val query: String)

    init {
        lifecycle {
            onMount {
                // Component is now in the DOM
                // `dom` property gives you the HTMLElement
                console.log("Mounted at", dom?.tagName)
            }

            onUpdate {
                // Component re-rendered (props or state changed)
            }

            onUnmount {
                // Component removed from DOM
                // Streams auto-unsubscribe, but clean up other resources here
            }

            onNextProps { newProps, oldProps ->
                // Parent sent new props
                if (newProps.query != oldProps.query) {
                    reload()
                }
            }
        }
    }

    override fun VDom.render() {
        // ...
    }
}
```

**Key point:** Stream subscriptions (via `subscribingTo()`) auto-unsubscribe when the component unmounts. You don't need
to clean them up manually.

**Subscribing to the same hook multiple times is totally fine.** Handlers don't replace each other — they stack. When
the event fires, each registered handler is called one after the other in the order it was registered. This lets you
split unrelated concerns into separate blocks without merging them into a single handler:

```kotlin
init {
    lifecycle {
        onMount {
            analytics.trackView("dashboard")
        }
        onMount {
            tooltip = Tooltip(dom!!)
        }
    }
}
```

## Component references

Sometimes a parent needs to call methods on a child. Use `ComponentRef`:

```kotlin
class DrawingApp(ctx: NoProps) : PureComponent(ctx) {

    // Create a ref tracker for the SignaturePad component
    private val padRef = ComponentRef.Tracker<SignaturePad>()

    override fun VDom.render() {
        // The SignaturePad component, tracked by our ref
        SignaturePad {
            // configuration...
        }.track(padRef)

        // Use the ref to access the child component
        padRef { pad ->
            ui.button {
                onClick { pad.clear() }
                +"Clear signature"
            }

            if (pad.isEmpty()) {
                ui.red.label { +"No signature yet" }
            }
        }
    }
}
```

The `padRef { ... }` block only renders when the ref is attached — it's safe by design.

## Composing components

Components compose naturally through the HTML DSL:

```kotlin
class App(ctx: NoProps) : PureComponent(ctx) {
    override fun VDom.render() {
        div(classes = "app") {
            NavBar()
            ui.container {
                Sidebar()
                MainContent()
            }
            Footer()
        }
    }
}
```

### Passing callbacks

Components communicate upward through callback props:

```kotlin
class TodoItem(ctx: Ctx<Props>) : Component<TodoItem.Props>(ctx) {
    data class Props(
        val text: String,
        val done: Boolean,
        val onToggle: () -> Unit,
        val onDelete: () -> Unit,
    )

    override fun VDom.render() {
        ui.item {
            ui.checkbox {
                input(type = InputType.checkBox) {
                    checked = props.done
                    onChange { props.onToggle() }
                }
                label { +props.text }
            }
            ui.red.icon.button {
                onClick { props.onDelete() }
                icon.trash()
            }
        }
    }
}
```

### The `shouldRedraw` optimization

By default, components redraw whenever props change. Override `shouldRedraw` to skip unnecessary renders:

```kotlin
override fun shouldRedraw(nextProps: Props): Boolean {
    return nextProps != props  // Only redraw if props actually changed
}
```

Since `Props` is a data class, `!=` does a structural comparison — this is efficient and correct.

## Accessing app services

Components can access app-level services through the attribute system:

```kotlin
override fun VDom.render() {
    // These are available in any component
    val router = router           // Navigation
    val modals = modals           // Show modals
    val toasts = toasts           // Show notifications
    val responsive = responsive   // Screen size info
}
```

These are injected via Kraft's attribute system — no manual wiring needed.

---

### State Management {#kraft-state-management}

# State Management

Three mechanisms, each for a different job. No ceremony, no boilerplate.

## 1. Local state with `value()`

The simplest and most common pattern. Declare a property with `value()`, change it, and the component redraws:

```kotlin
class Counter(ctx: NoProps) : PureComponent(ctx) {
    private var count by value(0)

    override fun VDom.render() {
        div { +"Count: $count" }
        ui.button {
            onClick { count++ }  // This triggers a redraw
            +"Increment"
        }
    }
}
```

`value()` uses Kotlin's **delegated properties**. Under the hood, the setter calls `triggerRedraw()` — you don't need to
think about it.

### With change callbacks

React to state changes:

```kotlin
private var query by value("") { newValue ->
    console.log("Query changed to: $newValue")
    search(newValue)
}
```

### Complex state

Use a data class when you have related state:

```kotlin
data class State(
    val name: String = "",
    val email: String = "",
    val agreed: Boolean = false,
)

private var state by value(State())

// Update one field — immutable copy, automatic redraw
onClick { state = state.copy(agreed = true) }
```

## 2. Stream-backed state

For state that should be debounced, throttled, or transformed before triggering a redraw:

```kotlin
class SearchBox(ctx: NoProps) : PureComponent(ctx) {

    // Debounce input by 300ms before searching
    private var query by stream("") {
        it.debounce(300.milliseconds)
    } handler { debouncedQuery ->
        performSearch(debouncedQuery)
    }

    override fun VDom.render() {
        ui.input {
            input {
                value = query
                onInput { query = it.target.asDynamic().value as String }
            }
        }
    }
}
```

The stream config lambda lets you apply operators from the Ultra streams library — `debounce`, `throttle`, `map`,
`filter`, and more.

## 3. Subscribing to external streams

When state lives outside the component — global state, shared data, tickers:

```kotlin
class LiveDashboard(ctx: NoProps) : PureComponent(ctx) {

    // Subscribe to a global auth state
    private val auth by subscribingTo(AppState.auth)

    // Subscribe to a ticker that emits every second
    private val tick by subscribingTo(ticker(1.seconds))

    override fun VDom.render() {
        div { +"Logged in as: ${auth.user?.name}" }
        div { +"Uptime: ${tick.count}s" }
    }
}
```

**Key behaviors:**

- The component redraws whenever the stream emits a new value
- Subscriptions auto-unsubscribe when the component unmounts — no memory leaks
- You can subscribe to any `Stream<T>` from the Ultra streams library

### Reactive transformations

Combine streams with operators:

```kotlin
// A sine wave that updates every 100ms
private val wave by subscribingTo(
    ticker(100.milliseconds).map { tick ->
        sin(tick.count * 10 * PI / 180.0)
    }
)
```

## Persisting state in local storage

Kraft integrates with browser local storage for state that should survive page reloads:

```kotlin
@Serializable
data class UserPreferences(val theme: String = "dark", val lang: String = "en")

class SettingsPage(ctx: NoProps) : PureComponent(ctx) {

    // This state persists in localStorage under key "user-prefs"
    private val prefs = StreamSource(UserPreferences())
        .persistInLocalStorage("user-prefs", UserPreferences.serializer())

    private val currentPrefs by subscribingTo(prefs)

    override fun VDom.render() {
        div { +"Theme: ${currentPrefs.theme}" }

        ui.button {
            onClick {
                prefs(currentPrefs.copy(theme = "light"))
            }
            +"Switch to light"
        }
    }
}
```

The value is serialized to JSON and stored in `localStorage`. On next page load, it's restored automatically.

## Async data loading

For loading async data with loading/error/loaded states, Kraft provides the `dataLoader` pattern. Here's a quick
example:

```kotlin
private val loader = dataLoader {
    flow {
        val user = api.getUser(props.userId)
        emit(user)
    }
}

override fun VDom.render() {
    loader(this) {
        loading { ui.active.loader { } }
        error { err -> ui.negative.message { +"Failed: ${err.message}" } }
        loaded { user -> ui.header { +user.name } }
    }
}
```

This covers the basics -- reloading, silent refresh, state inspection, and more are documented in the dedicated Data
Loading section below.

## Patterns summary

| What you need             | Use this                                 | Example           |
|---------------------------|------------------------------------------|-------------------|
| Simple local state        | `var x by value(0)`                      | Counter, toggles  |
| Debounced/throttled state | `stream("") { it.debounce(300.ms) }`     | Search input      |
| Global/shared state       | `val x by subscribingTo(stream)`         | Auth, preferences |
| Persistent state          | `StreamSource().persistInLocalStorage()` | User settings     |
| Async data                | `dataLoader { flow { emit(data) } }`     | API calls         |

---

### Data Loading {#kraft-data-loading}

# Data Loading

Load async data with built-in loading, loaded, and error states.

The `DataLoader` wraps an async operation and tracks its state. You define what to load, and it gives you a sealed state
you can render against -- no manual boolean flags.

## Basic usage

Create a loader in your component with `dataLoader`. It starts loading immediately on mount:

```kotlin
class UserList(ctx: NoProps) : PureComponent(ctx) {

    private val loader = dataLoader {
        flow {
            val users = api.fetchUsers()
            emit(users)
        }
    }

    override fun VDom.render() {
        loader(this) {
            loading {
                ui.loading.segment { +"Loading users..." }
            }
            loaded { users ->
                ui.list {
                    users.forEach { user ->
                        noui.item { +user.name }
                    }
                }
            }
            error { err ->
                ui.error.message { +"Failed: ${err.message}" }
            }
        }
    }
}
```

The `loader(this)` call renders the appropriate block based on the current state. Only one block runs at a time.

## Fixed values

When you already have the data and don't need to load it asynchronously, use `dataLoaderOf`:

```kotlin
private val loader = dataLoaderOf(listOf("Alice", "Bob", "Charlie"))
```

This creates a loader that starts in the `Loaded` state immediately.

## Reloading

Trigger a reload from a button click or any event. Two variants:

```kotlin
// Standard reload — resets to Loading state, shows loading UI
loader.reload()

// Silent reload — refetches without showing loading state
loader.reloadSilently()
```

Both methods accept an optional debounce in milliseconds (default: 200ms):

```kotlin
// Debounce rapid reloads (e.g. from a search input)
loader.reload(debounceMs = 500)
loader.reloadSilently(debounceMs = 500)
```

## State inspection

Check the current state programmatically:

```kotlin
loader.isLoading()     // true during initial load or after reload()
loader.isLoaded()      // true when data is available
loader.isError()       // true when the flow threw an exception

// Negated versions for convenience
loader.isNotLoading()
loader.isNotLoaded()
loader.isNotError()
```

## Modifying loaded data

Update the loaded value without triggering a full reload:

```kotlin
// Transform the current value
loader.modifyValue { users ->
    users.filter { it.isActive }
}

// Replace the value entirely
loader.setLoaded(newUsers)

// Set an arbitrary state
loader.setState(DataLoader.State.Loading())
```

## Stream access

The loader exposes its state and value as Streams, so you can subscribe to changes or compose with other reactive
sources:

```kotlin
// The full state (Loading | Loaded | Error)
val stateStream: Stream<DataLoader.State<T>> = loader.state

// Just the loaded value (null when not loaded)
val valueStream: Stream<T?> = loader.value
```

## Full example

A component that loads data, shows a refresh button, and handles all three states:

```kotlin
class ProductList(ctx: NoProps) : PureComponent(ctx) {

    private val loader = dataLoader {
        flow { emit(api.fetchProducts()) }
    }

    override fun VDom.render() {
        ui.segment {
            ui.blue.button {
                onClick { loader.reloadSilently() }
                icon.sync_alternate.render()
                +"Refresh"
            }

            loader(this) {
                loading {
                    ui.active.centered.inline.loader {}
                }
                loaded { products ->
                    ui.relaxed.divided.list {
                        products.forEach { product ->
                            noui.item {
                                noui.content {
                                    noui.header { +product.name }
                                    noui.description { +product.description }
                                }
                            }
                        }
                    }
                }
                error { err ->
                    ui.error.message {
                        noui.header { +"Could not load products" }
                        p { +err.message.orEmpty() }
                    }
                }
            }
        }
    }
}
```

---

### Messaging {#kraft-messaging}

# Messaging

Type-safe message passing between components, bubbling up the tree.

Messages let a deeply nested child component notify an ancestor without threading callbacks through every intermediate
component. A message bubbles up from child to parent until it reaches the root or is stopped.

## Defining a message

A message is a data class that extends `MessageBase`:

```kotlin
data class ItemSelected(
    override val sender: Component<*>,
    val itemId: String,
) : MessageBase<Component<*>>(sender)
```

The `sender` field tracks which component sent the message. Add any payload fields you need -- here it's `itemId`.

## Sending a message

Call `sendMessage` from any component:

```kotlin
class ItemCard(ctx: Ctx<Props>) : Component<ItemCard.Props>(ctx) {
    data class Props(val item: Item)

    override fun VDom.render() {
        ui.card {
            onClick {
                sendMessage(ItemSelected(sender = this@ItemCard, itemId = props.item.id))
            }
            noui.content {
                noui.header { +props.item.name }
            }
        }
    }
}
```

The message dispatches on the sender's parent, then continues up the tree. The sender itself does not receive its own
message.

## Listening for messages

Register a listener in your component's `init` block with `onMessage`:

```kotlin
class ItemBrowser(ctx: NoProps) : PureComponent(ctx) {

    private var selectedId: String? by value(null)

    init {
        onMessage<ItemSelected> { msg ->
            selectedId = msg.itemId
        }
    }

    override fun VDom.render() {
        ui.cards {
            items.forEach { item ->
                ItemCard(item)
            }
        }
        selectedId?.let { id ->
            p { +"Selected: $id" }
        }
    }
}
```

The type parameter on `onMessage<ItemSelected>` filters by message type -- only `ItemSelected` messages trigger this
handler.

## Stopping propagation

By default, messages continue bubbling after a handler processes them. To stop a message from reaching further
ancestors, call `stop()`:

```kotlin
onMessage<ItemSelected> { msg ->
    selectedId = msg.itemId
    msg.stop()  // no ancestor will see this message
}
```

## When to use messaging

| Pattern           | When to use                                                                                |
|-------------------|--------------------------------------------------------------------------------------------|
| **Callback prop** | Direct parent-child. The parent passes `onSelect: (String) -> Unit` as a prop.             |
| **Message**       | Deeply nested child needs to notify a distant ancestor. Intermediaries don't need to know. |
| **Stream**        | Cross-cutting state that multiple unrelated components observe. See State Management.      |

---

### Routing {#kraft-routing}

# Routing

Type-safe routes with parameters, middleware for auth guards, and nested layouts.

## Defining routes

Routes are objects — not strings scattered across your codebase:

```kotlin
object Nav {
    val home = Static("/")
    val about = Static("/about")
    val userProfile = Route1("/users/{id}")
    val userPost = Route2("/users/{id}/posts/{postId}")
}
```

`Static` is for routes with no parameters. `Route1` through `Route7` support 1-7 typed parameters.

## Setting up the router

```kotlin
val app = kraftApp {
    routing {
        usePathStrategy()  // Clean URLs: /users/123
        // or useHashStrategy() for hash URLs: /#/users/123

        mount(Nav.home) { HomePage() }
        mount(Nav.about) { AboutPage() }

        mount(Nav.userProfile) { route ->
            val userId = route["id"]
            UserProfilePage(userId)
        }

        catchAll { NotFoundPage() }
    }
}
```

The `catchAll` block handles any URL that doesn't match a defined route.

## Rendering the current route

Drop `RouterComponent()` wherever you want the active page to appear:

```kotlin
class App(ctx: NoProps) : PureComponent(ctx) {
    override fun VDom.render() {
        NavBar()
        div(classes = "content") {
            RouterComponent()  // Active page renders here
        }
        Footer()
    }
}
```

## Navigation

### Programmatic navigation

```kotlin
// Navigate to a static route
router.navToUri(Nav.about())

// Navigate with parameters
router.navToUri(Nav.userProfile("alice"))

// Navigate handling a click event (prevents default, uses router)
ui.button {
    onClick { evt -> router.navToUri(evt, Nav.home()) }
    +"Go home"
}

// Go back
router.navBack()

// Replace current URL (no history entry)
router.replaceUri(Nav.home())
```

### Link-style navigation with `A.href(route)`

For anchor tags, use the `href` helper on `A` — it takes a bound route and writes the correct URL into the `href`
attribute:

```kotlin
ui.menu {
  noui.item A { href(Nav.home()); +"Home" }
  noui.item A { href(Nav.about()); +"About" }
  noui.item A { href(Nav.userProfile("alice")); +"Alice" }
}
```

**Always use `A.href(route)` for links — don't hand-write `href = "/about"`.** The helper asks the active router to
render the URL using the configured path strategy, so the same code produces `/about` under `usePathStrategy()` and
`#/about` under `useHashStrategy()`. Hard-coded href strings bypass the strategy and break when you switch modes (or
when the app is mounted under a non-root base path).

The helper lives in `io.peekandpoke.kraft.routing`:

```kotlin
/** Sets the href attribute of an anchor tag from a bound route. */
fun A.href(route: Route.Bound) {
  val c = consumer as? VDomTagConsumer ?: error("Consumer must be a VDomTagConsumer")
  val router = c.host.router

  href = router.strategy.render(route)
}
```

Because it sets a real `href`, the browser handles Ctrl/Cmd+Click, middle-click, right-click → "Open in new tab", and
link previews natively. For clicks that should navigate in-place (and not reload the page), combine `href` with an
`onClick` that calls `router.navToUri(evt, route)` — Kraft will detect modifier keys and let the browser handle
new-tab clicks:

```kotlin
a {
  href(Nav.userProfile("alice"))
  onClick { evt -> router.navToUri(evt, Nav.userProfile("alice")) }
  +"Alice"
}
```

## Layouts

Wrap groups of routes in a shared layout:

```kotlin
routing {
    layout({ content ->
        div(classes = "app-shell") {
            NavBar()
            div(classes = "main") {
                Sidebar()
                div(classes = "content") { content() }
            }
            Footer()
        }
    }) {
        mount(Nav.home) { HomePage() }
        mount(Nav.about) { AboutPage() }
        mount(Nav.userProfile) { route -> UserProfilePage(route["id"]) }
    }
}
```

The layout receives a `content` function — call it where you want the page to render. You can nest layouts too.

## Middleware

Middleware intercepts navigation before a page renders. The most common use case: auth guards.

```kotlin
routing {
    // Public routes
    mount(Nav.login) { LoginPage() }

    // Protected routes
    middleware({ ctx ->
        if (AppState.auth.isLoggedIn) {
            RouterMiddlewareResult.Proceed
        } else {
            RouterMiddlewareResult.Redirect(Nav.login())
        }
    }) {
        layout({ LoggedInLayout(it) }) {
            mount(Nav.dashboard) { DashboardPage() }
            mount(Nav.profile) { ProfilePage() }
        }
    }

    catchAll { NotFoundPage() }
}
```

Middleware returns one of:

- `RouterMiddlewareResult.Proceed` — allow navigation
- `RouterMiddlewareResult.Redirect(uri)` — redirect to another route

## Reacting to route changes

Subscribe to the router's `current` stream from any component:

```kotlin
class BreadCrumb(ctx: NoProps) : PureComponent(ctx) {
    private val currentRoute by subscribingTo(router.current)

    override fun VDom.render() {
        ui.breadcrumb {
            div(classes = "section") { +"Home" }
            div(classes = "divider") { +"/" }
            div(classes = "active section") {
                +currentRoute.uri
            }
        }
    }
}
```

## Setting the page title

Kraft provides a `PageTitle` component:

```kotlin
class AboutPage(ctx: NoProps) : PureComponent(ctx) {
    override fun VDom.render() {
        PageTitle("About Us")

        ui.segment {
            // page content
        }
    }
}
```

## Real-world example

Here's the routing setup from the funktor-demo admin app:

```kotlin
object Nav {
    val auth = AuthFrontendRoutes()
    val dashboard = Static("")
    val profile = Static("/profile")
}

fun RootRouterBuilder.mountNav(authState: AuthState<AdminUserModel>) {
    // Auth module mounts its own routes (login, logout, etc.)
    authState.mount(this)

    // Auth middleware protects everything below
    val authMiddleware = authState.routerMiddleWare(Nav.auth.login())

    middleware(authMiddleware) {
        layout({ LoggedInLayout(it) }) {
            mount(Nav.dashboard) { DashboardPage() }
            mount(Nav.profile) { ProfilePage() }
        }
    }

    catchAll { NotFoundPage() }
}
```

This pattern — defining routes as objects, mounting them with middleware and layouts — scales well from small apps to
large ones.

---

### Forms & Validation {#kraft-forms}

# Forms & Validation

Type-safe form fields, built-in validation rules, and the draft/commit pattern.

## The basics

Every form field follows the same pattern: **current value + onChange callback**.

```kotlin
class ContactForm(ctx: NoProps) : PureComponent(ctx) {
    private var name by value("")
    private var email by value("")

    override fun VDom.render() {
        ui.form {
            UiInputField(name, { name = it }) {
                label("Name")
                placeholder("Enter your name")
            }

            UiInputField(email, { email = it }) {
                label("Email")
                placeholder("you@example.com")
            }

            ui.blue.button {
                onClick { submit() }
                +"Send"
            }
        }
    }
}
```

`UiInputField` is the workhorse — it handles strings, numbers, dates, and more.

## Field types

### Text input

```kotlin
UiInputField(value, { value = it }) {
    label("Username")
    placeholder("Choose a username")
    rightClearingIcon()  // Shows an X to clear the field
}
```

### Number input

```kotlin
UiInputField(price, { price = it }) {
    label("Price")
    step(0.01)
    leftIcon { icon.dollar_sign() }
}
```

The type is inferred from the value — pass an `Int`, get an integer field. Pass a `Double`, get a decimal field.

### Password

```kotlin
UiPasswordField(password, { password = it }) {
    label("Password")
    revealPasswordIcon()  // Toggle visibility
}
```

### Date and time

```kotlin
UiDateField(::birthday) {
    label("Birthday")
}

UiDateTimeField(::appointmentTime) {
    label("Appointment")
}

UiTimeField(::startTime) {
    label("Start time")
}
```

These work with Ultra's multiplatform date types (`MpLocalDate`, `MpLocalDateTime`, `MpLocalTime`).

### Textarea

```kotlin
UiTextArea(notes, { notes = it }) {
    label("Notes")
    placeholder("Additional information...")
}
```

### Checkbox

```kotlin
// Simple boolean
UiCheckboxField(agreed, { agreed = it }) {
    label("I agree to the terms")
}

// Toggle style
UiCheckboxField(notifications, { notifications = it }) {
    label("Enable notifications")
    toggle()
}

// Slider style with custom values
UiCheckboxField(
    value = status,
    off = "inactive",
    on = "active",
    onChange = { status = it },
) {
    label("Status")
    slider()
}
```

### Nullable fields

Fields that can be empty/null:

```kotlin
UiInputField.nullable(middleName, { middleName = it }) {
    label("Middle name (optional)")
}
```

## Validation

Add validation rules with `accepts()`:

```kotlin
UiInputField(username, { username = it }) {
    label("Username")
    accepts(
        notBlank(),
        minLength(3),
        maxLength(20),
        matchesPattern(Regex("^[a-z0-9_]+$")) {
            "Only lowercase letters, numbers, and underscores"
        },
    )
}

UiInputField(age, { age = it }) {
    label("Age")
    accepts(
        greaterThan(0.0),
        lessThan(150.0),
    )
}
```

### Built-in validators

| Validator               | What it checks              |
|-------------------------|-----------------------------|
| `notBlank()`            | Not empty or whitespace     |
| `minLength(n)`          | Minimum character count     |
| `maxLength(n)`          | Maximum character count     |
| `greaterThan(n)`        | Numeric minimum (exclusive) |
| `lessThan(n)`           | Numeric maximum (exclusive) |
| `matchesPattern(regex)` | Regex match                 |

Each validator can take a custom error message:

```kotlin
accepts(
    notBlank() { "This field is required" },
    minLength(3) { "Must be at least 3 characters" },
)
```

Validation errors render automatically below the field as red labels.

## The form controller

Use `formController()` to track the overall form validity:

```kotlin
class RegistrationForm(ctx: NoProps) : PureComponent(ctx) {
    private var name by value("")
    private var email by value("")
    private val formCtrl = formController()

    override fun VDom.render() {
        ui.form {
            UiInputField(name, { name = it }) {
                label("Name")
                accepts(notBlank())
            }

            UiInputField(email, { email = it }) {
                label("Email")
                accepts(notBlank())
            }

            // Button disabled when form is invalid
            ui.blue.button.given(!formCtrl.isValid) { disabled }.then {
                onClick {
                    if (formCtrl.validate()) {
                        // All fields passed validation
                        submitForm()
                    }
                }
                +"Register"
            }
        }
    }
}
```

`formCtrl.isValid` reactively reflects whether all fields pass validation.
`formCtrl.validate()` triggers validation on all fields and returns `true` if everything passes.

## The draft/commit pattern

For forms where you want to edit a copy and only commit changes on submit:

```kotlin
class ProfileEditor(ctx: NoProps) : PureComponent(ctx) {
    data class Profile(
        val name: String = "",
        val bio: String = "",
        val website: String = "",
    )

    // The committed state
    private var profile by value(Profile())
    // The editing copy
    private var draft by value(profile)
    // Tracks validity
    private val formCtrl = formController()

    override fun VDom.render() {
        ui.form {
            UiInputField(draft.name, { draft = draft.copy(name = it) }) {
                label("Name")
                accepts(notBlank())
            }

            UiInputField(draft.bio, { draft = draft.copy(bio = it) }) {
                label("Bio")
            }

            UiInputField(draft.website, { draft = draft.copy(website = it) }) {
                label("Website")
            }

            // Only enable submit when valid AND changed
            val canSubmit = formCtrl.isValid && draft != profile

            ui.buttons {
                ui.blue.button.given(!canSubmit) { disabled }.then {
                    onClick {
                        if (formCtrl.validate()) {
                            profile = draft  // Commit changes
                        }
                    }
                    +"Save"
                }

                ui.basic.button {
                    onClick {
                        draft = profile  // Discard changes
                        formCtrl.resetAllFields()
                    }
                    +"Reset"
                }
            }
        }
    }
}
```

This pattern gives you:

- **Edit isolation** — changes don't affect the real state until committed
- **Dirty detection** — `draft != profile` tells you if anything changed
- **Easy reset** — just copy the committed state back to draft
- **Structural equality** — data classes give you correct comparison for free

## Field appearance

### Icons

```kotlin
UiInputField(search, { search = it }) {
    leftIcon { icon.search() }
}

UiInputField(amount, { amount = it }) {
    leftIcon { green.percent() }
    rightClearingIcon()
}
```

### Labels (attached)

```kotlin
UiInputField(weight, { weight = it }) {
    rightLabel { +"kg" }
}

UiInputField(url, { url = it }) {
    leftLabel { +"https://" }
}
```

### Disabled state

```kotlin
UiInputField(readonly, { readonly = it }) {
    label("Read-only field")
    disabled(true)
}
```

### Auto-focus

```kotlin
UiInputField(query, { query = it }) {
    autofocusOnDesktop(responsive)  // Only auto-focus on desktop
}
```

---

### SemanticUI DSL {#kraft-semantic-ui}

# SemanticUI DSL

A fluent, type-safe Kotlin DSL for the [FomanticUI](https://fomantic-ui.com/) CSS framework.
Write `ui.blue.button` instead of `class="ui blue button"`.

## How it works

The DSL starts with `ui` (or `noui` for non-UI classes) and chains modifiers using Kotlin properties:

```kotlin
// This:
ui.inverted.blue.segment {
    ui.header H2 { +"Welcome" }
    ui.basic.button { +"Click me" }
}

// Generates this HTML:
// <div class="ui inverted blue segment">
//   <h2 class="ui header">Welcome</h2>
//   <button class="ui basic button">Click me</button>
// </div>
```

Every modifier is a Kotlin property that adds a CSS class. The final call (`segment {}`, `button {}`, `header H2 {}`)
creates the HTML element and opens a block for its children.

## Core elements

### Buttons

```kotlin
ui.button { +"Default" }
ui.blue.button { +"Blue" }
ui.basic.green.button { +"Outlined green" }
ui.icon.button { icon.heart() }
ui.labeled.icon.button { icon.download(); +"Download" }

// Button group
ui.buttons {
    ui.button { +"One" }
    ui.button { +"Two" }
    ui.button { +"Three" }
}
```

### Segments

```kotlin
ui.segment { +"Basic segment" }
ui.raised.segment { +"Raised" }
ui.padded.segment { +"Extra padding" }
ui.inverted.segment { +"Dark background" }
ui.placeholder.segment { +"Empty state placeholder" }

// Stacked segments
ui.segments {
    ui.segment { +"First" }
    ui.segment { +"Second" }
}
```

### Headers

```kotlin
ui.header H1 { +"Page title" }
ui.header H2 { +"Section header" }
ui.sub.header { +"Sub-header" }
ui.icon.header {
    icon.settings()
    noui.content { +"Settings" }
}
```

### Grid system

```kotlin
ui.two.column.grid {
    noui.column {
        +"Left column"
    }
    noui.column {
        +"Right column"
    }
}

// Responsive
ui.four.column.doubling.stackable.grid {
    noui.column { +"A" }
    noui.column { +"B" }
    noui.column { +"C" }
    noui.column { +"D" }
}
```

### Cards

```kotlin
ui.three.cards {
    noui.card {
        noui.content {
            ui.header { +"Card title" }
            noui.description { +"Card content" }
        }
        noui.extra.content {
            icon.user()
            +"4 members"
        }
    }
}
```

### Labels

```kotlin
ui.label { +"Default" }
ui.red.label { +"Error" }
ui.green.label { +"Success" }
ui.circular.blue.label { +"42" }
```

### Menus

```kotlin
ui.vertical.inverted.menu {
    noui.item A {
        onClick { evt -> router.navToUri(evt, Nav.home()) }
        +"Home"
    }
    noui.item A {
        onClick { evt -> router.navToUri(evt, Nav.about()) }
        +"About"
    }
}
```

### Tables

```kotlin
ui.celled.table Table {
    thead {
        tr {
            th { +"Name" }
            th { +"Status" }
        }
    }
    tbody {
        users.forEach { user ->
            tr {
                td { +user.name }
                td { ui.green.label { +"Active" } }
            }
        }
    }
}
```

## Conditional styling

The `given()` modifier applies styles based on a condition:

```kotlin
// Disable button when form is invalid
ui.blue.button.given(!isValid) { disabled }.then {
    onClick { submit() }
    +"Submit"
}

// Highlight active menu item
ui.item.given(isActive) { active }.then A {
    +"Dashboard"
}

// Add loading state
ui.button.given(isLoading) { loading }.then {
    +"Save"
}
```

**How it reads:** "A blue button, given it's not valid, make it disabled, then render it."

## Colors

16 named colors plus brand colors:

```kotlin
ui.red.label { +"Red" }
ui.orange.label { +"Orange" }
ui.yellow.label { +"Yellow" }
ui.olive.label { +"Olive" }
ui.green.label { +"Green" }
ui.teal.label { +"Teal" }
ui.blue.label { +"Blue" }
ui.violet.label { +"Violet" }
ui.purple.label { +"Purple" }
ui.pink.label { +"Pink" }
ui.brown.label { +"Brown" }
ui.grey.label { +"Grey" }
ui.black.label { +"Black" }
ui.white.label { +"White" }
```

## Sizes

```kotlin
ui.mini.button { +"Mini" }
ui.tiny.button { +"Tiny" }
ui.small.button { +"Small" }
ui.medium.button { +"Medium" }
ui.large.button { +"Large" }
ui.big.button { +"Big" }
ui.huge.button { +"Huge" }
ui.massive.button { +"Massive" }
```

## Icons

FomanticUI includes Font Awesome icons:

```kotlin
icon.home()
icon.user()
icon.search()
icon.trash()
icon.check_circle()
icon.exclamation_circle()
icon.sign_out_alternate()
icon.grip_vertical()
icon.eraser()
icon.dollar_sign()
icon.heart()
icon.settings()
icon.download()
```

## Modals

```kotlin
ui.button {
    onClick {
        modals.show { handle ->
            OkCancelModal {
                mini(
                    handle = handle,
                    header = { ui.header { +"Confirm" } },
                    content = {
                        ui.content {
                            +"Are you sure you want to delete this?"
                        }
                    },
                ) { result ->
                    when (result) {
                        OkCancelModal.Result.Ok -> deleteItem()
                        OkCancelModal.Result.Cancel -> { /* do nothing */ }
                    }
                }
            }
        }
    }
    +"Delete"
}
```

Modal sizes: `mini()`, `tiny()`, `small()`, `large()`, and full-size.

## Toast notifications

```kotlin
// Info toast
toasts.info("Settings saved successfully")

// Warning
toasts.warning("Your session expires in 5 minutes")

// Error
toasts.error("Failed to save changes")

// Random type
toasts.append(Message.Type.entries.random(), "Some message")
```

## Context menus and popups

```kotlin
// Context menu on right-click
ui.segment {
    onContextMenuStoppingEvent { evt ->
        popups.showContextMenu(evt, PopupsManager.Positioning.BottomLeft) {
            ui.vertical.menu {
                noui.item { +"Copy" }
                noui.item { +"Paste" }
                noui.item { +"Delete" }
            }
        }
    }
    +"Right-click me"
}

// Hover popup
ui.button {
    popups.showHoverPopup.topLeft(tag = this) {
        +"This is a tooltip"
    }
    +"Hover me"
}
```

## Custom CSS

Combine the DSL with inline CSS when needed:

```kotlin
div {
    css {
        marginTop = 16.px
        marginLeft = 250.px
        width = 100.pct
        height = 50.vh
    }
    +"Styled content"
}
```

---

### Overlays {#kraft-overlays}

# Overlays

Modal dialogs, toast notifications, popup menus, and context menus.

Kraft provides three overlay systems that share the same pattern: a manager you access from any component, a stage that
renders the overlays, and handles for controlling individual overlays. All three are registered automatically when you
call `semanticUI()` in your app setup.

```kotlin
val app = kraftApp {
    semanticUI()  // registers modals, toasts, and popups with SemanticUI styling
}
```

Each system is accessible from any component via a delegated property:

```kotlin
class MyComponent(ctx: Ctx<Props>) : Component<Props>(ctx) {
    override fun VDom.render() {
        // modals, toasts, and popups are available on every component
        modals.show { handle -> /* ... */ }
        toasts.info("Saved")
        popups.showContextMenu(event) { handle -> /* ... */ }
    }
}
```

## Modals

Show a modal by calling `modals.show` with a render function. You receive a `Handle` that lets you close the modal and
register close callbacks:

```kotlin
modals.show { handle ->
    div {
        h2 { +"Confirm deletion" }
        p { +"This cannot be undone." }
        ui.red.button {
            onClick {
                performDelete()
                handle.close()
            }
            +"Delete"
        }
        ui.button {
            onClick { handle.close() }
            +"Cancel"
        }
    }
}.onClose {
    // called after the modal closes, regardless of how
}
```

Close all open modals at once:

```kotlin
modals.closeAll()
```

### OkCancelModal

For the common confirm/cancel pattern, use the built-in `OkCancelModal`. It comes in three sizes:

```kotlin
modals.show { handle ->
    OkCancelModal.mini(
        handle = handle,
        header = { ui.header { +"Are you sure?" } },
        content = { +"This action cannot be undone." },
        okText = { +"Delete" },
        cancelText = { +"Cancel" },
    ) { result ->
        when (result) {
            OkCancelModal.Result.Ok -> performDelete()
            OkCancelModal.Result.Cancel -> { /* nothing */ }
        }
    }
}

// Also available:
// OkCancelModal.tiny(...)
// OkCancelModal.small(...)
```

## Toasts

Toast notifications appear in the top-right corner and auto-dismiss after a configurable duration (default: 7 seconds).
Three convenience methods cover the common types:

```kotlin
toasts.info("Changes saved")
toasts.warning("Session expires in 5 minutes")
toasts.error("Failed to save — check your connection")
```

Control the duration per toast, or pass `null` to keep it visible until clicked:

```kotlin
import kotlin.time.Duration.Companion.seconds

toasts.info("Quick note", duration = 3.seconds)
toasts.error("Read this carefully", duration = null)  // stays until clicked
```

### Custom toast settings

Configure defaults when setting up the app:

```kotlin
val app = kraftApp {
    semanticUI {
        // this block configures the ToastsManager.Builder
        defaultDuration = 5.seconds
    }
}
```

### Appending messages

For API responses that return typed `Message` objects, append them directly:

```kotlin
// Single message
toasts.append(Message.Type.info, "Item created")

// From a Messages collection (e.g. from an API response)
toasts.append(apiResponse.messages)
```

## Popups & Context Menus

Context menus appear near the triggering element. Pass a `UIEvent` and the menu renders at the event position:

```kotlin
ui.button {
    onClick { evt ->
        popups.showContextMenu(evt, PopupsManager.Positioning.BottomLeft) { handle ->
            ui.vertical.menu {
                noui.item A {
                    onClick { handle.close() }
                    +"Edit"
                }
                noui.item A {
                    onClick { handle.close() }
                    +"Delete"
                }
            }
        }
    }
    +"Actions"
}
```

### Positioning

Six positioning options control where the popup appears relative to the anchor:

| Value                      | Position             |
|----------------------------|----------------------|
| `Positioning.TopLeft`      | Above, left-aligned  |
| `Positioning.TopCenter`    | Above, centered      |
| `Positioning.TopRight`     | Above, right-aligned |
| `Positioning.BottomLeft`   | Below, left-aligned  |
| `Positioning.BottomCenter` | Below, centered      |
| `Positioning.BottomRight`  | Below, right-aligned |

### Hover popups

For tooltips that appear on hover, use the `showHoverPopup` helpers. These attach to an element and manage show/hide
automatically:

```kotlin
ui.blue.label {
    popups.showHoverPopup.topCenter(this) {
        +"This is a tooltip"
    }
    +"Hover me"
}
```

Close all open popups programmatically:

```kotlin
popups.closeAll()
```

---

### Drag & Drop {#kraft-drag-and-drop}

# Drag & Drop

Type-safe drag and drop with typed payloads and accept predicates.

Kraft's DnD system uses two components: a `DndDragHandle` that makes an element draggable, and a `DndDropTarget` that
accepts drops. Both are generic over a payload type, so the compiler ensures you only drop compatible items.

DnD is part of the `kraft-semanticui` module.

## Drag handles

Wrap any element in a `DndDragHandle` to make it draggable. The `payload` parameter is the data object that travels with
the drag:

```kotlin
data class Task(val id: String, val title: String)

// Inside your component's render:
val task = Task("1", "Write docs")

DndDragHandle(payload = task) {
    ui.segment {
        icon.bars.render()
        +task.title
    }
}
```

When the user starts dragging, a visual clone follows the cursor. The payload type (`Task`) is inferred from the
argument.

## Drop targets

Define a drop zone with `DndDropTarget`. The type parameter must match the drag handle's payload type:

```kotlin
DndDropTarget<Task> {
    accepts = { task -> task.id != currentTask.id }  // filter what's accepted
    onDrop = { task ->
        // handle the dropped task
        moveTask(task, targetColumn)
    }
}
```

The `accepts` predicate controls which payloads this target will highlight for and accept. If it returns `false`, the
target ignores the drag.

## Visual feedback

Use the built-in highlight helpers for standard visual feedback during drag operations:

```kotlin
DndDropTarget<Task> {
    greenHighlights()  // green border on valid hover
    onDrop = { task -> moveTask(task) }
}

// Or blue:
DndDropTarget<Task> {
    blueHighlights()
    onDrop = { task -> moveTask(task) }
}
```

For custom feedback, use the individual callbacks:

```kotlin
DndDropTarget<Task> {
    onDragStart = { target ->
        // a compatible drag started somewhere — highlight this zone
    }
    onDragEnd = { target ->
        // drag ended (dropped or cancelled) — remove highlight
    }
    onMouseOver = { target ->
        // dragged item is hovering over this target
    }
    onMouseOut = { target ->
        // dragged item left this target
    }
    onDrop = { task ->
        // item was dropped here
    }
}
```

## Full example

A Kanban-style board with draggable cards and two columns:

```kotlin
data class Card(val id: String, val title: String)

class KanbanBoard(ctx: NoProps) : PureComponent(ctx) {

    private var todo by value(listOf(Card("1", "Design"), Card("2", "Implement")))
    private var done by value(listOf<Card>())

    override fun VDom.render() {
        ui.two.column.grid {
            noui.column {
                h3 { +"To Do" }
                DndDropTarget<Card> {
                    greenHighlights()
                    accepts = { card -> card !in todo }
                    onDrop = { card ->
                        done = done - card
                        todo = todo + card
                    }
                }
                todo.forEach { card ->
                    DndDragHandle(payload = card) {
                        ui.segment { +card.title }
                    }
                }
            }
            noui.column {
                h3 { +"Done" }
                DndDropTarget<Card> {
                    blueHighlights()
                    accepts = { card -> card !in done }
                    onDrop = { card ->
                        todo = todo - card
                        done = done + card
                    }
                }
                done.forEach { card ->
                    DndDragHandle(payload = card) {
                        ui.segment { +card.title }
                    }
                }
            }
        }
    }
}
```

---

### Addons {#kraft-addons}

# Addons

Kotlin-friendly wrappers for JavaScript libraries, loaded on demand. Every addon is dynamically imported via
`js("import(...)")`. The JS library ships in its own webpack chunk and only downloads when a component needs it.

## The AddonRegistry pattern

An addon is a typed facade over a JavaScript library. Register it in your app, then subscribe to it from any
component:

```kotlin
// 1. Register in your KraftApp builder
val kraft = kraftApp {
    semanticUI()

    addons {
        marked()
        signaturePad()
        pixiJs(lazy = true)   // only load when first component subscribes
    }
}

// 2. Subscribe from any component
class MarkdownView(ctx: NoProps) : PureComponent(ctx) {
    private val marked: MarkedAddon? by subscribingTo(addons.marked)

    override fun VDom.render() {
        val addon = marked
        if (addon == null) {
            ui.placeholder.segment { +"Loading..." }
            return
        }

        ui.segment {
            unsafe { +addon.markdown2html("# Hello, Kraft!") }
        }
    }
}
```

The addon property is `null` until the JS library finishes loading. The component re-renders automatically when
the addon becomes ready — subscribing is just like any other stream.

### Eager vs lazy loading

- `marked()` — **eager** (default). Loads immediately on app startup.
- `pixiJs(lazy = true)` — **lazy**. Loads only when the first component subscribes. Good for heavy libraries (pixi.js
  is ~300 KB) that aren't needed on every page.

## Available addons

| Addon                    | Wraps                                        | What it does                        |
|--------------------------|----------------------------------------------|-------------------------------------|
| `marked`                 | [marked](https://marked.js.org/) + DOMPurify | Markdown → sanitized HTML           |
| `prismjs`                | [Prism](https://prismjs.com/)                | Syntax highlighting with plugins    |
| `chartjs`                | [Chart.js](https://www.chartjs.org/)         | Bar, line, pie, radar charts        |
| `pdfjs`                  | [pdf.js](https://mozilla.github.io/pdf.js/)  | Render PDFs in-browser (CDN-loaded) |
| `pixijs`                 | [PixiJS v8](https://pixijs.com/)             | 2D WebGL/WebGPU rendering           |
| `threejs`                | [Three.js](https://threejs.org/)             | 3D WebGL rendering                  |
| `signaturepad`           | SignaturePad                                 | Capture handwritten signatures      |
| `jwtdecode`              | jwt-decode                                   | Decode JWT tokens client-side       |
| `avatars`                | MinIdenticons                                | SVG identicons from strings         |
| `browserdetect`          | Bowser                                       | Browser and OS detection            |
| `nxcompile`              | @nx-js/compiler-util                         | Sandboxed JS code execution         |
| `sourcemappedstacktrace` | sourcemapped-stacktrace                      | Map minified stack traces to source |

## Adding addons

In your `build.gradle.kts`:

```kotlin
jsMain {
    dependencies {
        implementation("io.peekandpoke.kraft:addons-chartjs:0.102.0")
        implementation("io.peekandpoke.kraft:addons-prismjs:0.102.0")
        // ... add the ones you need
    }
}
```

## chartjs — Data visualization

The `ChartJs` component subscribes to the addon internally — just register `chartJs()` in your app and use it:

```kotlin
class SalesChart(ctx: NoProps) : PureComponent(ctx) {

    // Animate the chart — multiply data by a sine wave
    private val factor by subscribingTo(
        ticker(1.seconds).map { 1.0 + sin(it.count * 10 * PI / 180.0) }
    )

    override fun VDom.render() {
        div {
            css { height = 50.vh }

            ChartJs(chartJsData {
                jsObject {
                    type = "bar"
                    data = jsObject {
                        labels = arrayOf("Jan", "Feb", "Mar", "Apr", "May")
                        datasets = arrayOf(jsObject {
                            label = "Sales"
                            data = arrayOf(12, 19, 3, 5, 8)
                                .map { it * factor }
                                .toTypedArray()
                            backgroundColor = value("rgba(99, 132, 255, 0.5)")
                        })
                    }
                }
            })
        }
    }
}
```

The chart re-renders whenever `factor` changes — reactive charts with no extra wiring.

## Prism.js — Syntax highlighting

Display code with syntax highlighting:

```kotlin
override fun VDom.render() {
    PrismKotlin("""
        fun greet(name: String) = "Hello, $name!"
    """.trimIndent()) {
        lineNumbers()
        copyToClipboard()
    }

    PrismJson("""{ "status": "ok" }""") {
        lineNumbers(start = 1)
    }

    PrismHtml("<div>Hello</div>") {
        lineNumbers(softWrap = true)
    }
}
```

Available language components: `PrismKotlin`, `PrismJava`, `PrismJavascript`, `PrismJson`, `PrismHtml`, `PrismCss`, and
more.

Plugins: `lineNumbers()`, `copyToClipboard()`, `inlineColor()`.

## PDF.js — PDF viewer

Embed a scrollable PDF viewer:

```kotlin
override fun VDom.render() {
    ScrollingPdfViewer(
        src = PdfSource.Url("https://example.com/document.pdf"),
        options = ScrollingPdfViewer.Options(
            maxHeightLandscapeVh = 80,
            maxHeightPortraitVh = 80,
            scaleRange = 0.1..3.0,
        ),
        onChange = { state ->
            console.log("Page ${state.currentPage} of ${state.totalPages}")
        },
    )
}
```

## pixijs — 2D WebGL/WebGPU

PixiJS is a hardware-accelerated 2D renderer for games and interactive graphics. Register it as lazy — it's a big
library and usually only needed on specific pages:

```kotlin
addons {
    pixiJs(lazy = true)
}
```

```kotlin
class Scene(ctx: NoProps) : PureComponent(ctx) {
    private val pixi: PixiJsAddon? by subscribingTo(addons.pixiJs)
    private var app: Application? = null
    private var starting: Boolean = false

    init {
        lifecycle {
            onMount { tryStart() }
            onUpdate { tryStart() }
            onUnmount {
                app?.destroy(rendererDestroy = true)
                app = null
            }
        }
    }

    private fun tryStart() {
        val addon = pixi ?: return
        if (app != null || starting) return
        val container = dom as? HTMLDivElement ?: return

        starting = true
        launch {
            val a = addon.createApplication()
            a.init(jsObject {
                width = 800
                height = 600
                backgroundColor = 0x1a1a2e
            }).await()
            container.append(a.canvas)
            app = a

            val g = addon.createGraphics()
            g.rect(100.0, 100.0, 200.0, 150.0).fill(0xff3355)
            a.stage.addChild(g)
        }
    }

    override fun VDom.render() {
        div { css { width = 800.px; height = 600.px } }
    }
}
```

The `starting` flag prevents a race: `onMount` and `onUpdate` both call `tryStart()`, but the `launch { }` is async.
Without a sync flag, you'd create two Applications before the first finishes.

## threejs — 3D WebGL

Three.js is the standard 3D engine for the web. Register it as lazy — it's a large library and usually only needed on
specific pages:

```kotlin
addons {
    threeJs(lazy = true)
}
```

The `ThreeJs` component wraps a canvas and runs the render loop for you. Build the scene in `onReady`; advance animation
state in `onFrame`:

```kotlin
class SpinningCube(ctx: NoProps) : PureComponent(ctx) {
    private val three: ThreeJsAddon? by subscribingTo(addons.threeJs)
    private var cube: Mesh? = null

    override fun VDom.render() {
        val addon = three ?: return ui.placeholder.segment { +"Loading..." }

        ThreeJs(
            clearColor = 0x1a1a2e,
            onReady = { ctx ->
                ctx.scene.add(addon.createAmbientLight(0xffffff, 0.6))
                val dir = addon.createDirectionalLight(0xffffff, 0.8)
                dir.position.set(5.0, 10.0, 7.5)
                ctx.scene.add(dir)

                val mesh = addon.createMesh(
                    addon.createBoxGeometry(1.5, 1.5, 1.5),
                    addon.createMeshStandardMaterial(jsObject {
                        this.color = 0xe94560
                        this.metalness = 0.3
                        this.roughness = 0.4
                    }),
                )
                ctx.scene.add(mesh)
                cube = mesh
            },
            onFrame = { f ->
                val c = cube ?: return@ThreeJs
                val r = f.deltaMs / 1000.0
                c.rotation.x += 0.6 * r
                c.rotation.y += 0.9 * r
            },
        )
    }
}
```

The component creates a default `PerspectiveCamera` at `z = 5`. Override via `createCamera = { aspect -> ... }` if you
need an orthographic or repositioned camera.

## Signature Pad — Capture signatures

```kotlin
class SignatureCapture(ctx: NoProps) : PureComponent(ctx) {
    private var signaturePng: FileBase64? by value(null)
    private val padRef = ComponentRef.Tracker<SignaturePad>()

    override fun VDom.render() {
        div {
            css { position = Position.relative; height = 200.px }

            SignaturePad {
                it.export {
                    signaturePng = toPng()
                }
            }.track(padRef)
        }

        padRef { pad ->
            ui.buttons {
                ui.button {
                    onClick { pad.clear() }
                    icon.eraser(); +"Clear"
                }
            }
        }

        signaturePng?.let { png ->
            img { src = png.asDataUrl() }
        }
    }
}
```

Export as PNG, JPG, or SVG with `toPng()`, `toJpg(quality)`, `toSvg()`.

## marked — Markdown rendering

```kotlin
class MarkdownView(ctx: NoProps) : PureComponent(ctx) {
    private val marked: MarkedAddon? by subscribingTo(addons.marked)

    override fun VDom.render() {
        val addon = marked ?: return ui.placeholder.segment { +"Loading..." }

        ui.segment {
            unsafe { +addon.markdown2html("# Hello\n- item 1\n- item 2") }
        }
    }
}
```

`markdown2html()` sanitizes the output through DOMPurify automatically, so user-generated markdown can't inject
`<script>` tags.

## jwtdecode — Decode JWTs

```kotlin
class TokenInspector(ctx: NoProps) : PureComponent(ctx) {
    private val jwt: JwtDecodeAddon? by subscribingTo(addons.jwtDecode)
    private var token by value("eyJhbGc...")

    override fun VDom.render() {
        val addon = jwt ?: return
        val claims = addon.decodeJwtAsMap(token)

        pre { +JSON.stringify(addon.decodeJwt(token)) }
    }
}
```

## avatars — SVG identicons

Generate unique SVG avatars from any string:

```kotlin
class UserAvatar(ctx: Ctx<Props>) : Component<UserAvatar.Props>(ctx) {
    data class Props(val email: String)

    private val avatars: AvatarsAddon? by subscribingTo(addons.avatars)

    override fun VDom.render() {
        val addon = avatars ?: return
        img { src = addon.getDataUrl(props.email) }
    }
}
```

## browserdetect — Browser & OS info

```kotlin
class Diagnostics(ctx: NoProps) : PureComponent(ctx) {
    private val bd: BrowserDetectAddon? by subscribingTo(addons.browserDetect)

    override fun VDom.render() {
        val addon = bd ?: return
        val detect = addon.forCurrentBrowser()

        ui.list {
            li { +"Browser: ${detect.getBrowser().name}" }
            li { +"OS: ${detect.getOs().name}" }
            li { +"Platform: ${detect.getPlatform().type}" }
        }
    }
}
```

## Writing your own addon

The addon pattern is ~50 lines of Kotlin:

```kotlin
package my.app.addons.fuse

import io.peekandpoke.kraft.KraftDsl
import io.peekandpoke.kraft.addons.registry.*
import kotlinx.coroutines.await
import kotlin.js.Promise

// Facade — the typed API your components will use
class FuseAddon internal constructor(
    private val fuseModule: dynamic,
) {
    fun <T> createSearch(items: Array<T>, options: dynamic): dynamic {
        val ctor = fuseModule
        return js("new ctor(items, options)")
    }
}

// Registry key + DSL + accessor
val fuseAddonKey = AddonKey<FuseAddon>("fuse")

@KraftDsl
fun AddonRegistryBuilder.fuse(lazy: Boolean = false): Addon<FuseAddon> = register(
    key = fuseAddonKey,
    name = "fuse",
    lazy = lazy,
) {
    val module: dynamic = (js("import('fuse.js')") as Promise<dynamic>).await()
    FuseAddon(fuseModule = module.default ?: module)
}

val AddonRegistry.fuse: Addon<FuseAddon>
    get() = this[fuseAddonKey]
```

The `val ctor = fuseModule` pattern is important: `js("new this.x(...)")` doesn't work because `this` inside `js()`
refers to the JavaScript `this`, not the Kotlin instance. Capture the reference into a local val first.

## Script Loader

Need a JS library that doesn't have a Kraft addon? Load it dynamically:

```kotlin
ui.button {
    onClick {
        launch {
            val result = ScriptLoader.loadJavascript(
                "https://js.stripe.com/v3/"
            ).await()

            val stripe = window.asDynamic().Stripe("pk_test_...")
        }
    }
    +"Load Stripe"
}
```

For ES modules:

```kotlin
val module = ScriptLoader.load(
    ScriptLoader.Javascript.Module<MyModuleType>(
        src = "https://esm.sh/my-library@1.0"
    )
).await()
```

---

### Utilities & Testing {#kraft-utilities}

# Utilities & Testing

Responsive design, CSS-in-Kotlin, file handling, and component testing.

## Responsive design

The `ResponsiveController` tracks the browser window size and exposes the current display type as a reactive Stream.
Access it from any component:

```kotlin
class MyComponent(ctx: Ctx<Props>) : Component<Props>(ctx) {
    override fun VDom.render() {
        val state = responsiveCtrl()

        when (state.displayType) {
            DisplayType.Mobile -> renderMobileLayout()
            DisplayType.Tablet -> renderTabletLayout()
            DisplayType.Desktop -> renderDesktopLayout()
        }
    }
}
```

### Breakpoints

Default breakpoints:

| Display type | Window width    |
|--------------|-----------------|
| `Mobile`     | < 768px         |
| `Tablet`     | 768px -- 1199px |
| `Desktop`    | >= 1200px       |

Convenience checks on the state:

```kotlin
val state = responsiveCtrl()

state.isDesktop      // true when >= 1200px
state.isMobile       // true when < 768px
state.isNotDesktop   // tablet or mobile
state.isNotMobile    // tablet or desktop
state.windowSize     // Vector2D with current width and height
```

## CSS & Styling

Kraft supports CSS-in-Kotlin through the `StyleSheet` class. Define rules as delegated properties -- class names are
automatically mangled to avoid collisions:

```kotlin
object MyStyles : StyleSheet("my-component") {
    val container by rule {
        display = Display.flex
        justifyContent = JustifyContent.center
        padding = Padding(16.px)
    }

    val highlight by rule {
        backgroundColor = Color("#fff3cd")
        borderRadius = BorderRadius(4.px)
    }
}
```

Use the generated class names in your components:

```kotlin
override fun VDom.render() {
    div {
        className = MyStyles.container.selector
        div {
            className = MyStyles.highlight.selector
            +"Highlighted content"
        }
    }
}
```

### Scoped rules

Create nested rules that apply within a parent selector:

```kotlin
val card by rule {
    padding = Padding(12.px)
}

val cardTitle by rule(card) {
    // generates: .card-abc123 .cardTitle-def456
    fontSize = FontSize(1.2.em)
    fontWeight = FontWeight.bold
}
```

### Mounting stylesheets

Mount a stylesheet to inject its CSS into the page. Unmount to remove it:

```kotlin
import de.peekandpoke.kraft.addons.styling.StyleSheets

StyleSheets.mount(MyStyles)    // injects CSS
StyleSheets.unmount(MyStyles)  // removes CSS
```

### Raw CSS and external stylesheets

For raw CSS strings or external stylesheet links:

```kotlin
// Inject a raw CSS string
val rawCss = RawStyleSheet("""
    .custom-class { color: red; }
""")
StyleSheets.mount(rawCss)

// Link an external stylesheet
val externalCss = StyleSheetTag {
    rel = "stylesheet"
    href = "https://cdn.example.com/styles.css"
}
StyleSheets.mount(externalCss)
```

## File handling

Read files from a file input as Base64-encoded data:

```kotlin
input {
    type = InputType.file
    multiple = true
    onChange { evt ->
        launch {
            val files = evt.target.unsafeCast<HTMLInputElement>().files!!
            val loaded: List<LoadedFileBase64> = files.loadAllAsBase64()

            loaded.forEach { file ->
                println(file.file.name)      // original filename
                println(file.mimeType)       // e.g. "image/png"
                println(file.dataBase64)     // base64 content
                println(file.dataUrl)        // full data URL
            }
        }
    }
}
```

### Responsive images

The `SrcSetImage` component generates `srcset` and `sizes` attributes automatically from a single image URL:

```kotlin
SrcSetImage(
    src = "https://cdn.example.com/photo.jpg",
    sizes = ImageSizes.default,
    alt = "A photo",
)
```

## Testing

The `kraft-testing` module provides `TestBed` for rendering components and `KQuery` for querying the rendered DOM.

### Rendering a component

```kotlin
@Test
fun counter_increments() = TestBed.preact(
    view = { Counter(start = 0) },
) { root ->
    // root is a KQuery<Element> wrapping the rendered DOM
    root.selectCss("h2").textContent() shouldBe "Count: 0"

    root.selectCss<HTMLButtonElement>("button").click()

    root.selectCss("h2").textContent() shouldBe "Count: 1"
}
```

### KQuery API

KQuery provides jQuery-like element querying:

```kotlin
// Select elements by CSS selector
val buttons = root.selectCss("button")
val inputs = root.selectCss<HTMLInputElement>("input[type=text]")

// Get text content of matched elements
buttons.textContent()           // combined text of all matches
buttons.containsText("Save")   // true if any match contains "Save"

// Simulate clicks
buttons.click()
```

---
