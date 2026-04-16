package dev.herald.storage.dao

import dev.herald.core.model.Variable
import dev.herald.storage.HeraldDatabase

class VariableDao(private val database: HeraldDatabase) {
    private val queries = database.heraldQueries

    fun getByEnvironment(environmentId: Long): List<Variable> =
        queries.selectVariablesByEnvironment(environmentId).executeAsList().map { it.toModel() }

    fun getEnabledByEnvironment(environmentId: Long): List<Variable> =
        queries.selectEnabledVariablesByEnvironment(environmentId).executeAsList().map { it.toModel() }

    fun insert(environmentId: Long, key: String, value: String, enabled: Boolean): Long {
        queries.insertVariable(environmentId, key, value, if (enabled) 1L else 0L)
        return queries.lastInsertId().executeAsOne()
    }

    fun update(id: Long, key: String, value: String, enabled: Boolean, environmentId: Long) {
        queries.updateVariable(key, value, if (enabled) 1L else 0L, environmentId, id)
    }

    fun delete(id: Long) {
        queries.deleteVariable(id)
    }
}

private fun dev.herald.storage.Variable.toModel() = Variable(
    id = id,
    environmentId = environment_id,
    key = key,
    value = value_,
    enabled = enabled == 1L,
)
