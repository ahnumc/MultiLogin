package moe.caa.multilogin.api.internal.main

import moe.caa.multilogin.api.MapperConfigAPI
import moe.caa.multilogin.api.internal.auth.AuthAPI
import moe.caa.multilogin.api.internal.command.CommandAPI
import moe.caa.multilogin.api.internal.handle.HandlerAPI
import moe.caa.multilogin.api.internal.language.LanguageAPI
import moe.caa.multilogin.api.internal.plugin.IPlugin
import moe.caa.multilogin.api.internal.skinrestorer.SkinRestorerAPI
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface MultiCoreAPI {
    @Throws(Exception::class)
    fun load()

    @Throws(Exception::class)
    fun close()

    val commandHandler: CommandAPI
    val languageHandler: LanguageAPI
    val authHandler: AuthAPI
    val skinRestorerHandler: SkinRestorerAPI
    val playerHandler: HandlerAPI
    val mapperConfig: MapperConfigAPI
    val plugin: IPlugin
}
