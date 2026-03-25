package io.peekandpoke.karango.e2e.crud

import io.peekandpoke.karango.testdomain.TestPerson
import io.peekandpoke.karango.testdomain.TestPersonDetails

internal val JonBonJovi = TestPerson(
    name = "Jon Jovi",
    details = TestPersonDetails(title = "Dr.", middleName = "Bon"),
    addresses = listOf()
)

internal val EdgarAllanPoe = TestPerson(
    name = "Edgar Poe",
    details = TestPersonDetails(middleName = "Allan"),
    addresses = listOf()
)

internal val HeinzRudolfKunze = TestPerson(
    name = "Heinz Kunze",
    details = TestPersonDetails(title = "Herr", middleName = "Rudolf"),
    addresses = listOf()
)
