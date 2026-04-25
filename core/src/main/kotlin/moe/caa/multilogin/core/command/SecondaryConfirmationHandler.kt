package moe.caa.multilogin.core.command

import moe.caa.multilogin.api.internal.plugin.IPlayer
import moe.caa.multilogin.api.internal.plugin.ISender
import moe.caa.multilogin.api.internal.util.Pair
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * 二次确认快处工具
 */
class SecondaryConfirmationHandler {
    private val concurrentHashMap: MutableMap<IPlayer, ConfirmEntry> = ConcurrentHashMap()
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
        val currentCore = requireNotNull(CommandHandler.core)
        when {
            sender.isPlayer -> concurrentHashMap[requireNotNull(sender.asPlayer)] = ConfirmEntry(callbackConfirmCommand)
            sender.isConsole -> consoleConfirm.set(ConfirmEntry(callbackConfirmCommand))
            else -> {
                sender.sendMessagePL(currentCore.languageHandler.getMessage("command_message_confirm_unidentified"))
                return
            }
        }

        sender.sendMessagePL(
            currentCore.languageHandler.getMessage(
                "command_message_confirm_warning",
                Pair<Any?, Any?>("desc", desc),
                Pair<Any?, Any?>("consequences", consequences)
            )
        )
    }

    /**
     * 对风险指令进行确认
     */
    @Throws(Exception::class)
    fun confirm(sender: ISender) {
        val currentCore = requireNotNull(CommandHandler.core)
        concurrentHashMap.values.removeIf { it.isInvalid }
        consoleConfirm.updateAndGet { it?.takeUnless { e -> e.isInvalid } }

        when {
            sender.isPlayer -> {
                val player = requireNotNull(sender.asPlayer)
                val entry = concurrentHashMap.remove(player) ?: run {
                    sender.sendMessagePL(currentCore.languageHandler.getMessage("command_message_confirm_not_found"))
                    return
                }
                entry.confirm()
            }
            sender.isConsole -> {
                val entry = consoleConfirm.getAndSet(null)
                if (entry == null) {
                    sender.sendMessagePL(currentCore.languageHandler.getMessage("command_message_confirm_not_found"))
                    return
                }
                entry.confirm()
            }
            else -> sender.sendMessagePL(currentCore.languageHandler.getMessage("command_message_confirm_unidentified"))
        }
    }

    interface CallbackConfirmCommand {
        @Throws(Exception::class)
        fun confirm()
    }

    private class ConfirmEntry(private val callbackConfirmCommand: CallbackConfirmCommand) {
        private val subTime: Long = System.currentTimeMillis()

        val isInvalid: Boolean
            get() = subTime + requireNotNull(CommandHandler.core).pluginConfig
                .confirmCommandValidTimeMills < System.currentTimeMillis()

        @Throws(Exception::class)
        fun confirm() = callbackConfirmCommand.confirm()
    }
}
