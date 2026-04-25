package moe.caa.multilogin.api.internal.plugin

import org.jetbrains.annotations.ApiStatus
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * 公共服务器线程调度器对象
 */
@ApiStatus.Internal
abstract class BaseScheduler {
    private val asyncThreadId = AtomicInteger(0)
    private val asyncExecutor: ScheduledExecutorService = Executors.newScheduledThreadPool(
        5
    ) { r: Runnable? -> Thread(r, "MultiLogin Async #" + asyncThreadId.incrementAndGet()) }

    /**
     * 执行一个异步任务
     * 
     * @param runnable 任务对象
     */
    fun runTaskAsync(runnable: Runnable) {
        asyncExecutor.execute(runnable)
    }

    /**
     * 执行一个异步任务
     * 
     * @param runnable 任务对象
     * @param delay    延时
     */
    fun runTaskAsync(runnable: Runnable, delay: Long) {
        asyncExecutor.schedule(runnable, delay, TimeUnit.MILLISECONDS)
    }

    /**
     * 执行一个周期异步任务
     * 
     * @param run    任务
     * @param delay  延迟
     * @param period 周期
     */
    fun runTaskAsyncTimer(run: Runnable, delay: Long, period: Long) {
        asyncExecutor.scheduleAtFixedRate(run, delay, period, TimeUnit.MILLISECONDS)
    }

    /**
     * 关闭线程池
     */
    @Synchronized
    fun shutdown() {
        if (asyncExecutor.isShutdown) return
        asyncExecutor.shutdown()
    }

    /**
     * 执行一个同步任务
     * 
     * @param run   任务
     * @param delay 延迟
     */
    abstract fun runTask(run: Runnable, delay: Long)

    /**
     * 执行一个同步任务
     * 
     * @param run 任务
     */
    fun runTask(run: Runnable) {
        runTask(run, 0)
    }
}
