package dev.herald.core.model

data class Variable(
    val id: Long = 0,
    val environmentId: Long,
    val key: String,
    val value: String,
    val enabled: Boolean = true,
)
