package moe.caa.multilogin.core.database.table

import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.util.ValueUtil.bytesToUuid
import moe.caa.multilogin.api.internal.util.ValueUtil.uuidToBytes
import moe.caa.multilogin.core.command.CommandHandler
import moe.caa.multilogin.core.database.SQLManager
import moe.caa.multilogin.core.database.query
import moe.caa.multilogin.core.database.queryAll
import moe.caa.multilogin.core.database.update
import java.sql.Connection
import java.sql.SQLException
import java.sql.Types
import java.text.MessageFormat
import java.util.*

/**
 * 玩家数据表
 */
class UserDataTableV3(
    private val sqlManager: SQLManager,
    private val tableName: String,
    private val tableNameV2: String
) {
    private val pool
        get() = sqlManager.pool

    data class OnlineRecord(
        val onlineName: String?,
        val inGameUUID: UUID?,
        val whitelist: Boolean
    )

    data class LinkedProfile(
        val onlineUUID: UUID,
        val onlineName: String?,
        val serviceId: Int
    )

    @Throws(SQLException::class)
    fun init(connection: Connection) {
        val sql = MessageFormat.format(
            "CREATE TABLE IF NOT EXISTS {0} ( " +
                    "{1} BINARY(16) NOT NULL, " +
                    "{2} INTEGER NOT NULL, " +
                    "{3} VARCHAR(64) DEFAULT NULL, " +
                    "{4} BINARY(16) DEFAULT NULL, " +
                    "{5} BOOL DEFAULT FALSE, " +
                    "PRIMARY KEY ( {1}, {2} ))",
            tableName, fieldOnlineUUID, fieldServiceId, fieldOnlineName, fieldInGameProfileUuid, fieldWhitelist
        )
        connection.prepareStatement(sql).use { preparedStatement ->
            preparedStatement.executeUpdate()
            connection.prepareStatement("SELECT COUNT(0) FROM $tableName").use { stmt ->
                stmt.executeQuery().use { rs ->
                    rs.next()
                    if (rs.getInt(1) != 0) return
                }
            }
            try {
                connection.prepareStatement("SELECT COUNT(0) FROM $tableNameV2").use { stmt ->
                    stmt.executeQuery().use { rs ->
                        rs.next()
                        if (rs.getInt(1) == 0) return
                    }
                }
            } catch (ignored: Exception) {
                return
            }
        }
        LoggerProvider.logger.info("Updating user data...")
        data class V2Entry(val onlineUUID: ByteArray, val serviceId: Int, val inGameProfileUUID: ByteArray, val whitelist: Boolean)
        val oldData = mutableListOf<V2Entry>()
        connection.prepareStatement("SELECT online_uuid, yggdrasil_id, in_game_profile_uuid, whitelist FROM $tableNameV2")
            .use { stmt ->
                stmt.executeQuery().use { rs ->
                    while (rs.next()) oldData.add(V2Entry(rs.getBytes(1), rs.getBytes(2)[0].toInt(), rs.getBytes(3), rs.getBoolean(4)))
                }
            }
        val insertSql = "INSERT INTO $tableName ($fieldOnlineUUID, $fieldServiceId, $fieldInGameProfileUuid, $fieldWhitelist) VALUES (?, ?, ?, ?)"
        for (datum in oldData) {
            connection.prepareStatement(insertSql).use { stmt ->
                stmt.setBytes(1, datum.onlineUUID)
                stmt.setInt(2, datum.serviceId)
                stmt.setBytes(3, datum.inGameProfileUUID)
                stmt.setBoolean(4, datum.whitelist)
                stmt.executeUpdate()
            }
        }
        LoggerProvider.logger.info("Updated user data, total ${oldData.size}.")
    }

    @Throws(SQLException::class)
    fun get(onlineUUID: UUID, serviceId: Int): OnlineRecord? =
        pool.query(
            "SELECT $fieldOnlineName, $fieldInGameProfileUuid, $fieldWhitelist FROM $tableName WHERE $fieldOnlineUUID = ? AND $fieldServiceId = ? LIMIT 1",
            { setBytes(1, uuidToBytes(onlineUUID)); setInt(2, serviceId) }
        ) { OnlineRecord(getString(1), getBytes(2)?.let { bytesToUuid(it) }, getBoolean(3)) }

    @Throws(SQLException::class)
    fun getOnlineUUID(username: String, serviceId: Int): UUID? =
        pool.query(
            "SELECT $fieldOnlineUUID FROM $tableName WHERE lower($fieldOnlineName) = ? AND $fieldServiceId = ? LIMIT 1",
            { setString(1, username.lowercase()); setInt(2, serviceId) }
        ) { getBytes(1)?.let { bytesToUuid(it) } }

    /**
     * 从数据库中检索用户游戏内 UUID
     */
    @Throws(SQLException::class)
    fun getInGameUUID(onlineUUID: UUID, serviceId: Int): UUID? =
        pool.query(
            "SELECT $fieldInGameProfileUuid FROM $tableName WHERE $fieldOnlineUUID = ? AND $fieldServiceId = ? LIMIT 1",
            { setBytes(1, uuidToBytes(onlineUUID)); setInt(2, serviceId) }
        ) { getBytes(1)?.let { bytesToUuid(it) } }

    /**
     * 从数据库中检索用户登录时所用的账户验证服务器 ID
     */
    @Throws(SQLException::class)
    fun getOnlineServiceIds(inGameUUID: UUID): Set<Int> =
        pool.queryAll(
            "SELECT $fieldServiceId FROM $tableName WHERE $fieldInGameProfileUuid = ?",
            { setBytes(1, uuidToBytes(inGameUUID)) }
        ) { getInt(1) }.toSet()

    /**
     * 返回档案集合
     */
    @Throws(SQLException::class)
    fun getOnlineProfiles(inGameUUID: UUID): Set<LinkedProfile> =
        pool.queryAll(
            "SELECT $fieldOnlineUUID, $fieldOnlineName, $fieldServiceId FROM $tableName WHERE $fieldInGameProfileUuid = ?",
            { setBytes(1, uuidToBytes(inGameUUID)) }
        ) {
            LinkedProfile(
                onlineUUID = requireNotNull(bytesToUuid(getBytes(1))) { "Online UUID is missing from user data." },
                onlineName = getString(2),
                serviceId = getInt(3)
            )
        }.toSet()

    /**
     * 设置游戏内 UUID
     */
    @Throws(SQLException::class)
    fun setInGameUUID(onlineUUID: UUID, serviceId: Int, newInGameUUID: UUID): Int =
        pool.update(
            "UPDATE $tableName SET $fieldInGameProfileUuid = ? WHERE $fieldOnlineUUID = ? AND $fieldServiceId = ? LIMIT 1"
        ) {
            setBytes(1, uuidToBytes(newInGameUUID))
            setBytes(2, uuidToBytes(onlineUUID))
            setInt(3, serviceId)
        }

    /**
     * 查询数据是否存在
     */
    @Throws(SQLException::class)
    fun dataExists(onlineUUID: UUID, serviceId: Int): Boolean =
        pool.query(
            "SELECT 1 FROM $tableName WHERE $fieldOnlineUUID = ? AND $fieldServiceId = ? LIMIT 1",
            { setBytes(1, uuidToBytes(onlineUUID)); setInt(2, serviceId) }
        ) { true } ?: false

    /**
     * 插入一条用户数据
     */
    @Throws(SQLException::class)
    fun insertNewData(onlineUUID: UUID, serviceId: Int, onlineName: String?, inGameUUID: UUID?): Int =
        pool.update(
            "INSERT INTO $tableName ($fieldOnlineUUID, $fieldServiceId, $fieldOnlineName, $fieldInGameProfileUuid) VALUES (?, ?, ?, ?) "
        ) {
            setBytes(1, uuidToBytes(onlineUUID))
            setInt(2, serviceId)
            setString(3, onlineName)
            if (inGameUUID == null) setNull(4, Types.BINARY) else setBytes(4, uuidToBytes(inGameUUID))
        }

    /**
     * 设置白名单
     */
    @Throws(SQLException::class)
    fun setWhitelist(onlineUUID: UUID, serviceId: Int, whitelist: Boolean) {
        pool.update(
            "UPDATE $tableName SET $fieldWhitelist = ? WHERE $fieldOnlineUUID = ? AND $fieldServiceId = ? LIMIT 1"
        ) {
            setBoolean(1, whitelist)
            setBytes(2, uuidToBytes(onlineUUID))
            setInt(3, serviceId)
        }
    }

    /**
     * 查询白名单
     */
    @Throws(SQLException::class)
    fun hasWhitelist(onlineUUID: UUID, serviceId: Int): Boolean =
        pool.query(
            "SELECT $fieldWhitelist FROM $tableName WHERE $fieldOnlineUUID = ? AND $fieldServiceId = ? LIMIT 1",
            { setBytes(1, uuidToBytes(onlineUUID)); setInt(2, serviceId) }
        ) { getBoolean(1) } ?: false

    /**
     * 查询白名单
     */
    @Throws(SQLException::class)
    fun hasWhitelist(inGameUUID: UUID): Boolean =
        pool.query(
            "SELECT $fieldWhitelist FROM $tableName WHERE $fieldInGameProfileUuid = ? LIMIT 1",
            { setBytes(1, uuidToBytes(inGameUUID)) }
        ) { getBoolean(1) } ?: false

    /**
     * 设置白名单
     */
    @Throws(SQLException::class)
    fun setWhitelist(inGameUUID: UUID, whitelist: Boolean) {
        pool.update(
            "UPDATE $tableName SET $fieldWhitelist = ? WHERE $fieldInGameProfileUuid = ?LIMIT 1"
        ) {
            setBoolean(1, whitelist)
            setBytes(2, uuidToBytes(inGameUUID))
        }
    }

    /**
     * 列出白名单
     */
    @Throws(SQLException::class)
    fun listWhitelist(verbose: Boolean): List<String?> {
        val sql = if (verbose)
            "SELECT $fieldOnlineName, $fieldServiceId, $fieldOnlineUUID, $fieldInGameProfileUuid FROM $tableName WHERE $fieldWhitelist = true"
        else
            "SELECT $fieldOnlineName FROM $tableName WHERE $fieldWhitelist = true"
        val commandCore = CommandHandler.core
        return pool.queryAll(sql) {
            if (verbose) {
                val serviceId = getInt(2)
                val serviceConfig = commandCore.pluginConfig.serviceIdMap[serviceId]
                val serviceName = serviceConfig?.serviceName
                    ?: commandCore.languageHandler.getMessage("command_message_find_profile_entry_unused_service")
                "${getString(1)} ($fieldServiceId=$serviceId($serviceName), $fieldOnlineUUID=${bytesToUuid(getBytes(3))}, $fieldInGameProfileUuid=${getBytes(4)?.let { bytesToUuid(it) }})"
            } else {
                getString(1)
            }
        }
    }

    @Throws(SQLException::class)
    fun setOnlineName(onlineUUID: UUID, serviceId: Int, onlineName: String?) {
        pool.update(
            "UPDATE $tableName SET $fieldOnlineName = ? WHERE $fieldOnlineUUID = ? AND $fieldServiceId = ? LIMIT 1"
        ) {
            setString(1, onlineName)
            setBytes(2, uuidToBytes(onlineUUID))
            setInt(3, serviceId)
        }
    }

    @Throws(SQLException::class)
    fun getOnlineName(onlineUUID: UUID, serviceId: Int): String? =
        pool.query(
            "SELECT $fieldOnlineName FROM $tableName WHERE $fieldOnlineUUID = ? AND $fieldServiceId = ? LIMIT 1",
            { setBytes(1, uuidToBytes(onlineUUID)); setInt(2, serviceId) }
        ) { getString(1) }

    companion object {
        private const val fieldOnlineUUID = "online_uuid"
        private const val fieldOnlineName = "online_name"
        private const val fieldServiceId = "service_id"
        private const val fieldInGameProfileUuid = "in_game_profile_uuid"
        private const val fieldWhitelist = "whitelist"
    }
}
