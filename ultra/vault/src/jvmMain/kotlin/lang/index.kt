@file:Suppress("FunctionName")

package de.peekandpoke.ultra.vault.lang

// //  DSL annotations  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

@DslMarker
annotation class VaultDslMarker

@DslMarker
annotation class VaultFunctionMarker

@DslMarker
annotation class VaultTerminalExpressionMarker

@DslMarker
annotation class VaultInputValueMarker

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

