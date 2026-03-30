package io.peekandpoke.funktor.demo.server.api.showcase

import io.peekandpoke.funktor.core.broker.OutgoingConverter
import io.peekandpoke.funktor.demo.common.showcase.RetryAttemptLog
import io.peekandpoke.funktor.demo.common.showcase.RetryDemoResponse
import io.peekandpoke.funktor.demo.common.showcase.ShowcaseApiClient
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.remote.ApiResponse
import kotlinx.coroutines.delay

class CoreShowcaseApi(converter: OutgoingConverter) : ApiRoutes("showcase-core", converter) {

    val postRetryDemo = ShowcaseApiClient.PostRetryDemo.mount {
        docs {
            name = "Run retry demo"
        }.codeGen {
            funcName = "postRetryDemo"
        }.authorize {
            public()
        }.handle { body ->
            val attempts = mutableListOf<RetryAttemptLog>()
            var finalSuccess = false

            for (attempt in 1..body.maxAttempts) {
                val start = System.currentTimeMillis()

                if (attempt > 1) {
                    delay(body.delayMs)
                }

                val succeeded = attempt >= body.failUntilAttempt
                val duration = System.currentTimeMillis() - start

                attempts.add(
                    RetryAttemptLog(
                        attempt = attempt,
                        succeeded = succeeded,
                        message = if (succeeded) "Success on attempt $attempt" else "Failed on attempt $attempt",
                        durationMs = duration,
                    )
                )

                if (succeeded) {
                    finalSuccess = true
                    break
                }
            }

            ApiResponse.ok(RetryDemoResponse(attempts = attempts, finalSuccess = finalSuccess))
        }
    }
}
