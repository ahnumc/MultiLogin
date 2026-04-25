package moe.caa.multilogin.core.auth.service.yggdrasil

import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.util.ValueUtil
import moe.caa.multilogin.core.configuration.service.yggdrasil.BaseYggdrasilServiceConfig
import moe.caa.multilogin.core.main.MultiCore
import moe.caa.multilogin.flows.workflows.EntrustFlows
import moe.caa.multilogin.flows.workflows.Signal
import java.sql.SQLException

/**
 * HasJoined 集中处理程序
 */
class YggdrasilAuthenticationService(private val core: MultiCore) {
    @Throws(SQLException::class)
    fun hasJoined(username: String?, serverId: String?, ip: String?): YggdrasilAuthenticationResult {
        val ids = core.pluginConfig.serviceIdMap.entries
            .filter { (_, v) -> v is BaseYggdrasilServiceConfig }
            .map { (k, _) -> k }
            .toSet()
        if (ids.isEmpty()) return YggdrasilAuthenticationResult.ofNoService()

        val primaries = mutableSetOf<Int?>()

        if (ids.size == 1) {
            primaries.add(ids.first())
        } else {
            core.sqlManager.inGameProfileTable!!.getInGameUUIDIgnoreCase(username ?: "")?.let {
                primaries.addAll(core.sqlManager.userDataTable!!.getOnlineServiceIds(it))
            }
        }

        val secondaries = ids.filter { it !in primaries }.toSet()

        LoggerProvider.logger.debug(
            "${username}'s hasJoined verification order: [${ValueUtil.join(", ", ", ", primaries)}], [${ValueUtil.join(", ", ", ", secondaries)}]"
        )

        var serverBreakdown = false
        if (primaries.isNotEmpty()) {
            val result = hasJoined0(username, serverId, ip, primaries)
            if (result.reason == YggdrasilAuthenticationResult.Reason.ALLOWED) return result
            if (result.reason == YggdrasilAuthenticationResult.Reason.SERVER_BREAKDOWN) serverBreakdown = true
        }
        if (secondaries.isNotEmpty()) {
            val result = hasJoined0(username, serverId, ip, secondaries)
            if (result.reason == YggdrasilAuthenticationResult.Reason.ALLOWED) return result
            if (result.reason == YggdrasilAuthenticationResult.Reason.SERVER_BREAKDOWN) serverBreakdown = true
        }
        return if (serverBreakdown) YggdrasilAuthenticationResult.ofServerBreakdown()
        else YggdrasilAuthenticationResult.ofValidationFailed()
    }

    private fun hasJoined0(
        username: String?,
        serverId: String?,
        ip: String?,
        ids: Set<Int?>
    ): YggdrasilAuthenticationResult {
        val resolvedUsername = requireNotNull(username)
        val resolvedServerId = requireNotNull(serverId)
        val serviceConfigs = ids.mapNotNull { core.pluginConfig.serviceIdMap[it] as? BaseYggdrasilServiceConfig }
        val flows = EntrustFlows<HasJoinedContext?>(
            serviceConfigs.map { YggdrasilAuthenticationFlows(core, resolvedUsername, resolvedServerId, ip, it) }
        )

        val context = HasJoinedContext(username, serverId, ip)
        val run = flows.run(context)
        if (run == Signal.PASSED) {
            val response = requireNotNull(context.response.get())
            return YggdrasilAuthenticationResult.ofAllowed(
                response.value1,
                response.value2
            )
        }
        if (context.serviceUnavailable.isNotEmpty()) {
            for ((key, value) in context.serviceUnavailable) {
                LoggerProvider.logger.debug(
                    "An exception occurred during authentication of the yggdrasil service whose ID is ${key?.serviceId ?: -1}",
                    value
                )
            }
            return YggdrasilAuthenticationResult.ofServerBreakdown()
        }
        return YggdrasilAuthenticationResult.ofValidationFailed()
    }
}
