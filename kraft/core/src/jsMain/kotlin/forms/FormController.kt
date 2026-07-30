package io.peekandpoke.kraft.forms

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.messages.onMessage
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource
import kotlinx.coroutines.Job

/**
 * Controls form field registration, validation, and event handling for a component.
 *
 * Listens for form field mount/unmount/change messages and tracks all registered fields.
 * When [stopEvents] is true, form events are consumed and not propagated to parent components.
 */
open class FormController(private val component: Component<*>, val stopEvents: Boolean = true) {

    companion object {
        /** Creates a form controller that observes but does not stop form events. */
        fun watcher(component: Component<*>) =
            FormController(component = component, stopEvents = false)
    }

    /** Lifecycle status of the controller's validate/process pipeline (for spinners / guards). */
    enum class Status {
        /** Ready — nothing in progress. */
        IDLE,

        /** Validation rules (incl. async server checks) are running. */
        VALIDATING,

        /** Validation passed and the `onValid` callback is running. */
        PROCESSING,

        /** The last validate/process pipeline threw. */
        ERROR,
    }

    private val _fields = mutableSetOf<FormField<*>>()

    private val _status = StreamSource(Status.IDLE)

    /** All form fields currently registered with this controller. */
    val fields get() = _fields

    /** The current pipeline [Status]. */
    val status: Status get() = _status()

    /** Subscribable stream of [status] changes — subscribe to render spinners / disable buttons. */
    val statusStream: Stream<Status> get() = _status.readonly

    /**
     * True while validation or processing is in progress.
     *
     * Used to guard [validate] against re-entrancy, which conveniently prevents double-clicks.
     */
    val isBusy: Boolean get() = status == Status.VALIDATING || status == Status.PROCESSING

    /**
     * Returns `true` when all fields are valid.
     *
     * Reads the fields' cached error state (validation may be async, so it cannot be re-run
     * synchronously here). The cache is kept current because every value change re-validates.
     */
    val isValid: Boolean get() = _fields.none { it.hasErrors }

    /**
     * Returns `true` when some fields are NOT valid
     */
    val isNotValid get() = !isValid

    /**
     * Number of fields with errors
     */
    val numErrors: Int get() = _fields.count { it.hasErrors }

    /**
     * Resets the input of all fields
     */
    fun resetAllFields() {
        _fields.forEach { it.reset() }
    }

    private fun setStatus(next: Status) {
        _status(next)
        component.triggerRedraw()
    }

    /** Touches every field, runs all validation rules, and returns whether the whole form is valid. */
    private suspend fun runValidation(): Boolean {
        _fields.forEach { it.touch() }

        // Validate every field (each is suspend, so async rules resolve here).
        _fields.forEach { it.validate() }

        component.triggerRedraw()

        // Re-derive the verdict from the fields' freshest cached error state rather than trusting the
        // per-field booleans captured as each field finished: while a SLOW async field is validating,
        // a FAST field's value can change and re-validate to invalid (latest-wins), and that newer
        // result must win. Reading hasErrors after all validations reflects that.
        return _fields.none { it.hasErrors }
    }

    /**
     * Validates all fields by touching them first (so their errors show) and returns the result.
     *
     * Awaitable form for callers already inside a coroutine. This form is intentionally
     * **status-neutral** — the [status] pipeline and the double-submit guard belong to the [validate]
     * callback overload. Keeping it status-neutral means awaiting it can never clobber the status of a
     * concurrent callback-form run.
     */
    suspend fun validate(): Boolean {
        return runValidation()
    }

    /**
     * Validates all fields and, if the form is valid, runs [onValid].
     *
     * The [status] moves IDLE → VALIDATING → (PROCESSING while [onValid] runs) → IDLE, so the UI can
     * show a spinner. While [isBusy] this is a no-op returning `null`, which prevents double-clicks /
     * double-submits: the status flips to VALIDATING **synchronously, before the coroutine is
     * dispatched**, so two clicks in the same event-loop tick cannot both pass the guard.
     *
     * IMPORTANT: [onValid] is `suspend` — call/await its work directly. Do NOT detach it into another
     * `launch { }`: that returns immediately, so PROCESSING/[isBusy] would no longer span the work and
     * an exception would escape the ERROR handling.
     *
     * Returns the launched [Job], or `null` if the call was ignored because the controller was busy.
     */
    fun validate(onValid: suspend () -> Unit): Job? {
        if (isBusy) return null

        // Flip synchronously (before dispatch) so a same-tick second call observes isBusy and drops.
        setStatus(Status.VALIDATING)

        return launch {
            try {
                if (runValidation()) {
                    setStatus(Status.PROCESSING)
                    onValid()
                }

                setStatus(Status.IDLE)
            } catch (t: Throwable) {
                console.error(t)
                setStatus(Status.ERROR)
            }
        }
    }

    init {
        component.onMessage<FormFieldInputChanged<*>> {
            component.triggerRedraw()

            if (stopEvents) {
                it.stop()
            }
        }

        component.onMessage<FormFieldMountedMessage<*>> {
            _fields.add(it.field)

            if (stopEvents) {
                it.stop()
            }
        }

        component.onMessage<FormFieldUnmountedMessage<*>> {
            _fields.remove(it.sender)

            if (stopEvents) {
                it.stop()
            }
        }
    }
}
