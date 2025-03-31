package de.peekandpoke.funktor.testing

import de.peekandpoke.funktor.core.config.AppConfig
import de.peekandpoke.ultra.common.MutableTypedAttributes
import de.peekandpoke.ultra.common.TypedKey
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.kontainer.Kontainer
import de.peekandpoke.ultra.kontainer.KontainerAware
import io.kotest.core.spec.style.scopes.FreeSpecContainerScope
import io.kotest.core.spec.style.scopes.FreeSpecTerminalScope
import io.kotest.core.test.TestCase
import kotlinx.coroutines.delay
import org.junit.jupiter.api.fail

@DslMarker
annotation class TestStoryMarker

@DslMarker
annotation class TestStoryStepMarker

@TestStoryMarker
suspend fun FreeSpecContainerScope.story(name: String, builder: TestStory.StoryBuilder.() -> Unit) {
    @Suppress("UNCHECKED_CAST")
    val spec = testCase.spec as? AppSpec<AppConfig>
        ?: fail("story test only work inside of AppSpec test")

    val story = TestStory.StoryBuilder(spec = spec, name = name)
        .apply(builder).build()

    var failedStep: TestCase? = null

    suspend fun FreeSpecContainerScope.addSteps(steps: List<TestStoryStep>) {
        steps.forEachIndexed { idx, step ->

            val number = (idx + 1).toString().padStart(2, '0')
            val stepName = "$number. ${step.name}"

            when (step) {
                is TestStoryStep.Single -> stepName {

                    println("".padStart(80, '='))
                    println(testScope.testCase.name.testName)
                    println("".padStart(80, '='))

                    listOfNotNull(story.description)
                        .plus(story.urls.map { "See: $it" })
                        .takeIf { it.isNotEmpty() }
                        ?.let { println(it.joinToString("\n")) }

                    when (val f = failedStep) {
                        null -> {
                            delay(1)
                            step.code.invoke(this)
                        }

                        else -> fail("Previous step '${f.name.testName}' has already failed.")
                    }

                    println()
                    println("Code at ${step.definedAt}")
                    println()
                }

                is TestStoryStep.Container -> stepName - {
                    addSteps(step.steps)
                }
            }
        }
    }

    story.name - {
        val storyScope = this

        storyScope.afterAny { (test, result) ->
            if (result.isErrorOrFailure) {
                failedStep = test
            }
        }

        addSteps(story.steps)
    }
}

class TestStory(
    val name: String,
    val description: String?,
    val urls: List<String>,
    val steps: List<TestStoryStep>,
) {
    class State<T>(
        private val spec: AppSpec<*>,
        private val state: MutableTypedAttributes,
        private val key: TypedKey<T>,
        private val type: TypeRef<T>,
    ) : KontainerAware, () -> T {
        override val kontainer: Kontainer get() = spec.kontainer

        operator fun invoke(value: T?): T {
            if (value != null) {
                state[key] = value

                return value
            } else {
                fail("The value of type ${type.type} must not be null")
            }
        }

        override operator fun invoke(): T {
            if (!state.has(key)) {
                fail("There is no data set for this key")
            }

            @Suppress("UNCHECKED_CAST")
            return state[key] as T
        }

        fun getOrNull(): T? {
            return state[key]
        }

        inline fun modify(block: (T) -> T): T {
            return block(invoke()).also { invoke(it) }
        }
    }

    interface Steps : AppSpecAware<AppConfig>, KontainerAware {
        @TestStoryStepMarker
        fun step(name: String, code: suspend FreeSpecTerminalScope.() -> Unit)

        @TestStoryStepMarker
        fun group(name: String, builder: TestStoryStep.Container.() -> Unit)
    }

    class StoryBuilder(
        override val spec: AppSpec<AppConfig>,
        val name: String,
    ) : Steps {
        override val kontainer: Kontainer get() = spec.kontainer

        private var description: String? = null
        private var urls = mutableListOf<String>()
        private val steps = mutableListOf<TestStoryStep>()

        @PublishedApi
        internal val state = MutableTypedAttributes.empty()

        internal fun build() = TestStory(
            name = name,
            description = description,
            urls = urls.toList(),
            steps = steps.toList(),
        )

        fun description(description: String) {
            this.description = description
        }

        fun url(url: String) {
            this.urls.add(url)
        }

        @TestStoryStepMarker
        override fun step(name: String, code: suspend FreeSpecTerminalScope.() -> Unit) {
            steps.add(
                TestStoryStep.Single(name, code)
            )
        }

        @TestStoryStepMarker
        override fun group(name: String, builder: TestStoryStep.Container.() -> Unit) {
            steps.add(
                TestStoryStep.Container(spec, name).apply(builder)
            )
        }

        inline fun <reified T> data() = State<T>(this.spec, state, TypedKey(), kType())
    }
}

sealed class TestStoryStep {
    abstract val name: String

    class Single(
        override val name: String,
        val code: suspend FreeSpecTerminalScope.() -> Unit,
    ) : TestStoryStep() {

        val definedAt = RuntimeException().stackTrace
            .firstOrNull { !it.className.startsWith(TestStoryStep::class.java.`package`.name) }
    }

    class Container(
        override val spec: AppSpec<AppConfig>,
        override val name: String,
    ) : TestStoryStep(), TestStory.Steps, KontainerAware, AppSpecAware<AppConfig> {
        override val kontainer: Kontainer get() = spec.kontainer

        val steps = mutableListOf<TestStoryStep>()

        @PublishedApi
        internal val state = MutableTypedAttributes.empty()

        @TestStoryStepMarker
        override fun step(name: String, code: suspend FreeSpecTerminalScope.() -> Unit) {
            steps.add(
                Single(name, code)
            )
        }

        @TestStoryStepMarker
        override fun group(name: String, builder: Container.() -> Unit) {
            steps.add(
                Container(spec, name).apply(builder)
            )
        }

        inline fun <reified T> data() = TestStory.State<T>(spec, state, TypedKey(), kType())
    }
}

