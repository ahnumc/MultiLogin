package moe.caa.multilogin.flows.workflows

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * 表示一个工作流
 *
 * @param <CONTEXT> 加工上下文
 */
abstract class BaseFlows<CONTEXT> {
    abstract fun run(context: CONTEXT?): Signal?

    companion object {
        private val asyncThreadId = AtomicInteger(0)

        @JvmStatic
        protected val executorService: ExecutorService = Executors.newCachedThreadPool { r: Runnable? ->
            val thread = Thread(r, "MultiLogin Flows #" + asyncThreadId.incrementAndGet())
            thread.isDaemon = true
            thread
        }

        @JvmStatic
        @Synchronized
        fun close() {
            if (executorService.isShutdown) return
            executorService.shutdown()
        }
    }
}
