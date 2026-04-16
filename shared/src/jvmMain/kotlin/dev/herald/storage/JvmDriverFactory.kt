package dev.herald.storage

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File
import java.util.Properties

actual class DriverFactory(private val dbPath: String? = null) {
    actual fun createDriver(): SqlDriver {
        val url = if (dbPath != null) {
            val file = File(dbPath)
            file.parentFile?.mkdirs()
            "jdbc:sqlite:$dbPath"
        } else {
            JdbcSqliteDriver.IN_MEMORY
        }
        val driver = JdbcSqliteDriver(
            url = url,
            properties = Properties(),
            schema = HeraldDatabase.Schema
        )
        driver.execute(
            identifier = null,
            sql = "PRAGMA foreign_keys = ON",
            parameters = 0,
            binders = null,
        ).value
        return driver
    }
}
