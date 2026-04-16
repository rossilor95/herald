package dev.herald.storage.dao

import dev.herald.storage.DatabaseProvider
import dev.herald.storage.DriverFactory
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CollectionDaoTest {
    private fun createDao(): CollectionDao {
        val provider = DatabaseProvider(DriverFactory())
        return CollectionDao(provider.database)
    }

    @Test
    fun insertAndRetrieveCollection() {
        val dao = createDao()
        val now = Clock.System.now().toEpochMilliseconds()
        val id = dao.insert("My API", now)

        val result = dao.getById(id)

        assertNotNull(result)
        assertEquals("My API", result.name)
        assertEquals(now, result.createdAt)
    }

    @Test
    fun getAllReturnsAlphabetical() {
        val dao = createDao()
        val now = Clock.System.now().toEpochMilliseconds()
        dao.insert("Zebra", now)
        dao.insert("Alpha", now)

        val all = dao.getAll()

        assertEquals(2, all.size)
        assertEquals("Alpha", all[0].name)
        assertEquals("Zebra", all[1].name)
    }

    @Test
    fun updateCollection() {
        val dao = createDao()
        val now = Clock.System.now().toEpochMilliseconds()
        val id = dao.insert("Old Name", now)
        dao.update(id, "New Name", now + 1000)

        val result = dao.getById(id)

        assertNotNull(result)
        assertEquals("New Name", result.name)
        assertEquals(now + 1000, result.updatedAt)
    }

    @Test
    fun deleteCollection() {
        val dao = createDao()
        val now = Clock.System.now().toEpochMilliseconds()
        val id = dao.insert("To Delete", now)
        dao.delete(id)

        assertNull(dao.getById(id))
    }

    @Test
    fun deleteCollectionCascadesFolders() {
        val dao = createDao()
        val folderDao = FolderDao(dao.database)
        val now = Clock.System.now().toEpochMilliseconds()
        val collId = dao.insert("API", now)
        folderDao.insert(collId, "Users", 0, now)
        dao.delete(collId)

        assertTrue(folderDao.getByCollection(collId).isEmpty())
    }
}
