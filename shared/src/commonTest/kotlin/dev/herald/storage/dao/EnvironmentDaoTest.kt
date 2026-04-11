package dev.herald.storage.dao

import dev.herald.storage.DatabaseProvider
import dev.herald.storage.DriverFactory
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class EnvironmentDaoTest {
    private fun createDao(): EnvironmentDao {
        val provider = DatabaseProvider(DriverFactory())
        return EnvironmentDao(provider.database)
    }

    @Test
    fun insertAndRetrieve() {
        val dao = createDao()
        val now = Clock.System.now().toEpochMilliseconds()
        val id = dao.insert("dev", now)

        val env = dao.getById(id)

        assertNotNull(env)
        assertEquals("dev", env.name)
        assertEquals(now, env.createdAt)
    }

    @Test
    fun getAllReturnsAlphabetical() {
        val dao = createDao()
        val now = Clock.System.now().toEpochMilliseconds()
        dao.insert("staging", now)
        dao.insert("dev", now)
        dao.insert("prod", now)

        val all = dao.getAll()

        assertEquals(listOf("dev", "prod", "staging"), all.map { it.name })
    }

    @Test
    fun updateEnvironment() {
        val dao = createDao()
        val now = Clock.System.now().toEpochMilliseconds()
        val id = dao.insert("old", now)
        dao.update(id, "new", now + 1000)

        val updated = dao.getById(id)

        assertNotNull(updated)
        assertEquals("new", updated.name)
        assertEquals(now + 1000, updated.updatedAt)
    }

    @Test
    fun deleteEnvironment() {
        val dao = createDao()
        val now = Clock.System.now().toEpochMilliseconds()
        val id = dao.insert("delete-me", now)
        dao.delete(id)

        assertNull(dao.getById(id))
    }
}
