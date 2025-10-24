package de.peekandpoke.monko

data class MonkoConfig(
    val connectionString: String,
    val database: String = "test",
    val flags: Flags = Flags(),
) {

    data class Flags(
        val enableProfiler: Boolean = false,
        val enableExplain: Boolean = false,
    )

    companion object {
        val forUnitTests
            get() = MonkoConfig(
                connectionString = "mongodb://root:root@localhost:27017",
            )
    }
}
