package moe.caa.multilogin.flows.workflows

import moe.caa.multilogin.flows.ProcessingFailedException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

class EntrustFlows<C>(steps: List<BaseFlows<C?>?>) : BaseFlows<C?>() {
    private val steps: List<BaseFlows<C?>> = steps.filterNotNull()

    override fun run(context: C?): Signal {
        val passed = AtomicBoolean(false)
        val latch = CountDownLatch(1)
        val currentTasks = CopyOnWriteArrayList<BaseFlows<C?>>()
        var flag = false
        for (step in steps) {
            flag = true
            currentTasks.add(step)
            executorService.execute {
                try {
                    if (step.run(context) == Signal.PASSED) {
                        passed.set(true)
                        latch.countDown()
                    }
                } finally {
                    currentTasks.remove(step)
                    if (currentTasks.isEmpty()) latch.countDown()
                }
            }
        }

        if (flag) try {
            latch.await()
        } catch (e: InterruptedException) {
            throw ProcessingFailedException(e)
        }

        return if (passed.get()) Signal.PASSED else Signal.TERMINATED
    }
}
