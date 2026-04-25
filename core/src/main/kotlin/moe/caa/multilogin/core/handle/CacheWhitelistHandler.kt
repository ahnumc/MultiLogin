package moe.caa.multilogin.core.handle

import java.util.concurrent.ConcurrentHashMap

class CacheWhitelistHandler {
    val cachedWhitelist: MutableSet<String?> = ConcurrentHashMap.newKeySet()
}
