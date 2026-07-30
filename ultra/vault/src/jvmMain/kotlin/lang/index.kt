@file:Suppress("FunctionName")

package io.peekandpoke.ultra.vault.lang

// //  DSL annotations  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Only VaultDslMarker is put on receiver types, so only it actually scopes anything. The other four
// sit on functions, where @DslMarker has no effect — they read as categories, not as constraints.

/** Scopes the query-builder receivers, so an inner builder shadows the enclosing one. */
@DslMarker
annotation class VaultDslMarker

/** Marks a DSL function that maps to a backend function call, e.g. `LENGTH(..)`. */
@DslMarker
annotation class VaultFunctionMarker

/** Marks a DSL function returning a terminal expression, i.e. one a result cursor can be built from. */
@DslMarker
annotation class VaultTerminalExpressionMarker

/** Marks a DSL function that lifts a programmatic value into an expression. */
@DslMarker
annotation class VaultInputValueMarker

/** Marks a DSL function that re-types an expression. */
@DslMarker
annotation class VaultTypeConversionMarker

// //  Convenience type aliases  ///////////////////////////////////////////////////////////////////////////////////////////////////////////

/** Type alias for a simple list */
typealias L1<T> = List<T>

/** Type alias for a list of lists */
typealias L2<T> = List<List<T>>

/** Type alias for a list of lists of lists */
typealias L3<T> = List<List<List<T>>>

/** Type alias for a list of lists of lists of lists */
typealias L4<T> = List<List<List<List<T>>>>

/** Type alias for a list of lists of lists of lists of lists*/
typealias L5<T> = List<List<List<List<List<T>>>>>
