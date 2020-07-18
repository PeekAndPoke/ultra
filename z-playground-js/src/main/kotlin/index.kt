import de.peekandpoke.common.remote.ApiClient
import de.peekandpoke.common.remote.ApiEndpoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration

fun main() {

    val config = ApiClient.Config(
        baseUrl = "https://api.exchangeratesapi.io",
        codec = Json(
            configuration = JsonConfiguration.Stable.copy(
                classDiscriminator = "_type",
                ignoreUnknownKeys = true
            )
        )
    )

    val client = TestApiClient(config)

    GlobalScope.launch {
        client.latest().collect {
            println("RESPONSE")
            println(it)
        }
    }
}

class TestApiClient(config: Config) : ApiClient(config) {

    companion object {
        val latest = ApiEndpoint.Get("latest")
    }

    fun latest() = latest { it }
}
