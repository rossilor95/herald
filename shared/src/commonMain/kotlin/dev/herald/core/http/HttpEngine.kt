package dev.herald.core.http

import dev.herald.core.model.HttpMethod as DomainHttpMethod
import dev.herald.core.model.KeyValueRow
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpMethod as KtorHttpMethod
import kotlinx.coroutines.CancellationException
import kotlin.time.TimeSource

sealed class HttpResponse {
    data class Success(
        val status: Int,
        val headers: List<KeyValueRow>,
        val body: String,
        val durationMs: Long,
    ) : HttpResponse()

    data class Failure(
        val message: String,
        val cause: Throwable? = null,
    ) : HttpResponse()
}

class HttpEngine(
    engine: HttpClientEngine? = null,
) {
    private val client: HttpClient =
        if (engine != null) {
            HttpClient(engine)
        } else {
            HttpClient()
        }

    suspend fun execute(
        method: DomainHttpMethod,
        url: String,
        headers: List<KeyValueRow>,
        body: String?,
    ): HttpResponse {
        val start = TimeSource.Monotonic.markNow()

        return try {
            val response = client.request(url) {
                this.method = method.toKtorMethod()
                headers.filter { it.enabled }.forEach { headerRow ->
                    this.headers.append(headerRow.name, headerRow.value)
                }
                if (body != null) {
                    setBody(body)
                }
            }

            HttpResponse.Success(
                status = response.status.value,
                headers = response.headers.toRows(),
                body = response.bodyAsText(),
                durationMs = start.elapsedNow().inWholeMilliseconds,
            )
        } catch (cause: Throwable) {
            if (cause is CancellationException) {
                throw cause
            }
            HttpResponse.Failure(
                message = cause.message ?: "Request failed",
                cause = cause,
            )
        }
    }

    fun close() {
        client.close()
    }
}

private fun DomainHttpMethod.toKtorMethod(): KtorHttpMethod =
    when (this) {
        DomainHttpMethod.GET -> KtorHttpMethod.Get
        DomainHttpMethod.POST -> KtorHttpMethod.Post
        DomainHttpMethod.PUT -> KtorHttpMethod.Put
        DomainHttpMethod.DELETE -> KtorHttpMethod.Delete
        DomainHttpMethod.PATCH -> KtorHttpMethod.Patch
        DomainHttpMethod.HEAD -> KtorHttpMethod.Head
        DomainHttpMethod.OPTIONS -> KtorHttpMethod.Options
    }

private fun io.ktor.http.Headers.toRows(): List<KeyValueRow> =
    entries().flatMap { (name, values) ->
        values.map { value ->
            KeyValueRow(name = name, value = value)
        }
    }
