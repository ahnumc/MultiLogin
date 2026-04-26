package moe.caa.multilogin.core.configuration

import moe.caa.multilogin.api.MapperConfigAPI
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File
import java.util.*

/**
 * ChatSessionBlocker 数据包映射配置
 */
class MapperConfig internal constructor(private val dataFolder: File) : MapperConfigAPI {
    override val packetMapping: TreeMap<Int, Int> = object : TreeMap<Int, Int>() {
        override fun put(key: Int, value: Int): Int? {
            if (key < 761) return value
            if (value in this.values) {
                val existingKey = entries.firstOrNull { it.value == value }?.key
                if (existingKey != null && existingKey > key) {
                    super.remove(existingKey)
                    super.put(key, value)
                }
                return value
            }
            return super.put(key, value)
        }

        init {
            put(761, 0x20)
            put(762, 0x06)
            put(765, 0x07)
            put(768, 0x08)
            put(771, 0x09)
        }
    }

    override fun save() {
        try {
            val loader = YamlConfigurationLoader.builder().file(File(dataFolder, "mapper.yml")).indent(2).build()
            val rootNode = loader.load()
            val mapperNode = rootNode.node("mapper")
            for (entry in packetMapping.entries) {
                mapperNode.node(entry.key.toString()).set("0x%02X".format(entry.value))
            }
            loader.save(rootNode)
        } catch (e: ConfigurateException) {
            throw RuntimeException(e)
        }
    }

    override fun reload() {
        val loader = YamlConfigurationLoader.builder().file(File(dataFolder, "mapper.yml")).build()
        try {
            val mapperNode: ConfigurationNode = loader.load().node("mapper")
            for (entry in mapperNode.childrenMap().entries) {
                val key = entry.key.toString()
                entry.value.getString()?.let { hexValue ->
                    packetMapping.put(key.toInt(), Integer.decode(hexValue))
                }
            }
        } catch (e: ConfigurateException) {
            throw RuntimeException(e)
        }
    }
}
