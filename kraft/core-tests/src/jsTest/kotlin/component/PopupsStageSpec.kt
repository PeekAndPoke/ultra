package io.peekandpoke.kraft.coretests.component

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.testing.TestBed
import kotlinx.browser.document
import kotlinx.html.div

/** The popups stage every app mounts (ULTRA-00272): its document listeners go with it. */
class PopupsStageSpec : StringSpec({

    "the stage's document listeners are removed when the app goes: the very handlers that were added" {
        val doc = document.asDynamic()
        val add = doc.addEventListener
        val remove = doc.removeEventListener
        // The handlers on the document now; a removal of another function object removes nothing
        val live = js("([])")
        val tracking = js(
            "(function (add, remove, live) { return [" +
                "function (type, h, o) { if (type === 'click' || type === 'contextmenu') live.push(h); " +
                "return add.call(this, type, h, o); }, " +
                "function (type, h, o) { var i = live.indexOf(h); if (i >= 0) live.splice(i, 1); " +
                "return remove.call(this, type, h, o); }]; })"
        )(add, remove, live)
        doc.addEventListener = tracking[0]
        doc.removeEventListener = tracking[1]

        var addedDuring = 0

        try {
            TestBed.preact(view = { div { } }) {
                addedDuring = live.length as Int
            }
        } finally {
            doc.addEventListener = add
            doc.removeEventListener = remove
        }

        addedDuring shouldBe 2
        (live.length as Int) shouldBe 0
    }
})
