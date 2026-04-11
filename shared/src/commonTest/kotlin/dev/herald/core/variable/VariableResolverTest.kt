package dev.herald.core.variable

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VariableResolverTest {
    @Test
    fun resolvesSingleVariable() {
        val vars = mapOf("base_url" to "http://localhost:8080")

        val result = VariableResolver.resolve("{{base_url}}/users", vars)

        assertTrue(result is ResolveResult.Success)
        assertEquals("http://localhost:8080/users", result.resolved)
    }

    @Test
    fun resolvesMultipleVariables() {
        val vars = mapOf("host" to "localhost", "port" to "8080")

        val result = VariableResolver.resolve("http://{{host}}:{{port}}/api", vars)

        assertTrue(result is ResolveResult.Success)
        assertEquals("http://localhost:8080/api", result.resolved)
    }

    @Test
    fun reportsUnresolvedVariables() {
        val vars = mapOf("base_url" to "http://localhost")

        val result = VariableResolver.resolve("{{base_url}}/{{version}}/users", vars)

        assertTrue(result is ResolveResult.Unresolved)
        assertEquals(listOf("version"), result.variables)
    }

    @Test
    fun reportsMultipleUnresolved() {
        val vars = emptyMap<String, String>()

        val result = VariableResolver.resolve("{{host}}:{{port}}", vars)

        assertTrue(result is ResolveResult.Unresolved)
        assertEquals(listOf("host", "port"), result.variables)
    }

    @Test
    fun returnsStringUnchangedWhenNoVariables() {
        val result = VariableResolver.resolve("http://localhost/users", emptyMap())

        assertTrue(result is ResolveResult.Success)
        assertEquals("http://localhost/users", result.resolved)
    }

    @Test
    fun handlesEmptyString() {
        val result = VariableResolver.resolve("", emptyMap())

        assertTrue(result is ResolveResult.Success)
        assertEquals("", result.resolved)
    }

    @Test
    fun handlesAdjacentVariables() {
        val vars = mapOf("a" to "hello", "b" to "world")

        val result = VariableResolver.resolve("{{a}}{{b}}", vars)

        assertTrue(result is ResolveResult.Success)
        assertEquals("helloworld", result.resolved)
    }

    @Test
    fun reportsUnresolvedVariableNotPresentInMap() {
        val vars = mapOf("base_url" to "http://localhost")

        val result = VariableResolver.resolve("{{base_url}}/{{disabled_var}}", vars)

        assertTrue(result is ResolveResult.Unresolved)
        assertEquals(listOf("disabled_var"), result.variables)
    }

    @Test
    fun handlesMalformedBraces() {
        val vars = mapOf("x" to "val")

        val result = VariableResolver.resolve("{x} {{x}} {{{x}}}", vars)

        assertTrue(result is ResolveResult.Success)
        assertEquals("{x} val {val}", result.resolved)
    }

    @Test
    fun resolveAllOrNone() {
        val vars = mapOf("a" to "1")

        val result = VariableResolver.resolve("{{a}}-{{b}}", vars)

        assertTrue(result is ResolveResult.Unresolved)
        assertEquals(listOf("b"), result.variables)
    }

    @Test
    fun findVariableNamesReturnsDistinctNames() {
        val variables = VariableResolver.findVariableNames("{{a}}/{{b}}/{{a}}")

        assertEquals(listOf("a", "b"), variables)
    }

    @Test
    fun resolvesVariableNamesWithDashAndDot() {
        val vars = mapOf(
            "base-url" to "https://api.example.com",
            "api.version" to "v1",
        )

        val result = VariableResolver.resolve("{{base-url}}/{{api.version}}/users", vars)

        assertTrue(result is ResolveResult.Success)
        assertEquals("https://api.example.com/v1/users", result.resolved)
    }

    @Test
    fun reportsUnresolvedVariableWithDash() {
        val result = VariableResolver.resolve("{{base-url}}/users", emptyMap())

        assertTrue(result is ResolveResult.Unresolved)
        assertEquals(listOf("base-url"), result.variables)
    }
}
