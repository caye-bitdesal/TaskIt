package com.bitdesal.taskit.di

import app.cash.sqldelight.db.SqlDriver
import com.bitdesal.taskit.db.TaskItDatabase
import com.bitdesal.taskit.db.createSqlDriver
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@BindingContainer
object DatabaseBindings {
    @Provides
    @SingleIn(ServerScope::class)
    fun sqlDriver(): SqlDriver = createSqlDriver()

    @Provides
    @SingleIn(ServerScope::class)
    fun database(driver: SqlDriver): TaskItDatabase = TaskItDatabase(driver)
}