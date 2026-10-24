package io.peekandpoke.kraft.coretests.modals

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.modals.DefaultModalsManager
import io.peekandpoke.kraft.modals.ModalsManager
import io.peekandpoke.kraft.modals.showAndAwait
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Suppress("UNCHECKED_CAST")
private fun DefaultModalsManager.top(): ModalsManager.Handle<String> = invoke().last() as ModalsManager.Handle<String>

@OptIn(DelicateCoroutinesApi::class)
class ModalsManagerSpec : StringSpec() {

    init {
        "1: resolve then close answers once, onResult before onClose" {
            val m = DefaultModalsManager()
            val calls = mutableListOf<String>()

            val h = m.show<String> { }
                .onResult { calls += "result:$it" }
                .onClose { calls += "close" }

            h.resolve("A")
            h.close()

            calls shouldBe listOf("result:A", "close")
            m().shouldBeEmpty()
        }

        "2: close without resolve answers null" {
            val m = DefaultModalsManager()
            val answers = mutableListOf<String?>()

            m.show<String> { }.onResult { answers += it }.close()

            answers shouldBe listOf(null)
        }

        "3: the first resolve decides" {
            val m = DefaultModalsManager()
            val answers = mutableListOf<String?>()

            val h = m.show<String> { }.onResult { answers += it }
            h.resolve("A")
            h.resolve("B")
            h.close()

            answers shouldBe listOf("A")
        }

        "3b: a dismissal decides too, a later resolve is ignored" {
            val m = DefaultModalsManager()
            val answers = mutableListOf<String?>()

            val h = m.show<String> { }.onResult { answers += it }
            h.resolve(null)
            h.resolve("A")
            h.close()

            answers shouldBe listOf(null)
        }

        "4: handlers run once on a double close and on close then closeAll" {
            val m = DefaultModalsManager()
            var results = 0
            var closes = 0

            val h1 = m.show<String> { }.onResult { results++ }.onClose { closes++ }
            h1.close()
            h1.close()

            val h2 = m.show<String> { }.onResult { results++ }.onClose { closes++ }
            h2.close()
            m.closeAll()

            results shouldBe 2
            closes shouldBe 2
        }

        "5: closeAll delivers to every modal once, oldest first, and empties the stack" {
            val m = DefaultModalsManager()
            val calls = mutableListOf<String>()

            listOf("a", "b", "c").forEach { name ->
                m.show<String> { }
                    .onResult { calls += "$name:result:$it" }
                    .onClose { calls += "$name:close" }
            }

            m.closeAll()

            calls shouldBe listOf(
                "a:result:null", "a:close",
                "b:result:null", "b:close",
                "c:result:null", "c:close",
            )
            m().shouldBeEmpty()
        }

        "6: close from inside the own onResult delivers nothing more" {
            val m = DefaultModalsManager()
            var results = 0

            lateinit var h: ModalsManager.Handle<String>
            h = m.show<String> { }.onResult {
                results++
                h.close()
            }
            h.close()

            results shouldBe 1
        }

        "7a: the handle is off the stack when onResult runs" {
            val m = DefaultModalsManager()
            var stackDuringResult: Int? = null

            m.show<String> { }.onResult { stackDuringResult = m().size }.close()

            stackDuringResult shouldBe 0
        }

        "7b: a modal opened from onResult during closeAll stays open" {
            val m = DefaultModalsManager()
            var followUp: ModalsManager.Handle<String>? = null

            m.show<String> { }.onResult { followUp = m.show<String> { } }
            m.closeAll()

            m() shouldHaveSize 1
            m().single() shouldBe followUp
        }

        "8: a throwing handler does not cost the others their answer" {
            val m = DefaultModalsManager()
            val calls = mutableListOf<String>()

            m.show<String> { }
                .onResult { error("boom") }
                .onClose { calls += "first:close" }
            m.show<String> { }
                .onResult { calls += "second:result:$it" }

            m.closeAll()

            calls shouldBe listOf("first:close", "second:result:null")
        }

        "9: showAndAwait returns the answer" {
            val m = DefaultModalsManager()
            var answer: String? = "unset"

            val job = GlobalScope.launch(start = CoroutineStart.UNDISPATCHED) {
                answer = m.showAndAwait<String> { }
            }

            m.top().resolve("A")
            m.top().close()
            job.join()

            answer shouldBe "A"
        }

        "9a: cancelling showAndAwait closes the modal, answering null" {
            val m = DefaultModalsManager()
            val answers = mutableListOf<String?>()

            val job = GlobalScope.launch(start = CoroutineStart.UNDISPATCHED) {
                m.showAndAwait<String> { }
            }

            m() shouldHaveSize 1
            m.top().onResult { answers += it }

            job.cancel()
            job.join()

            m().shouldBeEmpty()
            answers shouldBe listOf(null)
        }

        "9b: cancelling showAndAwait after resolve delivers the resolved answer" {
            val m = DefaultModalsManager()
            val answers = mutableListOf<String?>()

            val job = GlobalScope.launch(start = CoroutineStart.UNDISPATCHED) {
                m.showAndAwait<String> { }
            }

            m.top().onResult { answers += it }
            m.top().resolve("A")

            job.cancel()
            job.join()

            m().shouldBeEmpty()
            answers shouldBe listOf("A")
        }
    }
}
