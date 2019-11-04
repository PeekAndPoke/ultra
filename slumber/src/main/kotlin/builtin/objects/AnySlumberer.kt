package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.slumber.Config
import de.peekandpoke.ultra.slumber.Slumberer

class AnySlumberer(private val config: Config) : Slumberer {

    override fun slumber(data: Any?): Any? {

        if (data == null) {
            return null
        }

        return config.getSlumberer(data::class).slumber(data)
    }
}
