package moe.caa.multilogin.velocity.injector

import com.google.common.collect.Iterables
import com.velocitypowered.api.network.ProtocolVersion
import com.velocitypowered.proxy.protocol.MinecraftPacket
import com.velocitypowered.proxy.protocol.StateRegistry
import com.velocitypowered.proxy.protocol.StateRegistry.PacketMapping
import com.velocitypowered.proxy.protocol.StateRegistry.PacketRegistry
import com.velocitypowered.proxy.protocol.StateRegistry.PacketRegistry.ProtocolRegistry
import com.velocitypowered.proxy.protocol.packet.EncryptionResponsePacket
import com.velocitypowered.proxy.protocol.packet.ServerLoginPacket
import io.netty.util.collection.IntObjectMap
import moe.caa.multilogin.api.internal.injector.Injector
import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.main.MultiCoreAPI
import moe.caa.multilogin.api.internal.util.reflect.NoSuchEnumException
import moe.caa.multilogin.api.internal.util.reflect.ReflectUtil.handleAccessible
import moe.caa.multilogin.velocity.injector.handler.MultiInitialLoginSessionHandler
import moe.caa.multilogin.velocity.injector.redirect.auth.MultiEncryptionResponse
import moe.caa.multilogin.velocity.injector.redirect.auth.MultiServerLogin
import moe.caa.multilogin.velocity.injector.redirect.chat.PlayerSessionPacketBlocker
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.util.*
import java.util.function.Supplier

/**
 * Velocity 注入程序
 */
class VelocityInjector : Injector {
    @Throws(
        NoSuchFieldException::class,
        ClassNotFoundException::class,
        NoSuchMethodException::class,
        IllegalAccessException::class,
        InvocationTargetException::class,
        NoSuchEnumException::class
    )
    override fun inject(api: MultiCoreAPI) {
        MultiInitialLoginSessionHandler.init()
        // auth
        run {
            val serverbound = getServerboundPacketRegistry(StateRegistry.LOGIN)
            redirectInput(
                serverbound,
                EncryptionResponsePacket::class.java,
                Supplier { MultiEncryptionResponse(api) })
            redirectInput(
                serverbound,
                ServerLoginPacket::class.java,
                Supplier { MultiServerLogin(api) })
        }
    }

    override fun registerChatSession(packetMapping: MutableMap<Int, Int>) {
        // chat
        try {
            val serverbound = getServerboundPacketRegistry(StateRegistry.PLAY)

            val playerSessionPacketMapping = packetMapping.map { (protocolVersion, packetId) ->
                LoggerProvider.logger
                    .debug("Register PlayerSessionPacketBlocker for protocol version: $protocolVersion")
                createPacketMapping(
                    packetId,
                    ProtocolVersion.getProtocolVersion(protocolVersion),
                    false
                )
            }
            registerPacket(
                serverbound,
                PlayerSessionPacketBlocker::class.java,
                { PlayerSessionPacketBlocker() },
                playerSessionPacketMapping.toTypedArray()
            )
        } catch (throwable: Throwable) {
            LoggerProvider.logger.error(
                "Unable to register PlayerSessionPacketBlocker, chat session blocker does not work as expected.",
                throwable
            )
        }
    }

    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    private fun getServerboundPacketRegistry(stateRegistry: StateRegistry): PacketRegistry {
        val serverboundField = handleAccessible(StateRegistry::class.java.getDeclaredField("serverbound"))
        return serverboundField.get(stateRegistry) as PacketRegistry
    }

    /**
     * 重定向数据包
     * 
     * @param bound            数据包方向
     * @param originalClass    原始数据包类对象
     * @param supplierRedirect 重定向后的 Supplier
     */
    @Throws(
        NoSuchFieldException::class,
        InvocationTargetException::class,
        IllegalAccessException::class,
        NoSuchMethodException::class
    )
    private fun <T> redirectInput(
        bound: PacketRegistry,
        originalClass: Class<T>,
        supplierRedirect: Supplier<out T>
    ) {
        val `f$packetIdToSupplier` = ProtocolRegistry::class.java.getDeclaredField("packetIdToSupplier")
        handleAccessible(`f$packetIdToSupplier`)


        val `map$entry$setValueMethod` = MutableMap.MutableEntry::class.java.getMethod("setValue", Any::class.java)

        for (protocolRegistry in getProtocolRegistries(bound)) {
            @Suppress("UNCHECKED_CAST")
            val packetIdToSupplier =
                `f$packetIdToSupplier`.get(protocolRegistry) as MutableMap<Any?, Supplier<out MinecraftPacket>>
            for (e in packetIdToSupplier.entries) {
                val minecraftPacketObject = e.value.get()
                // 类匹配则进行替换
                if (minecraftPacketObject.javaClass == originalClass) {
                    `map$entry$setValueMethod`.invoke(e, supplierRedirect)
                }
            }
        }
    }

    /**
     * 追加注册出口包
     * 
     * @param bound         数据包方向
     * @param originalClass 原始数据包类对象
     * @param appendClass   追加的数据包类对象
     */
    @Throws(
        NoSuchFieldException::class,
        InvocationTargetException::class,
        IllegalAccessException::class,
        NoSuchMethodException::class
    )
    private fun <T> redirectOutput(bound: PacketRegistry, originalClass: Class<T>, appendClass: Class<out T>) {
        val `f$packetClassToId` = ProtocolRegistry::class.java.getDeclaredField("packetClassToId")
        handleAccessible(`f$packetClassToId`)

        val `map$putMethod` = MutableMap::class.java.getMethod("put", Any::class.java, Any::class.java)

        for (protocolRegistry in getProtocolRegistries(bound)) {
            @Suppress("UNCHECKED_CAST")
            val packetClassToId =
                `f$packetClassToId`.get(protocolRegistry) as MutableMap<Class<out MinecraftPacket>, Int>

            @Suppress("UNCHECKED_CAST")
            val originalPacketClass = originalClass as Class<out MinecraftPacket>
            if (!packetClassToId.containsKey(originalPacketClass)) continue
            `map$putMethod`.invoke(packetClassToId, appendClass, packetClassToId[originalPacketClass])
        }
    }

    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    private fun getProtocolRegistries(bound: PacketRegistry): MutableCollection<ProtocolRegistry> {
        return getProtocolRegistriesMap(bound).values
    }

    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    private fun getProtocolRegistriesMap(bound: PacketRegistry): MutableMap<ProtocolVersion, ProtocolRegistry> {
        val `f$versions` = PacketRegistry::class.java.getDeclaredField("versions")
        handleAccessible(`f$versions`)

        @Suppress("UNCHECKED_CAST")
        return `f$versions`.get(bound) as MutableMap<ProtocolVersion, ProtocolRegistry>
    }

    @Throws(
        NoSuchMethodException::class,
        InvocationTargetException::class,
        InstantiationException::class,
        IllegalAccessException::class
    )
    private fun createPacketMapping(
        id: Int,
        protocolVersion: ProtocolVersion,
        lastValidProtocolVersion: ProtocolVersion?,
        packetDecoding: Boolean
    ): PacketMapping {
        @Suppress("UNCHECKED_CAST")
        val constructor = handleAccessible(
            PacketMapping::class.java.getDeclaredConstructor(
                Int::class.javaPrimitiveType,
                ProtocolVersion::class.java,
                ProtocolVersion::class.java,
                Boolean::class.javaPrimitiveType
            )
        ) as Constructor<PacketMapping>
        return constructor.newInstance(id, protocolVersion, lastValidProtocolVersion, packetDecoding)
    }

    @Throws(
        NoSuchMethodException::class,
        InvocationTargetException::class,
        InstantiationException::class,
        IllegalAccessException::class
    )
    private fun createPacketMapping(
        id: Int,
        protocolVersion: ProtocolVersion,
        packetDecoding: Boolean
    ): PacketMapping {
        return createPacketMapping(id, protocolVersion, null, packetDecoding)
    }

    @Throws(IllegalAccessException::class)
    private fun <P : MinecraftPacket> registerPacket(
        packetRegistry: PacketRegistry,
        clazz: Class<P>,
        packetSupplier: Supplier<P>,
        mappings: Array<PacketMapping>
    ) {
        try {
            register(packetRegistry, clazz, packetSupplier, *mappings)
        } catch (e: NoSuchFieldException) {
            throw RuntimeException(e)
        }
    }

    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    fun <P : MinecraftPacket> register(
        bound: PacketRegistry, clazz: Class<P>, packetSupplier: Supplier<P>,
        vararg mappings: PacketMapping
    ) {
        require(mappings.isNotEmpty()) { "At least one mapping must be provided." }

        for ((index, current) in mappings.withIndex()) {
            val next = mappings.getOrElse(index + 1) { current }

            val protocolVersion = current.javaClass.getDeclaredField("protocolVersion")
            protocolVersion.setAccessible(true)
            val from = protocolVersion.get(current) as ProtocolVersion
            val lastValidProtocolVersion = current.javaClass.getDeclaredField("lastValidProtocolVersion")
            lastValidProtocolVersion.setAccessible(true)
            val lastValid = lastValidProtocolVersion.get(current) as ProtocolVersion?
            if (lastValid != null) {
                require(next === current) { "Cannot add a mapping after last valid mapping" }
                require(!from.greaterThan(lastValid)) { "Last mapping version cannot be higher than highest mapping version" }
            }
            val nextProtocolVersion = handleAccessible(next.javaClass.getDeclaredField("protocolVersion"))
            val lastSupportedProtocol = Iterables.getLast(ProtocolVersion.SUPPORTED_VERSIONS)
            val to = if (current === next) lastValid ?: lastSupportedProtocol else nextProtocolVersion.get(next) as ProtocolVersion

            val lastInList = lastValid ?: lastSupportedProtocol

            require(!(from.noLessThan(to) && from !== lastInList)) {
                "Next mapping version ($to) should be lower then current ($from)"
            }

            for (protocol in EnumSet.range(from, to)) {
                if (protocol === to && next !== current) {
                    break
                }
                val registry = getProtocolRegistriesMap(bound)[protocol]
                requireNotNull(registry) { "Unknown protocol version $protocol" }

                val packetIdToSupplier = registry.javaClass.getDeclaredField("packetIdToSupplier")
                handleAccessible(packetIdToSupplier)
                @Suppress("UNCHECKED_CAST")
                val supplierIntObjectMap =
                    packetIdToSupplier.get(registry) as IntObjectMap<Supplier<out MinecraftPacket>>
                val idField = current.javaClass.getDeclaredField("id")
                handleAccessible(idField)
                if (supplierIntObjectMap.containsKey(idField.getInt(current))) {
                    continue
                    /*
                    throw new IllegalArgumentException(
                            "Can not register class "
                                    + clazz.getSimpleName()
                                    + " with id "
                                    + current.id
                                    + " for "
                                    + registry.version
                                    + " because another packet is already registered");
                     */
                }

                val packetClassToIdField = registry.javaClass.getDeclaredField("packetClassToId")
                handleAccessible(packetClassToIdField)
                @Suppress("UNCHECKED_CAST")
                val packetClassToId =
                    packetClassToIdField.get(registry) as MutableMap<Class<out MinecraftPacket>, Int>
                require(!packetClassToId.containsKey(clazz)) { clazz.getSimpleName() + " is already registered for version " + registry.version }

                val encodeOnly = current.javaClass.getDeclaredField("encodeOnly")
                handleAccessible(encodeOnly)
                if (!encodeOnly.getBoolean(current)) {
                    supplierIntObjectMap.put(idField.getInt(current), packetSupplier)
                }
                packetClassToId[clazz] = idField.getInt(current)
            }
        }
    }
}
