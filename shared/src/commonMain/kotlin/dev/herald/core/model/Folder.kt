package dev.herald.core.model

data class Folder(
    val id: Long = 0,
    val collectionId: Long,
    val name: String,
    val seq: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
