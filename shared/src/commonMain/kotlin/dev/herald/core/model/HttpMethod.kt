package dev.herald.core.model

enum class HttpMethod {
    GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS;

    companion object {
        fun fromString(value: String): HttpMethod =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown HTTP method: $value")
    }
}
