# Herald V1 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a desktop API client (Postman alternative) with request building, response viewing, collections, environments, tabs, and history.

**Architecture:** Two-module KMP Gradle project. `:shared` contains core logic (HTTP engine, variable resolution, storage via SQLDelight, domain model). `:desktop` contains Compose Multiplatform UI. Desktop calls Shared via suspend functions and StateFlow.

**Tech Stack:** Kotlin 2.3.20, Gradle 9.3, Compose Multiplatform 1.10.3, Ktor Client 3.4.2 (CIO engine), SQLDelight 2.3.2, kotlinx-serialization, kotlinx-coroutines

## Version Catalog

All dependency versions are centralized in `gradle/libs.versions.toml`:

```toml
[versions]
kotlin = "2.3.20"
compose-multiplatform = "1.10.3"
sqldelight = "2.3.2"
ktor = "3.4.2"
kotlinx-coroutines = "1.10.2"
kotlinx-serialization = "1.8.1"
kotlinx-datetime = "0.6.2"
junit = "5.12.2"
kotlin-test = "2.3.20"

[libraries]
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-cio = { module = "io.ktor:ktor-client-cio", version.ref = "ktor" }
sqldelight-sqlite-driver = { module = "app.cash.sqldelight:sqlite-driver", version.ref = "sqldelight" }
sqldelight-coroutines = { module = "app.cash.sqldelight:coroutines-extensions", version.ref = "sqldelight" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-swing = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-swing", version.ref = "kotlinx-coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }
junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
kotlin-test = { module = "org.jetbrains.kotlin:kotlin-test", version.ref = "kotlin-test" }
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
compose-multiplatform = { id = "org.jetbrains.compose", version.ref = "compose-multiplatform" }
compose-compiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
```

## Parallel Work Tracks

This plan is split into two tracks that can be worked in parallel after Task 1 (project scaffolding) is complete:

- **Track A (Shared module):** Tasks 2 through 7. Storage, domain model, HTTP engine, variable resolver, executor. No UI.
- **Track B (Desktop module):** Tasks 8 through 14. Compose UI, theming, state management. Depends on Task 1 only; can stub `:shared` interfaces early.

**Dependency graph:**
```
Task 1 (scaffold) ──┬── Track A: 2 → 3 → 4 → 5 → 6 → 7
                     └── Track B: 8 → 9 → 10 → 11 → 12
                                                   └── Tasks 13 → 14 → 15 → 16 (integration, after both tracks)
```

Track B tasks 8 through 12 use hardcoded/stub data and do not depend on Track A at all. Tasks 13 through 16 (integration) require both tracks to be complete.

## File Structure

### Root
```
settings.gradle.kts
build.gradle.kts
gradle.properties
gradle/libs.versions.toml
```

### `:shared` module
```
shared/
  build.gradle.kts
  src/
    commonMain/kotlin/dev/herald/
      core/
        model/
          Collection.kt
          Folder.kt
          Request.kt
          Environment.kt
          Variable.kt
          HistoryEntry.kt
          HttpMethod.kt
          KeyValueRow.kt
          RequestResult.kt
        http/
          HttpEngine.kt
        variable/
          VariableResolver.kt
        executor/
          RequestExecutor.kt
      storage/
        DriverFactory.kt
        DatabaseProvider.kt
        dao/
          CollectionDao.kt
          FolderDao.kt
          RequestDao.kt
          EnvironmentDao.kt
          VariableDao.kt
          HistoryDao.kt
    commonMain/sqldelight/dev/herald/storage/
      Herald.sq
      migrations/
        1.sqm
    jvmMain/kotlin/dev/herald/storage/
      JvmDriverFactory.kt
    commonTest/kotlin/dev/herald/
      core/
        variable/
          VariableResolverTest.kt
        http/
          HttpEngineTest.kt
        executor/
          RequestExecutorTest.kt
      storage/
        dao/
          CollectionDaoTest.kt
          FolderDaoTest.kt
          RequestDaoTest.kt
          EnvironmentDaoTest.kt
          VariableDaoTest.kt
          HistoryDaoTest.kt
```

### `:desktop` module
```
desktop/
  build.gradle.kts
  src/
    jvmMain/kotlin/dev/herald/desktop/
      Main.kt
      App.kt
      theme/
        Theme.kt
        Colors.kt
        Typography.kt
      state/
        AppState.kt
        TabState.kt
      ui/
        sidebar/
          Sidebar.kt
          CollectionTree.kt
          HistoryList.kt
          EnvironmentSelector.kt
        tabs/
          TabBar.kt
        request/
          RequestEditor.kt
          MethodDropdown.kt
          UrlBar.kt
          KeyValueEditor.kt
          BodyEditor.kt
        response/
          ResponseViewer.kt
          SyntaxHighlighter.kt
          StatusBar.kt
        history/
          HistoryDetailView.kt
        common/
          PromptDialog.kt
          ContextMenu.kt
    jvmMain/resources/fonts/
      JetBrainsMono-Regular.ttf
      JetBrainsMono-Bold.ttf
      DMSans-Regular.ttf
      DMSans-Medium.ttf
      DMSans-Bold.ttf
      OFL.txt
```

---

## Task 1: Project Scaffolding (prerequisite for both tracks)

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `gradle/libs.versions.toml`
- Create: `shared/build.gradle.kts`
- Create: `desktop/build.gradle.kts`
- Create: `shared/src/commonMain/kotlin/dev/herald/core/model/HttpMethod.kt`
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/Main.kt`

- [ ] **Step 1: Initialize Gradle wrapper**

Run:
```bash
gradle wrapper --gradle-version 9.3
```

- [ ] **Step 2: Create `gradle/libs.versions.toml`**

Use the version catalog from the top of this plan (copy verbatim).

- [ ] **Step 3: Create `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
kotlin.code.style=official
```

- [ ] **Step 4: Create root `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolution {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "herald"
include(":shared")
include(":desktop")
```

- [ ] **Step 5: Create root `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.sqldelight) apply false
}
```

- [ ] **Step 6: Create `shared/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.ktor.client.core)
            implementation(libs.sqldelight.coroutines)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(libs.sqldelight.sqlite.driver)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
        jvmTest.dependencies {
            implementation(libs.junit.jupiter)
            implementation(libs.sqldelight.sqlite.driver)
        }
    }
}

sqldelight {
    databases {
        create("HeraldDatabase") {
            packageName.set("dev.herald.storage")
            srcDirs("src/commonMain/sqldelight")
        }
    }
}
```

- [ ] **Step 7: Create `desktop/build.gradle.kts`**

```kotlin
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(project(":shared"))
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

compose.desktop {
    application {
        mainClass = "dev.herald.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Herald"
            packageVersion = "1.0.0"
        }
    }
}
```

- [ ] **Step 8: Create minimal SQLDelight schema to satisfy the plugin**

Create `shared/src/commonMain/sqldelight/dev/herald/storage/Herald.sq`:

```sql
CREATE TABLE IF NOT EXISTS method (
    name TEXT NOT NULL PRIMARY KEY
);

INSERT OR IGNORE INTO method (name) VALUES ('GET');
INSERT OR IGNORE INTO method (name) VALUES ('POST');
INSERT OR IGNORE INTO method (name) VALUES ('PUT');
INSERT OR IGNORE INTO method (name) VALUES ('DELETE');
INSERT OR IGNORE INTO method (name) VALUES ('PATCH');
INSERT OR IGNORE INTO method (name) VALUES ('HEAD');
INSERT OR IGNORE INTO method (name) VALUES ('OPTIONS');

selectAllMethods:
SELECT name FROM method ORDER BY name;
```

- [ ] **Step 9: Create expect/actual for DriverFactory**

Create `shared/src/commonMain/kotlin/dev/herald/storage/DriverFactory.kt`:

```kotlin
package dev.herald.storage

import app.cash.sqldelight.db.SqlDriver

expect class DriverFactory {
    fun createDriver(): SqlDriver
}
```

Create `shared/src/jvmMain/kotlin/dev/herald/storage/JvmDriverFactory.kt`:

```kotlin
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
        return driver
    }
}
```

- [ ] **Step 10: Create HttpMethod enum**

Create `shared/src/commonMain/kotlin/dev/herald/core/model/HttpMethod.kt`:

```kotlin
package dev.herald.core.model

enum class HttpMethod {
    GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS;

    companion object {
        fun fromString(value: String): HttpMethod =
            entries.first { it.name.equals(value, ignoreCase = true) }
    }
}
```

- [ ] **Step 11: Create desktop entry point**

Create `desktop/src/jvmMain/kotlin/dev/herald/desktop/Main.kt`:

```kotlin
package dev.herald.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Herald",
        state = rememberWindowState(size = DpSize(1200.dp, 800.dp))
    ) {
        MaterialTheme(colorScheme = darkColorScheme()) {
            Surface(modifier = Modifier.fillMaxSize()) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Herald", style = MaterialTheme.typography.headlineLarge)
                }
            }
        }
    }
}
```

- [ ] **Step 12: Verify the project compiles**

Run:
```bash
./gradlew build
```
Expected: BUILD SUCCESSFUL. Both modules compile. SQLDelight generates code from `Herald.sq`.

- [ ] **Step 13: Verify the desktop app launches**

Run:
```bash
./gradlew :desktop:run
```
Expected: A window opens titled "Herald" with dark background and centered "Herald" text. Close the window manually.

- [ ] **Step 14: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradle/ shared/ desktop/ .gitignore
git commit -m "feat: scaffold KMP project with shared and desktop modules"
```

---

## Track A: Shared Module

### Task 2: Domain Model (Track A)

**Files:**
- Create: `shared/src/commonMain/kotlin/dev/herald/core/model/Collection.kt`
- Create: `shared/src/commonMain/kotlin/dev/herald/core/model/Folder.kt`
- Create: `shared/src/commonMain/kotlin/dev/herald/core/model/Request.kt`
- Create: `shared/src/commonMain/kotlin/dev/herald/core/model/Environment.kt`
- Create: `shared/src/commonMain/kotlin/dev/herald/core/model/Variable.kt`
- Create: `shared/src/commonMain/kotlin/dev/herald/core/model/HistoryEntry.kt`
- Create: `shared/src/commonMain/kotlin/dev/herald/core/model/KeyValueRow.kt`
- Create: `shared/src/commonMain/kotlin/dev/herald/core/model/RequestResult.kt`

- [ ] **Step 1: Create `KeyValueRow` data class**

This is the shared type for headers and query params (the name/value/enabled triple serialized as JSON in the database).

```kotlin
package dev.herald.core.model

import kotlinx.serialization.Serializable

@Serializable
data class KeyValueRow(
    val name: String,
    val value: String,
    val enabled: Boolean = true,
)
```

- [ ] **Step 2: Create `Collection` data class**

```kotlin
package dev.herald.core.model

data class Collection(
    val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)
```

- [ ] **Step 3: Create `Folder` data class**

```kotlin
package dev.herald.core.model

data class Folder(
    val id: Long = 0,
    val collectionId: Long,
    val name: String,
    val seq: Int,
    val createdAt: Long,
    val updatedAt: Long,
)
```

- [ ] **Step 4: Create `Request` data class**

```kotlin
package dev.herald.core.model

data class Request(
    val id: Long = 0,
    val collectionId: Long,
    val folderId: Long? = null,
    val name: String,
    val method: HttpMethod = HttpMethod.GET,
    val url: String = "",
    val headers: List<KeyValueRow> = emptyList(),
    val queryParams: List<KeyValueRow> = emptyList(),
    val bodyType: String? = null,
    val bodyContent: String? = null,
    val seq: Int = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
```

- [ ] **Step 5: Create `Environment` data class**

```kotlin
package dev.herald.core.model

data class Environment(
    val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
)
```

- [ ] **Step 6: Create `Variable` data class**

```kotlin
package dev.herald.core.model

data class Variable(
    val id: Long = 0,
    val environmentId: Long,
    val key: String,
    val value: String,
    val enabled: Boolean = true,
)
```

- [ ] **Step 7: Create `HistoryEntry` data class**

```kotlin
package dev.herald.core.model

data class HistoryEntry(
    val id: Long = 0,
    val requestId: Long,
    val method: HttpMethod,
    val resolvedUrl: String,
    val requestHeaders: List<KeyValueRow> = emptyList(),
    val requestBody: String? = null,
    val responseStatus: Int,
    val responseHeaders: List<KeyValueRow> = emptyList(),
    val responseBody: String? = null,
    val durationMs: Long,
    val createdAt: Long,
)
```

- [ ] **Step 8: Create `RequestResult` sealed class**

This is the return type from the executor.

```kotlin
package dev.herald.core.model

sealed class RequestResult {
    data class Success(val historyEntry: HistoryEntry) : RequestResult()
    data class UnresolvedVariables(val variables: List<String>) : RequestResult()
    data class NetworkError(val message: String, val cause: Throwable? = null) : RequestResult()
}
```

- [ ] **Step 9: Verify compilation**

Run:
```bash
./gradlew :shared:compileKotlinJvm
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 10: Commit**

```bash
git add shared/src/commonMain/kotlin/dev/herald/core/model/
git commit -m "feat: add domain model data classes"
```

---

### Task 3: SQLDelight Schema and DAOs (Track A)

**Files:**
- Modify: `shared/src/commonMain/sqldelight/dev/herald/storage/Herald.sq`
- Create: `shared/src/commonMain/kotlin/dev/herald/storage/DatabaseProvider.kt`
- Create: `shared/src/commonMain/kotlin/dev/herald/storage/dao/CollectionDao.kt`
- Create: `shared/src/commonMain/kotlin/dev/herald/storage/dao/FolderDao.kt`
- Create: `shared/src/commonMain/kotlin/dev/herald/storage/dao/RequestDao.kt`
- Create: `shared/src/commonMain/kotlin/dev/herald/storage/dao/EnvironmentDao.kt`
- Create: `shared/src/commonMain/kotlin/dev/herald/storage/dao/VariableDao.kt`
- Create: `shared/src/commonMain/kotlin/dev/herald/storage/dao/HistoryDao.kt`
- Create: `shared/src/commonTest/kotlin/dev/herald/storage/dao/CollectionDaoTest.kt`
- Create: `shared/src/commonTest/kotlin/dev/herald/storage/dao/FolderDaoTest.kt`
- Create: `shared/src/commonTest/kotlin/dev/herald/storage/dao/RequestDaoTest.kt`
- Create: `shared/src/commonTest/kotlin/dev/herald/storage/dao/EnvironmentDaoTest.kt`
- Create: `shared/src/commonTest/kotlin/dev/herald/storage/dao/VariableDaoTest.kt`
- Create: `shared/src/commonTest/kotlin/dev/herald/storage/dao/HistoryDaoTest.kt`

- [ ] **Step 1: Write full SQLDelight schema**

Replace `shared/src/commonMain/sqldelight/dev/herald/storage/Herald.sq` with:

```sql
-- Method lookup table
CREATE TABLE IF NOT EXISTS method (
    name TEXT NOT NULL PRIMARY KEY
);

INSERT OR IGNORE INTO method (name) VALUES ('GET');
INSERT OR IGNORE INTO method (name) VALUES ('POST');
INSERT OR IGNORE INTO method (name) VALUES ('PUT');
INSERT OR IGNORE INTO method (name) VALUES ('DELETE');
INSERT OR IGNORE INTO method (name) VALUES ('PATCH');
INSERT OR IGNORE INTO method (name) VALUES ('HEAD');
INSERT OR IGNORE INTO method (name) VALUES ('OPTIONS');

-- Collections
CREATE TABLE IF NOT EXISTS collection (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Folders
CREATE TABLE IF NOT EXISTS folder (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    collection_id INTEGER NOT NULL REFERENCES collection(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    seq INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Requests
CREATE TABLE IF NOT EXISTS request (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    collection_id INTEGER NOT NULL REFERENCES collection(id) ON DELETE CASCADE,
    folder_id INTEGER REFERENCES folder(id) ON DELETE SET NULL,
    name TEXT NOT NULL,
    method TEXT NOT NULL REFERENCES method(name),
    url TEXT NOT NULL DEFAULT '',
    headers TEXT NOT NULL DEFAULT '[]',
    query_params TEXT NOT NULL DEFAULT '[]',
    body_type TEXT,
    body_content TEXT,
    seq INTEGER NOT NULL DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Environments
CREATE TABLE IF NOT EXISTS environment (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- Variables
CREATE TABLE IF NOT EXISTS variable (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    environment_id INTEGER NOT NULL REFERENCES environment(id) ON DELETE CASCADE,
    key TEXT NOT NULL,
    value TEXT NOT NULL DEFAULT '',
    enabled INTEGER NOT NULL DEFAULT 1
);

-- History entries
CREATE TABLE IF NOT EXISTS history_entry (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    request_id INTEGER NOT NULL REFERENCES request(id) ON DELETE CASCADE,
    method TEXT NOT NULL REFERENCES method(name),
    resolved_url TEXT NOT NULL,
    request_headers TEXT NOT NULL DEFAULT '[]',
    request_body TEXT,
    response_status INTEGER NOT NULL,
    response_headers TEXT NOT NULL DEFAULT '[]',
    response_body TEXT,
    duration_ms INTEGER NOT NULL,
    created_at INTEGER NOT NULL
);

-- ============ Collection Queries ============

selectAllCollections:
SELECT * FROM collection ORDER BY name ASC;

selectCollectionById:
SELECT * FROM collection WHERE id = ?;

insertCollection:
INSERT INTO collection (name, created_at, updated_at) VALUES (?, ?, ?);

lastInsertId:
SELECT last_insert_rowid();

updateCollection:
UPDATE collection SET name = ?, updated_at = ? WHERE id = ?;

deleteCollection:
DELETE FROM collection WHERE id = ?;

-- ============ Folder Queries ============

selectFoldersByCollection:
SELECT * FROM folder WHERE collection_id = ? ORDER BY seq ASC;

selectFolderById:
SELECT * FROM folder WHERE id = ?;

insertFolder:
INSERT INTO folder (collection_id, name, seq, created_at, updated_at) VALUES (?, ?, ?, ?, ?);

updateFolder:
UPDATE folder SET name = ?, seq = ?, updated_at = ? WHERE id = ?;

deleteFolder:
DELETE FROM folder WHERE id = ?;

maxFolderSeq:
SELECT COALESCE(MAX(seq), -1) FROM folder WHERE collection_id = ?;

-- ============ Request Queries ============

selectRequestsByCollection:
SELECT * FROM request WHERE collection_id = ? AND folder_id IS NULL ORDER BY seq ASC;

selectRequestsByFolder:
SELECT * FROM request WHERE folder_id = ? ORDER BY seq ASC;

selectRequestById:
SELECT * FROM request WHERE id = ?;

insertRequest:
INSERT INTO request (collection_id, folder_id, name, method, url, headers, query_params, body_type, body_content, seq, created_at, updated_at)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

updateRequest:
UPDATE request SET collection_id = ?, folder_id = ?, name = ?, method = ?, url = ?, headers = ?, query_params = ?, body_type = ?, body_content = ?, seq = ?, updated_at = ? WHERE id = ?;

deleteRequest:
DELETE FROM request WHERE id = ?;

maxRequestSeqInFolder:
SELECT COALESCE(MAX(seq), -1) FROM request WHERE folder_id = ?;

maxRequestSeqInCollectionRoot:
SELECT COALESCE(MAX(seq), -1) FROM request WHERE collection_id = ? AND folder_id IS NULL;

-- ============ Environment Queries ============

selectAllEnvironments:
SELECT * FROM environment ORDER BY name ASC;

selectEnvironmentById:
SELECT * FROM environment WHERE id = ?;

insertEnvironment:
INSERT INTO environment (name, created_at, updated_at) VALUES (?, ?, ?);

updateEnvironment:
UPDATE environment SET name = ?, updated_at = ? WHERE id = ?;

deleteEnvironment:
DELETE FROM environment WHERE id = ?;

-- ============ Variable Queries ============

selectVariablesByEnvironment:
SELECT * FROM variable WHERE environment_id = ?;

selectEnabledVariablesByEnvironment:
SELECT * FROM variable WHERE environment_id = ? AND enabled = 1;

insertVariable:
INSERT INTO variable (environment_id, key, value, enabled) VALUES (?, ?, ?, ?);

updateVariable:
UPDATE variable SET key = ?, value = ?, enabled = ?, environment_id = ? WHERE id = ?;

deleteVariable:
DELETE FROM variable WHERE id = ?;

-- ============ History Queries ============

selectHistoryByRequest:
SELECT * FROM history_entry WHERE request_id = ? ORDER BY created_at DESC;

selectAllHistory:
SELECT * FROM history_entry ORDER BY created_at DESC LIMIT ?;

selectHistoryById:
SELECT * FROM history_entry WHERE id = ?;

insertHistoryEntry:
INSERT INTO history_entry (request_id, method, resolved_url, request_headers, request_body, response_status, response_headers, response_body, duration_ms, created_at)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);

pruneHistory:
DELETE FROM history_entry WHERE id NOT IN (SELECT id FROM history_entry ORDER BY created_at DESC LIMIT ?);

countHistory:
SELECT COUNT(*) FROM history_entry;

deleteHistoryById:
DELETE FROM history_entry WHERE id = ?;
```

- [ ] **Step 2: Verify SQLDelight generates code**

Run:
```bash
./gradlew :shared:generateCommonMainHeraldDatabaseInterface
```
Expected: BUILD SUCCESSFUL. Generated code appears under `shared/build/`.

- [ ] **Step 3: Create `DatabaseProvider`**

```kotlin
package dev.herald.storage

class DatabaseProvider(driverFactory: DriverFactory) {
    val database: HeraldDatabase = HeraldDatabase(driverFactory.createDriver())
}
```

- [ ] **Step 4: Write `CollectionDao` test**

```kotlin
package dev.herald.storage.dao

import dev.herald.core.model.Collection
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
```

- [ ] **Step 5: Implement `CollectionDao`**

```kotlin
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
```

- [ ] **Step 6: Run `CollectionDaoTest`**

Run:
```bash
./gradlew :shared:jvmTest --tests "dev.herald.storage.dao.CollectionDaoTest"
```
Expected: All tests PASS.

- [ ] **Step 7: Write `FolderDao` test**

```kotlin
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
        val folderId = folderDao.insert(collId, "Users", 0, now)
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
```

- [ ] **Step 8: Implement `FolderDao`**

```kotlin
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
```

- [ ] **Step 9: Run `FolderDaoTest`**

Run:
```bash
./gradlew :shared:jvmTest --tests "dev.herald.storage.dao.FolderDaoTest"
```
Expected: All tests PASS.

- [ ] **Step 10: Write `RequestDao` test**

```kotlin
package dev.herald.storage.dao

import dev.herald.storage.DatabaseProvider
import dev.herald.storage.DriverFactory
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
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
            collectionId = collId, folderId = null, name = "Get Users",
            method = "GET", url = "{{base_url}}/users",
            headers = "[]", queryParams = "[]",
            bodyType = null, bodyContent = null, seq = 0, now = now,
        )
        val requests = reqDao.getByCollectionRoot(collId)
        assertEquals(1, requests.size)
        assertEquals("Get Users", requests[0].name)
        assertNull(requests[0].folderId)
    }

    @Test
    fun insertRequestInFolder() {
        val (collDao, folderDao, reqDao) = createDaos()
        val now = Clock.System.now().toEpochMilliseconds()
        val collId = collDao.insert("API", now)
        val folderId = folderDao.insert(collId, "Users", 0, now)
        reqDao.insert(
            collectionId = collId, folderId = folderId, name = "Get Users",
            method = "GET", url = "/users",
            headers = "[]", queryParams = "[]",
            bodyType = null, bodyContent = null, seq = 0, now = now,
        )
        val requests = reqDao.getByFolder(folderId)
        assertEquals(1, requests.size)
        assertEquals(folderId, requests[0].folderId)
    }

    @Test
    fun updateRequest() {
        val (collDao, _, reqDao) = createDaos()
        val now = Clock.System.now().toEpochMilliseconds()
        val collId = collDao.insert("API", now)
        val reqId = reqDao.insert(
            collectionId = collId, folderId = null, name = "Old",
            method = "GET", url = "/old",
            headers = "[]", queryParams = "[]",
            bodyType = null, bodyContent = null, seq = 0, now = now,
        )
        reqDao.update(
            id = reqId, collectionId = collId, folderId = null, name = "New",
            method = "POST", url = "/new",
            headers = "[]", queryParams = "[]",
            bodyType = "json", bodyContent = "{}", seq = 0, now = now + 1000,
        )
        val req = reqDao.getById(reqId)
        assertNotNull(req)
        assertEquals("New", req.name)
        assertEquals("POST", req.method.name)
        assertEquals("json", req.bodyType)
    }

    @Test
    fun deleteRequest() {
        val (collDao, _, reqDao) = createDaos()
        val now = Clock.System.now().toEpochMilliseconds()
        val collId = collDao.insert("API", now)
        val reqId = reqDao.insert(
            collectionId = collId, folderId = null, name = "Delete Me",
            method = "GET", url = "/gone",
            headers = "[]", queryParams = "[]",
            bodyType = null, bodyContent = null, seq = 0, now = now,
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
            collectionId = collId, folderId = folderId, name = "Get Users",
            method = "GET", url = "/users",
            headers = "[]", queryParams = "[]",
            bodyType = null, bodyContent = null, seq = 0, now = now,
        )
        folderDao.delete(folderId)
        val rootRequests = reqDao.getByCollectionRoot(collId)
        assertEquals(1, rootRequests.size)
        assertNull(rootRequests[0].folderId)
    }
}
```

- [ ] **Step 11: Implement `RequestDao`**

```kotlin
package dev.herald.storage.dao

import dev.herald.core.model.HttpMethod
import dev.herald.core.model.KeyValueRow
import dev.herald.core.model.Request
import dev.herald.storage.HeraldDatabase
import kotlinx.serialization.json.Json

class RequestDao(private val database: HeraldDatabase) {
    private val queries = database.heraldQueries

    fun getByCollectionRoot(collectionId: Long): List<Request> =
        queries.selectRequestsByCollection(collectionId).executeAsList().map { it.toModel() }

    fun getByFolder(folderId: Long): List<Request> =
        queries.selectRequestsByFolder(folderId).executeAsList().map { it.toModel() }

    fun getById(id: Long): Request? =
        queries.selectRequestById(id).executeAsOneOrNull()?.toModel()

    fun insert(
        collectionId: Long, folderId: Long?, name: String,
        method: String, url: String, headers: String, queryParams: String,
        bodyType: String?, bodyContent: String?, seq: Int, now: Long,
    ): Long {
        queries.insertRequest(
            collectionId, folderId, name, method, url, headers, queryParams,
            bodyType, bodyContent, seq.toLong(), now, now,
        )
        return queries.lastInsertId().executeAsOne()
    }

    fun update(
        id: Long, collectionId: Long, folderId: Long?, name: String,
        method: String, url: String, headers: String, queryParams: String,
        bodyType: String?, bodyContent: String?, seq: Int, now: Long,
    ) {
        queries.updateRequest(
            collectionId, folderId, name, method, url, headers, queryParams,
            bodyType, bodyContent, seq.toLong(), now, id,
        )
    }

    fun delete(id: Long) {
        queries.deleteRequest(id)
    }
}

private fun dev.herald.storage.Request.toModel() = Request(
    id = id,
    collectionId = collection_id,
    folderId = folder_id,
    name = name,
    method = HttpMethod.fromString(method),
    url = url,
    headers = Json.decodeFromString<List<KeyValueRow>>(headers),
    queryParams = Json.decodeFromString<List<KeyValueRow>>(query_params),
    bodyType = body_type,
    bodyContent = body_content,
    seq = seq.toInt(),
    createdAt = created_at,
    updatedAt = updated_at,
)
```

- [ ] **Step 12: Run `RequestDaoTest`**

Run:
```bash
./gradlew :shared:jvmTest --tests "dev.herald.storage.dao.RequestDaoTest"
```
Expected: All tests PASS.

- [ ] **Step 13: Write `EnvironmentDao` test**

```kotlin
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
        assertEquals("new", dao.getById(id)?.name)
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
```

- [ ] **Step 14: Implement `EnvironmentDao`**

```kotlin
package dev.herald.storage.dao

import dev.herald.core.model.Environment
import dev.herald.storage.HeraldDatabase

class EnvironmentDao(private val database: HeraldDatabase) {
    private val queries = database.heraldQueries

    fun getAll(): List<Environment> =
        queries.selectAllEnvironments().executeAsList().map { it.toModel() }

    fun getById(id: Long): Environment? =
        queries.selectEnvironmentById(id).executeAsOneOrNull()?.toModel()

    fun insert(name: String, now: Long): Long {
        queries.insertEnvironment(name, now, now)
        return queries.lastInsertId().executeAsOne()
    }

    fun update(id: Long, name: String, now: Long) {
        queries.updateEnvironment(name, now, id)
    }

    fun delete(id: Long) {
        queries.deleteEnvironment(id)
    }
}

private fun dev.herald.storage.Environment.toModel() = Environment(
    id = id,
    name = name,
    createdAt = created_at,
    updatedAt = updated_at,
)
```

- [ ] **Step 15: Run `EnvironmentDaoTest`**

Run:
```bash
./gradlew :shared:jvmTest --tests "dev.herald.storage.dao.EnvironmentDaoTest"
```
Expected: All tests PASS.

- [ ] **Step 16: Write `VariableDao` test**

```kotlin
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
    }

    @Test
    fun updateVariable() {
        val (envDao, varDao) = createDaos()
        val now = Clock.System.now().toEpochMilliseconds()
        val envId = envDao.insert("dev", now)
        val varId = varDao.insert(envId, "key", "old", true)
        varDao.update(varId, "key", "new", false, envId)
        val vars = varDao.getByEnvironment(envId)
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
```

- [ ] **Step 17: Implement `VariableDao`**

```kotlin
package dev.herald.storage.dao

import dev.herald.core.model.Variable
import dev.herald.storage.HeraldDatabase

class VariableDao(private val database: HeraldDatabase) {
    private val queries = database.heraldQueries

    fun getByEnvironment(environmentId: Long): List<Variable> =
        queries.selectVariablesByEnvironment(environmentId).executeAsList().map { it.toModel() }

    fun getEnabledByEnvironment(environmentId: Long): List<Variable> =
        queries.selectEnabledVariablesByEnvironment(environmentId).executeAsList().map { it.toModel() }

    fun insert(environmentId: Long, key: String, value: String, enabled: Boolean): Long {
        queries.insertVariable(environmentId, key, value, if (enabled) 1L else 0L)
        return queries.lastInsertId().executeAsOne()
    }

    fun update(id: Long, key: String, value: String, enabled: Boolean, environmentId: Long) {
        queries.updateVariable(key, value, if (enabled) 1L else 0L, environmentId, id)
    }

    fun delete(id: Long) {
        queries.deleteVariable(id)
    }
}

private fun dev.herald.storage.Variable.toModel() = Variable(
    id = id,
    environmentId = environment_id,
    key = key,
    value = value_,
    enabled = enabled == 1L,
)
```

- [ ] **Step 18: Run `VariableDaoTest`**

Run:
```bash
./gradlew :shared:jvmTest --tests "dev.herald.storage.dao.VariableDaoTest"
```
Expected: All tests PASS.

- [ ] **Step 19: Write `HistoryDao` test**

```kotlin
package dev.herald.storage.dao

import dev.herald.storage.DatabaseProvider
import dev.herald.storage.DriverFactory
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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
            collectionId = collId, folderId = null, name = "Test",
            method = "GET", url = "/test", headers = "[]", queryParams = "[]",
            bodyType = null, bodyContent = null, seq = 0, now = now,
        )
    }

    @Test
    fun insertAndRetrieve() {
        val daos = createDaos()
        val reqId = daos.insertTestRequest()
        val now = Clock.System.now().toEpochMilliseconds()
        val id = daos.historyDao.insert(
            requestId = reqId, method = "GET", resolvedUrl = "http://localhost/test",
            requestHeaders = "[]", requestBody = null,
            responseStatus = 200, responseHeaders = "[]",
            responseBody = """{"ok":true}""", durationMs = 42, createdAt = now,
        )
        val entry = daos.historyDao.getById(id)
        assertNotNull(entry)
        assertEquals(200, entry.responseStatus)
        assertEquals(42, entry.durationMs)
    }

    @Test
    fun getAllReturnsNewestFirst() {
        val daos = createDaos()
        val reqId = daos.insertTestRequest()
        val now = Clock.System.now().toEpochMilliseconds()
        daos.historyDao.insert(
            requestId = reqId, method = "GET", resolvedUrl = "/first",
            requestHeaders = "[]", requestBody = null,
            responseStatus = 200, responseHeaders = "[]",
            responseBody = null, durationMs = 10, createdAt = now,
        )
        daos.historyDao.insert(
            requestId = reqId, method = "GET", resolvedUrl = "/second",
            requestHeaders = "[]", requestBody = null,
            responseStatus = 201, responseHeaders = "[]",
            responseBody = null, durationMs = 20, createdAt = now + 1000,
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
                requestId = reqId, method = "GET", resolvedUrl = "/req-$i",
                requestHeaders = "[]", requestBody = null,
                responseStatus = 200, responseHeaders = "[]",
                responseBody = null, durationMs = 10, createdAt = now + i * 1000L,
            )
        }
        daos.historyDao.prune(3)
        val count = daos.historyDao.count()
        assertEquals(3, count)
        val all = daos.historyDao.getAll(100)
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
            requestId = reqId, method = "GET", resolvedUrl = "/test",
            requestHeaders = "[]", requestBody = null,
            responseStatus = 200, responseHeaders = "[]",
            responseBody = null, durationMs = 10, createdAt = now,
        )
        daos.requestDao.delete(reqId)
        assertEquals(0, daos.historyDao.count())
    }
}
```

- [ ] **Step 20: Implement `HistoryDao`**

```kotlin
package dev.herald.storage.dao

import dev.herald.core.model.HistoryEntry
import dev.herald.core.model.HttpMethod
import dev.herald.core.model.KeyValueRow
import dev.herald.storage.HeraldDatabase
import kotlinx.serialization.json.Json

class HistoryDao(private val database: HeraldDatabase) {
    private val queries = database.heraldQueries

    fun getAll(limit: Int): List<HistoryEntry> =
        queries.selectAllHistory(limit.toLong()).executeAsList().map { it.toModel() }

    fun getByRequest(requestId: Long): List<HistoryEntry> =
        queries.selectHistoryByRequest(requestId).executeAsList().map { it.toModel() }

    fun getById(id: Long): HistoryEntry? =
        queries.selectHistoryById(id).executeAsOneOrNull()?.toModel()

    fun insert(
        requestId: Long, method: String, resolvedUrl: String,
        requestHeaders: String, requestBody: String?,
        responseStatus: Int, responseHeaders: String, responseBody: String?,
        durationMs: Long, createdAt: Long,
    ): Long {
        queries.insertHistoryEntry(
            requestId, method, resolvedUrl, requestHeaders, requestBody,
            responseStatus.toLong(), responseHeaders, responseBody, durationMs, createdAt,
        )
        return queries.lastInsertId().executeAsOne()
    }

    fun prune(maxEntries: Int) {
        queries.pruneHistory(maxEntries.toLong())
    }

    fun count(): Long =
        queries.countHistory().executeAsOne()

    fun delete(id: Long) {
        queries.deleteHistoryById(id)
    }
}

private fun dev.herald.storage.History_entry.toModel() = HistoryEntry(
    id = id,
    requestId = request_id,
    method = HttpMethod.fromString(method),
    resolvedUrl = resolved_url,
    requestHeaders = Json.decodeFromString<List<KeyValueRow>>(request_headers),
    requestBody = request_body,
    responseStatus = response_status.toInt(),
    responseHeaders = Json.decodeFromString<List<KeyValueRow>>(response_headers),
    responseBody = response_body,
    durationMs = duration_ms,
    createdAt = created_at,
)
```

- [ ] **Step 21: Run all DAO tests**

Run:
```bash
./gradlew :shared:jvmTest --tests "dev.herald.storage.dao.*"
```
Expected: All tests PASS.

- [ ] **Step 22: Commit**

```bash
git add shared/src/
git commit -m "feat: add SQLDelight schema and DAO layer with tests"
```

---

### Task 4: Variable Resolver (Track A)

**Files:**
- Create: `shared/src/commonMain/kotlin/dev/herald/core/variable/VariableResolver.kt`
- Create: `shared/src/commonTest/kotlin/dev/herald/core/variable/VariableResolverTest.kt`

- [ ] **Step 1: Write `VariableResolverTest`**

```kotlin
package dev.herald.core.variable

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VariableResolverTest {
    @Test
    fun resolvesSingleVariable() {
        val vars = mapOf("base_url" to "http://localhost:8080")
        val result = VariableResolver.resolve("{{base_url}}/users", vars)
        assertTrue(result is ResolveResult.Success)
        assertEquals("http://localhost:8080/users", result.resolved)
    }

    @Test
    fun resolvesMultipleVariables() {
        val vars = mapOf("host" to "localhost", "port" to "8080")
        val result = VariableResolver.resolve("http://{{host}}:{{port}}/api", vars)
        assertTrue(result is ResolveResult.Success)
        assertEquals("http://localhost:8080/api", result.resolved)
    }

    @Test
    fun reportsUnresolvedVariables() {
        val vars = mapOf("base_url" to "http://localhost")
        val result = VariableResolver.resolve("{{base_url}}/{{version}}/users", vars)
        assertTrue(result is ResolveResult.Unresolved)
        assertEquals(listOf("version"), result.variables)
    }

    @Test
    fun reportsMultipleUnresolved() {
        val vars = emptyMap<String, String>()
        val result = VariableResolver.resolve("{{host}}:{{port}}", vars)
        assertTrue(result is ResolveResult.Unresolved)
        assertEquals(listOf("host", "port"), result.variables.sorted())
    }

    @Test
    fun returnsStringUnchangedWhenNoVariables() {
        val result = VariableResolver.resolve("http://localhost/users", emptyMap())
        assertTrue(result is ResolveResult.Success)
        assertEquals("http://localhost/users", result.resolved)
    }

    @Test
    fun handlesEmptyString() {
        val result = VariableResolver.resolve("", emptyMap())
        assertTrue(result is ResolveResult.Success)
        assertEquals("", result.resolved)
    }

    @Test
    fun handlesAdjacentVariables() {
        val vars = mapOf("a" to "hello", "b" to "world")
        val result = VariableResolver.resolve("{{a}}{{b}}", vars)
        assertTrue(result is ResolveResult.Success)
        assertEquals("helloworld", result.resolved)
    }

    @Test
    fun ignoresDisabledVariablesWhenNotInMap() {
        val vars = mapOf("base_url" to "http://localhost")
        val result = VariableResolver.resolve("{{base_url}}/{{disabled_var}}", vars)
        assertTrue(result is ResolveResult.Unresolved)
        assertEquals(listOf("disabled_var"), result.variables)
    }

    @Test
    fun handlesMalformedBraces() {
        val vars = mapOf("x" to "val")
        val result = VariableResolver.resolve("{x} {{x}} {{{x}}}", vars)
        assertTrue(result is ResolveResult.Success)
        assertEquals("{x} val {val}", result.resolved)
    }

    @Test
    fun resolveAllOrNone() {
        val vars = mapOf("a" to "1")
        val result = VariableResolver.resolve("{{a}}-{{b}}", vars)
        assertTrue(result is ResolveResult.Unresolved)
        assertEquals(listOf("b"), result.variables)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :shared:jvmTest --tests "dev.herald.core.variable.VariableResolverTest"
```
Expected: FAIL (class not found).

- [ ] **Step 3: Implement `VariableResolver`**

```kotlin
package dev.herald.core.variable

sealed class ResolveResult {
    data class Success(val resolved: String) : ResolveResult()
    data class Unresolved(val variables: List<String>) : ResolveResult()
}

object VariableResolver {
    private val VARIABLE_PATTERN = Regex("""\{\{(\w+)}}""")

    fun resolve(template: String, variables: Map<String, String>): ResolveResult {
        val matches = VARIABLE_PATTERN.findAll(template).toList()
        if (matches.isEmpty()) return ResolveResult.Success(template)

        val unresolved = matches
            .map { it.groupValues[1] }
            .distinct()
            .filter { it !in variables }

        if (unresolved.isNotEmpty()) return ResolveResult.Unresolved(unresolved)

        val resolved = VARIABLE_PATTERN.replace(template) { match ->
            variables[match.groupValues[1]] ?: match.value
        }
        return ResolveResult.Success(resolved)
    }

    fun findVariableNames(template: String): List<String> =
        VARIABLE_PATTERN.findAll(template).map { it.groupValues[1] }.distinct().toList()
}
```

- [ ] **Step 4: Run tests**

Run:
```bash
./gradlew :shared:jvmTest --tests "dev.herald.core.variable.VariableResolverTest"
```
Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/dev/herald/core/variable/ shared/src/commonTest/kotlin/dev/herald/core/variable/
git commit -m "feat: add variable resolver with {{placeholder}} substitution"
```

---

### Task 5: HTTP Engine (Track A)

**Files:**
- Create: `shared/src/commonMain/kotlin/dev/herald/core/http/HttpEngine.kt`
- Create: `shared/src/commonTest/kotlin/dev/herald/core/http/HttpEngineTest.kt`

- [ ] **Step 1: Write `HttpEngineTest`**

Uses Ktor's `MockEngine` to test without network calls.

```kotlin
package dev.herald.core.http

import dev.herald.core.model.HttpMethod
import dev.herald.core.model.KeyValueRow
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HttpEngineTest {
    @Test
    fun sendsGetRequest() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("http://localhost/users", request.url.toString())
            assertEquals(HttpMethod.Get, request.method)
            respond(
                content = """{"users":[]}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val engine = HttpEngine(mockEngine)
        val result = engine.execute(
            method = dev.herald.core.model.HttpMethod.GET,
            url = "http://localhost/users",
            headers = emptyList(),
            body = null,
        )
        assertEquals(200, result.status)
        assertEquals("""{"users":[]}""", result.body)
        assertTrue(result.durationMs >= 0)
    }

    @Test
    fun sendsPostWithBody() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            val body = request.body.toByteArray().decodeToString()
            assertEquals("""{"name":"test"}""", body)
            respond(
                content = """{"id":1}""",
                status = HttpStatusCode.Created,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val engine = HttpEngine(mockEngine)
        val result = engine.execute(
            method = dev.herald.core.model.HttpMethod.POST,
            url = "http://localhost/users",
            headers = listOf(KeyValueRow("Content-Type", "application/json")),
            body = """{"name":"test"}""",
        )
        assertEquals(201, result.status)
    }

    @Test
    fun includesCustomHeaders() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("Bearer token123", request.headers["Authorization"])
            assertEquals("custom-value", request.headers["X-Custom"])
            respond(content = "", status = HttpStatusCode.OK)
        }
        val engine = HttpEngine(mockEngine)
        engine.execute(
            method = dev.herald.core.model.HttpMethod.GET,
            url = "http://localhost/api",
            headers = listOf(
                KeyValueRow("Authorization", "Bearer token123"),
                KeyValueRow("X-Custom", "custom-value"),
            ),
            body = null,
        )
    }

    @Test
    fun capturesResponseHeaders() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = "ok",
                status = HttpStatusCode.OK,
                headers = headersOf("X-Request-Id" to listOf("abc-123")),
            )
        }
        val engine = HttpEngine(mockEngine)
        val result = engine.execute(
            method = dev.herald.core.model.HttpMethod.GET,
            url = "http://localhost/api",
            headers = emptyList(),
            body = null,
        )
        assertTrue(result.headers.any { it.name == "X-Request-Id" && it.value == "abc-123" })
    }

    @Test
    fun handlesNetworkError() = runTest {
        val mockEngine = MockEngine {
            throw java.net.ConnectException("Connection refused")
        }
        val engine = HttpEngine(mockEngine)
        val result = engine.execute(
            method = dev.herald.core.model.HttpMethod.GET,
            url = "http://localhost/api",
            headers = emptyList(),
            body = null,
        )
        assertTrue(result is HttpResponse.Failure)
        assertTrue(result.message.contains("Connection refused"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :shared:jvmTest --tests "dev.herald.core.http.HttpEngineTest"
```
Expected: FAIL (class not found).

- [ ] **Step 3: Implement `HttpEngine`**

```kotlin
package dev.herald.core.http

import dev.herald.core.model.HttpMethod
import dev.herald.core.model.KeyValueRow
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlin.time.measureTimedValue

sealed class HttpResponse {
    data class Success(
        val status: Int,
        val headers: List<KeyValueRow>,
        val body: String?,
        val durationMs: Long,
    ) : HttpResponse()

    data class Failure(
        val message: String,
        val cause: Throwable? = null,
    ) : HttpResponse()
}

class HttpEngine(engine: HttpClientEngine? = null) {
    private val client: HttpClient = if (engine != null) {
        HttpClient(engine) { expectSuccess = false }
    } else {
        HttpClient { expectSuccess = false }
    }

    suspend fun execute(
        method: HttpMethod,
        url: String,
        headers: List<KeyValueRow>,
        body: String?,
    ): HttpResponse {
        return try {
            val (response, duration) = measureTimedValue {
                client.request(url) {
                    this.method = method.toKtorMethod()
                    headers.forEach { header(it.name, it.value) }
                    if (body != null) {
                        setBody(body)
                    }
                }
            }
            val responseHeaders = response.headers.entries().flatMap { (name, values) ->
                values.map { KeyValueRow(name, it) }
            }
            HttpResponse.Success(
                status = response.status.value,
                headers = responseHeaders,
                body = response.bodyAsText(),
                durationMs = duration.inWholeMilliseconds,
            )
        } catch (e: Exception) {
            HttpResponse.Failure(
                message = e.message ?: "Unknown error",
                cause = e,
            )
        }
    }

    fun close() {
        client.close()
    }
}

private fun HttpMethod.toKtorMethod(): io.ktor.http.HttpMethod = when (this) {
    HttpMethod.GET -> io.ktor.http.HttpMethod.Get
    HttpMethod.POST -> io.ktor.http.HttpMethod.Post
    HttpMethod.PUT -> io.ktor.http.HttpMethod.Put
    HttpMethod.DELETE -> io.ktor.http.HttpMethod.Delete
    HttpMethod.PATCH -> io.ktor.http.HttpMethod.Patch
    HttpMethod.HEAD -> io.ktor.http.HttpMethod.Head
    HttpMethod.OPTIONS -> io.ktor.http.HttpMethod("OPTIONS")
}
```

- [ ] **Step 4: Run tests**

Run:
```bash
./gradlew :shared:jvmTest --tests "dev.herald.core.http.HttpEngineTest"
```
Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/dev/herald/core/http/ shared/src/commonTest/kotlin/dev/herald/core/http/
git commit -m "feat: add HTTP engine wrapping Ktor Client with mock tests"
```

---

### Task 6: Request Executor (Track A)

**Files:**
- Create: `shared/src/commonMain/kotlin/dev/herald/core/executor/RequestExecutor.kt`
- Create: `shared/src/commonTest/kotlin/dev/herald/core/executor/RequestExecutorTest.kt`

- [ ] **Step 1: Write `RequestExecutorTest`**

```kotlin
package dev.herald.core.executor

import dev.herald.core.http.HttpEngine
import dev.herald.core.http.HttpResponse
import dev.herald.core.model.*
import dev.herald.storage.DatabaseProvider
import dev.herald.storage.DriverFactory
import dev.herald.storage.dao.CollectionDao
import dev.herald.storage.dao.HistoryDao
import dev.herald.storage.dao.RequestDao
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RequestExecutorTest {
    private fun createExecutor(
        mockEngine: MockEngine,
    ): Triple<RequestExecutor, CollectionDao, RequestDao> {
        val provider = DatabaseProvider(DriverFactory())
        val collDao = CollectionDao(provider.database)
        val reqDao = RequestDao(provider.database)
        val historyDao = HistoryDao(provider.database)
        val httpEngine = HttpEngine(mockEngine)
        val executor = RequestExecutor(httpEngine, historyDao)
        return Triple(executor, collDao, reqDao)
    }

    @Test
    fun successfulExecution() = runTest {
        val mockEngine = MockEngine {
            respond(
                content = """{"ok":true}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val (executor, collDao, reqDao) = createExecutor(mockEngine)
        val now = Clock.System.now().toEpochMilliseconds()
        val collId = collDao.insert("API", now)
        val reqId = reqDao.insert(
            collectionId = collId, folderId = null, name = "Test",
            method = "GET", url = "{{base}}/users",
            headers = "[]", queryParams = "[]",
            bodyType = null, bodyContent = null, seq = 0, now = now,
        )
        val request = reqDao.getById(reqId)!!
        val variables = mapOf("base" to "http://localhost")
        val result = executor.execute(request, variables)
        assertTrue(result is RequestResult.Success)
        val entry = (result as RequestResult.Success).historyEntry
        assertEquals(200, entry.responseStatus)
        assertEquals("http://localhost/users", entry.resolvedUrl)
    }

    @Test
    fun blocksOnUnresolvedVariables() = runTest {
        val mockEngine = MockEngine { respond(content = "", status = HttpStatusCode.OK) }
        val (executor, collDao, reqDao) = createExecutor(mockEngine)
        val now = Clock.System.now().toEpochMilliseconds()
        val collId = collDao.insert("API", now)
        val reqId = reqDao.insert(
            collectionId = collId, folderId = null, name = "Test",
            method = "GET", url = "{{base}}/{{version}}/users",
            headers = "[]", queryParams = "[]",
            bodyType = null, bodyContent = null, seq = 0, now = now,
        )
        val request = reqDao.getById(reqId)!!
        val variables = mapOf("base" to "http://localhost")
        val result = executor.execute(request, variables)
        assertTrue(result is RequestResult.UnresolvedVariables)
        assertEquals(listOf("version"), (result as RequestResult.UnresolvedVariables).variables)
    }

    @Test
    fun handlesNetworkError() = runTest {
        val mockEngine = MockEngine {
            throw java.net.ConnectException("Connection refused")
        }
        val (executor, collDao, reqDao) = createExecutor(mockEngine)
        val now = Clock.System.now().toEpochMilliseconds()
        val collId = collDao.insert("API", now)
        val reqId = reqDao.insert(
            collectionId = collId, folderId = null, name = "Test",
            method = "GET", url = "http://localhost/test",
            headers = "[]", queryParams = "[]",
            bodyType = null, bodyContent = null, seq = 0, now = now,
        )
        val request = reqDao.getById(reqId)!!
        val result = executor.execute(request, emptyMap())
        assertTrue(result is RequestResult.NetworkError)
    }

    @Test
    fun persistsHistoryOnSuccess() = runTest {
        val mockEngine = MockEngine {
            respond(content = "ok", status = HttpStatusCode.OK)
        }
        val provider = DatabaseProvider(DriverFactory())
        val collDao = CollectionDao(provider.database)
        val reqDao = RequestDao(provider.database)
        val historyDao = HistoryDao(provider.database)
        val httpEngine = HttpEngine(mockEngine)
        val executor = RequestExecutor(httpEngine, historyDao)

        val now = Clock.System.now().toEpochMilliseconds()
        val collId = collDao.insert("API", now)
        val reqId = reqDao.insert(
            collectionId = collId, folderId = null, name = "Test",
            method = "GET", url = "http://localhost/api",
            headers = "[]", queryParams = "[]",
            bodyType = null, bodyContent = null, seq = 0, now = now,
        )
        val request = reqDao.getById(reqId)!!
        executor.execute(request, emptyMap())
        assertEquals(1, historyDao.count())
    }

    @Test
    fun resolvesVariablesInHeaders() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals("Bearer my-token", request.headers["Authorization"])
            respond(content = "", status = HttpStatusCode.OK)
        }
        val (executor, collDao, reqDao) = createExecutor(mockEngine)
        val now = Clock.System.now().toEpochMilliseconds()
        val collId = collDao.insert("API", now)
        val headers = """[{"name":"Authorization","value":"Bearer {{token}}","enabled":true}]"""
        val reqId = reqDao.insert(
            collectionId = collId, folderId = null, name = "Test",
            method = "GET", url = "http://localhost",
            headers = headers, queryParams = "[]",
            bodyType = null, bodyContent = null, seq = 0, now = now,
        )
        val request = reqDao.getById(reqId)!!
        val variables = mapOf("token" to "my-token")
        val result = executor.execute(request, variables)
        assertTrue(result is RequestResult.Success)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
./gradlew :shared:jvmTest --tests "dev.herald.core.executor.RequestExecutorTest"
```
Expected: FAIL (class not found).

- [ ] **Step 3: Implement `RequestExecutor`**

```kotlin
package dev.herald.core.executor

import dev.herald.core.http.HttpEngine
import dev.herald.core.http.HttpResponse
import dev.herald.core.model.*
import dev.herald.core.variable.ResolveResult
import dev.herald.core.variable.VariableResolver
import dev.herald.storage.dao.HistoryDao
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json

class RequestExecutor(
    private val httpEngine: HttpEngine,
    private val historyDao: HistoryDao,
) {
    suspend fun execute(
        request: Request,
        variables: Map<String, String>,
    ): RequestResult {
        // Resolve URL
        val urlResult = VariableResolver.resolve(request.url, variables)
        if (urlResult is ResolveResult.Unresolved) {
            return RequestResult.UnresolvedVariables(urlResult.variables)
        }
        val resolvedUrl = (urlResult as ResolveResult.Success).resolved

        // Resolve query params and append to URL
        val resolvedParams = mutableListOf<KeyValueRow>()
        val unresolvedVars = mutableListOf<String>()
        for (param in request.queryParams.filter { it.enabled }) {
            val nameResult = VariableResolver.resolve(param.name, variables)
            val valueResult = VariableResolver.resolve(param.value, variables)
            if (nameResult is ResolveResult.Unresolved) unresolvedVars.addAll(nameResult.variables)
            if (valueResult is ResolveResult.Unresolved) unresolvedVars.addAll(valueResult.variables)
            if (nameResult is ResolveResult.Success && valueResult is ResolveResult.Success) {
                resolvedParams.add(KeyValueRow(nameResult.resolved, valueResult.resolved))
            }
        }
        if (unresolvedVars.isNotEmpty()) {
            return RequestResult.UnresolvedVariables(unresolvedVars.distinct())
        }

        val fullUrl = buildUrl(resolvedUrl, resolvedParams)

        // Resolve headers
        val resolvedHeaders = mutableListOf<KeyValueRow>()
        for (header in request.headers.filter { it.enabled }) {
            val nameResult = VariableResolver.resolve(header.name, variables)
            val valueResult = VariableResolver.resolve(header.value, variables)
            if (nameResult is ResolveResult.Unresolved) unresolvedVars.addAll(nameResult.variables)
            if (valueResult is ResolveResult.Unresolved) unresolvedVars.addAll(valueResult.variables)
            if (nameResult is ResolveResult.Success && valueResult is ResolveResult.Success) {
                resolvedHeaders.add(KeyValueRow(nameResult.resolved, valueResult.resolved))
            }
        }
        if (unresolvedVars.isNotEmpty()) {
            return RequestResult.UnresolvedVariables(unresolvedVars.distinct())
        }

        // Resolve body
        val resolvedBody = if (request.bodyContent != null) {
            val bodyResult = VariableResolver.resolve(request.bodyContent, variables)
            if (bodyResult is ResolveResult.Unresolved) {
                return RequestResult.UnresolvedVariables(bodyResult.variables)
            }
            (bodyResult as ResolveResult.Success).resolved
        } else null

        // Execute
        val response = httpEngine.execute(
            method = request.method,
            url = fullUrl,
            headers = resolvedHeaders,
            body = resolvedBody,
        )

        return when (response) {
            is HttpResponse.Failure -> RequestResult.NetworkError(
                message = response.message,
                cause = response.cause,
            )
            is HttpResponse.Success -> {
                val now = Clock.System.now().toEpochMilliseconds()
                val headersJson = Json.encodeToString(KeyValueRow.serializer().list(), resolvedHeaders)
                val responseHeadersJson = Json.encodeToString(KeyValueRow.serializer().list(), response.headers)

                val historyId = historyDao.insert(
                    requestId = request.id,
                    method = request.method.name,
                    resolvedUrl = fullUrl,
                    requestHeaders = headersJson,
                    requestBody = resolvedBody,
                    responseStatus = response.status,
                    responseHeaders = responseHeadersJson,
                    responseBody = response.body,
                    durationMs = response.durationMs,
                    createdAt = now,
                )
                historyDao.prune(100)

                val entry = HistoryEntry(
                    id = historyId,
                    requestId = request.id,
                    method = request.method,
                    resolvedUrl = fullUrl,
                    requestHeaders = resolvedHeaders,
                    requestBody = resolvedBody,
                    responseStatus = response.status,
                    responseHeaders = response.headers,
                    responseBody = response.body,
                    durationMs = response.durationMs,
                    createdAt = now,
                )
                RequestResult.Success(entry)
            }
        }
    }

    private fun buildUrl(baseUrl: String, params: List<KeyValueRow>): String {
        if (params.isEmpty()) return baseUrl
        val separator = if ("?" in baseUrl) "&" else "?"
        val queryString = params.joinToString("&") { "${it.name}=${it.value}" }
        return "$baseUrl$separator$queryString"
    }
}

private fun <T> kotlinx.serialization.KSerializer<T>.list() =
    kotlinx.serialization.builtins.ListSerializer(this)
```

- [ ] **Step 4: Run tests**

Run:
```bash
./gradlew :shared:jvmTest --tests "dev.herald.core.executor.RequestExecutorTest"
```
Expected: All tests PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/dev/herald/core/executor/ shared/src/commonTest/kotlin/dev/herald/core/executor/
git commit -m "feat: add request executor (resolve → send → persist flow)"
```

---

### Task 7: Run All Shared Module Tests (Track A checkpoint)

- [ ] **Step 1: Run all shared tests**

Run:
```bash
./gradlew :shared:jvmTest
```
Expected: All tests PASS. This validates the entire shared module.

- [ ] **Step 2: Commit any fixes if needed**

If tests required adjustments (e.g. import fixes, type mismatches from SQLDelight generated code), commit them:
```bash
git add shared/
git commit -m "fix: resolve shared module test issues"
```

---

## Track B: Desktop Module

### Task 8: Theme, Colors, Typography (Track B)

**Files:**
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/theme/Colors.kt`
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/theme/Typography.kt`
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/theme/Theme.kt`
- Download: font files to `desktop/src/jvmMain/resources/fonts/`

- [ ] **Step 1: Download fonts**

Download JetBrains Mono (Regular, Bold) and DM Sans (Regular, Medium, Bold) TTF files. Place them in `desktop/src/jvmMain/resources/fonts/`. Include the OFL license file as `OFL.txt`.

- [ ] **Step 2: Create `Colors.kt`**

```kotlin
package dev.herald.desktop.theme

import androidx.compose.ui.graphics.Color

object HeraldColors {
    // Backgrounds
    val background = Color(0xFF0A0A0B)
    val surface = Color(0xFF141416)
    val surfaceVariant = Color(0xFF1E1E22)
    val surfaceHover = Color(0xFF252529)

    // Chrome
    val border = Color(0xFF2A2A2E)
    val borderSubtle = Color(0xFF1E1E22)

    // Text
    val textPrimary = Color(0xFFE0E0E0)
    val textSecondary = Color(0xFF9E9E9E)
    val textMuted = Color(0xFF6B6B6B)

    // Method colors
    val methodGet = Color(0xFF4CAF50)
    val methodPost = Color(0xFF42A5F5)
    val methodPut = Color(0xFFFFA726)
    val methodDelete = Color(0xFFEF5350)
    val methodPatch = Color(0xFFAB47BC)
    val methodHead = Color(0xFF78909C)
    val methodOptions = Color(0xFF78909C)

    // Status code colors
    val status2xx = Color(0xFF4CAF50)
    val status3xx = Color(0xFFFFA726)
    val status4xx = Color(0xFFEF5350)
    val status5xx = Color(0xFFEF5350)

    // Semantic
    val error = Color(0xFFEF5350)
    val warning = Color(0xFFFFA726)
    val success = Color(0xFF4CAF50)
    val info = Color(0xFF42A5F5)

    // Accent
    val accent = Color(0xFF42A5F5)
    val accentMuted = Color(0xFF1E3A5F)

    // Focus
    val focusRing = Color(0xFF42A5F5)
}
```

- [ ] **Step 3: Create `Typography.kt`**

```kotlin
package dev.herald.desktop.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp

val JetBrainsMono = FontFamily(
    Font(resource = "fonts/JetBrainsMono-Regular.ttf", weight = FontWeight.Normal),
    Font(resource = "fonts/JetBrainsMono-Bold.ttf", weight = FontWeight.Bold),
)

val DMSans = FontFamily(
    Font(resource = "fonts/DMSans-Regular.ttf", weight = FontWeight.Normal),
    Font(resource = "fonts/DMSans-Medium.ttf", weight = FontWeight.Medium),
    Font(resource = "fonts/DMSans-Bold.ttf", weight = FontWeight.Bold),
)

val HeraldTypography = Typography(
    // Labels and chrome use DM Sans
    titleLarge = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = DMSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
    // Headlines use mono for data-heavy displays
    headlineLarge = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
)
```

- [ ] **Step 4: Create `Theme.kt`**

```kotlin
package dev.herald.desktop.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val HeraldColorScheme = darkColorScheme(
    background = HeraldColors.background,
    surface = HeraldColors.surface,
    surfaceVariant = HeraldColors.surfaceVariant,
    onBackground = HeraldColors.textPrimary,
    onSurface = HeraldColors.textPrimary,
    onSurfaceVariant = HeraldColors.textSecondary,
    primary = HeraldColors.accent,
    onPrimary = HeraldColors.textPrimary,
    outline = HeraldColors.border,
    outlineVariant = HeraldColors.borderSubtle,
    error = HeraldColors.error,
    onError = HeraldColors.textPrimary,
)

@Composable
fun HeraldTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HeraldColorScheme,
        typography = HeraldTypography,
        content = content,
    )
}
```

- [ ] **Step 5: Update `Main.kt` to use the theme**

Replace `desktop/src/jvmMain/kotlin/dev/herald/desktop/Main.kt`:

```kotlin
package dev.herald.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.herald.desktop.theme.HeraldTheme

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Herald",
        state = rememberWindowState(size = DpSize(1200.dp, 800.dp))
    ) {
        HeraldTheme {
            Surface(modifier = Modifier.fillMaxSize()) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Herald", style = MaterialTheme.typography.headlineLarge)
                }
            }
        }
    }
}
```

- [ ] **Step 6: Verify the app launches with dark theme**

Run:
```bash
./gradlew :desktop:run
```
Expected: Window opens with dark near-black background and "Herald" text in JetBrains Mono.

- [ ] **Step 7: Commit**

```bash
git add desktop/src/jvmMain/
git commit -m "feat: add Herald dark theme with custom colors and typography"
```

---

### Task 9: App State Management (Track B)

**Files:**
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/state/TabState.kt`
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/state/AppState.kt`

- [ ] **Step 1: Create `TabState`**

```kotlin
package dev.herald.desktop.state

import dev.herald.core.model.HttpMethod
import dev.herald.core.model.KeyValueRow

data class TabState(
    val id: String,
    val requestId: Long? = null,
    val name: String = "New Request",
    val method: HttpMethod = HttpMethod.GET,
    val url: String = "",
    val headers: List<KeyValueRow> = listOf(KeyValueRow("", "", true)),
    val queryParams: List<KeyValueRow> = listOf(KeyValueRow("", "", true)),
    val bodyType: String? = null,
    val bodyContent: String? = null,
    val isDirty: Boolean = false,
)
```

- [ ] **Step 2: Create `AppState`**

```kotlin
package dev.herald.desktop.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.herald.core.model.HttpMethod
import java.util.UUID

class AppState {
    val tabs = mutableStateListOf<TabState>()
    var activeTabIndex by mutableStateOf(-1)
        private set
    var activeEnvironmentId by mutableStateOf<Long?>(null)
    var sidebarCollapsed by mutableStateOf(false)

    val activeTab: TabState?
        get() = tabs.getOrNull(activeTabIndex)

    fun openNewTab(): String {
        val tab = TabState(id = UUID.randomUUID().toString())
        tabs.add(tab)
        activeTabIndex = tabs.size - 1
        return tab.id
    }

    fun openRequestTab(requestId: Long, name: String, method: HttpMethod, url: String,
                       headers: List<dev.herald.core.model.KeyValueRow>,
                       queryParams: List<dev.herald.core.model.KeyValueRow>,
                       bodyType: String?, bodyContent: String?) {
        val existingIndex = tabs.indexOfFirst { it.requestId == requestId }
        if (existingIndex >= 0) {
            activeTabIndex = existingIndex
            return
        }
        val tab = TabState(
            id = UUID.randomUUID().toString(),
            requestId = requestId,
            name = name,
            method = method,
            url = url,
            headers = headers.ifEmpty { listOf(dev.herald.core.model.KeyValueRow("", "", true)) },
            queryParams = queryParams.ifEmpty { listOf(dev.herald.core.model.KeyValueRow("", "", true)) },
            bodyType = bodyType,
            bodyContent = bodyContent,
        )
        tabs.add(tab)
        activeTabIndex = tabs.size - 1
    }

    fun switchToTab(index: Int) {
        if (index in tabs.indices) {
            activeTabIndex = index
        }
    }

    fun closeTab(index: Int): Boolean {
        if (index !in tabs.indices) return false
        if (tabs[index].isDirty) return false // caller should show confirmation first
        tabs.removeAt(index)
        activeTabIndex = when {
            tabs.isEmpty() -> -1
            index <= activeTabIndex && activeTabIndex > 0 -> activeTabIndex - 1
            activeTabIndex >= tabs.size -> tabs.size - 1
            else -> activeTabIndex
        }
        return true
    }

    fun forceCloseTab(index: Int) {
        if (index !in tabs.indices) return
        tabs.removeAt(index)
        activeTabIndex = when {
            tabs.isEmpty() -> -1
            index <= activeTabIndex && activeTabIndex > 0 -> activeTabIndex - 1
            activeTabIndex >= tabs.size -> tabs.size - 1
            else -> activeTabIndex
        }
    }

    fun updateTab(index: Int, transform: (TabState) -> TabState) {
        if (index in tabs.indices) {
            tabs[index] = transform(tabs[index])
        }
    }

    fun markTabDirty(index: Int) {
        updateTab(index) { it.copy(isDirty = true) }
    }

    fun markTabClean(index: Int) {
        updateTab(index) { it.copy(isDirty = false) }
    }
}
```

- [ ] **Step 3: Verify compilation**

Run:
```bash
./gradlew :desktop:compileKotlinDesktop
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add desktop/src/jvmMain/kotlin/dev/herald/desktop/state/
git commit -m "feat: add app and tab state management"
```

---

### Task 10: Sidebar Shell (Track B)

**Files:**
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/ui/sidebar/Sidebar.kt`
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/ui/sidebar/CollectionTree.kt`
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/ui/sidebar/HistoryList.kt`
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/ui/sidebar/EnvironmentSelector.kt`
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/App.kt`

This task creates the sidebar layout with hardcoded stub data. Integration with real data happens in Task 13.

- [ ] **Step 1: Create `CollectionTree`**

```kotlin
package dev.herald.desktop.ui.sidebar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.herald.core.model.HttpMethod
import dev.herald.desktop.theme.HeraldColors

data class TreeRequest(
    val id: Long,
    val name: String,
    val method: HttpMethod,
)

data class TreeFolder(
    val id: Long,
    val name: String,
    val requests: List<TreeRequest>,
)

data class TreeCollection(
    val id: Long,
    val name: String,
    val folders: List<TreeFolder>,
    val rootRequests: List<TreeRequest>,
)

@Composable
fun CollectionTree(
    collections: List<TreeCollection>,
    onRequestClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        if (collections.isEmpty()) {
            Text(
                "No collections",
                style = MaterialTheme.typography.bodySmall,
                color = HeraldColors.textMuted,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        collections.forEach { collection ->
            CollectionNode(collection, onRequestClick)
        }
    }
}

@Composable
private fun CollectionNode(
    collection: TreeCollection,
    onRequestClick: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics {
                role = Role.Button
                contentDescription = "${collection.name} collection, ${if (expanded) "expanded" else "collapsed"}"
            },
    ) {
        Icon(
            if (expanded) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = HeraldColors.textSecondary,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            collection.name,
            style = MaterialTheme.typography.labelLarge,
            color = HeraldColors.textPrimary,
        )
    }

    if (expanded) {
        collection.folders.forEach { folder ->
            FolderNode(folder, onRequestClick)
        }
        collection.rootRequests.forEach { request ->
            RequestNode(request, onRequestClick, indent = 1)
        }
    }
}

@Composable
private fun FolderNode(
    folder: TreeFolder,
    onRequestClick: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(true) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
            .padding(start = 24.dp, end = 8.dp, top = 2.dp, bottom = 2.dp)
            .semantics {
                role = Role.Button
                contentDescription = "${folder.name} folder, ${if (expanded) "expanded" else "collapsed"}"
            },
    ) {
        Icon(
            Icons.Default.Folder,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = HeraldColors.textMuted,
        )
        Spacer(Modifier.width(4.dp))
        Text(
            folder.name,
            style = MaterialTheme.typography.bodyMedium,
            color = HeraldColors.textSecondary,
        )
    }

    if (expanded) {
        folder.requests.forEach { request ->
            RequestNode(request, onRequestClick, indent = 2)
        }
    }
}

@Composable
private fun RequestNode(
    request: TreeRequest,
    onClick: (Long) -> Unit,
    indent: Int,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(request.id) }
            .padding(start = (12 + indent * 16).dp, end = 8.dp, top = 2.dp, bottom = 2.dp)
            .semantics {
                role = Role.Button
                contentDescription = "${request.method.name} ${request.name}"
            },
    ) {
        Text(
            request.method.name,
            style = MaterialTheme.typography.labelSmall,
            color = request.method.color(),
            modifier = Modifier.width(48.dp),
        )
        Text(
            request.name,
            style = MaterialTheme.typography.bodySmall,
            color = HeraldColors.textPrimary,
            maxLines = 1,
        )
    }
}

private fun HttpMethod.color() = when (this) {
    HttpMethod.GET -> HeraldColors.methodGet
    HttpMethod.POST -> HeraldColors.methodPost
    HttpMethod.PUT -> HeraldColors.methodPut
    HttpMethod.DELETE -> HeraldColors.methodDelete
    HttpMethod.PATCH -> HeraldColors.methodPatch
    HttpMethod.HEAD -> HeraldColors.methodHead
    HttpMethod.OPTIONS -> HeraldColors.methodOptions
}
```

- [ ] **Step 2: Create `HistoryList`**

```kotlin
package dev.herald.desktop.ui.sidebar

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.herald.core.model.HttpMethod
import dev.herald.desktop.theme.HeraldColors

data class HistoryListItem(
    val id: Long,
    val method: HttpMethod,
    val url: String,
    val status: Int,
    val timestamp: String,
)

@Composable
fun HistoryList(
    entries: List<HistoryListItem>,
    onEntryClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(vertical = 4.dp)) {
        Text(
            "History",
            style = MaterialTheme.typography.labelLarge,
            color = HeraldColors.textSecondary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
        )
        if (entries.isEmpty()) {
            Text(
                "No history yet",
                style = MaterialTheme.typography.bodySmall,
                color = HeraldColors.textMuted,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
        entries.forEach { entry ->
            HistoryRow(entry, onEntryClick)
        }
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryListItem,
    onClick: (Long) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(entry.id) }
            .padding(horizontal = 12.dp, vertical = 3.dp)
            .semantics {
                role = Role.Button
                contentDescription = "${entry.method.name} ${entry.url}, status ${entry.status}"
            },
    ) {
        Text(
            entry.method.name,
            style = MaterialTheme.typography.labelSmall,
            color = entry.method.color(),
            modifier = Modifier.width(48.dp),
        )
        Text(
            entry.url,
            style = MaterialTheme.typography.bodySmall,
            color = HeraldColors.textPrimary,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            entry.status.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = statusColor(entry.status),
        )
    }
}

private fun HttpMethod.color() = when (this) {
    HttpMethod.GET -> HeraldColors.methodGet
    HttpMethod.POST -> HeraldColors.methodPost
    HttpMethod.PUT -> HeraldColors.methodPut
    HttpMethod.DELETE -> HeraldColors.methodDelete
    HttpMethod.PATCH -> HeraldColors.methodPatch
    HttpMethod.HEAD -> HeraldColors.methodHead
    HttpMethod.OPTIONS -> HeraldColors.methodOptions
}

private fun statusColor(status: Int) = when (status) {
    in 200..299 -> HeraldColors.status2xx
    in 300..399 -> HeraldColors.status3xx
    in 400..499 -> HeraldColors.status4xx
    else -> HeraldColors.status5xx
}
```

- [ ] **Step 3: Create `EnvironmentSelector`**

```kotlin
package dev.herald.desktop.ui.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.herald.desktop.theme.HeraldColors

data class EnvironmentItem(
    val id: Long,
    val name: String,
)

@Composable
fun EnvironmentSelector(
    environments: List<EnvironmentItem>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = environments.find { it.id == selectedId }?.name ?: "No Environment"

    Box(modifier = modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, HeraldColors.border, RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .semantics {
                    contentDescription = "Environment selector, currently: $selectedName"
                },
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (selectedId != null) HeraldColors.success else HeraldColors.textMuted,
                        RoundedCornerShape(50),
                    ),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                selectedName,
                style = MaterialTheme.typography.bodySmall,
                color = HeraldColors.textPrimary,
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("No Environment", style = MaterialTheme.typography.bodySmall) },
                onClick = { onSelect(null); expanded = false },
            )
            environments.forEach { env ->
                DropdownMenuItem(
                    text = { Text(env.name, style = MaterialTheme.typography.bodySmall) },
                    onClick = { onSelect(env.id); expanded = false },
                )
            }
        }
    }
}
```

- [ ] **Step 4: Create `Sidebar`**

```kotlin
package dev.herald.desktop.ui.sidebar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.herald.desktop.theme.HeraldColors

@Composable
fun Sidebar(
    collections: List<TreeCollection>,
    historyEntries: List<HistoryListItem>,
    environments: List<EnvironmentItem>,
    selectedEnvironmentId: Long?,
    onRequestClick: (Long) -> Unit,
    onHistoryClick: (Long) -> Unit,
    onEnvironmentSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(260.dp)
            .fillMaxHeight()
            .background(HeraldColors.surface),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            CollectionTree(
                collections = collections,
                onRequestClick = onRequestClick,
            )
            Divider(color = HeraldColors.border, thickness = 1.dp)
            HistoryList(
                entries = historyEntries,
                onEntryClick = onHistoryClick,
            )
        }
        Divider(color = HeraldColors.border, thickness = 1.dp)
        EnvironmentSelector(
            environments = environments,
            selectedId = selectedEnvironmentId,
            onSelect = onEnvironmentSelect,
        )
    }
}
```

- [ ] **Step 5: Create `App.kt` with stub data**

```kotlin
package dev.herald.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.herald.core.model.HttpMethod
import dev.herald.desktop.state.AppState
import dev.herald.desktop.theme.HeraldColors
import dev.herald.desktop.ui.sidebar.*

@Composable
fun App() {
    val appState = remember { AppState() }

    val stubCollections = remember {
        listOf(
            TreeCollection(
                id = 1, name = "My API",
                folders = listOf(
                    TreeFolder(id = 1, name = "Users", requests = listOf(
                        TreeRequest(1, "Get Users", HttpMethod.GET),
                        TreeRequest(2, "Create User", HttpMethod.POST),
                    )),
                    TreeFolder(id = 2, name = "Auth", requests = listOf(
                        TreeRequest(3, "Login", HttpMethod.POST),
                    )),
                ),
                rootRequests = listOf(
                    TreeRequest(4, "Health Check", HttpMethod.GET),
                ),
            ),
        )
    }

    val stubHistory = remember { emptyList<HistoryListItem>() }

    val stubEnvironments = remember {
        listOf(
            EnvironmentItem(1, "dev"),
            EnvironmentItem(2, "staging"),
            EnvironmentItem(3, "prod"),
        )
    }

    Row(modifier = Modifier.fillMaxSize().background(HeraldColors.background)) {
        Sidebar(
            collections = stubCollections,
            historyEntries = stubHistory,
            environments = stubEnvironments,
            selectedEnvironmentId = appState.activeEnvironmentId,
            onRequestClick = { /* wired in Task 13 */ },
            onHistoryClick = { /* wired in Task 13 */ },
            onEnvironmentSelect = { appState.activeEnvironmentId = it },
        )
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Select a request to begin",
                style = MaterialTheme.typography.bodyLarge,
                color = HeraldColors.textMuted,
            )
        }
    }
}
```

- [ ] **Step 6: Update `Main.kt` to use `App`**

```kotlin
package dev.herald.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.herald.desktop.theme.HeraldTheme

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Herald",
        state = rememberWindowState(size = DpSize(1200.dp, 800.dp))
    ) {
        HeraldTheme {
            App()
        }
    }
}
```

- [ ] **Step 7: Verify the app launches with sidebar**

Run:
```bash
./gradlew :desktop:run
```
Expected: Window with dark sidebar on the left showing "My API" collection with folders, environment selector at bottom, and "Select a request to begin" in the main area.

- [ ] **Step 8: Commit**

```bash
git add desktop/src/jvmMain/kotlin/dev/herald/desktop/
git commit -m "feat: add sidebar with collection tree, history list, and environment selector"
```

---

### Task 11: Tab Bar, Request Editor, Response Viewer (Track B)

**Files:**
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/ui/tabs/TabBar.kt`
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/ui/request/MethodDropdown.kt`
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/ui/request/UrlBar.kt`
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/ui/request/KeyValueEditor.kt`
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/ui/request/BodyEditor.kt`
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/ui/request/RequestEditor.kt`
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/ui/response/SyntaxHighlighter.kt`
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/ui/response/StatusBar.kt`
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/ui/response/ResponseViewer.kt`

- [ ] **Step 1: Create `TabBar`**

```kotlin
package dev.herald.desktop.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.herald.core.model.HttpMethod
import dev.herald.desktop.theme.HeraldColors

data class TabInfo(
    val index: Int,
    val name: String,
    val method: HttpMethod,
    val isDirty: Boolean,
    val isActive: Boolean,
)

@Composable
fun TabBar(
    tabs: List<TabInfo>,
    onTabClick: (Int) -> Unit,
    onTabClose: (Int) -> Unit,
    onNewTab: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(HeraldColors.surface)
            .padding(horizontal = 4.dp),
    ) {
        tabs.forEach { tab ->
            TabItem(tab, onTabClick, onTabClose)
        }
        IconButton(
            onClick = onNewTab,
            modifier = Modifier.size(28.dp).semantics { contentDescription = "New tab" },
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = HeraldColors.textMuted)
        }
    }
}

@Composable
private fun TabItem(
    tab: TabInfo,
    onClick: (Int) -> Unit,
    onClose: (Int) -> Unit,
) {
    val bgColor = if (tab.isActive) HeraldColors.surfaceVariant else HeraldColors.surface
    val methodColor = when (tab.method) {
        HttpMethod.GET -> HeraldColors.methodGet
        HttpMethod.POST -> HeraldColors.methodPost
        HttpMethod.PUT -> HeraldColors.methodPut
        HttpMethod.DELETE -> HeraldColors.methodDelete
        HttpMethod.PATCH -> HeraldColors.methodPatch
        HttpMethod.HEAD -> HeraldColors.methodHead
        HttpMethod.OPTIONS -> HeraldColors.methodOptions
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 1.dp)
            .background(bgColor, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            .clickable { onClick(tab.index) }
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .semantics { contentDescription = "${tab.method.name} ${tab.name}${if (tab.isDirty) ", unsaved changes" else ""}" },
    ) {
        Text(tab.method.name, style = MaterialTheme.typography.labelSmall, color = methodColor)
        Spacer(Modifier.width(6.dp))
        Text(
            tab.name,
            style = MaterialTheme.typography.bodySmall,
            color = HeraldColors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 120.dp),
        )
        if (tab.isDirty) {
            Spacer(Modifier.width(4.dp))
            Text("●", style = MaterialTheme.typography.labelSmall, color = HeraldColors.textMuted)
        }
        Spacer(Modifier.width(4.dp))
        IconButton(
            onClick = { onClose(tab.index) },
            modifier = Modifier.size(16.dp).semantics { contentDescription = "Close tab ${tab.name}" },
        ) {
            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(12.dp), tint = HeraldColors.textMuted)
        }
    }
}
```

- [ ] **Step 2: Create `MethodDropdown`**

```kotlin
package dev.herald.desktop.ui.request

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.herald.core.model.HttpMethod
import dev.herald.desktop.theme.HeraldColors

@Composable
fun MethodDropdown(
    selected: HttpMethod,
    onSelect: (HttpMethod) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val color = when (selected) {
        HttpMethod.GET -> HeraldColors.methodGet
        HttpMethod.POST -> HeraldColors.methodPost
        HttpMethod.PUT -> HeraldColors.methodPut
        HttpMethod.DELETE -> HeraldColors.methodDelete
        HttpMethod.PATCH -> HeraldColors.methodPatch
        HttpMethod.HEAD -> HeraldColors.methodHead
        HttpMethod.OPTIONS -> HeraldColors.methodOptions
    }

    Box(modifier = modifier) {
        Text(
            selected.name,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            modifier = Modifier
                .border(1.dp, HeraldColors.border, RoundedCornerShape(4.dp))
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .semantics { contentDescription = "HTTP method: ${selected.name}" },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            HttpMethod.entries.forEach { method ->
                DropdownMenuItem(
                    text = { Text(method.name, style = MaterialTheme.typography.labelLarge) },
                    onClick = { onSelect(method); expanded = false },
                )
            }
        }
    }
}
```

- [ ] **Step 3: Create `UrlBar`**

```kotlin
package dev.herald.desktop.ui.request

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.herald.core.model.HttpMethod
import dev.herald.desktop.theme.HeraldColors
import dev.herald.desktop.theme.JetBrainsMono

@Composable
fun UrlBar(
    method: HttpMethod,
    url: String,
    onMethodChange: (HttpMethod) -> Unit,
    onUrlChange: (String) -> Unit,
    onSend: () -> Unit,
    isSending: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(8.dp),
    ) {
        MethodDropdown(selected = method, onSelect = onMethodChange)
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            placeholder = { Text("Enter URL or paste cURL", color = HeraldColors.textMuted) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMono),
            modifier = Modifier
                .weight(1f)
                .semantics { contentDescription = "Request URL" },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HeraldColors.accent,
                unfocusedBorderColor = HeraldColors.border,
                cursorColor = HeraldColors.accent,
            ),
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = onSend,
            enabled = !isSending && url.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = HeraldColors.accent),
            modifier = Modifier.semantics { contentDescription = "Send request" },
        ) {
            Text(if (isSending) "Sending..." else "Send")
        }
    }
}
```

- [ ] **Step 4: Create `KeyValueEditor`**

```kotlin
package dev.herald.desktop.ui.request

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.herald.core.model.KeyValueRow
import dev.herald.desktop.theme.HeraldColors
import dev.herald.desktop.theme.JetBrainsMono

@Composable
fun KeyValueEditor(
    rows: List<KeyValueRow>,
    onRowsChange: (List<KeyValueRow>) -> Unit,
    namePlaceholder: String = "Name",
    valuePlaceholder: String = "Value",
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(8.dp)) {
        rows.forEachIndexed { index, row ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            ) {
                Checkbox(
                    checked = row.enabled,
                    onCheckedChange = { checked ->
                        onRowsChange(rows.toMutableList().apply {
                            this[index] = row.copy(enabled = checked)
                        })
                    },
                    modifier = Modifier.size(20.dp).semantics {
                        contentDescription = "Enable row ${index + 1}"
                    },
                )
                Spacer(Modifier.width(4.dp))
                OutlinedTextField(
                    value = row.name,
                    onValueChange = { name ->
                        onRowsChange(rows.toMutableList().apply {
                            this[index] = row.copy(name = name)
                        })
                    },
                    placeholder = { Text(namePlaceholder, color = HeraldColors.textMuted) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HeraldColors.accent,
                        unfocusedBorderColor = HeraldColors.border,
                    ),
                )
                Spacer(Modifier.width(4.dp))
                OutlinedTextField(
                    value = row.value,
                    onValueChange = { value ->
                        onRowsChange(rows.toMutableList().apply {
                            this[index] = row.copy(value = value)
                        })
                    },
                    placeholder = { Text(valuePlaceholder, color = HeraldColors.textMuted) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HeraldColors.accent,
                        unfocusedBorderColor = HeraldColors.border,
                    ),
                )
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = {
                        onRowsChange(rows.toMutableList().apply { removeAt(index) })
                    },
                    modifier = Modifier.size(20.dp).semantics {
                        contentDescription = "Delete row ${index + 1}"
                    },
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = HeraldColors.textMuted)
                }
            }
        }
        TextButton(
            onClick = {
                onRowsChange(rows + KeyValueRow("", "", true))
            },
            modifier = Modifier.semantics { contentDescription = "Add row" },
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text("Add", style = MaterialTheme.typography.labelSmall)
        }
    }
}
```

- [ ] **Step 5: Create `BodyEditor`**

```kotlin
package dev.herald.desktop.ui.request

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.herald.desktop.theme.HeraldColors
import dev.herald.desktop.theme.JetBrainsMono

@Composable
fun BodyEditor(
    bodyType: String?,
    bodyContent: String?,
    onBodyTypeChange: (String?) -> Unit,
    onBodyContentChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(8.dp)) {
        Row(modifier = Modifier.padding(bottom = 8.dp)) {
            FilterChip(
                selected = bodyType == null,
                onClick = { onBodyTypeChange(null) },
                label = { Text("None", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.semantics { contentDescription = "No body" },
            )
            Spacer(Modifier.width(4.dp))
            FilterChip(
                selected = bodyType == "raw",
                onClick = { onBodyTypeChange("raw") },
                label = { Text("Raw", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.semantics { contentDescription = "Raw text body" },
            )
            Spacer(Modifier.width(4.dp))
            FilterChip(
                selected = bodyType == "json",
                onClick = { onBodyTypeChange("json") },
                label = { Text("JSON", style = MaterialTheme.typography.labelSmall) },
                modifier = Modifier.semantics { contentDescription = "JSON body" },
            )
        }

        if (bodyType != null) {
            OutlinedTextField(
                value = bodyContent ?: "",
                onValueChange = onBodyContentChange,
                placeholder = {
                    Text(
                        if (bodyType == "json") """{"key": "value"}""" else "Request body",
                        color = HeraldColors.textMuted,
                    )
                },
                textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp)
                    .semantics { contentDescription = "Request body editor" },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HeraldColors.accent,
                    unfocusedBorderColor = HeraldColors.border,
                ),
            )
        }
    }
}
```

- [ ] **Step 6: Create `RequestEditor`**

```kotlin
package dev.herald.desktop.ui.request

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.herald.core.model.HttpMethod
import dev.herald.core.model.KeyValueRow
import dev.herald.desktop.theme.HeraldColors

@Composable
fun RequestEditor(
    method: HttpMethod,
    url: String,
    headers: List<KeyValueRow>,
    queryParams: List<KeyValueRow>,
    bodyType: String?,
    bodyContent: String?,
    isSending: Boolean,
    onMethodChange: (HttpMethod) -> Unit,
    onUrlChange: (String) -> Unit,
    onHeadersChange: (List<KeyValueRow>) -> Unit,
    onQueryParamsChange: (List<KeyValueRow>) -> Unit,
    onBodyTypeChange: (String?) -> Unit,
    onBodyContentChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf("Headers", "Params", "Body")

    Column(modifier = modifier) {
        UrlBar(
            method = method,
            url = url,
            onMethodChange = onMethodChange,
            onUrlChange = onUrlChange,
            onSend = onSend,
            isSending = isSending,
        )
        TabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = HeraldColors.surface,
            contentColor = HeraldColors.textPrimary,
            modifier = Modifier.fillMaxWidth(),
        ) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    text = { Text(title, style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.semantics { contentDescription = "$title tab" },
                )
            }
        }
        when (selectedSubTab) {
            0 -> KeyValueEditor(
                rows = headers,
                onRowsChange = onHeadersChange,
                namePlaceholder = "Header name",
                valuePlaceholder = "Header value",
            )
            1 -> KeyValueEditor(
                rows = queryParams,
                onRowsChange = onQueryParamsChange,
                namePlaceholder = "Param name",
                valuePlaceholder = "Param value",
            )
            2 -> BodyEditor(
                bodyType = bodyType,
                bodyContent = bodyContent,
                onBodyTypeChange = onBodyTypeChange,
                onBodyContentChange = onBodyContentChange,
            )
        }
    }
}
```

- [ ] **Step 7: Create `SyntaxHighlighter`**

```kotlin
package dev.herald.desktop.ui.response

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import dev.herald.desktop.theme.HeraldColors

object SyntaxHighlighter {
    private val jsonKeyPattern = Regex(""""([^"\\]|\\.)*"\s*:""")
    private val jsonStringPattern = Regex(""":\s*"([^"\\]|\\.)*"""")
    private val jsonNumberPattern = Regex(""":\s*(-?\d+\.?\d*([eE][+-]?\d+)?)""")
    private val jsonBoolNullPattern = Regex("""\b(true|false|null)\b""")

    fun highlightJson(text: String): AnnotatedString {
        if (text.length > 500_000) return AnnotatedString(text)

        val builder = AnnotatedString.Builder(text)

        jsonKeyPattern.findAll(text).forEach { match ->
            val keyEnd = match.range.last - 1 // exclude the colon
            val keyStart = match.range.first
            builder.addStyle(
                SpanStyle(color = HeraldColors.accent, fontWeight = FontWeight.Normal),
                keyStart,
                keyEnd + 1,
            )
        }

        jsonStringPattern.findAll(text).forEach { match ->
            val colonAndSpace = match.value.indexOf('"')
            val start = match.range.first + colonAndSpace
            builder.addStyle(
                SpanStyle(color = Color(0xFF6A9955)),
                start,
                match.range.last + 1,
            )
        }

        jsonNumberPattern.findAll(text).forEach { match ->
            val colonAndSpace = match.value.indexOf(match.groupValues[1])
            val start = match.range.first + colonAndSpace
            builder.addStyle(
                SpanStyle(color = Color(0xFFB5CEA8)),
                start,
                start + match.groupValues[1].length,
            )
        }

        jsonBoolNullPattern.findAll(text).forEach { match ->
            builder.addStyle(
                SpanStyle(color = Color(0xFF569CD6), fontWeight = FontWeight.Bold),
                match.range.first,
                match.range.last + 1,
            )
        }

        return builder.toAnnotatedString()
    }
}
```

- [ ] **Step 8: Create `StatusBar`**

```kotlin
package dev.herald.desktop.ui.response

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.herald.desktop.theme.HeraldColors
import dev.herald.desktop.theme.JetBrainsMono

@Composable
fun StatusBar(
    status: Int,
    durationMs: Long,
    bodySize: Long,
    modifier: Modifier = Modifier,
) {
    val statusColor = when (status) {
        in 200..299 -> HeraldColors.status2xx
        in 300..399 -> HeraldColors.status3xx
        in 400..499 -> HeraldColors.status4xx
        else -> HeraldColors.status5xx
    }
    val statusText = "$status ${statusPhrase(status)}"
    val sizeText = formatSize(bodySize)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .semantics { contentDescription = "Status $status, ${durationMs}ms, $sizeText" },
    ) {
        Text(
            statusText,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            ),
            color = statusColor,
        )
        Spacer(Modifier.width(16.dp))
        Text(
            "${durationMs}ms",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
            color = HeraldColors.textSecondary,
        )
        Spacer(Modifier.width(16.dp))
        Text(
            sizeText,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
            color = HeraldColors.textSecondary,
        )
    }
}

private fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)} KB"
    else -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
}

private fun statusPhrase(status: Int): String = when (status) {
    200 -> "OK"
    201 -> "Created"
    204 -> "No Content"
    301 -> "Moved Permanently"
    302 -> "Found"
    304 -> "Not Modified"
    400 -> "Bad Request"
    401 -> "Unauthorized"
    403 -> "Forbidden"
    404 -> "Not Found"
    405 -> "Method Not Allowed"
    409 -> "Conflict"
    422 -> "Unprocessable Entity"
    429 -> "Too Many Requests"
    500 -> "Internal Server Error"
    502 -> "Bad Gateway"
    503 -> "Service Unavailable"
    504 -> "Gateway Timeout"
    else -> ""
}
```

- [ ] **Step 9: Create `ResponseViewer`**

```kotlin
package dev.herald.desktop.ui.response

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import dev.herald.core.model.KeyValueRow
import dev.herald.desktop.theme.HeraldColors
import dev.herald.desktop.theme.JetBrainsMono

data class ResponseData(
    val status: Int,
    val headers: List<KeyValueRow>,
    val body: String?,
    val durationMs: Long,
)

@Composable
fun ResponseViewer(
    response: ResponseData?,
    errorMessage: String?,
    modifier: Modifier = Modifier,
) {
    if (errorMessage != null) {
        Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text(
                errorMessage,
                color = HeraldColors.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    if (response == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Send a request to see the response",
                color = HeraldColors.textMuted,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        return
    }

    var viewMode by remember { mutableStateOf("pretty") }
    var selectedTab by remember { mutableIntStateOf(0) }
    val bodySize = (response.body?.length ?: 0).toLong()
    val truncated = bodySize > 1_000_000

    Column(modifier = modifier) {
        StatusBar(
            status = response.status,
            durationMs = response.durationMs,
            bodySize = bodySize,
        )
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = HeraldColors.surface,
            contentColor = HeraldColors.textPrimary,
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Body", style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.semantics { contentDescription = "Response body tab" },
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Headers", style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.semantics { contentDescription = "Response headers tab" },
            )
        }

        when (selectedTab) {
            0 -> {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    FilterChip(
                        selected = viewMode == "pretty",
                        onClick = { viewMode = "pretty" },
                        label = { Text("Pretty", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.semantics { contentDescription = "Pretty view mode" },
                    )
                    Spacer(Modifier.width(4.dp))
                    FilterChip(
                        selected = viewMode == "raw",
                        onClick = { viewMode = "raw" },
                        label = { Text("Raw", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.semantics { contentDescription = "Raw view mode" },
                    )
                }

                val displayBody = if (truncated) {
                    (response.body?.take(1_000_000) ?: "") + "\n\n[Truncated. Body exceeds 1MB]"
                } else {
                    response.body ?: ""
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                        .horizontalScroll(rememberScrollState()),
                ) {
                    if (viewMode == "pretty" && !truncated && bodySize <= 500_000) {
                        Text(
                            text = SyntaxHighlighter.highlightJson(displayBody),
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                        )
                    } else {
                        Text(
                            text = displayBody,
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                            color = HeraldColors.textPrimary,
                        )
                    }
                }
            }
            1 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    response.headers.forEach { header ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(
                                header.name,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                                color = HeraldColors.accent,
                                modifier = Modifier.width(200.dp),
                            )
                            Text(
                                header.value,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                                color = HeraldColors.textPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 10: Verify compilation**

Run:
```bash
./gradlew :desktop:compileKotlinDesktop
```
Expected: BUILD SUCCESSFUL.

- [ ] **Step 11: Commit**

```bash
git add desktop/src/jvmMain/kotlin/dev/herald/desktop/ui/
git commit -m "feat: add tab bar, request editor, and response viewer components"
```

---

### Task 12: Wire Stub App Layout (Track B)

**Files:**
- Modify: `desktop/src/jvmMain/kotlin/dev/herald/desktop/App.kt`
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/ui/common/PromptDialog.kt`

This task connects the tab bar, request editor, and response viewer into the main `App` composable using stub data. No real backend yet.

- [ ] **Step 1: Create `PromptDialog`**

```kotlin
package dev.herald.desktop.ui.common

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String = "OK",
    dismissText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.semantics { contentDescription = confirmText },
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.semantics { contentDescription = dismissText },
            ) { Text(dismissText) }
        },
    )
}
```

- [ ] **Step 2: Update `App.kt` with full layout**

Replace `desktop/src/jvmMain/kotlin/dev/herald/desktop/App.kt` with:

```kotlin
package dev.herald.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.herald.core.model.HttpMethod
import dev.herald.core.model.KeyValueRow
import dev.herald.desktop.state.AppState
import dev.herald.desktop.theme.HeraldColors
import dev.herald.desktop.ui.common.ConfirmDialog
import dev.herald.desktop.ui.request.RequestEditor
import dev.herald.desktop.ui.response.ResponseData
import dev.herald.desktop.ui.response.ResponseViewer
import dev.herald.desktop.ui.sidebar.*
import dev.herald.desktop.ui.tabs.TabBar
import dev.herald.desktop.ui.tabs.TabInfo

@Composable
fun App() {
    val appState = remember { AppState() }
    var pendingCloseTabIndex by remember { mutableStateOf<Int?>(null) }
    var responseData by remember { mutableStateOf<ResponseData?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }

    val stubCollections = remember {
        listOf(
            TreeCollection(
                id = 1, name = "My API",
                folders = listOf(
                    TreeFolder(id = 1, name = "Users", requests = listOf(
                        TreeRequest(1, "Get Users", HttpMethod.GET),
                        TreeRequest(2, "Create User", HttpMethod.POST),
                    )),
                ),
                rootRequests = listOf(
                    TreeRequest(3, "Health Check", HttpMethod.GET),
                ),
            ),
        )
    }
    val stubEnvironments = remember {
        listOf(EnvironmentItem(1, "dev"), EnvironmentItem(2, "prod"))
    }

    // Unsaved changes confirmation dialog
    pendingCloseTabIndex?.let { index ->
        ConfirmDialog(
            title = "Unsaved Changes",
            message = "This tab has unsaved changes. Close anyway?",
            confirmText = "Close",
            dismissText = "Cancel",
            onConfirm = {
                appState.forceCloseTab(index)
                pendingCloseTabIndex = null
            },
            onDismiss = { pendingCloseTabIndex = null },
        )
    }

    Row(modifier = Modifier.fillMaxSize().background(HeraldColors.background)) {
        Sidebar(
            collections = stubCollections,
            historyEntries = emptyList(),
            environments = stubEnvironments,
            selectedEnvironmentId = appState.activeEnvironmentId,
            onRequestClick = { /* wired in Task 13 */ },
            onHistoryClick = { /* wired in Task 13 */ },
            onEnvironmentSelect = { appState.activeEnvironmentId = it },
        )
        Divider(
            color = HeraldColors.border,
            modifier = Modifier.fillMaxHeight().width(1.dp),
        )
        Column(modifier = Modifier.fillMaxSize()) {
            TabBar(
                tabs = appState.tabs.mapIndexed { index, tab ->
                    TabInfo(
                        index = index,
                        name = tab.name,
                        method = tab.method,
                        isDirty = tab.isDirty,
                        isActive = index == appState.activeTabIndex,
                    )
                },
                onTabClick = { appState.switchToTab(it) },
                onTabClose = { index ->
                    if (!appState.closeTab(index)) {
                        pendingCloseTabIndex = index
                    }
                },
                onNewTab = { appState.openNewTab() },
            )
            Divider(color = HeraldColors.border, thickness = 1.dp)

            val activeTab = appState.activeTab
            if (activeTab != null) {
                Row(modifier = Modifier.fillMaxSize()) {
                    RequestEditor(
                        method = activeTab.method,
                        url = activeTab.url,
                        headers = activeTab.headers,
                        queryParams = activeTab.queryParams,
                        bodyType = activeTab.bodyType,
                        bodyContent = activeTab.bodyContent,
                        isSending = isSending,
                        onMethodChange = { method ->
                            appState.updateTab(appState.activeTabIndex) {
                                it.copy(method = method, isDirty = true)
                            }
                        },
                        onUrlChange = { url ->
                            appState.updateTab(appState.activeTabIndex) {
                                it.copy(url = url, isDirty = true)
                            }
                        },
                        onHeadersChange = { headers ->
                            appState.updateTab(appState.activeTabIndex) {
                                it.copy(headers = headers, isDirty = true)
                            }
                        },
                        onQueryParamsChange = { params ->
                            appState.updateTab(appState.activeTabIndex) {
                                it.copy(queryParams = params, isDirty = true)
                            }
                        },
                        onBodyTypeChange = { type ->
                            appState.updateTab(appState.activeTabIndex) {
                                it.copy(bodyType = type, isDirty = true)
                            }
                        },
                        onBodyContentChange = { content ->
                            appState.updateTab(appState.activeTabIndex) {
                                it.copy(bodyContent = content, isDirty = true)
                            }
                        },
                        onSend = { /* wired in Task 13 */ },
                        modifier = Modifier.weight(1f),
                    )
                    Divider(
                        color = HeraldColors.border,
                        modifier = Modifier.fillMaxHeight().width(1.dp),
                    )
                    ResponseViewer(
                        response = responseData,
                        errorMessage = errorMessage,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "Open a request or create a new tab to begin",
                        style = MaterialTheme.typography.bodyLarge,
                        color = HeraldColors.textMuted,
                    )
                }
            }
        }
    }
}
```

- [ ] **Step 3: Verify the full UI layout**

Run:
```bash
./gradlew :desktop:run
```
Expected: Full layout visible. Sidebar on left. Click "+" to create a new tab. Tab appears with method dropdown, URL bar, Send button, headers/params/body sub-tabs. Response area shows "Send a request to see the response". Environment selector works. Closing a dirty tab shows confirmation dialog.

- [ ] **Step 4: Commit**

```bash
git add desktop/src/jvmMain/kotlin/dev/herald/desktop/
git commit -m "feat: wire complete app layout with tabs, request editor, and response viewer"
```

---

## Integration (requires both tracks)

### Task 13: Wire Shared Module into Desktop (Integration)

**Files:**
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/ui/history/HistoryDetailView.kt`
- Modify: `desktop/src/jvmMain/kotlin/dev/herald/desktop/App.kt`
- Modify: `desktop/src/jvmMain/kotlin/dev/herald/desktop/Main.kt`

This task replaces all stub data with real database-backed state and connects the Send button to the request executor.

- [ ] **Step 1: Create `HistoryDetailView`**

```kotlin
package dev.herald.desktop.ui.history

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.herald.core.model.HistoryEntry
import dev.herald.core.model.KeyValueRow
import dev.herald.desktop.theme.HeraldColors
import dev.herald.desktop.theme.JetBrainsMono
import dev.herald.desktop.ui.response.ResponseData
import dev.herald.desktop.ui.response.ResponseViewer
import dev.herald.desktop.ui.response.StatusBar

@Composable
fun HistoryDetailView(
    entry: HistoryEntry,
    onReplay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Response", "Request")

    Column(modifier = modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Text(
                "${entry.method.name} ${entry.resolvedUrl}",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = JetBrainsMono),
                color = HeraldColors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onReplay,
                colors = ButtonDefaults.buttonColors(containerColor = HeraldColors.accent),
                modifier = Modifier.semantics { contentDescription = "Replay this request" },
            ) {
                Text("Replay")
            }
        }
        StatusBar(
            status = entry.responseStatus,
            durationMs = entry.durationMs,
            bodySize = (entry.responseBody?.length ?: 0).toLong(),
        )
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = HeraldColors.surface,
            contentColor = HeraldColors.textPrimary,
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title, style = MaterialTheme.typography.labelMedium) },
                )
            }
        }
        when (selectedTab) {
            0 -> ResponseViewer(
                response = ResponseData(
                    status = entry.responseStatus,
                    headers = entry.responseHeaders,
                    body = entry.responseBody,
                    durationMs = entry.durationMs,
                ),
                errorMessage = null,
            )
            1 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text("Headers", style = MaterialTheme.typography.labelLarge, color = HeraldColors.textSecondary)
                    Spacer(Modifier.height(4.dp))
                    entry.requestHeaders.forEach { header ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(
                                header.name,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                                color = HeraldColors.accent,
                                modifier = Modifier.width(200.dp),
                            )
                            Text(
                                header.value,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                                color = HeraldColors.textPrimary,
                            )
                        }
                    }
                    if (entry.requestBody != null) {
                        Spacer(Modifier.height(12.dp))
                        Text("Body", style = MaterialTheme.typography.labelLarge, color = HeraldColors.textSecondary)
                        Spacer(Modifier.height(4.dp))
                        Box(modifier = Modifier.horizontalScroll(rememberScrollState())) {
                            Text(
                                entry.requestBody,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                                color = HeraldColors.textPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Update `Main.kt` to initialize database**

```kotlin
package dev.herald.desktop

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.herald.desktop.theme.HeraldTheme
import dev.herald.storage.DatabaseProvider
import dev.herald.storage.DriverFactory

fun main() {
    val dbPath = resolveDbPath()
    val databaseProvider = DatabaseProvider(DriverFactory(dbPath))

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Herald",
            state = rememberWindowState(size = DpSize(1200.dp, 800.dp))
        ) {
            HeraldTheme {
                App(databaseProvider = databaseProvider)
            }
        }
    }
}

private fun resolveDbPath(): String {
    val os = System.getProperty("os.name").lowercase()
    val baseDir = when {
        os.contains("mac") -> System.getProperty("user.home") + "/Library/Application Support/herald"
        os.contains("win") -> System.getenv("APPDATA") + "\\herald"
        else -> System.getProperty("user.home") + "/.local/share/herald"
    }
    java.io.File(baseDir).mkdirs()
    return "$baseDir/data.db"
}
```

- [ ] **Step 3: Rewrite `App.kt` with real database wiring**

This is a large replacement. Replace the entire `App.kt` with the version that:
- Creates DAO instances from the `DatabaseProvider`
- Loads collections, folders, requests, environments, and history from the database
- Connects the Send button to the `RequestExecutor`
- Connects sidebar clicks to opening tabs with real data
- Connects history clicks to the `HistoryDetailView`
- CRUD operations via context menus (create/rename/delete collections, folders, requests, environments)

The full implementation should:
1. Accept `databaseProvider: DatabaseProvider` as a parameter
2. Create all DAOs: `CollectionDao`, `FolderDao`, `RequestDao`, `EnvironmentDao`, `VariableDao`, `HistoryDao`
3. Create `HttpEngine()` (real CIO engine) and `RequestExecutor(httpEngine, historyDao)`
4. Use `LaunchedEffect` to load initial data from the database
5. Convert database models to `TreeCollection`/`TreeFolder`/`TreeRequest` for the sidebar
6. Convert database models to `HistoryListItem` for the history list
7. Wire `onRequestClick` to load the request from DB and open in a tab via `appState.openRequestTab()`
8. Wire `onSend` to:
   - Collect enabled variables from the active environment
   - Call `requestExecutor.execute(request, variables)` in a coroutine
   - Update `responseData`/`errorMessage` based on the result
   - Refresh history list
9. Wire `onHistoryClick` to show `HistoryDetailView` (replace the response viewer with history detail when a history entry is selected)
10. Wire context menu CRUD operations to insert/update/delete via DAOs, refresh sidebar state

This is the most complex task. The implementer should build this incrementally: start with loading data, then add Send, then add CRUD, then add history detail view.

- [ ] **Step 4: Verify end-to-end flow**

Run:
```bash
./gradlew :desktop:run
```
Test the following flow:
1. Right-click sidebar → Create collection "Test API"
2. Right-click collection → Create folder "Users"
3. Right-click folder → Create request "Get Users"
4. Click request → opens in tab
5. Set URL to `https://httpbin.org/get`
6. Click Send → response appears with status 200 and JSON body
7. History entry appears in sidebar
8. Click history entry → shows read-only detail with Replay button
9. Create environment "dev", add variable `base_url` = `https://httpbin.org`
10. Edit request URL to `{{base_url}}/get`
11. Select "dev" environment, click Send → should resolve and succeed
12. Change to no environment, click Send → should show "unresolved variables" error

- [ ] **Step 5: Commit**

```bash
git add desktop/src/jvmMain/kotlin/dev/herald/desktop/
git commit -m "feat: wire shared module into desktop with real database and HTTP execution"
```

---

### Task 14: Context Menus for CRUD (Integration)

**Files:**
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/ui/common/ContextMenu.kt`
- Modify: `desktop/src/jvmMain/kotlin/dev/herald/desktop/ui/sidebar/CollectionTree.kt`
- Modify: `desktop/src/jvmMain/kotlin/dev/herald/desktop/ui/sidebar/Sidebar.kt`

- [ ] **Step 1: Create `ContextMenu` helper**

```kotlin
package dev.herald.desktop.ui.common

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.runtime.Composable

@Composable
fun TreeContextMenu(
    items: List<Pair<String, () -> Unit>>,
    content: @Composable () -> Unit,
) {
    ContextMenuArea(
        items = { items.map { (label, action) -> ContextMenuItem(label, action) } },
        content = content,
    )
}
```

- [ ] **Step 2: Add context menus to `CollectionTree`**

Update `CollectionTree.kt` to wrap each node with `TreeContextMenu`:
- **Collection node**: "New Folder", "New Request", "Rename", "Delete"
- **Folder node**: "New Request", "Rename", "Delete"
- **Request node**: "Rename", "Delete"

Add callback parameters to `CollectionTree`:
```kotlin
onNewFolder: (collectionId: Long) -> Unit,
onNewRequest: (collectionId: Long, folderId: Long?) -> Unit,
onRenameCollection: (id: Long, currentName: String) -> Unit,
onRenameFolder: (id: Long, currentName: String) -> Unit,
onRenameRequest: (id: Long, currentName: String) -> Unit,
onDeleteCollection: (id: Long) -> Unit,
onDeleteFolder: (id: Long) -> Unit,
onDeleteRequest: (id: Long) -> Unit,
```

Thread these through `CollectionNode`, `FolderNode`, and `RequestNode`.

- [ ] **Step 3: Add rename dialog**

Add to `PromptDialog.kt`:

```kotlin
@Composable
fun RenameDialog(
    title: String,
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                modifier = Modifier.semantics { contentDescription = "New name" },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
```

- [ ] **Step 4: Wire context menu callbacks in `App.kt`**

Connect each callback to the appropriate DAO operation + state refresh.

- [ ] **Step 5: Verify CRUD operations**

Run:
```bash
./gradlew :desktop:run
```
Test: create/rename/delete collections, folders, and requests via right-click context menus. Verify sidebar updates correctly after each operation.

- [ ] **Step 6: Commit**

```bash
git add desktop/src/jvmMain/kotlin/dev/herald/desktop/
git commit -m "feat: add context menu CRUD for collections, folders, and requests"
```

---

### Task 15: Environment and Variable Editor (Integration)

**Files:**
- Create: `desktop/src/jvmMain/kotlin/dev/herald/desktop/ui/sidebar/EnvironmentEditor.kt`
- Modify: `desktop/src/jvmMain/kotlin/dev/herald/desktop/ui/sidebar/EnvironmentSelector.kt`
- Modify: `desktop/src/jvmMain/kotlin/dev/herald/desktop/App.kt`

The spec requires users to create/edit/delete environments and their variables. The `EnvironmentSelector` only handles selection. This task adds editing.

- [ ] **Step 1: Create `EnvironmentEditor` dialog**

```kotlin
package dev.herald.desktop.ui.sidebar

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.herald.core.model.Variable
import dev.herald.desktop.theme.HeraldColors
import dev.herald.desktop.theme.JetBrainsMono

data class EditableEnvironment(
    val id: Long,
    val name: String,
    val variables: List<Variable>,
)

@Composable
fun EnvironmentEditorDialog(
    environments: List<EditableEnvironment>,
    onCreateEnvironment: (String) -> Unit,
    onRenameEnvironment: (Long, String) -> Unit,
    onDeleteEnvironment: (Long) -> Unit,
    onAddVariable: (environmentId: Long, key: String, value: String) -> Unit,
    onUpdateVariable: (Variable) -> Unit,
    onDeleteVariable: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedEnvId by remember { mutableStateOf(environments.firstOrNull()?.id) }
    var newEnvName by remember { mutableStateOf("") }
    val selectedEnv = environments.find { it.id == selectedEnvId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Environments") },
        text = {
            Row(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                // Environment list
                Column(modifier = Modifier.width(160.dp)) {
                    environments.forEach { env ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        ) {
                            TextButton(
                                onClick = { selectedEnvId = env.id },
                                modifier = Modifier.weight(1f),
                            ) {
                                Text(
                                    env.name,
                                    color = if (env.id == selectedEnvId) HeraldColors.accent else HeraldColors.textPrimary,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            IconButton(
                                onClick = { onDeleteEnvironment(env.id) },
                                modifier = Modifier.size(20.dp).semantics { contentDescription = "Delete ${env.name}" },
                            ) {
                                Icon(Icons.Default.Delete, null, Modifier.size(14.dp), tint = HeraldColors.textMuted)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newEnvName,
                            onValueChange = { newEnvName = it },
                            placeholder = { Text("New env", color = HeraldColors.textMuted) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = {
                                if (newEnvName.isNotBlank()) {
                                    onCreateEnvironment(newEnvName)
                                    newEnvName = ""
                                }
                            },
                            modifier = Modifier.semantics { contentDescription = "Create environment" },
                        ) {
                            Icon(Icons.Default.Add, null, Modifier.size(16.dp))
                        }
                    }
                }
                Divider(
                    color = HeraldColors.border,
                    modifier = Modifier.fillMaxHeight().width(1.dp).padding(horizontal = 8.dp),
                )
                // Variable editor for selected environment
                if (selectedEnv != null) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            selectedEnv.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = HeraldColors.textPrimary,
                        )
                        Spacer(Modifier.height(8.dp))
                        selectedEnv.variables.forEach { variable ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            ) {
                                Checkbox(
                                    checked = variable.enabled,
                                    onCheckedChange = { checked ->
                                        onUpdateVariable(variable.copy(enabled = checked))
                                    },
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                OutlinedTextField(
                                    value = variable.key,
                                    onValueChange = { onUpdateVariable(variable.copy(key = it)) },
                                    placeholder = { Text("key") },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                                    modifier = Modifier.weight(1f),
                                )
                                Spacer(Modifier.width(4.dp))
                                OutlinedTextField(
                                    value = variable.value,
                                    onValueChange = { onUpdateVariable(variable.copy(value = it)) },
                                    placeholder = { Text("value") },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMono),
                                    modifier = Modifier.weight(1f),
                                )
                                IconButton(
                                    onClick = { onDeleteVariable(variable.id) },
                                    modifier = Modifier.size(20.dp),
                                ) {
                                    Icon(Icons.Default.Delete, null, Modifier.size(14.dp), tint = HeraldColors.textMuted)
                                }
                            }
                        }
                        TextButton(
                            onClick = { onAddVariable(selectedEnv.id, "", "") },
                        ) {
                            Icon(Icons.Default.Add, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Variable", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        },
    )
}
```

- [ ] **Step 2: Add "Manage" button to `EnvironmentSelector`**

Update `EnvironmentSelector.kt` to add a "Manage Environments" option in the dropdown that triggers opening the editor dialog. Add an `onManageClick: () -> Unit` parameter.

- [ ] **Step 3: Wire environment editor in `App.kt`**

Add state for showing the environment editor dialog. Connect all callbacks to `EnvironmentDao` and `VariableDao` operations. Refresh environment list after mutations.

- [ ] **Step 4: Verify environment CRUD**

Run:
```bash
./gradlew :desktop:run
```
Test: Click environment selector → "Manage Environments" → Create "dev" → Add variables `base_url` and `api_key` → Select "dev" → Send request with `{{base_url}}` in URL → Verify resolution works.

- [ ] **Step 5: Commit**

```bash
git add desktop/src/jvmMain/kotlin/dev/herald/desktop/
git commit -m "feat: add environment and variable editor dialog"
```

---

### Task 16: Final Polish and Full Test (Integration)

- [ ] **Step 1: Run all shared module tests**

Run:
```bash
./gradlew :shared:jvmTest
```
Expected: All tests PASS.

- [ ] **Step 2: Run full build**

Run:
```bash
./gradlew build
```
Expected: BUILD SUCCESSFUL. No warnings from SQLDelight or Compose compiler.

- [ ] **Step 3: Manual end-to-end smoke test**

Run:
```bash
./gradlew :desktop:run
```

Test checklist:
- [ ] Create a collection
- [ ] Create a folder inside the collection
- [ ] Create a request in the folder
- [ ] Create a request at the collection root
- [ ] Edit request: set method to POST, add URL, add headers, add JSON body
- [ ] Send request to `https://httpbin.org/post` with JSON body
- [ ] Verify response: 200 status, JSON body displayed with syntax highlighting, headers tab shows response headers
- [ ] Toggle to Raw view mode
- [ ] Check history: entry appears in sidebar
- [ ] Click history entry: detail view shows request and response
- [ ] Replay from history: sends the same request again
- [ ] Create environment "dev" with variable `base_url` = `https://httpbin.org`
- [ ] Update request URL to `{{base_url}}/get`, change method to GET
- [ ] Select "dev" environment, send: should resolve and succeed
- [ ] Deselect environment (No Environment), send: should block with unresolved variable error
- [ ] Open multiple tabs: verify no duplicates when clicking same request
- [ ] Modify a tab, try to close: unsaved changes dialog appears
- [ ] Rename a collection, folder, request via context menu
- [ ] Delete a request: verify history is also deleted (cascade)
- [ ] Delete a collection: verify all contents removed
- [ ] Keyboard navigation: Tab through sidebar, tabs, and request editor fields
- [ ] Verify contrast: text is readable, focus indicators visible

- [ ] **Step 4: Update `.gitignore`**

Ensure `.gitignore` includes:
```
.gradle/
build/
*.db
.DS_Store
```

- [ ] **Step 5: Final commit**

```bash
git add .
git commit -m "feat: Herald V1 complete with request builder, collections, environments, tabs, and history"
```

---
