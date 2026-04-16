package dev.herald.core.model

sealed class RequestResult {
    data class Success(val historyEntry: HistoryEntry) : RequestResult()
    data class UnresolvedVariables(val variables: List<String>) : RequestResult()
    data class NetworkError(val message: String, val cause: Throwable? = null) : RequestResult()
}
