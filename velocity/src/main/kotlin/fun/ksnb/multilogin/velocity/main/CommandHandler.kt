package `fun`.ksnb.multilogin.velocity.main

import com.velocitypowered.api.command.CommandManager
import com.velocitypowered.api.command.SimpleCommand
import `fun`.ksnb.multilogin.velocity.impl.VelocitySender

/**
 * Velocity 的指令处理程序
 */
class CommandHandler(private val multiLoginVelocity: MultiLoginVelocity) {
    private val coreApi: moe.caa.multilogin.api.internal.main.MultiCoreAPI
        get() = requireNotNull(multiLoginVelocity.multiCoreAPI)

    private val simpleCommand: SimpleCommand = object : SimpleCommand {
        override fun execute(invocation: SimpleCommand.Invocation) {
            val arguments = invocation.arguments()
            val ns = Array(arguments.size + 1) { "" }
            System.arraycopy(arguments, 0, ns, 1, arguments.size)
            ns[0] = invocation.alias()
            coreApi.commandHandler.execute(VelocitySender(invocation.source()), ns)
        }

        override fun suggest(invocation: SimpleCommand.Invocation): MutableList<String> {
            val arguments = invocation.arguments()
            val ns = Array(arguments.size + 1) { "" }
            System.arraycopy(arguments, 0, ns, 1, arguments.size)
            ns[0] = invocation.alias()
            return coreApi.commandHandler.tabComplete(
                VelocitySender(invocation.source()),
                ns
            )
        }
    }

    fun register(cmdName: String) {
        val commandManager: CommandManager = multiLoginVelocity.server.commandManager
        commandManager.register(commandManager.metaBuilder(cmdName).build(), simpleCommand)
    }
}
