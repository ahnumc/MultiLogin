package moe.caa.multilogin.core.database.pool

import java.sql.Connection
import java.sql.SQLException

/**
 * 表示数据库连接池
 */
interface ISQLConnectionPool {
    @get:Throws(SQLException::class)
    val connection: Connection

    /**
     * 获得该连接池名字
     */
    fun name(): String

    /**
     * 关闭链接
     */
    fun close()
}
