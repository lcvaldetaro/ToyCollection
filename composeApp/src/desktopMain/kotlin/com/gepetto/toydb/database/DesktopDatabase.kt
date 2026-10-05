package com.gepetto.toydb.database

import club.gepetto.GcLog
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet

class DesktopSqlCursor(private val resultSet: ResultSet) : SqlCursor {
    override fun next(): Boolean = resultSet.next()
    override fun getString(columnName: String): String? {
        val value = resultSet.getString(columnName)
        return if (resultSet.wasNull()) null else value
    }
    override fun getInt(columnName: String): Int? {
        val value = resultSet.getInt(columnName)
        return if (resultSet.wasNull()) null else value
    }
    override fun getDouble(columnName: String): Double? {
        val value = resultSet.getDouble(columnName)
        return if (resultSet.wasNull()) null else value
    }
    override fun close() {
        resultSet.close()
    }
}

class DesktopToyDatabase(dbName: String) : ToyDatabase {
    private val connection: Connection

    init {
        Class.forName("org.sqlite.JDBC")
        val url = "jdbc:sqlite:$dbName"
        connection = DriverManager.getConnection(url)
        checkUpgrade(this)
    }

    override fun execute(sql: String, bindArgs: List<Any?>) {
        val stmt = connection.prepareStatement(sql)
        bindArgs.forEachIndexed { index, value ->
            stmt.setObject(index + 1, value)
        }
        stmt.execute()
        stmt.close()
    }

    override fun query(sql: String, bindArgs: List<String>): SqlCursor {
        val stmt = connection.prepareStatement(sql)
        bindArgs.forEachIndexed { index, value ->
            stmt.setString(index + 1, value)
        }
        val rs = stmt.executeQuery()
        return DesktopSqlCursor(rs)
    }

    override fun close() {
        connection.close()
    }
}

actual fun createDatabase(platformContext: Any?, dbName: String): ToyDatabase {
    return DesktopToyDatabase(dbName)
}
