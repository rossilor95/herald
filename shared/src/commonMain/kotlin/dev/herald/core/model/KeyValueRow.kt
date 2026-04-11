package dev.herald.core.model

import kotlinx.serialization.Serializable

@Serializable
data class KeyValueRow(
    val name: String,
    val value: String,
    val enabled: Boolean = true,
)
