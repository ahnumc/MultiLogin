package moe.caa.multilogin.core.auth.validate.entry

import moe.caa.multilogin.core.auth.validate.ValidateContext
import moe.caa.multilogin.core.main.MultiCore
import moe.caa.multilogin.flows.workflows.BaseFlows
import moe.caa.multilogin.flows.workflows.Signal
import java.util.*

class WhitelistCheckFlows(private val core: MultiCore) : BaseFlows<ValidateContext?>() {
    @Throws(Exception::class)
    override fun run(context: ValidateContext?): Signal {
        val ctx = requireNotNull(context)
        val authResult = ctx.baseServiceAuthenticationResult
        val response = requireNotNull(authResult.response)
        val serviceConfig = requireNotNull(authResult.serviceConfig)
        val onlineUUID = requireNotNull(response.id)
        val dataTable = core.sqlManager.userDataTable

        val removed = core.cacheWhitelistHandler.cachedWhitelist.remove(
            response.name?.lowercase(Locale.ROOT)
        )
        if (removed) {
            dataTable.setWhitelist(onlineUUID, serviceConfig.serviceId, true)
        }
        if (!serviceConfig.whitelist) return Signal.PASSED
        if (dataTable.hasWhitelist(onlineUUID, serviceConfig.serviceId)) return Signal.PASSED
        ctx.setDisallowMessage(core.languageHandler.getMessage("auth_validate_failed_no_whitelist"))
        return Signal.TERMINATED
    }
}
