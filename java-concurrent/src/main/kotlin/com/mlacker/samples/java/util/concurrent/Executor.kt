package com.mlacker.samples.java.util.concurrent

interface Executor {
    fun execute(command: Runnable)
}