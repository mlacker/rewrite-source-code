package com.mlacker.samples.netflix.zuul

import com.netflix.zuul.ExecutionStatus
import com.netflix.zuul.ZuulFilterResult

abstract class ZuulFilter : IZuulFilter, Comparable<ZuulFilter> {

    abstract val filterType: FilterType

    abstract val filterOrder: Int

    fun runFilter(): ZuulFilterResult {
        var zr: ZuulFilterResult

        if (shouldFilter) {
            try {
                val res = run()
                zr = ZuulFilterResult(res, ExecutionStatus.SUCCESS)
            } catch (ex: Throwable) {
                zr = ZuulFilterResult(ExecutionStatus.FAILED)
                zr.exception = ex
            }
        } else {
            zr = ZuulFilterResult(ExecutionStatus.SKIPPED)
        }

        return zr
    }

    override fun compareTo(other: ZuulFilter): Int =
            this.filterOrder.compareTo(other.filterOrder)
}

