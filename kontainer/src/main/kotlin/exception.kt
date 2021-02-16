package de.peekandpoke.ultra.kontainer

open class KontainerException(message: String) : Throwable(message)

class InvalidServiceFactory(message: String) : KontainerException(message)

class InvalidClassProvided(message: String) : KontainerException(message)

class KontainerInconsistent(message: String) : KontainerException(message)

class ServiceNotFound(message: String) : KontainerException(message)

class ServiceAmbiguous(message: String) : KontainerException(message)
