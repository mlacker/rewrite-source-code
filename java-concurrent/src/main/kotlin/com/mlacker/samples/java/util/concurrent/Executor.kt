package com.mlacker.samples.java.util.concurrent

import com.mlacker.samples.java.lang.Runnable

interface Executor {
    fun execute(command: Runnable)
}