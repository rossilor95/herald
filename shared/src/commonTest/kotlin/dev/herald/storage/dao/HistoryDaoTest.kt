package dev.herald.storage.dao

import dev.herald.storage.DatabaseProvider
import dev.herald.storage.DriverFactory
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class HistoryDaoTest {
    private data class Daos(
        val collectionDao: CollectionDao,
        val requestDao: RequestDao,
        val historyDao: HistoryDao,
    )

    private fun createDaos(): Daos {
        val provider = DatabaseProvider(DriverFactory())
        return Daos(
            CollectionDao(provider.database),
            RequestDao(provider.database),
            HistoryDao(provider.database),
        )
    }

    private fun Daos.insertTestRequest(): Long {
        val now = Clock.System.now().toEpochMilliseconds()
        val collId = collectionDao.insert("API", now)
        return requestDao.insert(
            collectionId = collId,
            folderId = null,
            name = "Test",
            method = "GET",
            url = "/test",
            headers = "[]",
            queryParams = "[]",
            bodyType = null,
            bodyContent = null,
            seq = 0,
            now = now,
        )
    }

    @Test
    fun insertAndRetrieve() {
        val daos = createDaos()
        val reqId = daos.insertTestRequest()
        val now = Clock.System.now().toEpochMilliseconds()
        val id = daos.historyDao.insert(
            requestId = reqId,
            method = "GET",
            resolvedUrl = "http://localhost/test",
            requestHeaders = """[{"name":"Authorization","value":"Bearer token","enabled":true}]""",
            requestBody = null,
            responseStatus = 200,
            responseHeaders = """[{"name":"Content-Type","value":"application/json","enabled":true}]""",
            responseBody = """{"ok":true}""",
            durationMs = 42,
            createdAt = now,
        )

        val entry = daos.historyDao.getById(id)

        assertNotNull(entry)
        assertEquals(200, entry.responseStatus)
        assertEquals(42, entry.durationMs)
        assertEquals("Authorization", entry.requestHeaders[0].name)
        assertEquals("Content-Type", entry.responseHeaders[0].name)
    }

    @Test
    fun getAllReturnsNewestFirst() {
        val daos = createDaos()
        val reqId = daos.insertTestRequest()
        val now = Clock.System.now().toEpochMilliseconds()
        daos.historyDao.insert(
            requestId = reqId,
            method = "GET",
            resolvedUrl = "/first",
            requestHeaders = "[]",
            requestBody = null,
            responseStatus = 200,
            responseHeaders = "[]",
            responseBody = null,
            durationMs = 10,
            createdAt = now,
        )
        daos.historyDao.insert(
            requestId = reqId,
            method = "GET",
            resolvedUrl = "/second",
            requestHeaders = "[]",
            requestBody = null,
            responseStatus = 201,
            responseHeaders = "[]",
            responseBody = null,
            durationMs = 20,
            createdAt = now + 1000,
        )

        val all = daos.historyDao.getAll(100)

        assertEquals(2, all.size)
        assertEquals("/second", all[0].resolvedUrl)
        assertEquals("/first", all[1].resolvedUrl)
    }

    @Test
    fun pruneKeepsOnlyLimit() {
        val daos = createDaos()
        val reqId = daos.insertTestRequest()
        val now = Clock.System.now().toEpochMilliseconds()
        repeat(5) { i ->
            daos.historyDao.insert(
                requestId = reqId,
                method = "GET",
                resolvedUrl = "/req-$i",
                requestHeaders = "[]",
                requestBody = null,
                responseStatus = 200,
                responseHeaders = "[]",
                responseBody = null,
                durationMs = 10,
                createdAt = now + i * 1000L,
            )
        }
        daos.historyDao.prune(3)

        val count = daos.historyDao.count()
        val all = daos.historyDao.getAll(100)

        assertEquals(3, count)
        assertEquals("/req-4", all[0].resolvedUrl)
        assertEquals("/req-3", all[1].resolvedUrl)
        assertEquals("/req-2", all[2].resolvedUrl)
    }

    @Test
    fun cascadeDeleteOnRequest() {
        val daos = createDaos()
        val reqId = daos.insertTestRequest()
        val now = Clock.System.now().toEpochMilliseconds()
        daos.historyDao.insert(
            requestId = reqId,
            method = "GET",
            resolvedUrl = "/test",
            requestHeaders = "[]",
            requestBody = null,
            responseStatus = 200,
            responseHeaders = "[]",
            responseBody = null,
            durationMs = 10,
            createdAt = now,
        )
        daos.requestDao.delete(reqId)

        assertEquals(0, daos.historyDao.count())
    }
}
