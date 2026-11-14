package io.peekandpoke.ultra.datetime

/** True on JS: in a region zone its zone engine stops at the range of `Date` (+/-8.64e15 ms). */
expect val isJsPlatform: Boolean

/** True on native: its zone engine stops a day inside each end of the date range (kotlinx-native's own limit). */
expect val isNativePlatform: Boolean
