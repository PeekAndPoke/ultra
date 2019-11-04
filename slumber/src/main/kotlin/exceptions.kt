package de.peekandpoke.ultra.slumber

open class SlumberException(message: String) : Throwable(message)

class AwakerException(message: String) : SlumberException(message)

class SlumbererException(message: String) : SlumberException(message)
