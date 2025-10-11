package de.peekandpoke.karango.e2e.crud

import de.peekandpoke.karango.aql.CONCAT
import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.RETURN_NEW
import de.peekandpoke.karango.aql.UPDATE
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.e2e.database
import de.peekandpoke.karango.testdomain.details
import de.peekandpoke.karango.testdomain.middleName
import de.peekandpoke.karango.testdomain.name
import de.peekandpoke.karango.testdomain.testPersons
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@Suppress("ClassName")
class `E2E-Crud-Update-Spec` : StringSpec({

    "Modifying an entity with UPDATE must work" {

        // get coll and clear all entries
        val coll = database.testPersons.apply {
            removeAll()
        }

        // create some entries
        coll.insert(JonBonJovi)
        coll.insert(EdgarAllanPoe)
        coll.insert(HeinzRudolfKunze)

        val updatedJobBonJovi = JonBonJovi.copy(
            name = JonBonJovi.name + " UPDATED",
            details = JonBonJovi.details.copy(
                middleName = JonBonJovi.details.middleName + " UPDATED",
            )
        )

        val updatedEdgarAllanPoe = EdgarAllanPoe.copy(
            name = EdgarAllanPoe.name + " UPDATED",
            details = EdgarAllanPoe.details.copy(
                middleName = EdgarAllanPoe.details.middleName + " UPDATED",
            )
        )

        val updatedHeinzRudolfKunze = HeinzRudolfKunze.copy(
            name = HeinzRudolfKunze.name + " UPDATED",
            details = HeinzRudolfKunze.details.copy(
                middleName = HeinzRudolfKunze.details.middleName + " UPDATED",
            )
        )

        assertSoftly {

            val updated = coll.find {
                FOR("p", coll) { entry ->
                    UPDATE(entry, coll) {
                        put({ name }) {
                            CONCAT(name, " UPDATED".aql)
                        }
                        put({ details.middleName }) {
                            CONCAT(details.middleName, " UPDATED".aql)
                        }
                    }

                    RETURN_NEW(entry)
                }
            }

            withClue("The query must be built correctly") {
                updated.query.query shouldBe """
                    FOR `p` IN `test-persons`
                        UPDATE `p` WITH { 
                            `name`: CONCAT(`p`.`name`, @v_1),
                            `details`: {
                                `middleName`: CONCAT(`p`.`details`.`middleName`, @v_1),
                            },
                        } IN `test-persons`
                        RETURN NEW
                    
                """.trimIndent()
            }

            withClue("The update operation must return the correct entities") {
                updated.toList().size shouldBe 3
                updated.toList()[0].value shouldBe updatedJobBonJovi
                updated.toList()[1].value shouldBe updatedEdgarAllanPoe
                updated.toList()[2].value shouldBe updatedHeinzRudolfKunze
            }

            withClue("The entities must be updated in the database") {
                val reloaded = coll.findAll()
                reloaded.toList().size shouldBe 3
                reloaded.toList()[0].value shouldBe updatedJobBonJovi
                reloaded.toList()[1].value shouldBe updatedEdgarAllanPoe
                reloaded.toList()[2].value shouldBe updatedHeinzRudolfKunze
            }
        }
    }
})
