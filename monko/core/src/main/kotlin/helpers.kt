package io.peekandpoke.monko

import org.bson.Document

fun Document.getNumberOrNull(key: String) = get(key) as? Number
