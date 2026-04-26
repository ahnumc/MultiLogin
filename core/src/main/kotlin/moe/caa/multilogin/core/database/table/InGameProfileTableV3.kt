package moe.caa.multilogin.core.database.table

import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.util.ValueUtil.bytesToUuid
import moe.caa.multilogin.api.internal.util.ValueUtil.uuidToBytes
import moe.caa.multilogin.core.database.SQLManager
import moe.caa.multilogin.core.database.query
import moe.caa.multilogin.core.database.update
import moe.caa.multilogin.core.database.updateWithCommit
import java.sql.Connection
import java.sql.SQLException
import java.text.MessageFormat
import java.util.*

class InGameProfileTableV3(
    private val sqlManager: SQLManager,
    private val tableName: String?,
    private val tableNameV2: String?
) {
    private val pool
        get() = requireNotNull(sqlManager.pool)

    @Throws(SQLException::class)
    fun init(connection: Connection) {
        val sql = MessageFormat.format(
            "CREATE TABLE IF NOT EXISTS {0} ( " +
                    "{1} BINARY(16) NOT NULL, " +
                    "{2} VARCHAR(64) DEFAULT NULL, " +
                    "{3} VARCHAR(64) DEFAULT NULL, " +
                    "CONSTRAINT IGPT_V3_PR PRIMARY KEY ( {1} ), " +
                    "CONSTRAINT IGPT_V3_UN UNIQUE ( {2} ))",
            tableName, fieldInGameUuid, fieldCurrentUsernameLowerCase, fieldCurrentUsernameOriginal
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
        LoggerProvider.logger.info("Updating in game profile data...")
        val oldData = mutableListOf<Pair<ByteArray?, String?>>()
        connection.prepareStatement("SELECT in_game_uuid, current_username FROM $tableNameV2").use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) oldData.add(Pair(rs.getBytes(1), rs.getString(2)))
            }
        }
        val insertSql = "INSERT INTO $tableName ($fieldInGameUuid, $fieldCurrentUsernameLowerCase) VALUES (?, ?)"
        for (datum in oldData) {
            connection.prepareStatement(insertSql).use { stmt ->
                stmt.setBytes(1, datum.value1)
                stmt.setString(2, datum.value2?.lowercase())
                stmt.executeUpdate()
            }
        }
        LoggerProvider.logger.info("Updated in game profile data, total ${oldData.size}.")
    }

    @Throws(SQLException::class)
    fun get(inGameUUID: UUID): Pair<UUID?, String?>? =
        pool.query(
            "SELECT $fieldCurrentUsernameOriginal FROM $tableName WHERE $fieldInGameUuid = ? LIMIT 1",
            { setBytes(1, uuidToBytes(inGameUUID)) }
        ) { Pair(inGameUUID, getString(1)) }

    /**
     * 获得游戏内 UUID
     */
    @Throws(SQLException::class)
    fun getInGameUUIDIgnoreCase(currentUsername: String): UUID? =
        pool.query(
            "SELECT $fieldInGameUuid FROM $tableName WHERE LOWER($fieldCurrentUsernameLowerCase) = ? LIMIT 1",
            { setString(1, currentUsername.lowercase()) }
        ) { bytesToUuid(getBytes(1)) }

    /**
     * 查询数据是否存在
     */
    @Throws(SQLException::class)
    fun dataExists(inGameUUID: UUID): Boolean =
        pool.query(
            "SELECT 1 FROM $tableName WHERE $fieldInGameUuid = ? LIMIT 1",
            { setBytes(1, uuidToBytes(inGameUUID)) }
        ) { true } ?: false

    /**
     * 获得游戏内名字
     */
    @Throws(SQLException::class)
    fun getUsername(inGameUUID: UUID): String? =
        pool.query(
            "SELECT $fieldCurrentUsernameOriginal FROM $tableName WHERE $fieldInGameUuid = ? LIMIT 1",
            { setBytes(1, uuidToBytes(inGameUUID)) }
        ) { getString(1) }

    /**
     * 更新用户名
     */
    @Throws(SQLException::class)
    fun updateUsername(inGameUUID: UUID, currentUsername: String) {
        pool.update(
            "UPDATE $tableName SET $fieldCurrentUsernameLowerCase = ?, $fieldCurrentUsernameOriginal = ? WHERE $fieldInGameUuid = ?"
        ) {
            setString(1, currentUsername.lowercase())
            setString(2, currentUsername)
            setBytes(3, uuidToBytes(inGameUUID))
        }
    }

    /**
     * 插入一条新的数据
     */
    @Throws(SQLException::class)
    fun insertNewData(inGameUUID: UUID, currentUsername: String) {
        pool.updateWithCommit(
            "INSERT INTO $tableName ($fieldInGameUuid, $fieldCurrentUsernameLowerCase, $fieldCurrentUsernameOriginal) VALUES (?, ?, ?)"
        ) {
            setBytes(1, uuidToBytes(inGameUUID))
            setString(2, currentUsername.lowercase(Locale.getDefault()))
            setString(3, currentUsername)
        }
    }

    @Throws(SQLException::class)
    fun remove(uuid: UUID): Boolean =
        pool.update(
            "DELETE FROM $tableName WHERE $fieldInGameUuid = ?",
            { setBytes(1, uuidToBytes(uuid)) }
        ) == 1

    /**
     * 擦除用户名使用记录
     */
    @Throws(SQLException::class)
    fun eraseUsername(currentUsername: String): Int =
        pool.update(
            "UPDATE $tableName SET $fieldCurrentUsernameLowerCase = ?, $fieldCurrentUsernameOriginal = ? WHERE LOWER($fieldCurrentUsernameLowerCase) = ?"
        ) {
            setString(1, null)
            setString(2, null)
            setString(3, currentUsername.lowercase())
        }

    @Throws(SQLException::class)
    fun eraseAllUsername(): Int =
        pool.update(
            "UPDATE $tableName SET $fieldCurrentUsernameLowerCase = ?, $fieldCurrentUsernameOriginal = ?"
        ) {
            setString(1, null)
            setString(2, null)
        }

    companion object {
        private const val fieldInGameUuid = "in_game_uuid"
        private const val fieldCurrentUsernameLowerCase = "current_username_lower_case"
        private const val fieldCurrentUsernameOriginal = "current_username_original"
    }
}
