package moe.caa.multilogin.core.database

import moe.caa.multilogin.core.configuration.SqlConfig.SqlBackend
import moe.caa.multilogin.core.database.pool.H2ConnectionPool
import moe.caa.multilogin.core.database.pool.ISQLConnectionPool
import moe.caa.multilogin.core.database.pool.MysqlConnectionPool
import moe.caa.multilogin.core.database.table.InGameProfileTableV3
import moe.caa.multilogin.core.database.table.SkinRestoredCacheTableV2
import moe.caa.multilogin.core.database.table.UserDataTableV3
import moe.caa.multilogin.core.main.MultiCore
import java.sql.SQLException

/**
 * 数据库管理程序
 */
class SQLManager(val core: MultiCore) {
    lateinit var pool: ISQLConnectionPool
        private set

    lateinit var inGameProfileTable: InGameProfileTableV3
        private set

    lateinit var userDataTable: UserDataTableV3
        private set

    lateinit var skinRestoredCacheTable: SkinRestoredCacheTableV2
        private set


    @Throws(SQLException::class, ClassNotFoundException::class)
    fun init() {
        val sqlConfig = requireNotNull(core.pluginConfig.sqlConfig)
        pool = when (sqlConfig.backend) {
            SqlBackend.MYSQL -> MysqlConnectionPool(
                requireNotNull(sqlConfig.ip), sqlConfig.port, requireNotNull(sqlConfig.database),
                requireNotNull(sqlConfig.username), requireNotNull(sqlConfig.password),
                sqlConfig.connectUrl?.takeUnless(String::isBlank) ?: MysqlConnectionPool.defaultUrl
            )
            SqlBackend.H2 -> H2ConnectionPool(
                core.plugin.dataFolder, requireNotNull(sqlConfig.username), requireNotNull(sqlConfig.password),
                sqlConfig.connectUrl?.takeUnless(String::isBlank) ?: H2ConnectionPool.defaultUrl
            )
            else -> throw UnsupportedOperationException("Database type Unknown.")
        }
        val tablePrefix = requireNotNull(sqlConfig.tablePrefix) + '_'

        val inGameProfileTableNameV2 = tablePrefix + "in_game_profile_v2"
        val inGameProfileTableNameV3 = tablePrefix + "in_game_profile_v3"
        val userDataTableNameV2 = tablePrefix + "user_data_v2"
        val userDataTableNameV3 = tablePrefix + "user_data_v3"
        val skinRestorerCacheTableNameV2 = tablePrefix + "skin_restored_cache_v2"
        userDataTable = UserDataTableV3(this, userDataTableNameV3, userDataTableNameV2)
        skinRestoredCacheTable = SkinRestoredCacheTableV2(this, skinRestorerCacheTableNameV2)
        inGameProfileTable = InGameProfileTableV3(this, inGameProfileTableNameV3, inGameProfileTableNameV2)

        pool.connection.use { connection ->
            connection.setAutoCommit(false)
            userDataTable.init(connection)
            inGameProfileTable.init(connection)
            skinRestoredCacheTable.init(connection)
            connection.commit()
        }
    }

    fun close() {
        if (::pool.isInitialized) {
            pool.close()
        }
    }
}
