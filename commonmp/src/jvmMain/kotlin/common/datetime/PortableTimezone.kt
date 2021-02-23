package de.peekandpoke.ultra.common.datetime

import java.time.ZoneId

val ZoneId.portable get() = PortableTimezone(id)
