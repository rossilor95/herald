package dev.herald.storage.dao

import dev.herald.storage.DatabaseProvider
import dev.herald.storage.DriverFactory
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VariableDaoTest {
    private fun createDaos(): Pair<EnvironmentDao, VariableDao> {
        val provider = DatabaseProvider(DriverFactory())
        return Pair(
            EnvironmentDao(provider.database),
            VariableDao(provider.database),
        )
    }

    @Test
    fun insertAndRetrieveVariables() {
        val (envDao, varDao) = createDaos()
        val now = Clock.System.now().toEpochMilliseconds()
        val envId = envDao.insert("dev", now)
        varDao.insert(envId, "base_url", "http://localhost:8080", true)
        varDao.insert(envId, "api_key", "secret123", true)

        val vars = varDao.getByEnvironment(envId)

        assertEquals(2, vars.size)
        assertEquals(setOf("base_url", "api_key"), vars.map { it.key }.toSet())
    }

    @Test
    fun getEnabledOnly() {
        val (envDao, varDao) = createDaos()
        val now = Clock.System.now().toEpochMilliseconds()
        val envId = envDao.insert("dev", now)
        varDao.insert(envId, "active", "yes", true)
        varDao.insert(envId, "disabled", "no", false)

        val enabled = varDao.getEnabledByEnvironment(envId)

        assertEquals(1, enabled.size)
        assertEquals("active", enabled[0].key)
        assertTrue(enabled[0].enabled)
    }

    @Test
    fun updateVariable() {
        val (envDao, varDao) = createDaos()
        val now = Clock.System.now().toEpochMilliseconds()
        val envId = envDao.insert("dev", now)
        val varId = varDao.insert(envId, "key", "old", true)
        varDao.update(varId, "key", "new", false, envId)

        val vars = varDao.getByEnvironment(envId)

        assertEquals(1, vars.size)
        assertEquals("new", vars[0].value)
        assertEquals(false, vars[0].enabled)
    }

    @Test
    fun deleteVariable() {
        val (envDao, varDao) = createDaos()
        val now = Clock.System.now().toEpochMilliseconds()
        val envId = envDao.insert("dev", now)
        val varId = varDao.insert(envId, "key", "val", true)
        varDao.delete(varId)

        assertTrue(varDao.getByEnvironment(envId).isEmpty())
    }

    @Test
    fun cascadeDeleteOnEnvironment() {
        val (envDao, varDao) = createDaos()
        val now = Clock.System.now().toEpochMilliseconds()
        val envId = envDao.insert("dev", now)
        varDao.insert(envId, "key", "val", true)
        envDao.delete(envId)

        assertTrue(varDao.getByEnvironment(envId).isEmpty())
    }
}
