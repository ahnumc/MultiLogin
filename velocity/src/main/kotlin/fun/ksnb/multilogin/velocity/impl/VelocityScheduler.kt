package `fun`.ksnb.multilogin.velocity.impl

import moe.caa.multilogin.api.internal.plugin.BaseScheduler

/**
 * Velocity 调度器对象
 */
class VelocityScheduler : BaseScheduler() {
    override fun runTask(run: Runnable, delay: Long) {
        runTaskAsync(run, delay)
    }
}
