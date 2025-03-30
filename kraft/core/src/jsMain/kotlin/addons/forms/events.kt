package de.peekandpoke.kraft.addons.forms

import de.peekandpoke.kraft.messages.MessageBase


class FormFieldInputChanged<P>(val field: FormField<P>) : MessageBase<FormField<P>>(field)

class FormFieldMountedMessage<P>(val field: FormField<P>) : MessageBase<FormField<P>>(field)

class FormFieldUnmountedMessage<P>(val field: FormField<P>) : MessageBase<FormField<P>>(field)
