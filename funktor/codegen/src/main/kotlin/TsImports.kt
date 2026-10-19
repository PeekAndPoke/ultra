package io.peekandpoke.funktor.codegen

/**
 * The module specifiers an emitted `.ts` or `.vue` file imports: static, side-effect, re-export and `import()`.
 *
 * A text scan, not a parser -- enough for the generator's own output, whose import style it pins. Comments
 * are stripped first, so a KDoc example does not count as an import.
 */
object TsImports {

    private val blockComment = Regex("""/\*[\s\S]*?\*/""")
    private val htmlComment = Regex("""<!--[\s\S]*?-->""")
    private val lineComment = Regex("""^\s*//.*$""", RegexOption.MULTILINE)

    private val sideEffect = Regex("""^\s*import\s+['"]([^'"]+)['"]""", RegexOption.MULTILINE)
    private val fromClause = Regex("""^\s*(?:import|export)\b[^;'"]*?\bfrom\s*['"]([^'"]+)['"]""", RegexOption.MULTILINE)
    private val dynamic = Regex("""\bimport\(\s*['"]([^'"]+)['"]\s*\)""")

    /** Every specifier in [content], in no particular order, without duplicates. */
    fun of(content: String): Set<String> {
        val code = content
            .replace(blockComment, "")
            .replace(htmlComment, "")
            .replace(lineComment, "")

        return listOf(sideEffect, fromClause, dynamic)
            .flatMap { regex -> regex.findAll(code).map { it.groupValues[1] } }
            .toSet()
    }

    /** True for `./x` and `../x`, which resolve inside the SDK; anything else is a package. */
    fun isRelative(specifier: String): Boolean = specifier.startsWith("./") || specifier.startsWith("../")

    /** The npm package a bare [specifier] names: `vis-network/standalone` -> `vis-network`, `@a/b/c` -> `@a/b`. */
    fun packageOf(specifier: String): String {
        val parts = specifier.split('/')

        return if (specifier.startsWith("@") && parts.size >= 2) "${parts[0]}/${parts[1]}" else parts[0]
    }
}
