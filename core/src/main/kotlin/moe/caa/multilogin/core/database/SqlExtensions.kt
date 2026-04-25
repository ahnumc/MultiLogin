package moe.caa.multilogin.core.database

import moe.caa.multilogin.core.database.pool.ISQLConnectionPool
import java.sql.PreparedStatement
import java.sql.ResultSet

internal fun <T> ISQLConnectionPool.query(
    sql: String,
    bind: PreparedStatement.() -> Unit = {},
    map: ResultSet.() -> T?
): T? = connection.use { conn ->
    conn.prepareStatement(sql).use { stmt ->
        stmt.bind()
        stmt.executeQuery().use { rs -> if (rs.next()) rs.map() else null }
    }
}

internal fun <T> ISQLConnectionPool.queryAll(
    sql: String,
    bind: PreparedStatement.() -> Unit = {},
    map: ResultSet.() -> T
): List<T> = connection.use { conn ->
    conn.prepareStatement(sql).use { stmt ->
        stmt.bind()
        stmt.executeQuery().use { rs ->
            val result = mutableListOf<T>()
            while (rs.next()) result.add(rs.map())
            result
        }
    }
}

internal fun ISQLConnectionPool.update(
    sql: String,
    bind: PreparedStatement.() -> Unit = {}
): Int = connection.use { conn ->
    conn.prepareStatement(sql).use { stmt ->
        stmt.bind()
        stmt.executeUpdate()
    }
}

internal fun ISQLConnectionPool.updateWithCommit(
    sql: String,
    bind: PreparedStatement.() -> Unit = {}
): Int = connection.use { conn ->
    conn.prepareStatement(sql).use { stmt ->
        conn.autoCommit = false
        stmt.bind()
        val rows = stmt.executeUpdate()
        conn.commit()
        rows
    }
}
