package moe.caa.multilogin.core.auth.validate.entry

import moe.caa.multilogin.core.auth.validate.ValidateContext
import moe.caa.multilogin.core.main.MultiCore
import moe.caa.multilogin.flows.workflows.BaseFlows
import moe.caa.multilogin.flows.workflows.Signal

class InitialLoginDataFlows(private val core: MultiCore) : BaseFlows<ValidateContext?>() {
    @Throws(Exception::class)
    override fun run(context: ValidateContext?): Signal {
        val ctx = requireNotNull(context)
        val authResult = ctx.baseServiceAuthenticationResult
        val response = requireNotNull(authResult.response)
        val serviceConfig = requireNotNull(authResult.serviceConfig)
        val onlineUUID = requireNotNull(response.id)
        val dataTable = core.sqlManager.userDataTable
        if (!dataTable.dataExists(onlineUUID, serviceConfig.serviceId)) {
            dataTable.insertNewData(onlineUUID, serviceConfig.serviceId, response.name, null)
        } else {
            val currentName = dataTable.getOnlineName(onlineUUID, serviceConfig.serviceId)
            if (response.name != currentName) {
                dataTable.setOnlineName(onlineUUID, serviceConfig.serviceId, response.name)
                ctx.setOnlineNameUpdated(true)
            }
        }
        return Signal.PASSED
    }
}
