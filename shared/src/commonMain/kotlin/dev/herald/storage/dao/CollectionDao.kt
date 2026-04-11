package dev.herald.storage.dao

import dev.herald.core.model.Collection
import dev.herald.storage.HeraldDatabase

class CollectionDao(internal val database: HeraldDatabase) {
    private val queries = database.heraldQueries

    fun getAll(): List<Collection> =
        queries.selectAllCollections().executeAsList().map { it.toModel() }

    fun getById(id: Long): Collection? =
        queries.selectCollectionById(id).executeAsOneOrNull()?.toModel()

    fun insert(name: String, now: Long): Long {
        queries.insertCollection(name, now, now)
        return queries.lastInsertId().executeAsOne()
    }

    fun update(id: Long, name: String, now: Long) {
        queries.updateCollection(name, now, id)
    }

    fun delete(id: Long) {
        queries.deleteCollection(id)
    }
}

private fun dev.herald.storage.Collection.toModel() = Collection(
    id = id,
    name = name,
    createdAt = created_at,
    updatedAt = updated_at,
)
