package dev.herald.core.variable

sealed class ResolveResult {
    data class Success(val resolved: String) : ResolveResult()
    data class Unresolved(val variables: List<String>) : ResolveResult()
}

object VariableResolver {
    private val variablePattern = Regex("""\{\{([A-Za-z0-9_.-]+)}}""")

    fun resolve(template: String, variables: Map<String, String>): ResolveResult {
        val names = findVariableNames(template)
        if (names.isEmpty()) {
            return ResolveResult.Success(template)
        }

        val unresolved = names.filterNot { variables.containsKey(it) }
        if (unresolved.isNotEmpty()) {
            return ResolveResult.Unresolved(unresolved)
        }

        val resolved = variablePattern.replace(template) { matchResult ->
            variables.getValue(matchResult.groupValues[1])
        }
        return ResolveResult.Success(resolved)
    }

    fun findVariableNames(template: String): List<String> =
        variablePattern.findAll(template).map { it.groupValues[1] }.distinct().toList()
}
