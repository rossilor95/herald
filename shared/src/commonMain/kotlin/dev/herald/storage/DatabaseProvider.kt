package dev.herald.storage

class DatabaseProvider(driverFactory: DriverFactory) {
    val database: HeraldDatabase = HeraldDatabase(driverFactory.createDriver())
}
