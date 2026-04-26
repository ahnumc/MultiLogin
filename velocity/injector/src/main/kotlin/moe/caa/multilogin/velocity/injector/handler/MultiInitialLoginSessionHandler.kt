package moe.caa.multilogin.velocity.injector.handler

import com.google.common.primitives.Longs
import com.velocitypowered.proxy.VelocityServer
import com.velocitypowered.proxy.connection.MinecraftConnection
import com.velocitypowered.proxy.connection.client.AuthSessionHandler
import com.velocitypowered.proxy.connection.client.InitialLoginSessionHandler
import com.velocitypowered.proxy.connection.client.LoginInboundConnection
import com.velocitypowered.proxy.crypto.EncryptionUtils
import com.velocitypowered.proxy.protocol.StateRegistry
import com.velocitypowered.proxy.protocol.packet.EncryptionResponsePacket
import com.velocitypowered.proxy.protocol.packet.ServerLoginPacket
import moe.caa.multilogin.api.internal.auth.AuthResult
import moe.caa.multilogin.api.internal.logger.LoggerProvider
import moe.caa.multilogin.api.internal.main.MultiCoreAPI
import moe.caa.multilogin.api.internal.skinrestorer.SkinRestorerResult
import moe.caa.multilogin.api.internal.util.reflect.Accessor
import moe.caa.multilogin.api.internal.util.reflect.EnumAccessor
import moe.caa.multilogin.api.internal.util.reflect.NoSuchEnumException
import moe.caa.multilogin.api.internal.util.reflect.ReflectUtil.handleAccessible
import moe.caa.multilogin.api.profile.GameProfile
import moe.caa.multilogin.core.auth.LoginAuthResult
import net.kyori.adventure.text.Component
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.net.InetSocketAddress
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.util.concurrent.Callable

/**
 * 接管 InitialLoginSessionHandler 类的其中一个方法
 */
class MultiInitialLoginSessionHandler(
    private val initialLoginSessionHandler: InitialLoginSessionHandler,
    private val multiCoreAPI: MultiCoreAPI
) {
    internal val server: VelocityServer
    internal val mcConnection: MinecraftConnection
    internal val inbound: LoginInboundConnection

    // 运行时改动的实例
    private var login: ServerLoginPacket? = null
    private var verify: ByteArray = ByteArray(0)

    // 自己的对象，表示是否通过加密
    internal var encrypted = false

    init {
        try {
            this.server = getServerField.invoke(initialLoginSessionHandler) as VelocityServer
            this.mcConnection = getMcConnectionField.invoke(initialLoginSessionHandler) as MinecraftConnection
            this.inbound = getInboundField.invoke(initialLoginSessionHandler) as LoginInboundConnection
        } catch (e: Throwable) {
            throw RuntimeException(e)
        }
    }

    @Throws(Throwable::class)
    private fun initValues() {
        this.login = getLoginField.invoke(initialLoginSessionHandler) as ServerLoginPacket
        this.verify = getVerifyField.invoke(initialLoginSessionHandler) as ByteArray
    }

    @Throws(Throwable::class)
    fun handle(packet: EncryptionResponsePacket) {
        initValues()

        assertStateMethod.invoke(initialLoginSessionHandler, `loginStateEnum$ENCRYPTION_REQUEST_SENT`)
        setCurrentStateField.invoke(initialLoginSessionHandler, `loginStateEnum$ENCRYPTION_RESPONSE_RECEIVED`)

        val login = checkNotNull(this.login) { "No ServerLogin packet received yet." }
        check(verify.isNotEmpty()) { "No EncryptionRequest packet sent yet." }

        try {
            val serverKeyPair = server.serverKeyPair
            val playerKey = inbound.identifiedKey
            if (playerKey != null) {
                check(
                    playerKey.verifyDataSignature(
                        packet.getVerifyToken(),
                        verify,
                        Longs.toByteArray(packet.getSalt())
                    )
                ) { "Invalid client public signature." }
            } else {
                val decryptedSharedSecret = EncryptionUtils.decryptRsa(serverKeyPair, packet.getVerifyToken())
                check(
                    MessageDigest.isEqual(verify, decryptedSharedSecret)
                ) { "Unable to successfully decrypt the verification token." }
            }

            val decryptedSharedSecret = EncryptionUtils.decryptRsa(serverKeyPair, packet.getSharedSecret())

            encrypted = true
            val username = login.getUsername()
            val serverId = EncryptionUtils.generateServerId(decryptedSharedSecret, serverKeyPair.getPublic())
            val playerIp = (mcConnection.remoteAddress as InetSocketAddress).hostString

            multiCoreAPI.plugin.runServer.scheduler.runTaskAsync({
                val result = multiCoreAPI.authHandler.auth(username, serverId, playerIp) as LoginAuthResult
                try {
                    val encryptionEnabled = mcConnection.getChannel().eventLoop().submit(Callable<Boolean> {
                        if (mcConnection.isClosed()) return@Callable false
                        try {
                            mcConnection.enableEncryption(decryptedSharedSecret)
                            return@Callable true
                        } catch (var8: GeneralSecurityException) {
                            LoggerProvider.logger.error("Unable to enable encryption for connection", var8)
                            mcConnection.close(true)
                            return@Callable false
                        }
                    }).get()
                    if (encryptionEnabled) {
                        if (result.result == AuthResult.Result.ALLOW) {
                            var gameProfile: GameProfile = requireNotNull(result.response)

                            try {
                                val restorerResult: SkinRestorerResult =
                                    multiCoreAPI.skinRestorerHandler.doRestorer(result)
                                if (restorerResult.throwable != null) {
                                    LoggerProvider.logger.error(
                                        "An exception occurred while processing the skin repair.",
                                        restorerResult.throwable
                                    )
                                }
                                LoggerProvider.logger.debug(
                                    "Skin restore result of ${result.baseServiceAuthenticationResult?.response?.name} is ${restorerResult.reason}."
                                )

                                restorerResult.response?.let { gameProfile = it }
                            } catch (e: Exception) {
                                LoggerProvider.logger.debug(
                                    "Skin restore result of ${result.baseServiceAuthenticationResult?.response?.name} is error."
                                )
                                LoggerProvider.logger.debug(
                                    "An exception occurred while processing the skin repair.",
                                    e
                                )
                            }

                            val finalGameProfile = gameProfile
                            mcConnection.getChannel().eventLoop().submit {
                                try {
                                    mcConnection.setActiveSessionHandler(
                                        StateRegistry.LOGIN,
                                        authSessionHandler_allArgsConstructor.invoke(
                                            server, inbound, generateGameProfile(finalGameProfile), true, serverId
                                        ) as AuthSessionHandler?
                                    )
                                } catch (e: Throwable) {
                                    throw RuntimeException(e)
                                }
                            }.get()
                        } else {
                            inbound.disconnect(Component.text(result.kickMessage ?: ""))
                        }
                    }
                } catch (e: Throwable) {
                    LoggerProvider.logger.error("An exception occurred while processing validation results.", e)
                    if (encrypted) {
                        inbound.disconnect(Component.text(multiCoreAPI.languageHandler.getMessage("auth_error") ?: ""))
                    }
                    mcConnection.close(true)
                }
            })
        } catch (var9: GeneralSecurityException) {
            LoggerProvider.logger.error("Unable to enable encryption.", var9)
            this.mcConnection.close(true)
        }
    }

    private fun generateGameProfile(response: GameProfile): com.velocitypowered.api.util.GameProfile {
        return com.velocitypowered.api.util.GameProfile(
            response.id,
            response.name,
            response.propertyMap.values.map { s ->
                com.velocitypowered.api.util.GameProfile.Property(s.name, s.value, s.signature)
            }
        )
    }

    companion object {
        private lateinit var loginStatsEnumAccessor: EnumAccessor
        private lateinit var initialLoginSessionHandlerAccessor: Accessor

        private lateinit var `loginStateEnum$LOGIN_PACKET_EXPECTED`: Enum<*>
        private lateinit var `loginStateEnum$LOGIN_PACKET_RECEIVED`: Enum<*>
        private lateinit var `loginStateEnum$ENCRYPTION_REQUEST_SENT`: Enum<*>
        private lateinit var `loginStateEnum$ENCRYPTION_RESPONSE_RECEIVED`: Enum<*>

        private lateinit var assertStateMethod: MethodHandle
        private lateinit var setCurrentStateField: MethodHandle
        private lateinit var getLoginField: MethodHandle
        private lateinit var getVerifyField: MethodHandle
        private lateinit var getServerField: MethodHandle
        private lateinit var getInboundField: MethodHandle
        private lateinit var getMcConnectionField: MethodHandle
        private lateinit var getCurrentStateField: MethodHandle
        private lateinit var authSessionHandler_allArgsConstructor: MethodHandle

        @Throws(
            ClassNotFoundException::class,
            NoSuchMethodException::class,
            IllegalAccessException::class,
            NoSuchFieldException::class,
            NoSuchEnumException::class
        )
        fun init() {
            val initialLoginSessionHandlerClass: Class<InitialLoginSessionHandler> =
                InitialLoginSessionHandler::class.java
            initialLoginSessionHandlerAccessor = Accessor(initialLoginSessionHandlerClass)

            val loginStateEnum =
                Class.forName("com.velocitypowered.proxy.connection.client.InitialLoginSessionHandler\$LoginState")
            val loginStateAccessor = EnumAccessor(loginStateEnum)
            loginStatsEnumAccessor = loginStateAccessor

            `loginStateEnum$LOGIN_PACKET_EXPECTED` = loginStateAccessor.findByName("LOGIN_PACKET_EXPECTED")
            `loginStateEnum$LOGIN_PACKET_RECEIVED` = loginStateAccessor.findByName("LOGIN_PACKET_RECEIVED")
            `loginStateEnum$ENCRYPTION_REQUEST_SENT` = loginStateAccessor.findByName("ENCRYPTION_REQUEST_SENT")
            `loginStateEnum$ENCRYPTION_RESPONSE_RECEIVED` =
                loginStateAccessor.findByName("ENCRYPTION_RESPONSE_RECEIVED")

            val lookup = MethodHandles.lookup()
            val accessor = initialLoginSessionHandlerAccessor

            assertStateMethod = lookup.unreflect(
                handleAccessible(
                    accessor.findFirstMethodByName(true, "assertState")
                )
            )

            val currentState = handleAccessible(
                initialLoginSessionHandlerClass.getDeclaredField("currentState")
            )
            getCurrentStateField = lookup.unreflectGetter(currentState)
            setCurrentStateField = lookup.unreflectSetter(currentState)

            getLoginField = lookup.unreflectGetter(
                handleAccessible(
                    accessor.findFirstFieldByType(true, ServerLoginPacket::class.java)
                )
            )

            getVerifyField = lookup.unreflectGetter(
                handleAccessible(
                    accessor.findFirstFieldByType(true, ByteArray::class.java)
                )
            )

            getServerField = lookup.unreflectGetter(
                handleAccessible(
                    accessor.findFirstFieldByType(true, VelocityServer::class.java)
                )
            )

            getInboundField = lookup.unreflectGetter(
                handleAccessible(
                    accessor.findFirstFieldByType(true, LoginInboundConnection::class.java)
                )
            )

            getMcConnectionField = lookup.unreflectGetter(
                handleAccessible(
                    accessor.findFirstFieldByType(true, MinecraftConnection::class.java)
                )
            )

            authSessionHandler_allArgsConstructor = lookup.unreflectConstructor(
                handleAccessible(
                    AuthSessionHandler::class.java.getDeclaredConstructor(
                        VelocityServer::class.java,
                        LoginInboundConnection::class.java,
                        com.velocitypowered.api.util.GameProfile::class.java,
                        Boolean::class.javaPrimitiveType,
                        String::class.java
                    )
                )
            )
        }
    }
}
