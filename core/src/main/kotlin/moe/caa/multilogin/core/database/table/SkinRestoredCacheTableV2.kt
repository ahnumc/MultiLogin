package moe.caa.multilogin.core.database.table

import moe.caa.multilogin.core.database.SQLManager
import moe.caa.multilogin.core.database.query
import moe.caa.multilogin.core.database.update
import java.sql.Connection
import java.sql.SQLException
import java.text.MessageFormat

/**
 * 皮肤修复缓存表
 */
class SkinRestoredCacheTableV2(private val sqlManager: SQLManager, private val tableName: String?) {
    private val pool
        get() = requireNotNull(sqlManager.pool)

    @Throws(SQLException::class)
    fun init(connection: Connection) {
        val sql = MessageFormat.format(
            "CREATE TABLE IF NOT EXISTS {0} ( " +
                    "{1} BINARY(32) NOT NULL, " +
                    "{2} VARCHAR(16) NOT NULL, " +
                    "{3} LONGTEXT NOT NULL, " +
                    "{4} LONGTEXT NOT NULL, " +
                    "PRIMARY KEY ( {1}, {2} ))",
            tableName, fieldCurrentSkinUrlSha256, fieldCurrentSkinModel, fieldRestorerValue, fieldRestorerSignature
        )
        connection.prepareStatement(sql).use { it.executeUpdate() }
    }

    /**
     * 获得缓存的数据对象
     */
    @Throws(SQLException::class)
    fun getCacheRestored(urlSha256: ByteArray, model: String): Pair<String?, String?>? =
        pool.query(
            "SELECT $fieldRestorerValue, $fieldRestorerSignature FROM $tableName WHERE $fieldCurrentSkinUrlSha256 = ? AND $fieldCurrentSkinModel = ? LIMIT 1",
            { setBytes(1, urlSha256); setString(2, model) }
        ) { Pair(getString(1), getString(2)) }

    /**
     * 插入新的缓存对象
     */
    @Throws(SQLException::class)
    fun insertNew(urlSha256: ByteArray, model: String, value: String?, signature: String?) {
        pool.update(
            "INSERT INTO $tableName ($fieldCurrentSkinUrlSha256, $fieldCurrentSkinModel, $fieldRestorerValue, $fieldRestorerSignature) VALUES (?, ?, ?, ?) "
        ) {
            setBytes(1, urlSha256)
            setString(2, model)
            setString(3, value)
            setString(4, signature)
        }
    }

    companion object {
        private const val fieldCurrentSkinUrlSha256 = "current_skin_url_sha256"
        private const val fieldCurrentSkinModel = "current_skin_model"
        private const val fieldRestorerValue = "restorer_value"
        private const val fieldRestorerSignature = "restorer_signature"
    }
}
