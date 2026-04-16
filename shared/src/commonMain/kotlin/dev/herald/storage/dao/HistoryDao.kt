package dev.herald.storage.dao

import dev.herald.core.model.HistoryEntry
import dev.herald.core.model.HttpMethod
import dev.herald.core.model.KeyValueRow
import dev.herald.storage.HeraldDatabase
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class HistoryDao(private val database: HeraldDatabase) {
    private val queries = database.heraldQueries
    private val json = Json

    fun getAll(limit: Int): List<HistoryEntry> =
        queries.selectAllHistory(limit.toLong()).executeAsList().map { it.toModel(json) }

    fun getByRequest(requestId: Long): List<HistoryEntry> =
        queries.selectHistoryByRequest(requestId).executeAsList().map { it.toModel(json) }

    fun getById(id: Long): HistoryEntry? =
        queries.selectHistoryById(id).executeAsOneOrNull()?.toModel(json)

    fun insert(
        requestId: Long,
        method: String,
        resolvedUrl: String,
        requestHeaders: String,
        requestBody: String?,
        responseStatus: Int,
        responseHeaders: String,
        responseBody: String?,
        durationMs: Long,
        createdAt: Long,
    ): Long {
        queries.insertHistoryEntry(
            requestId,
            method,
            resolvedUrl,
            requestHeaders,
            requestBody,
            responseStatus.toLong(),
            responseHeaders,
            responseBody,
            durationMs,
            createdAt,
        )
        return queries.lastInsertId().executeAsOne()
    }

    fun prune(maxEntries: Int) {
        queries.pruneHistory(maxEntries.toLong())
    }

    fun count(): Long =
        queries.countHistory().executeAsOne()

    fun delete(id: Long) {
        queries.deleteHistoryById(id)
    }
}

private fun dev.herald.storage.History_entry.toModel(json: Json) = HistoryEntry(
    id = id,
    requestId = request_id,
    method = HttpMethod.fromString(method),
    resolvedUrl = resolved_url,
    requestHeaders = json.decodeFromString<List<KeyValueRow>>(request_headers),
    requestBody = request_body,
    responseStatus = response_status.toInt(),
    responseHeaders = json.decodeFromString<List<KeyValueRow>>(response_headers),
    responseBody = response_body,
    durationMs = duration_ms,
    createdAt = created_at,
)
