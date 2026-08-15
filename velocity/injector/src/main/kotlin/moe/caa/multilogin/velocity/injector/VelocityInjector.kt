package moe.caa.multilogin.velocity.injector

import com.velocitypowered.api.network.ProtocolVersion
import com.velocitypowered.proxy.protocol.MinecraftPacket
import com.velocitypowered.proxy.protocol.StateRegistry
import com.velocitypowered.proxy.protocol.StateRegistry.PacketRegistry
import com.velocitypowered.proxy.protocol.StateRegistry.PacketRegistry.ProtocolRegistry
import com.velocitypowered.proxy.protocol.packet.EncryptionResponsePacket
import com.velocitypowered.proxy.protocol.packet.ServerLoginPacket
import moe.caa.multilogin.api.internal.injector.Injector
import moe.caa.multilogin.api.internal.main.MultiCoreAPI
import moe.caa.multilogin.api.internal.util.reflect.NoSuchEnumException
import moe.caa.multilogin.api.internal.util.reflect.ReflectUtil.handleAccessible
import moe.caa.multilogin.velocity.injector.handler.MultiInitialLoginSessionHandler
import moe.caa.multilogin.velocity.injector.redirect.auth.MultiEncryptionResponse
import moe.caa.multilogin.velocity.injector.redirect.auth.MultiServerLogin
import java.lang.reflect.InvocationTargetException
import java.util.function.Supplier

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
        val serverbound = getServerboundPacketRegistry(StateRegistry.LOGIN)
        redirectInput(
            serverbound,
            EncryptionResponsePacket::class.java,
            Supplier { MultiEncryptionResponse(api) }
        )
        redirectInput(
            serverbound,
            ServerLoginPacket::class.java,
            Supplier { MultiServerLogin(api) }
        )
    }

    private fun getServerboundPacketRegistry(stateRegistry: StateRegistry): PacketRegistry {
        val field = handleAccessible(StateRegistry::class.java.getDeclaredField("serverbound"))
        return field.get(stateRegistry) as PacketRegistry
    }

    private fun <T> redirectInput(
        bound: PacketRegistry,
        originalClass: Class<T>,
        supplierRedirect: Supplier<out T>
    ) {
        val packetIdToSupplierField = handleAccessible(
            ProtocolRegistry::class.java.getDeclaredField("packetIdToSupplier")
        )
        val setValue = MutableMap.MutableEntry::class.java.getMethod("setValue", Any::class.java)

        for (protocolRegistry in getProtocolRegistries(bound)) {
            @Suppress("UNCHECKED_CAST")
            val packetIdToSupplier = packetIdToSupplierField.get(protocolRegistry)
                as MutableMap<Any?, Supplier<out MinecraftPacket>>
            for (entry in packetIdToSupplier.entries) {
                if (entry.value.get().javaClass == originalClass) {
                    setValue.invoke(entry, supplierRedirect)
                }
            }
        }
    }

    private fun getProtocolRegistries(bound: PacketRegistry): Collection<ProtocolRegistry> {
        val versionsField = handleAccessible(PacketRegistry::class.java.getDeclaredField("versions"))
        @Suppress("UNCHECKED_CAST")
        return (versionsField.get(bound) as Map<ProtocolVersion, ProtocolRegistry>).values
    }
}
