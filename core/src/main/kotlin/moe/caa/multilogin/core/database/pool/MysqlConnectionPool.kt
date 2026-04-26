package moe.caa.multilogin.core.database.pool

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.SQLException

/**
 * MySQL 链接池
 */
class MysqlConnectionPool @JvmOverloads constructor(
    ip: String,
    port: Int,
    database: String,
    username: String?,
    password: String?,
    url: String = defaultUrl
) : ISQLConnectionPool {
    private val dataSource: HikariDataSource

    init {
        var url = url
        Class.forName("com.mysql.cj.jdbc.Driver")
        url = url.replace("{0}", ip).replace("{1}", port.toString()).replace("{2}", database)
        val config = HikariConfig()
        config.setJdbcUrl(url)
        config.setUsername(username)
        config.setPassword(password)
        config.setMaximumPoolSize(20)
        dataSource = HikariDataSource(config)
    }

    @get:Throws(SQLException::class)
    override val connection: Connection
        get() = dataSource.getConnection()

    override fun name(): String {
        return "MySQL"
    }

    override fun close() {
        dataSource.close()
    }

    companion object {
        const val defaultUrl: String =
            "jdbc:mysql://{0}:{1}/{2}?autoReconnect=true&useUnicode=true&amp&characterEncoding=UTF-8&useSSL=false"
    }
}
