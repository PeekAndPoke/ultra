import de.peekandpoke.ultra.common.remote.ApiClient
import de.peekandpoke.ultra.common.remote.ApiEndpoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

fun main() {

    val config = ApiClient.Config(
        baseUrl = "https://api.exchangeratesapi.io",
        codec = Json {
            classDiscriminator = "_type"
            ignoreUnknownKeys = true
        }
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
