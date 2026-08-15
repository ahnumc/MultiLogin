package moe.caa.multilogin.core.configuration

import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.logger.bridges.DebugLoggerBridge.Companion.cancelDebugMode
import moe.caa.multilogin.api.internal.logger.bridges.DebugLoggerBridge.Companion.startDebugMode
import moe.caa.multilogin.api.internal.util.IOUtil.copy
import moe.caa.multilogin.api.internal.util.IOUtil.removeAllFiles
import moe.caa.multilogin.api.service.ServiceType
import moe.caa.multilogin.core.configuration.service.BaseServiceConfig
import moe.caa.multilogin.core.configuration.service.BaseServiceConfig.InitUUID
import moe.caa.multilogin.core.configuration.service.yggdrasil.BaseYggdrasilServiceConfig.HttpRequestMethod
import moe.caa.multilogin.core.configuration.service.yggdrasil.BlessingSkinYggdrasilServiceConfig
import moe.caa.multilogin.core.configuration.service.yggdrasil.CustomYggdrasilServiceConfig
import moe.caa.multilogin.core.configuration.service.yggdrasil.OfficialYggdrasilServiceConfig
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.serialize.SerializationException
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URISyntaxException
import java.nio.file.Files
import java.util.jar.JarFile

/**
 * 表示插件配置处理程序
 */
class PluginConfig(private val dataFolder: File) {
    var forceUseLogin = false
        private set

    var checkUpdate = false
        private set

    var sqlConfig: SqlConfig? = null
        private set

    var nameAllowedRegular: String? = null
        private set

    var welcomeMsg = false
        private set

    var serviceIdMap: Map<Int, BaseServiceConfig> = emptyMap()
        private set

    var confirmCommandValidTimeMills: Long = 0
        private set

    @Throws(IOException::class, URISyntaxException::class)
    fun reload() {
        val servicesFolder = File(dataFolder, "services")
        if (!dataFolder.exists()) Files.createDirectory(dataFolder.toPath())
        if (!servicesFolder.exists()) Files.createDirectory(servicesFolder.toPath())

        removeAllFiles(File(dataFolder, "examples"))
        saveResource("config.yml", false)
        saveResourceDir("examples", true)

        val configConfigurationNode =
            YamlConfigurationLoader.builder().file(File(dataFolder, "config.yml")).build().load()

        if (configConfigurationNode.node("debug").getBoolean(false)) startDebugMode() else cancelDebugMode()

        forceUseLogin = configConfigurationNode.node("forceUseLogin").getBoolean(true)
        checkUpdate = configConfigurationNode.node("checkUpdate").getBoolean(true)
        sqlConfig = SqlConfig.read(configConfigurationNode.node("sql"))
        nameAllowedRegular = configConfigurationNode.node("nameAllowedRegular").getString("^[0-9a-zA-Z_]{3,16}$")
        welcomeMsg = configConfigurationNode.node("welcomeMsg").getBoolean(true)
        confirmCommandValidTimeMills = configConfigurationNode.node("confirmCommandValidTimeMills").getLong(15000)

        val idMap = mutableMapOf<Int, BaseServiceConfig>()
        Files.list(servicesFolder.toPath()).use { list ->
            val tmp = list.toList()
                .filter { it.toFile().name.lowercase().endsWith(".yml") }
                .mapNotNull { path ->
                    try {
                        readServiceConfig(YamlConfigurationLoader.builder().path(path).build().load())
                    } catch (e: Exception) {
                        LoggerProvider.logger.error(
                            ConfException(
                                "Unable to read authentication service config under file $path",
                                e
                            )
                        )
                        null
                    }
                }

            val seen = mutableSetOf<ServiceType>()
            for (config in tmp) {
                onlyOneServiceInfoMap[config.serviceType]?.let { typeName ->
                    if (!seen.add(config.serviceType))
                        throw ConfException("Duplicates are not allowed for authentication services of type $typeName, but more than one was found.")
                }
            }

            for (config in tmp) {
                if (config.serviceId in idMap)
                    throw ConfException("The same authentication service id value ${config.serviceId} exists.")
                idMap[config.serviceId] = config
            }

        }

        idMap.forEach { (id, serviceConfig) ->
            if (serviceConfig.serviceName.equals("unnamed", ignoreCase = true)) {
                LoggerProvider.logger.warn("The name of authentication service whose id is $id has not been set.")
            }
            LoggerProvider.logger.info("Add a authentication service with id $id and name ${serviceConfig.serviceName}.")
        }

        if (idMap.isEmpty()) LoggerProvider.logger.warn(
            "The server has not added any authentication service, which will prevent all players from logging in."
        ) else LoggerProvider.logger.info("Added ${idMap.size} authentication services.")

        this.serviceIdMap = idMap.toMap()
    }

    @Throws(SerializationException::class, ConfException::class)
    private fun readServiceConfig(load: CommentedConfigurationNode): BaseServiceConfig {
        val nodeId = load.node("id")
        if (nodeId.empty()) throw ConfException("service id is null.")
        val id = nodeId.getInt()
        val name = load.node("name").getString("Unnamed")
        val serviceType = load.node("serviceType").get(ServiceType::class.java)
            ?: throw ConfException("service type is null.")

        val initUUID = load.node("initUUID").get(InitUUID::class.java, InitUUID.DEFAULT)
        val whitelist = load.node("whitelist").getBoolean(false)
        val skinRestorer: SkinRestorerConfig = SkinRestorerConfig.read(load.node("skinRestorer"))
        val initNameFormat = load.node("initNameFormat").getString("{name}")

        if (!serviceType.isYggdrasilService) {
            throw ConfException("Only Yggdrasil authentication services are supported.")
        }
        if (initUUID != InitUUID.DEFAULT || initNameFormat != "{name}") {
            throw ConfException("Secure profiles require initUUID DEFAULT and initNameFormat {name}.")
        }

        if (serviceType.isYggdrasilService) {
            val yggdrasilAuthNode = load.node("yggdrasilAuth")
            val trackIp = yggdrasilAuthNode.node("trackIp").getBoolean(false)
            val timeout = yggdrasilAuthNode.node("timeout").getInt(10000)
            val retry = yggdrasilAuthNode.node("retry").getInt(0)
            val retryDelay = yggdrasilAuthNode.node("retryDelay").getLong(0L)
            val authProxy: ProxyConfig = ProxyConfig.read(yggdrasilAuthNode.node("authProxy"))

            return when (serviceType) {
                ServiceType.OFFICIAL -> OfficialYggdrasilServiceConfig(
                    id, name, initUUID, initNameFormat, whitelist, skinRestorer, trackIp, timeout, retry, retryDelay,
                    authProxy, yggdrasilAuthNode.node("official").node("sessionServer")
                        .getString("https://sessionserver.mojang.com")
                )

                ServiceType.BLESSING_SKIN -> BlessingSkinYggdrasilServiceConfig(
                    id, name, initUUID, initNameFormat, whitelist, skinRestorer, trackIp, timeout, retry, retryDelay,
                    authProxy, yggdrasilAuthNode.node("blessingSkin").node("apiRoot").getString() ?: ""
                )

                ServiceType.CUSTOM_YGGDRASIL -> {
                    val customNode = yggdrasilAuthNode.node("custom")
                    CustomYggdrasilServiceConfig(
                        id,
                        name,
                        initUUID,
                        initNameFormat,
                        whitelist,
                        skinRestorer,
                        trackIp,
                        timeout,
                        retry,
                        retryDelay,
                        authProxy,
                        customNode.node("url").getString(),
                        customNode.node("postContent").getString(),
                        customNode.node("trackIpContent").getString(),
                        customNode.node("method").get(HttpRequestMethod::class.java, HttpRequestMethod.GET)
                    )
                }
            }
        }

        throw ConfException("Unknown service type ${serviceType.name}")
    }

    @Throws(IOException::class)
    fun saveResource(path: String, cover: Boolean) {
        saveResource(cover, dataFolder, path, path)
    }

    @Throws(IOException::class, URISyntaxException::class)
    fun saveResourceDir(path: String, cover: Boolean) {
        val file = File(dataFolder, path)
        if (!file.exists()) Files.createDirectory(file.toPath())
        JarFile(File(javaClass.protectionDomain.codeSource.location.toURI())).use { jarFile ->
            jarFile.entries().asSequence()
                .filter { it.realName.startsWith(path) && it.realName != "$path/" }
                .forEach { je ->
                    val realName = je.realName
                    saveResource(cover, file, realName, realName.substring(path.length))
                }
        }
    }

    @Throws(IOException::class)
    private fun saveResource(cover: Boolean, file: File?, realName: String?, fileName: String) {
        val subFile = File(file, fileName)
        val exists = subFile.exists()
        if (exists && !cover) return
        if (!exists) Files.createFile(subFile.toPath())
        (javaClass.getResourceAsStream("/$realName") ?: error("Resource not found: /$realName")).use { `is` ->
            FileOutputStream(subFile).use { fs -> copy(`is`, fs) }
        }
        LoggerProvider.logger.info(if (!exists) "Extract: $realName" else "Cover: $realName")
    }

    companion object {
        private val onlyOneServiceInfoMap: Map<ServiceType, String> = mapOf(
            ServiceType.OFFICIAL to "official"
        )
    }
}
