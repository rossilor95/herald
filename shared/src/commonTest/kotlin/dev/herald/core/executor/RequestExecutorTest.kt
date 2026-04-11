package dev.herald.core.executor

import dev.herald.core.http.HttpEngine
import dev.herald.core.model.HttpMethod
import dev.herald.core.model.KeyValueRow
import dev.herald.core.model.Request
import dev.herald.core.model.RequestResult
import dev.herald.storage.DatabaseProvider
import dev.herald.storage.DriverFactory
import dev.herald.storage.dao.CollectionDao
import dev.herald.storage.dao.HistoryDao
import dev.herald.storage.dao.RequestDao
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RequestExecutorTest {
    private val json = Json

    private data class TestContext(
        val collectionDao: CollectionDao,
        val requestDao: RequestDao,
        val historyDao: HistoryDao,
    )

    private fun createContext(): TestContext {
        val provider = DatabaseProvider(DriverFactory())
        return TestContext(
            collectionDao = CollectionDao(provider.database),
            requestDao = RequestDao(provider.database),
            historyDao = HistoryDao(provider.database),
        )
    }

    private fun createRequest(
        context: TestContext,
        method: HttpMethod = HttpMethod.GET,
        url: String = "https://example.com",
        headers: List<KeyValueRow> = emptyList(),
        queryParams: List<KeyValueRow> = emptyList(),
        bodyType: String? = null,
        bodyContent: String? = null,
    ): Request {
        val now = Clock.System.now().toEpochMilliseconds()
        val collectionId = context.collectionDao.insert("API-$now", now)
        val requestId = context.requestDao.insert(
            collectionId = collectionId,
            folderId = null,
            name = "Request-$now",
            method = method.name,
            url = url,
            headers = json.encodeToString(ListSerializer(KeyValueRow.serializer()), headers),
            queryParams = json.encodeToString(ListSerializer(KeyValueRow.serializer()), queryParams),
            bodyType = bodyType,
            bodyContent = bodyContent,
            seq = 0,
            now = now,
        )

        return requireNotNull(context.requestDao.getById(requestId))
    }

    @Test
    fun successfulExecutionPath() = runTest {
        var capturedUrl: String? = null
        var capturedHeader: String? = null
        var capturedBody: String? = null

        val engine = MockEngine { request ->
            capturedUrl = request.url.toString()
            capturedHeader = request.headers["Authorization"]
            capturedBody = (request.body as? TextContent)?.text
            respond(
                content = "created",
                status = HttpStatusCode.Created,
                headers = headersOf("X-Trace-Id", "trace-123"),
            )
        }
        val context = createContext()
        val request = createRequest(
            context = context,
            method = HttpMethod.POST,
            url = "{{baseUrl}}/users",
            headers = listOf(KeyValueRow(name = "Authorization", value = "Bearer {{token}}")),
            queryParams = listOf(
                KeyValueRow(name = "page", value = "1"),
                KeyValueRow(name = "ignored", value = "{{ignored}}", enabled = false),
            ),
            bodyType = "json",
            bodyContent = "{\"name\":\"{{userName}}\"}",
        )
        val httpEngine = HttpEngine(engine)
        val executor = RequestExecutor(httpEngine = httpEngine, historyDao = context.historyDao)

        val result = executor.execute(
            request = request,
            variables = mapOf(
                "baseUrl" to "https://api.example.com",
                "token" to "abc123",
                "userName" to "Herald",
            ),
        )

        httpEngine.close()

        val success = assertIs<RequestResult.Success>(result)
        assertEquals(request.id, success.historyEntry.requestId)
        assertEquals("https://api.example.com/users?page=1", success.historyEntry.resolvedUrl)
        assertEquals("Bearer abc123", success.historyEntry.requestHeaders.single().value)
        assertEquals("{\"name\":\"Herald\"}", success.historyEntry.requestBody)
        assertEquals(201, success.historyEntry.responseStatus)
        assertTrue(success.historyEntry.responseHeaders.any { it.name == "X-Trace-Id" && it.value == "trace-123" })
        assertEquals("https://api.example.com/users?page=1", capturedUrl)
        assertEquals("Bearer abc123", capturedHeader)
        assertEquals("{\"name\":\"Herald\"}", capturedBody)
    }

    @Test
    fun unresolvedVariablesPathBlocksSend() = runTest {
        var executeCalls = 0
        val engine = MockEngine {
            executeCalls += 1
            respond(content = "ok", status = HttpStatusCode.OK)
        }
        val context = createContext()
        val request = createRequest(
            context = context,
            method = HttpMethod.POST,
            url = "{{baseUrl}}/users/{{userId}}",
            headers = listOf(
                KeyValueRow(name = "Authorization", value = "Bearer {{token}}"),
                KeyValueRow(name = "Disabled", value = "{{shouldNotResolve}}", enabled = false),
            ),
            queryParams = listOf(KeyValueRow(name = "q", value = "{{query}}")),
            bodyType = "json",
            bodyContent = "{\"id\":\"{{userId}}\",\"query\":\"{{query}}\"}",
        )
        val httpEngine = HttpEngine(engine)
        val executor = RequestExecutor(httpEngine = httpEngine, historyDao = context.historyDao)

        val result = executor.execute(
            request = request,
            variables = mapOf("baseUrl" to "https://api.example.com"),
        )

        httpEngine.close()

        val unresolved = assertIs<RequestResult.UnresolvedVariables>(result)
        assertEquals(setOf("userId", "query", "token"), unresolved.variables.toSet())
        assertEquals(unresolved.variables.size, unresolved.variables.distinct().size)
        assertEquals(0, executeCalls)
        assertEquals(0, context.historyDao.count())
    }

    @Test
    fun networkErrorPath() = runTest {
        val expected = IllegalStateException("network offline")
        val engine = MockEngine { throw expected }
        val context = createContext()
        val request = createRequest(
            context = context,
            method = HttpMethod.GET,
            url = "https://api.example.com/ping",
        )
        val httpEngine = HttpEngine(engine)
        val executor = RequestExecutor(httpEngine = httpEngine, historyDao = context.historyDao)

        val result = executor.execute(request, emptyMap())

        httpEngine.close()

        val failure = assertIs<RequestResult.NetworkError>(result)
        assertTrue(failure.message.contains("network offline"))
        assertNotNull(failure.cause)
        assertEquals("network offline", failure.cause.message)
        assertEquals(0, context.historyDao.count())
    }

    @Test
    fun persistsHistoryOnSuccess() = runTest {
        val context = createContext()
        val request = createRequest(
            context = context,
            method = HttpMethod.GET,
            url = "https://api.example.com/items",
        )
        val seedNow = 1L
        repeat(100) { index ->
            context.historyDao.insert(
                requestId = request.id,
                method = HttpMethod.GET.name,
                resolvedUrl = "https://api.example.com/old-$index",
                requestHeaders = "[]",
                requestBody = null,
                responseStatus = 200,
                responseHeaders = "[]",
                responseBody = null,
                durationMs = 10,
                createdAt = seedNow + index,
            )
        }

        val engine = MockEngine {
            respond(content = "ok", status = HttpStatusCode.OK)
        }
        val httpEngine = HttpEngine(engine)
        val executor = RequestExecutor(httpEngine = httpEngine, historyDao = context.historyDao)

        val result = executor.execute(request, emptyMap())

        httpEngine.close()

        val success = assertIs<RequestResult.Success>(result)
        val historyByRequest = context.historyDao.getByRequest(request.id)

        assertEquals(100, context.historyDao.count())
        assertTrue(historyByRequest.any { it.id == success.historyEntry.id })
        assertTrue(historyByRequest.none { it.resolvedUrl == "https://api.example.com/old-0" })
    }

    @Test
    fun resolvesVariablesInHeaders() = runTest {
        var capturedHeader: String? = null
        val engine = MockEngine { request ->
            capturedHeader = request.headers["X-Token"]
            respond(content = "ok", status = HttpStatusCode.OK)
        }
        val context = createContext()
        val request = createRequest(
            context = context,
            method = HttpMethod.GET,
            url = "https://api.example.com/check",
            headers = listOf(
                KeyValueRow(name = "{{prefix}}-Token", value = "Bearer {{token}}"),
                KeyValueRow(name = "Disabled", value = "{{ignored}}", enabled = false),
            ),
        )
        val httpEngine = HttpEngine(engine)
        val executor = RequestExecutor(httpEngine = httpEngine, historyDao = context.historyDao)

        val result = executor.execute(
            request = request,
            variables = mapOf(
                "prefix" to "X",
                "token" to "secret",
            ),
        )

        httpEngine.close()

        val success = assertIs<RequestResult.Success>(result)
        val sentHeader = success.historyEntry.requestHeaders.single()
        assertEquals("X-Token", sentHeader.name)
        assertEquals("Bearer secret", sentHeader.value)
        assertEquals("Bearer secret", capturedHeader)
    }
}
