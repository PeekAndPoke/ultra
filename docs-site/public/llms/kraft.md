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
- **12 Addons** — ChartJS, PDF viewer, syntax highlighting, signature pad, and more — wrapped for Kotlin.

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
import de.peekandpoke.kraft.components.*
import de.peekandpoke.kraft.routing.Static
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.kraft.vdom.preact.PreactVDomEngine
import de.peekandpoke.ultra.semanticui.ui
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
import de.peekandpoke.kraft.components.*
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.semanticui.ui
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

The `dataLoader` pattern handles the loading/error/loaded lifecycle:

```kotlin
class UserProfile(ctx: Ctx<Props>) : Component<UserProfile.Props>(ctx) {
    data class Props(val userId: String)

    private val loader = dataLoader {
        flow {
            val user = api.getUser(props.userId)
            emit(user)
        }
    }

    override fun VDom.render() {
        loader(this) {
            loading {
                ui.placeholder.segment {
                    ui.active.loader { }
                }
            }
            error { err ->
                ui.negative.message {
                    +"Failed to load user: ${err.message}"
                }
            }
            loaded { user ->
                ui.card {
                    noui.content {
                        ui.header { +user.name }
                        noui.description { +user.email }
                    }
                }
            }
        }
    }
}
```

This gives you:

- Automatic loading state on first render
- Error handling if the flow throws
- Type-safe access to the loaded data
- A `reload()` method to refresh

## Patterns summary

| What you need             | Use this                                 | Example           |
|---------------------------|------------------------------------------|-------------------|
| Simple local state        | `var x by value(0)`                      | Counter, toggles  |
| Debounced/throttled state | `stream("") { it.debounce(300.ms) }`     | Search input      |
| Global/shared state       | `val x by subscribingTo(stream)`         | Auth, preferences |
| Persistent state          | `StreamSource().persistInLocalStorage()` | User settings     |
| Async data                | `dataLoader { flow { emit(data) } }`     | API calls         |

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

### Link-style navigation

In SemanticUI menus, use the `href` helper:

```kotlin
ui.menu {
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

### Addons {#kraft-addons}

# Addons

Kraft wraps popular JavaScript libraries so you can use them from Kotlin without touching JS.

## Available addons

| Addon                    | Wraps                                        | What it does                      |
|--------------------------|----------------------------------------------|-----------------------------------|
| `chartjs`                | [Chart.js](https://www.chartjs.org/)         | Bar, line, pie, radar charts      |
| `pdfjs`                  | [pdf.js](https://mozilla.github.io/pdf.js/)  | Render PDFs in-browser            |
| `prismjs`                | [Prism](https://prismjs.com/)                | Syntax highlighting               |
| `konva`                  | [Konva](https://konvajs.org/)                | 2D canvas graphics                |
| `marked`                 | [marked](https://marked.js.org/) + DOMPurify | Markdown rendering                |
| `signaturepad`           | SignaturePad                                 | Capture handwritten signatures    |
| `avatars`                | MinIdenticons                                | Generate SVG avatars from strings |
| `browserdetect`          | Bowser                                       | Browser and OS detection          |
| `jwtdecode`              | jwt-decode                                   | Decode JWT tokens client-side     |
| `nxcompile`              | @nx-js/compiler-util                         | Sandbox JS code execution         |
| `sourcemappedstacktrace` | sourcemapped-stacktrace                      | Map minified stack traces         |

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

## Chart.js — Data visualization

Render reactive charts that update with your component state:

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
                            backgroundColor = value(
                                "rgba(99, 132, 255, 0.5)",
                            )
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

## Konva — 2D canvas

Full access to the Konva canvas library for graphics-heavy applications:

```kotlin
class CanvasDemo(ctx: NoProps) : PureComponent(ctx) {
    init {
        lifecycle {
            onMount {
                dom?.let { container ->
                    val stage = Stage(jsObject {
                        this.container = container
                        width = container.clientWidth
                        height = container.clientHeight
                    })

                    val layer = Layer(jsObject<LayerConfig> {
                        listening = true
                    })
                    stage.add(layer)

                    layer.add(Circle(jsObject {
                        x = 200; y = 200
                        radius = 50
                        fill = "#5c7cfa"
                    }))
                }
            }
        }
    }

    override fun VDom.render() {
        div { css { width = 100.pct; height = 400.px } }
    }
}
```

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

## Marked — Markdown rendering

Render markdown to HTML with sanitization:

```kotlin
override fun VDom.render() {
    val markdownSource = """
        # Hello World
        - Item 1
        - Item 2

        ```kotlin
        fun main() = println("Hello!")
        ```
    """.trimIndent()

    ui.segment {
        unsafe {
            marked.use(jsObject { mangle = false; headerIds = false })
            +markdown2html(markdownSource)
        }
    }
}
```

`marked.use()` configures the parser. `markdown2html()` renders and sanitizes the output via DOMPurify.

## Avatars — Generated identicons

Generate unique SVG avatars from any string:

```kotlin
// Get an SVG string
val svg = Avatars.MinIdenticon.get(name = "alice@example.com")

// Get a data URL for use in <img> tags
val dataUrl = Avatars.MinIdenticon.getDataUrl(name = "alice@example.com")

img { src = dataUrl }
```

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
