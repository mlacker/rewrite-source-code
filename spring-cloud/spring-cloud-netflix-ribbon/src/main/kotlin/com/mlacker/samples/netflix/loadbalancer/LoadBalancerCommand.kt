package com.mlacker.samples.netflix.loadbalancer

import com.netflix.client.ClientException
import com.netflix.client.RetryHandler
import com.netflix.loadbalancer.LoadBalancerContext
import com.netflix.loadbalancer.Server
import com.netflix.loadbalancer.reactive.ExecutionListener
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.*

class LoadBalancerCommand<T>(
    private val loadBalancerContext: LoadBalancerContext,
    private val loadBalancerURI: URI,
    private val retryHandler: RetryHandler = loadBalancerContext.retryHandler,
    var loadBalancerKey: Any? = null,
    private val server: Server? = null,

    ) {
    private val logger = LoggerFactory.getLogger(LoadBalancerCommand::class.java)

    private fun selectServer(): Server {
        return loadBalancerContext.getServerFromLoadBalancer(loadBalancerURI, loadBalancerKey)
    }

    class ExecutionInfoContext {
        var server: Server? = null
            set(server) {
                field = server
                this.serverAttemptCount++

                this.attemptCount = 0
            }
        var serverAttemptCount = 0
            private set
        var attemptCount = 0
            private set

        fun incAttemptCount() {
            this.attemptCount++
        }
    }

    private fun retryPolicy(maxRetries: Int, same: Boolean): (Int, Throwable) -> Boolean {
        return canRetry@{ tryCount: Int, ex: Throwable ->
            if (ex is ExecutionListener.AbortExecutionException) {
                return@canRetry false
            }

            if (tryCount > maxRetries) {
                return@canRetry false
            }

            var exception = ex
            if (ex.cause != null && ex is RuntimeException) {
                exception = ex.cause!!
            }

            return@canRetry retryHandler.isRetriableException(exception, same)
        }
    }

    fun submit(operation: ServerOperation<T>): T {
        val context = ExecutionInfoContext()

        val maxRetriesSame = retryHandler.maxRetriesOnSameServer
        val maxRetriesNext = retryHandler.maxRetriesOnNextServer

        try {
            for (retriesNext in 0..maxRetriesNext + 1) {
                try {
                    val server = this.server ?: selectServer()

                    // server attempt count increment
                    context.server = server

                    for (retriesSame in 0..maxRetriesSame + 1) {
                        try {
                            // same server attempt count increment
                            context.incAttemptCount()

                            return operation(server)
                        } catch (ex: Throwable) {
                            logger.debug("Got error $ex when executed on server $server")

                            if (!(maxRetriesSame > 0 &&
                                        retryPolicy(maxRetriesSame, true)(retriesSame + 1, ex))
                            ) {
                                throw ex
                            }
                        }
                    }
                } catch (ex: Throwable) {
                    if (!((maxRetriesNext > 0 && this.server == null) &&
                                retryPolicy(maxRetriesNext, false)(retriesNext + 1, ex))
                    ) {
                        throw ex
                    }
                }
            }
        } catch (ex: Throwable) {
            var exception = ex
            if (context.attemptCount > 0) {
                if (maxRetriesNext > 0 && context.serverAttemptCount == (maxRetriesNext + 1)) {
                    exception = ClientException(
                        ClientException.ErrorType.NUMBEROF_RETRIES_NEXTSERVER_EXCEEDED,
                        "Number of retries on next server exceeded max $maxRetriesNext " +
                                "retries, while making a call for: ${context.server}", ex)
                } else if (maxRetriesSame > 0 && context.attemptCount == (maxRetriesSame + 1)) {
                    exception = ClientException(
                        ClientException.ErrorType.NUMBEROF_RETRIES_EXEEDED,
                        "Number of retries exceeded max $maxRetriesSame " +
                                "retries, while making a call for: ${context.server}", ex)
                }
            }

            throw exception
        }

        throw UnsupportedOperationException()
    }
}