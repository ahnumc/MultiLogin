package moe.caa.multilogin.api.internal.language

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface LanguageAPI {
    /**
     * 通过 节点 和 参数 构建这个可读文本字符串对象
     * 
     * @param node 节点
     * @return 可读文本字符串对象
     */
    fun getMessage(node: String?, vararg pairs: Pair<String, Any?>): String?
}
