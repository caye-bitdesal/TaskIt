fun createSqlDriver(): SqlDriver =
    JdbcSqliteDriver(
        url = "jdbc:sqlite:taskit.db",
        properties = Properties(),
        schema = TaskItDatabase.Schema,
    )

fun createDatabase(driver: SqlDriver = createSqlDriver()): TaskItDatabase =
    TaskItDatabase(driver)