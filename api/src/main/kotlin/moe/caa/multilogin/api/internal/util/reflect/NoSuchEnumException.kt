package moe.caa.multilogin.api.internal.util.reflect

import org.jetbrains.annotations.ApiStatus

/**
 * Signals that the enum class doesn't have an enum of a specified name.
 */
@ApiStatus.Internal
class NoSuchEnumException
/**
 * Constructor with a detail message.
 * 
 * @param s the detail message
 */
    (s: String?) : ReflectiveOperationException(s)
