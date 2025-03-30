package de.peekandpoke.kraft.addons.popups

import kotlinx.html.FlowContent

typealias PopupRenderer = FlowContent.(PopupsManager.Handle) -> Unit
