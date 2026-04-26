package moe.caa.multilogin.api

interface MapperConfigAPI {
    val packetMapping: MutableMap<Int, Int>
    fun save()
    fun reload()
}
