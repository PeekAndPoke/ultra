package de.peekandpoke.ultra.kontainer

class KontainerInconsistent(message: String) : Throwable(message)

class ServiceNotFound(message: String) : Throwable(message)

class ServiceAmbiguous(message: String) : Throwable(message)
