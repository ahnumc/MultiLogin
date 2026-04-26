package moe.caa.multilogin.core.main

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import moe.caa.multilogin.api.internal.logger.LoggerProvider
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.*
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.*
import java.util.zip.GZIPOutputStream
import javax.net.ssl.HttpsURLConnection

/**
 * 高度定制的bStats
 */
class MetricsLite(//插件
    private val core: MultiCore
) {
    //bStats是否启用
    private var enabled = false

    //服务器uuid
    private lateinit var serverUUID: String

    init {
        loadConfig()
        if (enabled) {
            startSubmitting()
        }
    }

    @Throws(IOException::class)
    private fun loadConfig() {
        val bStatsFolder = File(core.plugin.dataFolder.parentFile, "bStats")
        if (!bStatsFolder.exists()) {
            Files.createDirectories(bStatsFolder.toPath())
        }
        val configFile = File(bStatsFolder, "config.yml")
        val uuid: UUID?
        if (!configFile.exists()) {
            uuid = UUID.randomUUID()
            writeFile(
                configFile,
                "#bStats collects some data for plugin authors like how many servers are using their plugins.",
                "#To honor their work, you should not disable it.",
                "#This has nearly no effect on the server performance!",
                "#Check out https://bStats.org/ to learn more :)",
                "enabled: true",
                "serverUuid: \"" + uuid + "\"",
                "logFailedRequests: false",
                "logSentData: false",
                "logResponseStatusText: false"
            )
        }
        // Load configuration
        val load = YamlConfigurationLoader.builder().file(configFile).build().load()
        enabled = load.node("enabled").getBoolean(true)
        serverUUID = requireNotNull(load.node("serverUuid").getString())
    }

    //    配置文件写入
    @Throws(IOException::class)
    private fun writeFile(file: File, vararg lines: String) {
        BufferedWriter(FileWriter(file)).use { bufferedWriter ->
            for (line in lines) {
                bufferedWriter.write(line)
                bufferedWriter.newLine()
            }
        }
    }

    private fun startSubmitting() {
        //周期什么的不要动 会被封禁
        val initialDelay = (1000 * 60 * 4).toLong()
        val secondDelay = (1000 * 60 * 10).toLong()
        val scheduler = core.plugin.runServer.scheduler
        scheduler.runTaskAsync({ this.submitData() }, initialDelay)
        scheduler.runTaskAsyncTimer(
            { this.submitData() },
            initialDelay + secondDelay,
            1000 * 60 * 30
        )
    }


    val pluginData: JsonObject
        //    获取插件数据
        get() {
            val data = JsonObject()

            val pluginVersion = core.buildManifest.version

            data.addProperty("pluginName", "MultiLogin") // Append the name of the plugin
            data.addProperty("id", 21890) // Append the id of the plugin
            data.addProperty("pluginVersion", pluginVersion) // Append the version of the plugin

            val elements = JsonArray()
            val `object` = JsonObject()
            `object`.addProperty("chartId", "service_number")
            val d = JsonObject()
            d.addProperty("value", core.pluginConfig.serviceIdMap.size.toString())
            `object`.add("data", d)
            elements.add(`object`)
            data.add("customCharts", elements)

            return data
        }

    private val serverData: JsonObject
        //    获取服务器信息
        get() {
            // Minecraft 数据
            val runServer = core.plugin.runServer
            val playerAmount: Int = runServer.playerManager.onlinePlayers.size
            val serverVersion: String = runServer.version
            val serverCoreName: String = runServer.name

            // OS/Java 数据
            val javaVersion = System.getProperty("java.version")
            val osName = System.getProperty("os.name")
            val osArch = System.getProperty("os.arch")
            val osVersion = System.getProperty("os.version")
            val coreCount = Runtime.getRuntime().availableProcessors()

            val data = JsonObject()

            data.addProperty("serverUUID", serverUUID)

            data.addProperty("playerAmount", playerAmount)
            data.addProperty("onlineMode", 1)
            data.addProperty("bukkitVersion", serverVersion)
            data.addProperty("bukkitName", serverCoreName)

            data.addProperty("javaVersion", javaVersion)
            data.addProperty("osName", osName)
            data.addProperty("osArch", osArch)
            data.addProperty("osVersion", osVersion)
            data.addProperty("coreCount", coreCount)

            return data
        }

    //    提交数据
    private fun submitData() {
        try {
            senDataWithRetry()
        } catch (e: Exception) {
            LoggerProvider.logger.debug("bStats submit error", e)
        }
    }

    //    五次重试
    @Throws(Exception::class)
    private fun senDataWithRetry() {
        var thr: Exception? = null
        for (i in 0..4) {
            try {
                if (sendData()) return
            } catch (e: Exception) {
                thr = e
            }
        }
        throw thr ?: IOException("unknown")
    }

    //发信
    @Throws(Exception::class)
    private fun sendData(): Boolean {
        val data = this.serverData

        val pluginData = JsonArray()
        pluginData.add(this.pluginData)

        data.add("plugins", pluginData)
        val connection = URL("https://bStats.org/submitData/bukkit").openConnection() as HttpsURLConnection

        // 压缩数据发送
        val compressedData = compress(data.toString())

        // 添加浏览器头信息
        connection.setRequestMethod("POST")
        connection.addRequestProperty("Accept", "application/json")
        connection.addRequestProperty("Connection", "close")
        connection.addRequestProperty("Content-Encoding", "gzip")
        connection.addRequestProperty("Content-Length", compressedData.size.toString())
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("User-Agent", "MC-Server/1") //1是bStats版本

        // 发送数据
        connection.setDoOutput(true)
        connection.setDoInput(true)
        DataOutputStream(connection.getOutputStream()).use { outputStream ->
            outputStream.write(compressedData)
        }
        //        这里似乎必须这样
        Thread.sleep(1000)
        DataInputStream(connection.getInputStream()).use { inputStream ->
            val bytes = ByteArray(inputStream.available())
            inputStream.read(bytes)
            val s = String(bytes)
            if (s.isNotEmpty()) {
                LoggerProvider.logger.debug("bStats receive: " + s)
            }
        }
        return true
    }

    //gzip压缩 数据传输需要
    @Throws(IOException::class)
    private fun compress(str: String?): ByteArray {
        if (str == null) {
            return ByteArray(0)
        }
        val outputStream = ByteArrayOutputStream()
        GZIPOutputStream(outputStream).use { gzip ->
            gzip.write(str.toByteArray(StandardCharsets.UTF_8))
        }
        return outputStream.toByteArray()
    }
}
