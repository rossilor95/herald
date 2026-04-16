package dev.herald.core.model

data class Collection(
    val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)
