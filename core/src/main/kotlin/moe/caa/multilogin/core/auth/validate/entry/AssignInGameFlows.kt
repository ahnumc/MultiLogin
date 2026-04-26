package moe.caa.multilogin.core.auth.validate.entry

import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.util.ValueUtil.isEmpty
import moe.caa.multilogin.core.auth.validate.ValidateContext
import moe.caa.multilogin.core.main.MultiCore
import moe.caa.multilogin.flows.workflows.BaseFlows
import moe.caa.multilogin.flows.workflows.Signal
import java.sql.SQLIntegrityConstraintViolationException
import java.util.*

class AssignInGameFlows(private val core: MultiCore) : BaseFlows<ValidateContext?>() {
    @Throws(Exception::class)
    override fun run(context: ValidateContext?): Signal {
        val ctx = requireNotNull(context)
        val authResult = ctx.baseServiceAuthenticationResult
        val serviceConfig = requireNotNull(authResult.serviceConfig)
        val response = requireNotNull(authResult.response)
        val onlineUUID = requireNotNull(response.id)

        var inGameUUID = core.sqlManager.userDataTable.getInGameUUID(onlineUUID, serviceConfig.serviceId)

        val loginName = response.name
        if (inGameUUID == null) {
            inGameUUID = requireNotNull(serviceConfig.initUUID).generateUUID(onlineUUID, loginName)

            synchronized(AssignInGameFlows::class.java) {
                while (core.sqlManager.inGameProfileTable.dataExists(requireNotNull(inGameUUID))) {
                    LoggerProvider.logger.warn("UUID $inGameUUID has been used and will take a random value.")
                    inGameUUID = UUID.randomUUID()
                }
                core.sqlManager.userDataTable.setInGameUUID(onlineUUID, serviceConfig.serviceId, inGameUUID)
            }
        }
        val resolvedInGameUUID = requireNotNull(inGameUUID)
        if (core.pluginConfig.autoNameChange && ctx.onlineNameUpdated) {
            val username = core.sqlManager.inGameProfileTable.getUsername(resolvedInGameUUID)
            username?.takeUnless(::isEmpty)?.let { core.sqlManager.inGameProfileTable.eraseUsername(it) }
        }

        val exist = core.sqlManager.inGameProfileTable.dataExists(resolvedInGameUUID)
        if (exist) {
            val username = core.sqlManager.inGameProfileTable.getUsername(resolvedInGameUUID)
            if (!isEmpty(username)) {
                ctx.inGameProfile.id = resolvedInGameUUID
                ctx.inGameProfile.name = username
                return Signal.PASSED
            }
        }

        var fixName = serviceConfig.generateName(loginName ?: "")
        if (fixName.isEmpty()) fixName = "1"

        val initFixName = fixName
        if (core.pluginConfig.nameCorrect) {
            var modified = false
            var ownerUUID: UUID?
            while ((core.sqlManager.inGameProfileTable.getInGameUUIDIgnoreCase(fixName)
                    .also { ownerUUID = it }) != null
            ) {
                if (ownerUUID == inGameUUID) break
                fixName = incrementString(fixName)
                modified = true
            }

            if (modified) {
                val finalFixName = fixName
                LoggerProvider.logger.warn("The name $initFixName is occupied, change it to $fixName.")
                core.plugin.runServer.scheduler.runTaskAsync({
                    val player = core.plugin.runServer.playerManager.getPlayer(resolvedInGameUUID)
                    player?.sendMessagePL(
                        core.languageHandler.getMessage(
                            "name_correct_info",
                            "old_name" to initFixName,
                            "new_name" to finalFixName
                        )
                    )
                }, 2000)
            }
        }

        if (exist) {
            try {
                core.sqlManager.inGameProfileTable.updateUsername(resolvedInGameUUID, fixName)
                ctx.inGameProfile.id = resolvedInGameUUID
                ctx.inGameProfile.name = fixName
                return Signal.PASSED
            } catch (_: SQLIntegrityConstraintViolationException) {
                ctx.setDisallowMessage(
                    core.languageHandler.getMessage(
                        "auth_validate_failed_username_repeated",
                        "name" to ctx.inGameProfile.name
                    )
                )
                return Signal.TERMINATED
            }
        } else {
            try {
                core.sqlManager.inGameProfileTable.insertNewData(resolvedInGameUUID, fixName)
                ctx.inGameProfile.id = resolvedInGameUUID
                ctx.inGameProfile.name = fixName
                return Signal.PASSED
            } catch (_: SQLIntegrityConstraintViolationException) {
                ctx.setDisallowMessage(
                    core.languageHandler.getMessage(
                        "auth_validate_failed_username_repeated",
                        "name" to ctx.inGameProfile.name
                    )
                )
                return Signal.TERMINATED
            }
        }
    }

    private fun incrementString(source: String): String {
        if (source.isEmpty()) return "1"
        val c = source[source.length - 1]
        if (Character.isDigit(c)) {
            val i = Character.getNumericValue(c)
            return if (i == 9) incrementString(source.substring(0, source.length - 1)) + "0"
            else source.substring(0, source.length - 1) + (i + 1)
        }
        return source + "1"
    }
}
