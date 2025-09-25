package de.peekandpoke.kraft.semanticui.popups

import kotlinx.html.FlowContent

typealias PopupRenderer = FlowContent.(PopupsManager.Handle) -> Unit
