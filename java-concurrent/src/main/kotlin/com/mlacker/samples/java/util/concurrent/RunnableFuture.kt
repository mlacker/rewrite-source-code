package com.mlacker.samples.java.util.concurrent

import com.mlacker.samples.java.lang.Runnable
import java.util.concurrent.Future

interface RunnableFuture<V>: Runnable, Future<V> {

     override fun run()
}