package de.peekandpoke.funktor.logging

import de.peekandpoke.kraft.components.debugId
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.utils.DataLoader
import kotlinx.html.FlowContent

// TODO: make common multiplatform and consolidate duplicates
internal fun <T> DataLoader<T>.renderDefault(flow: FlowContent, loadedBlock: FlowContent.(data: T) -> Unit) {
    val loader = this
    loader(flow) {
        loading {
            basicLoader()
        }
        error {
            basicError { loader.reload() }
        }
        loaded { data ->
            loadedBlock(data)
        }
    }
}

// TODO: make common multiplatform and consolidate duplicates
internal fun FlowContent.basicLoader() {
    ui.active.inline.loader {}
}

// TODO: make common multiplatform and consolidate duplicates
internal fun FlowContent.basicError(onRetry: () -> Unit) {
    ui.very.padded.center.aligned.basic.massive.segment {
        debugId("data-not-loading")

        icon.huge.blue.robot()

        ui.header H1 { +"We got a problem ..." }

        ui.text P { +"The requested data could not be loaded." }

        ui.hidden.divider()

        ui.blue.button {
            onClick {
                onRetry()
            }
            icon.redo()
            +"Retry"
        }
    }
}
