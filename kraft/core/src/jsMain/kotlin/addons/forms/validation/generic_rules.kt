package de.peekandpoke.kraft.addons.forms.validation

fun <T> anyRuleOf(rule: Rule<T>, vararg rules: Rule<T>): Rule<T> {
    val allRules = listOf(rule) + rules

    return GenericRule(
        checkFn = { value -> allRules.any { it.check(value) } },
        messageFn = { value ->
            rules.filterNot { it.check(value) }
                .map { it.getMessage(value) }
                .filter { it.isNotBlank() }
                .joinToString(" or ")
        },
    )
}

fun <T> allRulesOf(rule: Rule<T>, vararg rules: Rule<T>): Rule<T> {
    val allRules = listOf(rule) + rules

    return GenericRule(
        checkFn = { value -> allRules.all { it.check(value) } },
        messageFn = { value ->
            rules.filterNot { it.check(value) }
                .map { it.getMessage(value) }
                .filter { it.isNotBlank() }
                .joinToString(" and ")
        },
    )
}

@KraftFormsRuleDsl
fun <T> nonNull(message: String = "Must not be empty"): Rule<T> = GenericRule(
    messageFn = { message },
    checkFn = { it != null },
)

@KraftFormsRuleDsl
fun <T> nullOrElse(inner: Rule<T>): Rule<T?> = GenericRule(
    checkFn = { it == null || inner.check(it) },
    messageFn = {
        if (it == null) {
            "Invalid input"
        } else {
            inner.getMessage(it)
        }
    }
)

@KraftFormsRuleDsl
fun <T> nonNullAnd(inner: Rule<T>): Rule<T?> = GenericRule(
    checkFn = { it != null && inner.check(it) },
    messageFn = {
        if (it == null) {
            "Must not be empty"
        } else {
            inner.getMessage(it)
        }
    }
)

@KraftFormsRuleDsl
fun <T> equalTo(compareWith: () -> T, message: (T) -> String): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it == compareWith() }
)

@KraftFormsRuleDsl
fun <T> equalTo(compareWith: () -> T, message: String = "Must be equal to '$compareWith()'"): Rule<T> =
    equalTo(compareWith) { message }

@KraftFormsRuleDsl
fun <T> equalTo(compareWith: T, message: String = "Must be equal to '$compareWith()'"): Rule<T> =
    equalTo({ compareWith }) { message }

@KraftFormsRuleDsl
fun <T> notEqualTo(
    compareWith: () -> T,
    message: (T) -> String = { "Must not be equal to '$compareWith()'" },
) = GenericRule(
    messageFn = message,
    checkFn = { it != compareWith() }
)

@KraftFormsRuleDsl
fun <T> notEqualTo(compareWith: () -> T, message: String = "Must not be equal to '$compareWith()'"): Rule<T> =
    notEqualTo(compareWith) { message }

@KraftFormsRuleDsl
fun <T> notEqualTo(compareWith: T, message: String = "Must not be equal to '$compareWith()'"): Rule<T> =
    notEqualTo({ compareWith }) { message }

@KraftFormsRuleDsl
fun <T> anyOf(values: () -> Collection<T>, message: (T) -> String): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it in values() },
)

@KraftFormsRuleDsl
fun <T> anyOf(values: Collection<T>, message: String = "Must be a valid input"): Rule<T> =
    anyOf({ values }) { message }

@KraftFormsRuleDsl
fun <T> noneOf(values: () -> Collection<T>, message: (T) -> String): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = { it !in values() },
)

@KraftFormsRuleDsl
fun <T> noneOf(values: () -> Collection<T>, message: String = "Must be a valid input"): Rule<T> =
    noneOf(values) { message }

@KraftFormsRuleDsl
fun <T> noneOf(values: Collection<T>, message: String = "Must be a valid input"): Rule<T> =
    noneOf({ values }) { message }

@KraftFormsRuleDsl
fun <T> given(
    check: (T) -> Boolean,
    message: (T) -> String = { "Must be a valid input" },
): Rule<T> = GenericRule(
    messageFn = message,
    checkFn = check,
)
