package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.common.model.tuple
import de.peekandpoke.ultra.vault.lang.ARRAY
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class OperationBooleanSpec : StringSpec({

    val samples = listOf(
        // EQ Operation
        tuple("EQ: partial and value", ARRAY(1.aql, 2.aql) ALL EQ(2), "[1, 2] ALL == 2"),
        tuple("EQ: partial and null", ARRAY(1.aql, 2.aql) ALL EQ(null as Int?), "[1, 2] ALL == null"),
        tuple("EQ: partial and expression", ARRAY(1.aql, 2.aql) ALL EQ(2.aql), "[1, 2] ALL == 2"),
        tuple("EQ: expression and value", 2.aql EQ 2, "2 == 2"),
        tuple("EQ: expression and null", 2.aql EQ null as Int?, "2 == null"),
        tuple("EQ: expression and expression", 2.aql EQ 2.aql, "2 == 2"),

        // NE Operation
        tuple("NE: partial and value", ARRAY(1.aql, 2.aql) ALL NE(2), "[1, 2] ALL != 2"),
        tuple("NE: partial and null", ARRAY(1.aql, 2.aql) ALL NE(null as Int?), "[1, 2] ALL != null"),
        tuple("NE: partial and expression", ARRAY(1.aql, 2.aql) ALL NE(2.aql), "[1, 2] ALL != 2"),
        tuple("NE: expression and value", 2.aql NE 2, "2 != 2"),
        tuple("NE: expression and null", 2.aql NE null as Int?, "2 != null"),
        tuple("NE: expression and expression", 2.aql NE 2.aql, "2 != 2"),

        // GT Operation
        tuple("GT: partial and value", ARRAY(1.aql, 2.aql) ALL GT(2), "[1, 2] ALL > 2"),
        tuple("GT: partial and null", ARRAY(1.aql, 2.aql) ALL GT(null as Int?), "[1, 2] ALL > null"),
        tuple("GT: partial and expression", ARRAY(1.aql, 2.aql) ALL GT(2.aql), "[1, 2] ALL > 2"),
        tuple("GT: expression and value", 2.aql GT 2, "2 > 2"),
        tuple("GT: expression and null", 2.aql GT null as Int?, "2 > null"),
        tuple("GT: expression and expression", 2.aql GT 2.aql, "2 > 2"),

        // GTE Operation
        tuple("GTE: partial and value", ARRAY(1.aql, 2.aql) ALL GTE(2), "[1, 2] ALL >= 2"),
        tuple("GTE: partial and null", ARRAY(1.aql, 2.aql) ALL GTE(null as Int?), "[1, 2] ALL >= null"),
        tuple("GTE: partial and expression", ARRAY(1.aql, 2.aql) ALL GTE(2.aql), "[1, 2] ALL >= 2"),
        tuple("GTE: expression and value", 2.aql GTE 2, "2 >= 2"),
        tuple("GTE: expression and null", 2.aql GTE null as Int?, "2 >= null"),
        tuple("GTE: expression and expression", 2.aql GTE 2.aql, "2 >= 2"),

        // LT Operation
        tuple("LT: partial and value", ARRAY(1.aql, 2.aql) ALL LT(2), "[1, 2] ALL < 2"),
        tuple("LT: partial and null", ARRAY(1.aql, 2.aql) ALL LT(null as Int?), "[1, 2] ALL < null"),
        tuple("LT: partial and expression", ARRAY(1.aql, 2.aql) ALL LT(2.aql), "[1, 2] ALL < 2"),
        tuple("LT: expression and value", 2.aql LT 2, "2 < 2"),
        tuple("LT: expression and null", 2.aql LT null as Int?, "2 < null"),
        tuple("LT: expression and expression", 2.aql LT 2.aql, "2 < 2"),

        // LTE Operation
        tuple("LTE: partial and value", ARRAY(1.aql, 2.aql) ALL LTE(2), "[1, 2] ALL <= 2"),
        tuple("LTE: partial and null", ARRAY(1.aql, 2.aql) ALL LTE(null as Int?), "[1, 2] ALL <= null"),
        tuple("LTE: partial and expression", ARRAY(1.aql, 2.aql) ALL LTE(2.aql), "[1, 2] ALL <= 2"),
        tuple("LTE: expression and value", 2.aql LTE 2, "2 <= 2"),
        tuple("LTE: expression and null", 2.aql LTE null as Int?, "2 <= null"),
        tuple("LTE: expression and expression", 2.aql LTE 2.aql, "2 <= 2"),

        // IN Operation
        tuple("IN: partial and array", ARRAY(1.aql, 2.aql) ALL IN(arrayOf(1)), "[1, 2] ALL IN [ 1 ]"),
        tuple("IN: partial and collection", ARRAY(1.aql, 2.aql) ALL IN(listOf(1)), "[1, 2] ALL IN [ 1 ]"),
        tuple("IN: partial and expression", ARRAY(1.aql, 2.aql) ALL IN(ARRAY(1.aql)), "[1, 2] ALL IN [1]"),
        tuple("IN: expression and array", 1.aql IN arrayOf(1), "1 IN [ 1 ]"),
        tuple("IN: expression and collection", 1.aql IN listOf(1), "1 IN [ 1 ]"),
        tuple("IN: expression and expression", 1.aql IN ARRAY(1.aql), "1 IN [1]"),

        // NOT_IN Operation
        tuple("NOT_IN: partial and array", ARRAY(1.aql, 2.aql) ALL NOT_IN(arrayOf(1)), "[1, 2] ALL NOT IN [ 1 ]"),
        tuple("NOT_IN: partial and collection", ARRAY(1.aql, 2.aql) ALL NOT_IN(listOf(1)), "[1, 2] ALL NOT IN [ 1 ]"),
        tuple("NOT_IN: partial and expression", ARRAY(1.aql, 2.aql) ALL NOT_IN(ARRAY(1.aql)), "[1, 2] ALL NOT IN [1]"),
        tuple("NOT_IN: expression and array", 1.aql NOT_IN arrayOf(1), "1 NOT IN [ 1 ]"),
        tuple("NOT_IN: expression and collection", 1.aql NOT_IN listOf(1), "1 NOT IN [ 1 ]"),
        tuple("NOT_IN: expression and expression", 1.aql NOT_IN ARRAY(1.aql), "1 NOT IN [1]"),

        // LIKE Operation
        tuple("LIKE: value", "abc".aql LIKE "*", "\"abc\" LIKE \"*\""),
        tuple("LIKE: expression", "abc".aql LIKE "*".aql, "\"abc\" LIKE \"*\""),

        // LIKE Operation
        tuple("REGEX: value", "abc".aql REGEX "*", "\"abc\" =~ \"*\""),
        tuple("REGEX: expression", "abc".aql REGEX "*".aql, "\"abc\" =~ \"*\""),

        // AND Operation
        tuple("AND: value", true.aql AND true, "(true) AND (true)"),
        tuple("AND: expression", true.aql AND true.aql, "(true) AND (true)"),

        // OR Operation
        tuple("OR: value", true.aql OR true, "(true) OR (true)"),
        tuple("OR: expression", true.aql OR true.aql, "(true) OR (true)"),

        // NOT Operation
        tuple("NOT", true.aql.NOT(), "NOT(true)"),

        // ALL, NONE, ANY Operations
        tuple("ALL", ARRAY(1.aql, 2.aql, 3.aql) ALL EQ(1.aql), "[1, 2, 3] ALL == 1"),
        tuple("NONE", ARRAY(1.aql, 2.aql, 3.aql) NONE EQ(1.aql), "[1, 2, 3] NONE == 1"),
        tuple("ANY", ARRAY(1.aql, 2.aql, 3.aql) ANY EQ(1.aql), "[1, 2, 3] ANY == 1"),

        // Combinations
        tuple("a AND b EQ c", true.aql AND (false.aql EQ false), "(true) AND (false == false)"),
        tuple("a AND b OR c", true.aql AND false.aql OR false, "((true) AND (false)) OR (false)")
    )

    for ((description, expression, printed) in samples) {

        "operation $description ($printed)" {
            expression.printRawQuery() shouldBe printed
        }
    }
})
