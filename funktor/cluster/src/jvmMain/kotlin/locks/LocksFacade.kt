package de.peekandpoke.funktor.cluster.locks

class LocksFacade(
    global: Lazy<GlobalLocksProvider>,
    beacons: Lazy<ServerBeaconRepository>,
) {
    val global: GlobalLocksProvider by global
    val beacons: ServerBeaconRepository by beacons
}
