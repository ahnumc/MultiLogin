package moe.caa.multilogin.api.internal.util.reflect

import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
class NoSuchConstructorException
/**
 * Constructor with a detail message.
 * 
 * @param s the detail message
 */
    (s: String?) : ReflectiveOperationException(s)
