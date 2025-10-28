package de.peekandpoke.monko

import org.bson.Document

fun Document.getNumberOrNull(key: String) = get(key) as? Number
