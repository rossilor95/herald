package dev.herald.core.executor

import dev.herald.core.http.HttpEngine
import dev.herald.core.http.HttpResponse
import dev.herald.core.model.HistoryEntry
import dev.herald.core.model.KeyValueRow
import dev.herald.core.model.Request
import dev.herald.core.model.RequestResult
import dev.herald.core.variable.ResolveResult
import dev.herald.core.variable.VariableResolver
import dev.herald.storage.dao.HistoryDao
import io.ktor.http.URLBuilder
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RequestExecutor(
    private val httpEngine: HttpEngine,
    private val historyDao: HistoryDao,
    private val clock: Clock = Clock.System,
) {
    private val json = Json

    suspend fun execute(
        request: Request,
        variables: Map<String, String>,
    ): RequestResult {
        val unresolvedVariables = mutableListOf<String>()

        val resolvedUrl = resolveTemplate(request.url, variables, unresolvedVariables)

        val resolvedQueryParams = request.queryParams
            .filter { it.enabled }
            .mapNotNull { query ->
                val resolvedName = resolveTemplate(query.name, variables, unresolvedVariables)
                val resolvedValue = resolveTemplate(query.value, variables, unresolvedVariables)
                if (resolvedName != null && resolvedValue != null) {
                    KeyValueRow(name = resolvedName, value = resolvedValue)
                } else {
                    null
                }
            }

        val resolvedHeaders = request.headers
            .filter { it.enabled }
            .mapNotNull { header ->
                val resolvedName = resolveTemplate(header.name, variables, unresolvedVariables)
                val resolvedValue = resolveTemplate(header.value, variables, unresolvedVariables)
                if (resolvedName != null && resolvedValue != null) {
                    KeyValueRow(name = resolvedName, value = resolvedValue)
                } else {
                    null
                }
            }

        val resolvedBody = request.bodyContent?.let { body ->
            resolveTemplate(body, variables, unresolvedVariables)
        }

        if (unresolvedVariables.isNotEmpty()) {
            return RequestResult.UnresolvedVariables(unresolvedVariables.distinct())
        }

        val finalUrl = buildUrlWithQueryParams(
            baseUrl = checkNotNull(resolvedUrl),
            queryParams = resolvedQueryParams,
        )
        val httpResponse = httpEngine.execute(
            method = request.method,
            url = finalUrl,
            headers = resolvedHeaders,
            body = resolvedBody,
        )

        return when (httpResponse) {
            is HttpResponse.Failure -> RequestResult.NetworkError(
                message = httpResponse.message,
                cause = httpResponse.cause,
            )

            is HttpResponse.Success -> {
                val createdAt = clock.now().toEpochMilliseconds()
                val requestHeadersJson = json.encodeToString(
                    ListSerializer(KeyValueRow.serializer()),
                    resolvedHeaders,
                )
                val responseHeadersJson = json.encodeToString(
                    ListSerializer(KeyValueRow.serializer()),
                    httpResponse.headers,
                )

                val historyId = historyDao.insert(
                    requestId = request.id,
                    method = request.method.name,
                    resolvedUrl = finalUrl,
                    requestHeaders = requestHeadersJson,
                    requestBody = resolvedBody,
                    responseStatus = httpResponse.status,
                    responseHeaders = responseHeadersJson,
                    responseBody = httpResponse.body,
                    durationMs = httpResponse.durationMs,
                    createdAt = createdAt,
                )
                historyDao.prune(MAX_HISTORY_ENTRIES)

                RequestResult.Success(
                    HistoryEntry(
                        id = historyId,
                        requestId = request.id,
                        method = request.method,
                        resolvedUrl = finalUrl,
                        requestHeaders = resolvedHeaders,
                        requestBody = resolvedBody,
                        responseStatus = httpResponse.status,
                        responseHeaders = httpResponse.headers,
                        responseBody = httpResponse.body,
                        durationMs = httpResponse.durationMs,
                        createdAt = createdAt,
                    ),
                )
            }
        }
    }

    private fun resolveTemplate(
        template: String,
        variables: Map<String, String>,
        unresolvedVariables: MutableList<String>,
    ): String? =
        when (val result = VariableResolver.resolve(template, variables)) {
            is ResolveResult.Success -> result.resolved
            is ResolveResult.Unresolved -> {
                unresolvedVariables.addAll(result.variables)
                null
            }
        }

    private fun buildUrlWithQueryParams(
        baseUrl: String,
        queryParams: List<KeyValueRow>,
    ): String {
        if (queryParams.isEmpty()) {
            return baseUrl
        }

        val builder = URLBuilder(baseUrl)
        queryParams.forEach { query ->
            builder.parameters.append(query.name, query.value)
        }
        return builder.buildString()
    }

    private companion object {
        const val MAX_HISTORY_ENTRIES = 100
    }
}
