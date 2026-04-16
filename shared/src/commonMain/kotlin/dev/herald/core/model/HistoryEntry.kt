package dev.herald.core.model

data class HistoryEntry(
    val id: Long = 0,
    val requestId: Long,
    val method: HttpMethod,
    val resolvedUrl: String,
    val requestHeaders: List<KeyValueRow> = emptyList(),
    val requestBody: String? = null,
    val responseStatus: Int,
    val responseHeaders: List<KeyValueRow> = emptyList(),
    val responseBody: String? = null,
    val durationMs: Long,
    val createdAt: Long,
)
