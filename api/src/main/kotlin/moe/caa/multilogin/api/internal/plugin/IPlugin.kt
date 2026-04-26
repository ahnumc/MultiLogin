package moe.caa.multilogin.api.internal.plugin

import org.jetbrains.annotations.ApiStatus
import java.io.File

@ApiStatus.Internal
interface IPlugin {
    val dataFolder: File
    val tempFolder: File
    val runServer: IServer
}
