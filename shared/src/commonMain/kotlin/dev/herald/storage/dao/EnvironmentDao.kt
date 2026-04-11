package dev.herald.storage.dao

import dev.herald.core.model.Environment
import dev.herald.storage.HeraldDatabase

class EnvironmentDao(private val database: HeraldDatabase) {
    private val queries = database.heraldQueries

    fun getAll(): List<Environment> =
        queries.selectAllEnvironments().executeAsList().map { it.toModel() }

    fun getById(id: Long): Environment? =
        queries.selectEnvironmentById(id).executeAsOneOrNull()?.toModel()

    fun insert(name: String, now: Long): Long {
        queries.insertEnvironment(name, now, now)
        return queries.lastInsertId().executeAsOne()
    }

    fun update(id: Long, name: String, now: Long) {
        queries.updateEnvironment(name, now, id)
    }

    fun delete(id: Long) {
        queries.deleteEnvironment(id)
    }
}

private fun dev.herald.storage.Environment.toModel() = Environment(
    id = id,
    name = name,
    createdAt = created_at,
    updatedAt = updated_at,
)
