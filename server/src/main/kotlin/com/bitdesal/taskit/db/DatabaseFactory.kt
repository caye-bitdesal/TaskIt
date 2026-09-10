package com.bitdesal.taskit.db
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.util.Properties

fun createSqlDriver(): SqlDriver =
    JdbcSqliteDriver(
        url = "jdbc:sqlite:taskit.db",
        properties = Properties(),
        schema = TaskItDatabase.Schema,
    )