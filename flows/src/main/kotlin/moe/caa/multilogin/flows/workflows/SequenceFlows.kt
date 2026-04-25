package moe.caa.multilogin.flows.workflows

class SequenceFlows<C>(steps: List<BaseFlows<C?>?>) : BaseFlows<C?>() {
    private val steps: List<BaseFlows<C?>> = steps.filterNotNull()

    override fun run(context: C?): Signal {
        for (step in steps) {
            if (step.run(context) == Signal.TERMINATED) return Signal.TERMINATED
        }
        return Signal.PASSED
    }
}
