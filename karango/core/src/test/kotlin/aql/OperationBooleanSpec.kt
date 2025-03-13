package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.vault.lang.ARRAY
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.row
import io.kotest.matchers.shouldBe

class OperationBooleanSpec : StringSpec({

    val samples = listOf(
        // EQ Operation
        row("EQ: partial and value", ARRAY(1.aql, 2.aql) ALL EQ(2), "[1, 2] ALL == 2"),
        row("EQ: partial and null", ARRAY(1.aql, 2.aql) ALL EQ(null as Int?), "[1, 2] ALL == null"),
        row("EQ: partial and expression", ARRAY(1.aql, 2.aql) ALL EQ(2.aql), "[1, 2] ALL == 2"),
        row("EQ: expression and value", 2.aql EQ 2, "2 == 2"),
        row("EQ: expression and null", 2.aql EQ null as Int?, "2 == null"),
        row("EQ: expression and expression", 2.aql EQ 2.aql, "2 == 2"),

        // NE Operation
        row("NE: partial and value", ARRAY(1.aql, 2.aql) ALL NE(2), "[1, 2] ALL != 2"),
        row("NE: partial and null", ARRAY(1.aql, 2.aql) ALL NE(null as Int?), "[1, 2] ALL != null"),
        row("NE: partial and expression", ARRAY(1.aql, 2.aql) ALL NE(2.aql), "[1, 2] ALL != 2"),
        row("NE: expression and value", 2.aql NE 2, "2 != 2"),
        row("NE: expression and null", 2.aql NE null as Int?, "2 != null"),
        row("NE: expression and expression", 2.aql NE 2.aql, "2 != 2"),

        // GT Operation
        row("GT: partial and value", ARRAY(1.aql, 2.aql) ALL GT(2), "[1, 2] ALL > 2"),
        row("GT: partial and null", ARRAY(1.aql, 2.aql) ALL GT(null as Int?), "[1, 2] ALL > null"),
        row("GT: partial and expression", ARRAY(1.aql, 2.aql) ALL GT(2.aql), "[1, 2] ALL > 2"),
        row("GT: expression and value", 2.aql GT 2, "2 > 2"),
        row("GT: expression and null", 2.aql GT null as Int?, "2 > null"),
        row("GT: expression and expression", 2.aql GT 2.aql, "2 > 2"),

        // GTE Operation
        row("GTE: partial and value", ARRAY(1.aql, 2.aql) ALL GTE(2), "[1, 2] ALL >= 2"),
        row("GTE: partial and null", ARRAY(1.aql, 2.aql) ALL GTE(null as Int?), "[1, 2] ALL >= null"),
        row("GTE: partial and expression", ARRAY(1.aql, 2.aql) ALL GTE(2.aql), "[1, 2] ALL >= 2"),
        row("GTE: expression and value", 2.aql GTE 2, "2 >= 2"),
        row("GTE: expression and null", 2.aql GTE null as Int?, "2 >= null"),
        row("GTE: expression and expression", 2.aql GTE 2.aql, "2 >= 2"),

        // LT Operation
        row("LT: partial and value", ARRAY(1.aql, 2.aql) ALL LT(2), "[1, 2] ALL < 2"),
        row("LT: partial and null", ARRAY(1.aql, 2.aql) ALL LT(null as Int?), "[1, 2] ALL < null"),
        row("LT: partial and expression", ARRAY(1.aql, 2.aql) ALL LT(2.aql), "[1, 2] ALL < 2"),
        row("LT: expression and value", 2.aql LT 2, "2 < 2"),
        row("LT: expression and null", 2.aql LT null as Int?, "2 < null"),
        row("LT: expression and expression", 2.aql LT 2.aql, "2 < 2"),

        // LTE Operation
        row("LTE: partial and value", ARRAY(1.aql, 2.aql) ALL LTE(2), "[1, 2] ALL <= 2"),
        row("LTE: partial and null", ARRAY(1.aql, 2.aql) ALL LTE(null as Int?), "[1, 2] ALL <= null"),
        row("LTE: partial and expression", ARRAY(1.aql, 2.aql) ALL LTE(2.aql), "[1, 2] ALL <= 2"),
        row("LTE: expression and value", 2.aql LTE 2, "2 <= 2"),
        row("LTE: expression and null", 2.aql LTE null as Int?, "2 <= null"),
        row("LTE: expression and expression", 2.aql LTE 2.aql, "2 <= 2"),

        // IN Operation
        row("IN: partial and array", ARRAY(1.aql, 2.aql) ALL IN(arrayOf(1)), "[1, 2] ALL IN [ 1 ]"),
        row("IN: partial and collection", ARRAY(1.aql, 2.aql) ALL IN(listOf(1)), "[1, 2] ALL IN [ 1 ]"),
        row("IN: partial and expression", ARRAY(1.aql, 2.aql) ALL IN(ARRAY(1.aql)), "[1, 2] ALL IN [1]"),
        row("IN: expression and array", 1.aql IN arrayOf(1), "1 IN [ 1 ]"),
        row("IN: expression and collection", 1.aql IN listOf(1), "1 IN [ 1 ]"),
        row("IN: expression and expression", 1.aql IN ARRAY(1.aql), "1 IN [1]"),

        // NOT_IN Operation
        row("NOT_IN: partial and array", ARRAY(1.aql, 2.aql) ALL NOT_IN(arrayOf(1)), "[1, 2] ALL NOT IN [ 1 ]"),
        row("NOT_IN: partial and collection", ARRAY(1.aql, 2.aql) ALL NOT_IN(listOf(1)), "[1, 2] ALL NOT IN [ 1 ]"),
        row("NOT_IN: partial and expression", ARRAY(1.aql, 2.aql) ALL NOT_IN(ARRAY(1.aql)), "[1, 2] ALL NOT IN [1]"),
        row("NOT_IN: expression and array", 1.aql NOT_IN arrayOf(1), "1 NOT IN [ 1 ]"),
        row("NOT_IN: expression and collection", 1.aql NOT_IN listOf(1), "1 NOT IN [ 1 ]"),
        row("NOT_IN: expression and expression", 1.aql NOT_IN ARRAY(1.aql), "1 NOT IN [1]"),

        // LIKE Operation
        row("LIKE: value", "abc".aql LIKE "*", "\"abc\" LIKE \"*\""),
        row("LIKE: expression", "abc".aql LIKE "*".aql, "\"abc\" LIKE \"*\""),

        // LIKE Operation
        row("REGEX: value", "abc".aql REGEX "*", "\"abc\" =~ \"*\""),
        row("REGEX: expression", "abc".aql REGEX "*".aql, "\"abc\" =~ \"*\""),

        // AND Operation
        row("AND: value", true.aql AND true, "(true) AND (true)"),
        row("AND: expression", true.aql AND true.aql, "(true) AND (true)"),

        // OR Operation
        row("OR: value", true.aql OR true, "(true) OR (true)"),
        row("OR: expression", true.aql OR true.aql, "(true) OR (true)"),

        // NOT Operation
        row("NOT", true.aql.NOT(), "NOT(true)"),

        // ALL, NONE, ANY Operations
        row("ALL", ARRAY(1.aql, 2.aql, 3.aql) ALL EQ(1.aql), "[1, 2, 3] ALL == 1"),
        row("NONE", ARRAY(1.aql, 2.aql, 3.aql) NONE EQ(1.aql), "[1, 2, 3] NONE == 1"),
        row("ANY", ARRAY(1.aql, 2.aql, 3.aql) ANY EQ(1.aql), "[1, 2, 3] ANY == 1"),

        // Combinations
        row("a AND b EQ c", true.aql AND (false.aql EQ false), "(true) AND (false == false)"),
        row("a AND b OR c", true.aql AND false.aql OR false, "((true) AND (false)) OR (false)")
    )

    for ((description, expression, printed) in samples) {

        "operation $description ($printed)" {
            expression.printRawQuery() shouldBe printed
        }
    }
})
