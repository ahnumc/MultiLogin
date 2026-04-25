package moe.caa.multilogin.core.auth.validate

import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.core.auth.service.BaseServiceAuthenticationResult
import moe.caa.multilogin.core.auth.validate.entry.AssignInGameFlows
import moe.caa.multilogin.core.auth.validate.entry.InitialLoginDataFlows
import moe.caa.multilogin.core.auth.validate.entry.NameAllowedRegularCheckFlows
import moe.caa.multilogin.core.auth.validate.entry.WhitelistCheckFlows
import moe.caa.multilogin.core.main.MultiCore
import moe.caa.multilogin.flows.workflows.SequenceFlows
import moe.caa.multilogin.flows.workflows.Signal

class ValidateAuthenticationService(private val core: MultiCore) {
    private val sequenceFlows = SequenceFlows<ValidateContext?>(
        listOf(
            InitialLoginDataFlows(core),
            NameAllowedRegularCheckFlows(core),
            WhitelistCheckFlows(core),
            AssignInGameFlows(core)
        )
    )

    /**
     * 开始游戏内验证
     */
    fun checkIn(baseServiceAuthenticationResult: BaseServiceAuthenticationResult): ValidateAuthenticationResult {
        val context = ValidateContext(baseServiceAuthenticationResult)
        val run = sequenceFlows.run(context)
        if (run == Signal.PASSED) {
            if (context.needWait) {
                try {
                    Thread.sleep(500)
                } catch (e: InterruptedException) {
                    LoggerProvider.logger.debug(e)
                }
            }
            return ValidateAuthenticationResult.ofAllowed(context.inGameProfile)
        }
        return ValidateAuthenticationResult.ofDisallowed(context.disallowMessage)
    }
}
