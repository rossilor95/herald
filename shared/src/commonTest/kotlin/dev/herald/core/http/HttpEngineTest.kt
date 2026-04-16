package dev.herald.core.http

import dev.herald.core.model.HttpMethod as DomainHttpMethod
import dev.herald.core.model.KeyValueRow
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod as KtorHttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class HttpEngineTest {
    @Test
    fun executeSendsGetRequestWithExpectedPathAndMethod() = runTest {
        var capturedMethod: KtorHttpMethod? = null
        var capturedPath: String? = null
        val engine = MockEngine { request ->
            capturedMethod = request.method
            capturedPath = request.url.encodedPath
            respond(
                content = "ok",
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", "text/plain"),
            )
        }
        val httpEngine = HttpEngine(engine)

        val response = httpEngine.execute(
            method = DomainHttpMethod.GET,
            url = "https://example.com/users",
            headers = emptyList(),
            body = null,
        )

        httpEngine.close()

        val success = assertIs<HttpResponse.Success>(response)
        assertEquals(200, success.status)
        assertEquals("ok", success.body)
        assertTrue(success.durationMs >= 0)
        assertEquals(KtorHttpMethod.Get, capturedMethod)
        assertEquals("/users", capturedPath)
    }

    @Test
    fun executeSendsPostRequestBody() = runTest {
        val payload = "{\"name\":\"Herald\"}"
        var capturedBody: String? = null
        val engine = MockEngine { request ->
            capturedBody = (request.body as TextContent).text
            respond(content = "created", status = HttpStatusCode.Created)
        }
        val httpEngine = HttpEngine(engine)

        val response = httpEngine.execute(
            method = DomainHttpMethod.POST,
            url = "https://example.com/items",
            headers = emptyList(),
            body = payload,
        )

        httpEngine.close()

        assertIs<HttpResponse.Success>(response)
        assertEquals(payload, capturedBody)
    }

    @Test
    fun executeSendsCustomHeaders() = runTest {
        var apiKeyHeader: String? = null
        val engine = MockEngine { request ->
            apiKeyHeader = request.headers["X-Api-Key"]
            respond(content = "ok", status = HttpStatusCode.OK)
        }
        val httpEngine = HttpEngine(engine)

        val response = httpEngine.execute(
            method = DomainHttpMethod.GET,
            url = "https://example.com/ping",
            headers = listOf(KeyValueRow(name = "X-Api-Key", value = "secret-token")),
            body = null,
        )

        httpEngine.close()

        assertIs<HttpResponse.Success>(response)
        assertEquals("secret-token", apiKeyHeader)
    }

    @Test
    fun executeCapturesResponseHeaders() = runTest {
        val engine = MockEngine {
            respond(
                content = "{}",
                status = HttpStatusCode.Accepted,
                headers = headersOf("X-Trace-Id", "trace-123"),
            )
        }
        val httpEngine = HttpEngine(engine)

        val response = httpEngine.execute(
            method = DomainHttpMethod.GET,
            url = "https://example.com/trace",
            headers = emptyList(),
            body = null,
        )

        httpEngine.close()

        val success = assertIs<HttpResponse.Success>(response)
        assertTrue(success.headers.any { it.name == "X-Trace-Id" && it.value == "trace-123" })
    }

    @Test
    fun executeReturnsFailureOnNetworkError() = runTest {
        val expected = IllegalStateException("network is down")
        val engine = MockEngine { throw expected }
        val httpEngine = HttpEngine(engine)

        val response = httpEngine.execute(
            method = DomainHttpMethod.GET,
            url = "https://example.com/error",
            headers = emptyList(),
            body = null,
        )

        httpEngine.close()

        val failure = assertIs<HttpResponse.Failure>(response)
        assertTrue(failure.message.contains("network is down"))
        assertTrue(failure.cause?.message?.contains("network is down") == true)
    }
}
