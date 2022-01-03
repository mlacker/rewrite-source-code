package com.mlacker.samples.java.util.concurrent

import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference

class FutureTask<V>(
    private val callable: Callable<V>
) : RunnableFuture<V> {

    @Volatile
    private var state: State = State.NEW

    private val runner: AtomicReference<Thread?> = AtomicReference(null)
    private var outcome: Any? = null

    private enum class State {
        NEW,
        COMPLETING,
        NORMAL,
        EXCEPTIONAL,
        CANCELLED,
        INTERRUPTING,
        INTERRUPTED
    }

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        TODO("Not yet implemented")
    }

    override fun isCancelled(): Boolean =
        state >= State.CANCELLED

    override fun isDone(): Boolean =
        state != State.NEW

    override fun get(): V {
        var s = state
        if (s <= State.COMPLETING) {
            s = awaitDone(false, 0L)
        }
        return report(s)
    }

    override fun get(timeout: Long, unit: TimeUnit): V {
        var s = state
        if (s <= State.COMPLETING) {
            s = awaitDone(true, unit.toNanos(timeout))
            if (s <= State.COMPLETING) {
                throw TimeoutException()
            }
        }
        return report(s)
    }

    override fun run() {
        if (state != State.NEW ||
            !runner.compareAndSet(null, Thread.currentThread())
        ) {
            return
        }

        try {
            if (state == State.NEW) {
                var result: V?
                var ran: Boolean

                try {
                    result = callable.call()
                    ran = true
                } catch (ex: Throwable) {
                    result = null
                    ran = false
                    state = State.EXCEPTIONAL
                }
                if (ran) {
                    state = State.NORMAL
                    outcome = result
                }
            }
        } finally {
            runner.set(null)
        }
    }

    private fun report(s: State): V {
        if (s == State.NORMAL) {
            @Suppress("UNCHECKED_CAST")
            return outcome as V
        }
        if (s >= State.CANCELLED) {
            throw CancellationException()
        }
        throw ExecutionException(outcome as Throwable)
    }

    private fun awaitDone(timed: Boolean, nanos: Long): State {
        TODO("Not yet implemented")
    }
}
