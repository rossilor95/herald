package dev.herald.core.model

enum class HttpMethod {
    GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS;

    companion object {
        fun fromString(value: String): HttpMethod =
            entries.first { it.name.equals(value, ignoreCase = true) }
    }
}
