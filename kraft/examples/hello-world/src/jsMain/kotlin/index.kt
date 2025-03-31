package de.peekandpoke.kraft.examples.helloworld

import de.peekandpoke.kraft.addons.modal.ModalsManager
import de.peekandpoke.kraft.addons.modal.ModalsStage
import de.peekandpoke.kraft.addons.popups.PopupsManager
import de.peekandpoke.kraft.addons.popups.PopupsStage
import de.peekandpoke.kraft.addons.routing.RouterComponent
import de.peekandpoke.kraft.addons.routing.Static
import de.peekandpoke.kraft.addons.routing.router
import de.peekandpoke.kraft.vdom.preact.PreactVDomEngine
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

val router = router {
    val main = Static("")

    mount(main) { MainPage() }

    catchAll { NotFoundPage() }
}

val modals = ModalsManager()
val popups = PopupsManager()

fun main() {
    val mountPoint = document.getElementById("spa") as HTMLElement

    PreactVDomEngine(mountPoint) {
        ModalsStage(modals)
        PopupsStage(popups)

        RouterComponent(router)
    }

    router.navigateToWindowUri()
}
