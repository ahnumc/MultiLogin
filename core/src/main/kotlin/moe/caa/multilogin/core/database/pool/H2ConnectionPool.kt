package moe.caa.multilogin.core.database.pool

import org.h2.jdbcx.JdbcConnectionPool
import java.io.File
import java.sql.Connection
import java.sql.SQLException

/**
 * H2 数据库链接池
 */
class H2ConnectionPool @JvmOverloads constructor(
    dataFolder: File,
    user: String?,
    password: String?,
    url: String = defaultUrl
) : ISQLConnectionPool {
    private val cp: JdbcConnectionPool

    init {
        Class.forName("org.h2.Driver")
        cp = JdbcConnectionPool.create(
            url.replace("{0}", dataFolder.getAbsolutePath() + File.separator + "multilogin"),
            user,
            password
        )
    }

    @get:Throws(SQLException::class)
    override val connection: Connection
        get() = cp.getConnection()

    override fun name(): String {
        return "H2"
    }

    override fun close() {
        cp.dispose()
    }

    companion object {
        const val defaultUrl: String = "jdbc:h2:{0};TRACE_LEVEL_FILE=0;TRACE_LEVEL_SYSTEM_OUT=0"
    }
}
