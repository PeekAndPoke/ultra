package de.peekandpoke.karango.utils

import com.arangodb.ArangoDatabaseAsync
import com.arangodb.Request
import com.arangodb.Request.Method
import com.arangodb.Response
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.common.encodeUriComponent
import kotlinx.coroutines.future.await

class ArangoDbRequestUtils(
    private val arangoDb: ArangoDatabaseAsync,
) {
    suspend fun explainQuery(query: String, bindVar: Map<String, Any?>): String {

        return try {
            // NOTICE: DO NOT use /_admin/aardvark/query/profile !!
            //         Why? This will execute the query again, even writing queries.
            //         This can result in writing more database entries than you wanted!

            val request = Request.Builder<Map<*, *>>().apply {
                db(arangoDb.name())
                method(Method.POST)
                path("/_admin/aardvark/query/explain")

                body(
                    mapOf(
                        "query" to query,
                        "bindVars" to bindVar,
                    )
                )
            }.build()

            val response: Response<Map<*, *>> = arangoDb.arango().execute(request, Map::class.java).await()

            val body = response.body

            (body["msg"] as? String)
                ?: ("ERROR: Unknown response format\n\n" + response.body.toString())

        } catch (e: Throwable) {
            "ERROR: ${e.stackTraceToString()}"
        }
    }

    suspend fun getRepositoryFigures(repo: String): Map<String, *> {
        val now = Kronos.systemUtc.millisNow()

        val request = Request.Builder<Map<String, *>>().apply {
            db(arangoDb.name())
            method(Method.GET)
            // http://localhost:8529/_db/thebase-dev/_api/collection/calendars/figures?details=true&_=1729107674598
            path("/_api/collection/${repo.encodeUriComponent()}/figures?details=true&_=$now")
        }.build()

        val response: Response<Map<*, *>> = arangoDb.arango().execute(request, Map::class.java).await()

        val body: Map<*, *> = response.body

        @Suppress("UNCHECKED_CAST")
        return body as Map<String, *>
    }
}
