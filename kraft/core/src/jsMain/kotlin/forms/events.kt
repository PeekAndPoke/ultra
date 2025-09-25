package de.peekandpoke.kraft.forms

import de.peekandpoke.kraft.messages.MessageBase

/**
 * This message is sent when the value of a form field changes.
 */
class FormFieldInputChanged<P>(val field: FormField<P>) : MessageBase<FormField<P>>(field)

/**
 * This message is sent when the form field is mounted.
 */
class FormFieldMountedMessage<P>(val field: FormField<P>) : MessageBase<FormField<P>>(field)

/**
 * This message is sent when the form field is unmounted.
 */
class FormFieldUnmountedMessage<P>(val field: FormField<P>) : MessageBase<FormField<P>>(field)
