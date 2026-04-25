package moe.caa.multilogin.flows.workflows

enum class Signal {
    /**
     * 通过
     */
    PASSED,

    /**
     * 异常终止
     */
    TERMINATED
}