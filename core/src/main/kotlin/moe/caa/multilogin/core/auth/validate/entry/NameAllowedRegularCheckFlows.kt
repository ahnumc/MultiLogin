package moe.caa.multilogin.core.auth.validate.entry

import moe.caa.multilogin.core.auth.validate.ValidateContext
import moe.caa.multilogin.core.main.MultiCore
import moe.caa.multilogin.flows.workflows.BaseFlows
import moe.caa.multilogin.flows.workflows.Signal
import java.util.regex.Pattern

/**
 * 玩家名字正则检查器
 */
class NameAllowedRegularCheckFlows(private val core: MultiCore) : BaseFlows<ValidateContext?>() {
    override fun run(context: ValidateContext?): Signal {
        val ctx = requireNotNull(context)
        val nameAllowedRegular = core.pluginConfig.nameAllowedRegular
        if (nameAllowedRegular.isNullOrEmpty()) return Signal.PASSED
        val name = requireNotNull(ctx.baseServiceAuthenticationResult.response).name
        if (!Pattern.matches(nameAllowedRegular, name)) {
            ctx.setDisallowMessage(
                core.languageHandler.getMessage(
                    "auth_validate_failed_username_mismatch",
                    "name" to name,
                    "regular" to nameAllowedRegular
                )
            )
            return Signal.TERMINATED
        }
        return Signal.PASSED
    }
}
