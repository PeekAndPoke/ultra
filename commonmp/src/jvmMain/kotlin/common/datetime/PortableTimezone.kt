package de.peekandpoke.ultra.common.datetime

import java.time.ZoneId

val ZoneId.portable: PortableTimezone
    get() = PortableTimezone(id)

val PortableTimezone.asZoneId: ZoneId
    get() = ZoneId.of(id)
