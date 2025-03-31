package de.peekandpoke.funktor.cluster

import de.peekandpoke.kraft.components.debugId
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.utils.DataLoader
import de.peekandpoke.ultra.common.datetime.MpTemporalPeriod
import kotlinx.html.FlowContent

// TODO: make common multiplatform and consolidate duplicates
internal fun <T> DataLoader<T>.renderDefault(flow: FlowContent, loadedBlock: FlowContent.(data: T) -> Unit) {
    val loader = this
    loader(flow) {
        loading {
            basicLoader()
        }
        error {
            console.error("Error loading", it)
            basicError { loader.reload() }
        }
        loaded { data ->
            loadedBlock(data)
        }
    }
}

// TODO: make common multiplatform and consolidate duplicates
internal fun FlowContent.basicLoader() {
    ui.basic.center.aligned.segment {
        ui.active.inline.loader {}
    }
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

fun MpTemporalPeriod.formatNonZeroComponents(fallback: String = "0 sec"): String {

    val items = listOfNotNull(
        if (years != 0) "$years years" else null,
        if (months != 0) "$months months" else null,
        if (days != 0) "$days days" else null,
        if (hours != 0) "$hours hours" else null,
        if (minutes != 0) "$minutes mins" else null,
        if (seconds != 0) "$seconds secs" else null,
        if (milliseconds != 0) "$milliseconds millis" else null,
    )

    return when (items.isEmpty()) {
        true -> fallback
        else -> items.joinToString(" ")
    }
}
