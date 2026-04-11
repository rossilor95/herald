package dev.herald.core.model

data class Request(
    val id: Long = 0,
    val collectionId: Long,
    val folderId: Long? = null,
    val name: String,
    val method: HttpMethod = HttpMethod.GET,
    val url: String = "",
    val headers: List<KeyValueRow> = emptyList(),
    val queryParams: List<KeyValueRow> = emptyList(),
    val bodyType: String? = null,
    val bodyContent: String? = null,
    val seq: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
