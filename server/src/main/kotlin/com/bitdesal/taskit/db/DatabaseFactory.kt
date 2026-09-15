package com.bitdesal.taskit.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.util.Properties

fun createSqlDriver(url: String = "jdbc:sqlite:taskit.db"): SqlDriver =
    JdbcSqliteDriver(
        url = url,
        properties = Properties(),
        schema = TaskItDatabase.Schema,
    )

fun createInMemorySqlDriver(): SqlDriver =
    createSqlDriver(JdbcSqliteDriver.IN_MEMORY)