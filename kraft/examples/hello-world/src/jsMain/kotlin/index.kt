package de.peekandpoke.kraft.examples.helloworld

import de.peekandpoke.kraft.Kraft
import de.peekandpoke.kraft.addons.routing.RouterComponent
import de.peekandpoke.kraft.addons.routing.Static
import de.peekandpoke.kraft.addons.routing.router
import de.peekandpoke.kraft.vdom.preact.PreactVDomEngine
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

val kraft = Kraft.initialize()

val router = router {
    val main = Static("")

    mount(main) { MainPage() }

    catchAll { NotFoundPage() }
}

fun main() {
    val mountPoint = document.getElementById("spa") as HTMLElement

    PreactVDomEngine(mountPoint) {
        kraft.mount(this) {
            RouterComponent(router)
        }
    }

    router.navigateToWindowUri()
}
