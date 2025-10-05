package de.peekandpoke.kraft.addons.marked

import js.objects.Object

/**
 * Renders Markdown to HTML and purge it from potential xss attacks.
 */
fun markdown2html(markdown: String): String {
    return markdown2html(markdown, null)
}

/**
 * Renders Markdown to HTML and purge it from potential xss attacks.
 *
 * For [purifyOptions] see [dompurify docs](https://www.npmjs.com/package/dompurify)
 */
fun markdown2html(markdown: String, purifyOptions: Object?): String {
    val dirty = marked.parse(markdown)
    val clean = dompurify.sanitize(dirty, purifyOptions)

    return clean
}
