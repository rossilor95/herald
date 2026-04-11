package dev.herald.storage.dao

import dev.herald.core.model.HttpMethod
import dev.herald.core.model.KeyValueRow
import dev.herald.core.model.Request
import dev.herald.storage.HeraldDatabase
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class RequestDao(private val database: HeraldDatabase) {
    private val queries = database.heraldQueries
    private val json = Json

    fun getByCollectionRoot(collectionId: Long): List<Request> =
        queries.selectRequestsByCollection(collectionId).executeAsList().map { it.toModel(json) }

    fun getByFolder(folderId: Long): List<Request> =
        queries.selectRequestsByFolder(folderId).executeAsList().map { it.toModel(json) }

    fun getById(id: Long): Request? =
        queries.selectRequestById(id).executeAsOneOrNull()?.toModel(json)

    fun insert(
        collectionId: Long,
        folderId: Long?,
        name: String,
        method: String,
        url: String,
        headers: String,
        queryParams: String,
        bodyType: String?,
        bodyContent: String?,
        seq: Int,
        now: Long,
    ): Long {
        requireFolderBelongsToCollection(folderId, collectionId)
        queries.insertRequest(
            collectionId,
            folderId,
            name,
            method,
            url,
            headers,
            queryParams,
            bodyType,
            bodyContent,
            seq.toLong(),
            now,
            now,
        )
        return queries.lastInsertId().executeAsOne()
    }

    fun update(
        id: Long,
        collectionId: Long,
        folderId: Long?,
        name: String,
        method: String,
        url: String,
        headers: String,
        queryParams: String,
        bodyType: String?,
        bodyContent: String?,
        seq: Int,
        now: Long,
    ) {
        requireFolderBelongsToCollection(folderId, collectionId)
        queries.updateRequest(
            collectionId,
            folderId,
            name,
            method,
            url,
            headers,
            queryParams,
            bodyType,
            bodyContent,
            seq.toLong(),
            now,
            id,
        )
    }

    fun delete(id: Long) {
        queries.deleteRequest(id)
    }

    fun maxSeqInFolder(folderId: Long): Int =
        queries.maxRequestSeqInFolder(folderId).executeAsOne().coerceAtLeast(-1).toInt()

    fun maxSeqInCollectionRoot(collectionId: Long): Int =
        queries.maxRequestSeqInCollectionRoot(collectionId).executeAsOne().coerceAtLeast(-1).toInt()

    private fun requireFolderBelongsToCollection(folderId: Long?, collectionId: Long) {
        if (folderId == null) {
            return
        }

        val folder = queries.selectFolderById(folderId).executeAsOneOrNull()
            ?: throw IllegalArgumentException("Folder $folderId does not exist")
        require(folder.collection_id == collectionId) {
            "Folder $folderId does not belong to collection $collectionId"
        }
    }
}

private fun dev.herald.storage.Request.toModel(json: Json) = Request(
    id = id,
    collectionId = collection_id,
    folderId = folder_id,
    name = name,
    method = HttpMethod.fromString(method),
    url = url,
    headers = json.decodeFromString<List<KeyValueRow>>(headers),
    queryParams = json.decodeFromString<List<KeyValueRow>>(query_params),
    bodyType = body_type,
    bodyContent = body_content,
    seq = seq.toInt(),
    createdAt = created_at,
    updatedAt = updated_at,
)
