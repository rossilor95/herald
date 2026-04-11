package dev.herald.storage.dao

import dev.herald.core.model.Folder
import dev.herald.storage.HeraldDatabase

class FolderDao(internal val database: HeraldDatabase) {
    private val queries = database.heraldQueries

    fun getByCollection(collectionId: Long): List<Folder> =
        queries.selectFoldersByCollection(collectionId).executeAsList().map { it.toModel() }

    fun getById(id: Long): Folder? =
        queries.selectFolderById(id).executeAsOneOrNull()?.toModel()

    fun insert(collectionId: Long, name: String, seq: Int, now: Long): Long {
        queries.insertFolder(collectionId, name, seq.toLong(), now, now)
        return queries.lastInsertId().executeAsOne()
    }

    fun update(id: Long, name: String, seq: Int, now: Long) {
        queries.updateFolder(name, seq.toLong(), now, id)
    }

    fun delete(id: Long) {
        queries.deleteFolder(id)
    }

    fun maxSeq(collectionId: Long): Int =
        queries.maxFolderSeq(collectionId).executeAsOne().coerceAtLeast(-1).toInt()
}

private fun dev.herald.storage.Folder.toModel() = Folder(
    id = id,
    collectionId = collection_id,
    name = name,
    seq = seq.toInt(),
    createdAt = created_at,
    updatedAt = updated_at,
)
