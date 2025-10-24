package de.peekandpoke.kraft.semanticui.popups

import de.peekandpoke.kraft.popups.PopupsManager
import de.peekandpoke.kraft.utils.Vector2D
import de.peekandpoke.ultra.semanticui.SemanticTag
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.html.CommonAttributeGroupFacade
import kotlinx.html.DIV
import org.w3c.dom.HTMLElement

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
