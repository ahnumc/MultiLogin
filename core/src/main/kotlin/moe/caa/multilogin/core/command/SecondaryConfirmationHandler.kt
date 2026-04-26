package moe.caa.multilogin.core.command

import moe.caa.multilogin.api.internal.plugin.IPlayer
import moe.caa.multilogin.api.internal.plugin.ISender
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * 二次确认快处工具
 */
class SecondaryConfirmationHandler {
    private val playerConfirmations: MutableMap<IPlayer, ConfirmEntry> = ConcurrentHashMap()
    private val consoleConfirm: AtomicReference<ConfirmEntry?> = AtomicReference()

    /**
     * 提交一个风险指令
     */
    fun submit(
        sender: ISender,
        callbackConfirmCommand: CallbackConfirmCommand,
        desc: String,
        consequences: String
    ) {
        val currentCore = CommandHandler.core
        when {
            sender.isPlayer -> playerConfirmations[requireNotNull(sender.asPlayer)] =
                ConfirmEntry(callbackConfirmCommand)
            sender.isConsole -> consoleConfirm.set(ConfirmEntry(callbackConfirmCommand))
            else -> {
                sender.sendMessagePL(currentCore.languageHandler.getMessage("command_message_confirm_unidentified"))
                return
            }
        }

        sender.sendMessagePL(
            currentCore.languageHandler.getMessage(
                "command_message_confirm_warning",
                "desc" to desc,
                "consequences" to consequences
            )
        )
    }

    /**
     * 对风险指令进行确认
     */
    @Throws(Exception::class)
    fun confirm(sender: ISender) {
        val currentCore = CommandHandler.core
        playerConfirmations.values.removeIf(ConfirmEntry::isInvalid)
        consoleConfirm.updateAndGet { it?.takeUnless(ConfirmEntry::isInvalid) }

        when {
            sender.isPlayer -> {
                val player = requireNotNull(sender.asPlayer)
                val entry = playerConfirmations.remove(player) ?: run {
                    sender.sendMessagePL(currentCore.languageHandler.getMessage("command_message_confirm_not_found"))
                    return
                }
                entry.confirm()
            }
            sender.isConsole -> {
                val entry = consoleConfirm.getAndSet(null) ?: run {
                    sender.sendMessagePL(currentCore.languageHandler.getMessage("command_message_confirm_not_found"))
                    return
                }
                entry.confirm()
            }
            else -> sender.sendMessagePL(currentCore.languageHandler.getMessage("command_message_confirm_unidentified"))
        }
    }

    fun interface CallbackConfirmCommand {
        @Throws(Exception::class)
        fun confirm()
    }

    private class ConfirmEntry(private val callbackConfirmCommand: CallbackConfirmCommand) {
        private val submitTimeMillis = System.currentTimeMillis()

        val isInvalid: Boolean
            get() = submitTimeMillis + CommandHandler.core.pluginConfig
                .confirmCommandValidTimeMills < System.currentTimeMillis()

        @Throws(Exception::class)
        fun confirm() = callbackConfirmCommand.confirm()
    }
}
