package dev.herald.storage.dao

import dev.herald.storage.DatabaseProvider
import dev.herald.storage.DriverFactory
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RequestDaoTest {
    private data class Daos(
        val collectionDao: CollectionDao,
        val folderDao: FolderDao,
        val requestDao: RequestDao,
    )

    private fun createDaos(): Daos {
        val provider = DatabaseProvider(DriverFactory())
        return Daos(
            CollectionDao(provider.database),
            FolderDao(provider.database),
            RequestDao(provider.database),
        )
    }

    @Test
    fun insertRequestAtCollectionRoot() {
        val (collDao, _, reqDao) = createDaos()
        val now = Clock.System.now().toEpochMilliseconds()
        val collId = collDao.insert("API", now)
        val reqId = reqDao.insert(
            collectionId = collId,
            folderId = null,
            name = "Get Users",
            method = "GET",
            url = "{{base_url}}/users",
            headers = """[{"name":"Authorization","value":"Bearer token","enabled":true}]""",
            queryParams = """[{"name":"page","value":"1","enabled":true}]""",
            bodyType = null,
            bodyContent = null,
            seq = 0,
            now = now,
        )

        val requests = reqDao.getByCollectionRoot(collId)

        assertEquals(1, requests.size)
        assertEquals("Get Users", requests[0].name)
        assertNull(requests[0].folderId)
        assertEquals("Authorization", requests[0].headers[0].name)
        assertEquals("page", requests[0].queryParams[0].name)
        assertNotNull(reqDao.getById(reqId))
    }

    @Test
    fun insertRequestInFolder() {
        val (collDao, folderDao, reqDao) = createDaos()
        val now = Clock.System.now().toEpochMilliseconds()
        val collId = collDao.insert("API", now)
        val folderId = folderDao.insert(collId, "Users", 0, now)
        reqDao.insert(
            collectionId = collId,
            folderId = folderId,
            name = "Get Users",
            method = "GET",
            url = "/users",
            headers = "[]",
            queryParams = "[]",
            bodyType = null,
            bodyContent = null,
            seq = 0,
            now = now,
        )

        val requests = reqDao.getByFolder(folderId)

        assertEquals(1, requests.size)
        assertEquals(folderId, requests[0].folderId)
    }

    @Test
    fun insertRequestRejectsFolderFromDifferentCollection() {
        val (collDao, folderDao, reqDao) = createDaos()
        val now = Clock.System.now().toEpochMilliseconds()
        val collOneId = collDao.insert("API One", now)
        val collTwoId = collDao.insert("API Two", now)
        val folderInFirstCollection = folderDao.insert(collOneId, "Users", 0, now)

        assertFailsWith<Exception> {
            reqDao.insert(
                collectionId = collTwoId,
                folderId = folderInFirstCollection,
                name = "Invalid",
                method = "GET",
                url = "/users",
                headers = "[]",
                queryParams = "[]",
                bodyType = null,
                bodyContent = null,
                seq = 0,
                now = now,
            )
        }
    }

    @Test
    fun updateRequest() {
        val (collDao, _, reqDao) = createDaos()
        val now = Clock.System.now().toEpochMilliseconds()
        val collId = collDao.insert("API", now)
        val reqId = reqDao.insert(
            collectionId = collId,
            folderId = null,
            name = "Old",
            method = "GET",
            url = "/old",
            headers = "[]",
            queryParams = "[]",
            bodyType = null,
            bodyContent = null,
            seq = 0,
            now = now,
        )
        reqDao.update(
            id = reqId,
            collectionId = collId,
            folderId = null,
            name = "New",
            method = "POST",
            url = "/new",
            headers = """[{"name":"Content-Type","value":"application/json","enabled":true}]""",
            queryParams = """[{"name":"limit","value":"10","enabled":true}]""",
            bodyType = "json",
            bodyContent = "{}",
            seq = 2,
            now = now + 1000,
        )

        val req = reqDao.getById(reqId)

        assertNotNull(req)
        assertEquals("New", req.name)
        assertEquals("POST", req.method.name)
        assertEquals("json", req.bodyType)
        assertEquals(2, req.seq)
        assertEquals("Content-Type", req.headers[0].name)
        assertEquals("limit", req.queryParams[0].name)
    }

    @Test
    fun deleteRequest() {
        val (collDao, _, reqDao) = createDaos()
        val now = Clock.System.now().toEpochMilliseconds()
        val collId = collDao.insert("API", now)
        val reqId = reqDao.insert(
            collectionId = collId,
            folderId = null,
            name = "Delete Me",
            method = "GET",
            url = "/gone",
            headers = "[]",
            queryParams = "[]",
            bodyType = null,
            bodyContent = null,
            seq = 0,
            now = now,
        )
        reqDao.delete(reqId)

        assertNull(reqDao.getById(reqId))
    }

    @Test
    fun deletingFolderSetsRequestFolderIdNull() {
        val (collDao, folderDao, reqDao) = createDaos()
        val now = Clock.System.now().toEpochMilliseconds()
        val collId = collDao.insert("API", now)
        val folderId = folderDao.insert(collId, "Users", 0, now)
        reqDao.insert(
            collectionId = collId,
            folderId = folderId,
            name = "Get Users",
            method = "GET",
            url = "/users",
            headers = "[]",
            queryParams = "[]",
            bodyType = null,
            bodyContent = null,
            seq = 0,
            now = now,
        )
        folderDao.delete(folderId)

        val rootRequests = reqDao.getByCollectionRoot(collId)

        assertEquals(1, rootRequests.size)
        assertNull(rootRequests[0].folderId)
    }

    @Test
    fun maxSeqHelpersReturnExpectedValues() {
        val (collDao, folderDao, reqDao) = createDaos()
        val now = Clock.System.now().toEpochMilliseconds()
        val collId = collDao.insert("API", now)
        val folderId = folderDao.insert(collId, "Users", 0, now)

        assertEquals(-1, reqDao.maxSeqInCollectionRoot(collId))
        assertEquals(-1, reqDao.maxSeqInFolder(folderId))

        reqDao.insert(
            collectionId = collId,
            folderId = null,
            name = "Root 1",
            method = "GET",
            url = "/r1",
            headers = "[]",
            queryParams = "[]",
            bodyType = null,
            bodyContent = null,
            seq = 0,
            now = now,
        )
        reqDao.insert(
            collectionId = collId,
            folderId = null,
            name = "Root 2",
            method = "GET",
            url = "/r2",
            headers = "[]",
            queryParams = "[]",
            bodyType = null,
            bodyContent = null,
            seq = 2,
            now = now,
        )
        reqDao.insert(
            collectionId = collId,
            folderId = folderId,
            name = "Folder 1",
            method = "GET",
            url = "/f1",
            headers = "[]",
            queryParams = "[]",
            bodyType = null,
            bodyContent = null,
            seq = 3,
            now = now,
        )

        assertEquals(2, reqDao.maxSeqInCollectionRoot(collId))
        assertEquals(3, reqDao.maxSeqInFolder(folderId))
    }
}
