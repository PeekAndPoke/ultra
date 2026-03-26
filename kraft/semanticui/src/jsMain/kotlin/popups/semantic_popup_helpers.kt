package io.peekandpoke.kraft.semanticui.popups

import io.peekandpoke.kraft.popups.PopupsManager
import io.peekandpoke.kraft.utils.Vector2D
import io.peekandpoke.ultra.semanticui.SemanticTag
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.CommonAttributeGroupFacade
import kotlinx.html.DIV
import org.w3c.dom.HTMLElement

/** Shows a hover popup positioned at the top-left of the target element. */
fun PopupsManager.ShowHoverPopup.topLeft(
    tag: CommonAttributeGroupFacade,
    appearance: SemanticTag.() -> SemanticTag = { this },
    content: DIV.() -> Unit,
) {
    val positioning: (target: HTMLElement, contentSize: Vector2D) -> Vector2D = { target, contentSize ->

        val rect = target.getBoundingClientRect()

        Vector2D(
            x = rect.left + contentSize.x,
            y = rect.top - (contentSize.y + 7)
        )
    }

    show(tag, positioning) {
        ui.appearance().popup.top.left.visible {
            content()
        }
    }
}

/** Shows a hover popup positioned at the top-center of the target element. */
fun PopupsManager.ShowHoverPopup.topCenter(
    tag: CommonAttributeGroupFacade,
    appearance: SemanticTag.() -> SemanticTag = { this },
    content: DIV.() -> Unit,
) {

    val positioning: (target: HTMLElement, contentSize: Vector2D) -> Vector2D = { target, contentSize ->

        val rect = target.getBoundingClientRect()

        Vector2D(
            x = rect.left + rect.width / 2 + contentSize.x / 2,
            y = rect.top - contentSize.y - 7
        )
    }

    show(tag, positioning) {
        ui.appearance().popup.top.center.visible {
            content()
        }
    }
}

/** Shows a hover popup positioned at the top-right of the target element. */
fun PopupsManager.ShowHoverPopup.topRight(
    tag: CommonAttributeGroupFacade,
    appearance: SemanticTag.() -> SemanticTag = { this },
    content: DIV.() -> Unit,
) {

    val positioning: (target: HTMLElement, contentSize: Vector2D) -> Vector2D = { target, contentSize ->

        val rect = target.getBoundingClientRect()

        Vector2D(
            x = rect.right,
            y = rect.top - contentSize.y - 7
        )
    }

    show(tag, positioning) {
        ui.appearance().popup.top.right.visible {
            content()
        }
    }
}
