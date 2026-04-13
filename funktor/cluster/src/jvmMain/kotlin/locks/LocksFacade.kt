package io.peekandpoke.funktor.cluster.locks

/** Facade grouping global lock and server beacon functionality. */
class LocksFacade(
    global: Lazy<GlobalLocksProvider>,
    beacons: Lazy<ServerBeaconRepository>,
) {
    val global: GlobalLocksProvider by global
    val beacons: ServerBeaconRepository by beacons
}
