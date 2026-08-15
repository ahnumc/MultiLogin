package moe.caa.multilogin.core.auth.validate.entry

import moe.caa.multilogin.core.auth.validate.ValidateContext
import moe.caa.multilogin.core.main.MultiCore
import moe.caa.multilogin.flows.workflows.BaseFlows
import moe.caa.multilogin.flows.workflows.Signal
import java.sql.SQLIntegrityConstraintViolationException
import java.util.UUID

internal enum class SecureIdentityMismatch { UUID, NAME }

internal fun secureIdentityMismatch(
    onlineUUID: UUID,
    mappedUUID: UUID?,
    onlineName: String,
    mappedName: String?
): SecureIdentityMismatch? = when {
    mappedUUID != null && mappedUUID != onlineUUID -> SecureIdentityMismatch.UUID
    !mappedName.isNullOrEmpty() && mappedName != onlineName -> SecureIdentityMismatch.NAME
    else -> null
}

class AssignInGameFlows(private val core: MultiCore) : BaseFlows<ValidateContext?>() {
    @Throws(Exception::class)
    override fun run(context: ValidateContext?): Signal {
        val ctx = requireNotNull(context)
        val authResult = ctx.baseServiceAuthenticationResult
        val serviceConfig = requireNotNull(authResult.serviceConfig)
        val response = requireNotNull(authResult.response)
        val onlineUUID = requireNotNull(response.id)
        val loginName = response.name

        val mappedUUID = core.sqlManager.userDataTable.getInGameUUID(onlineUUID, serviceConfig.serviceId)
        if (secureIdentityMismatch(onlineUUID, mappedUUID, loginName, null) == SecureIdentityMismatch.UUID) {
            ctx.setDisallowMessage(
                core.languageHandler.getMessage(
                    "auth_validate_failed_uuid_mismatch",
                    "online_uuid" to onlineUUID.toString(),
                    "mapped_uuid" to mappedUUID.toString()
                )
            )
            return Signal.TERMINATED
        }

        if (mappedUUID == null) {
            core.sqlManager.userDataTable.setInGameUUID(onlineUUID, serviceConfig.serviceId, onlineUUID)
        }

        val exist = core.sqlManager.inGameProfileTable.dataExists(onlineUUID)
        if (exist) {
            val username = core.sqlManager.inGameProfileTable.getUsername(onlineUUID)
            if (secureIdentityMismatch(onlineUUID, mappedUUID, loginName, username) == SecureIdentityMismatch.NAME) {
                ctx.setDisallowMessage(
                    core.languageHandler.getMessage(
                        "auth_validate_failed_profile_name_mismatch",
                        "online_name" to loginName,
                        "mapped_name" to username
                    )
                )
                return Signal.TERMINATED
            }
            ctx.inGameProfile.id = onlineUUID
            ctx.inGameProfile.name = loginName
            return Signal.PASSED
        }

        return try {
            core.sqlManager.inGameProfileTable.insertNewData(onlineUUID, loginName)
            ctx.inGameProfile.id = onlineUUID
            ctx.inGameProfile.name = loginName
            Signal.PASSED
        } catch (_: SQLIntegrityConstraintViolationException) {
            ctx.setDisallowMessage(
                core.languageHandler.getMessage(
                    "auth_validate_failed_username_repeated",
                    "name" to loginName
                )
            )
            Signal.TERMINATED
        }
    }
}
