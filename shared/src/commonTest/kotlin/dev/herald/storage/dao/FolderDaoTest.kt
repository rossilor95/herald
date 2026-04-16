package dev.herald.storage.dao

import dev.herald.storage.DatabaseProvider
import dev.herald.storage.DriverFactory
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FolderDaoTest {
    private fun createDaos(): Pair<CollectionDao, FolderDao> {
        val provider = DatabaseProvider(DriverFactory())
        return Pair(
            CollectionDao(provider.database),
            FolderDao(provider.database),
        )
    }

    @Test
    fun insertAndRetrieveFolder() {
        val (collDao, folderDao) = createDaos()
        val now = Clock.System.now().toEpochMilliseconds()
        val collId = collDao.insert("API", now)
        folderDao.insert(collId, "Users", 0, now)

        val folders = folderDao.getByCollection(collId)

        assertEquals(1, folders.size)
        assertEquals("Users", folders[0].name)
        assertEquals(0, folders[0].seq)
    }

    @Test
    fun foldersOrderedBySeq() {
        val (collDao, folderDao) = createDaos()
        val now = Clock.System.now().toEpochMilliseconds()
        val collId = collDao.insert("API", now)
        folderDao.insert(collId, "Zebra", 2, now)
        folderDao.insert(collId, "Alpha", 0, now)
        folderDao.insert(collId, "Middle", 1, now)

        val folders = folderDao.getByCollection(collId)

        assertEquals(listOf("Alpha", "Middle", "Zebra"), folders.map { it.name })
    }

    @Test
    fun updateFolder() {
        val (collDao, folderDao) = createDaos()
        val now = Clock.System.now().toEpochMilliseconds()
        val collId = collDao.insert("API", now)
        val folderId = folderDao.insert(collId, "Old", 0, now)
        folderDao.update(folderId, "New", 1, now + 1000)

        val folder = folderDao.getById(folderId)

        assertNotNull(folder)
        assertEquals("New", folder.name)
        assertEquals(1, folder.seq)
        assertEquals(now + 1000, folder.updatedAt)
    }

    @Test
    fun deleteFolder() {
        val (collDao, folderDao) = createDaos()
        val now = Clock.System.now().toEpochMilliseconds()
        val collId = collDao.insert("API", now)
        val folderId = folderDao.insert(collId, "Users", 0, now)
        folderDao.delete(folderId)

        assertTrue(folderDao.getByCollection(collId).isEmpty())
    }

    @Test
    fun maxSeqReturnsNegativeOneForEmpty() {
        val (collDao, folderDao) = createDaos()
        val now = Clock.System.now().toEpochMilliseconds()
        val collId = collDao.insert("API", now)

        assertEquals(-1, folderDao.maxSeq(collId))
    }
}
