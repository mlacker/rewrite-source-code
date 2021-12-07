package com.mlacker.samples.cloud.netflix.zuul.filters.route.support

import com.mlacker.samples.cloud.netflix.zuul.filters.route.RibbonCommand
import com.mlacker.samples.netflix.client.AbstractLoadBalancerAwareClient
import com.mlacker.samples.netflix.zuul.context.RequestContext
import com.netflix.client.ClientRequest
import com.netflix.client.config.DefaultClientConfigImpl
import com.netflix.client.config.IClientConfig
import com.netflix.client.config.IClientConfigKey
import com.netflix.client.http.HttpResponse
import com.netflix.config.DynamicPropertyFactory
import com.netflix.hystrix.HystrixCommand
import com.netflix.hystrix.HystrixCommandGroupKey
import com.netflix.hystrix.HystrixCommandKey
import com.netflix.hystrix.HystrixCommandProperties
import com.netflix.hystrix.HystrixThreadPoolKey
import com.netflix.zuul.constants.ZuulConstants
import org.slf4j.LoggerFactory
import org.springframework.cloud.netflix.ribbon.RibbonClientConfiguration
import org.springframework.cloud.netflix.ribbon.RibbonHttpResponse
import org.springframework.cloud.netflix.ribbon.support.RibbonCommandContext
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties
import org.springframework.cloud.netflix.zuul.filters.route.FallbackProvider
import org.springframework.http.client.ClientHttpResponse

abstract class AbstractRibbonCommand<LBC : AbstractLoadBalancerAwareClient<RQ, RS>,
        RQ : ClientRequest, RS : HttpResponse>(
        commandKey: String,
        private val client: LBC,
        protected val context: RibbonCommandContext,
        properties: ZuulProperties,
        private val fallbackProvider: FallbackProvider?,
        private val config: IClientConfig
) : HystrixCommand<ClientHttpResponse>(getSetter(commandKey, properties, config)),
        RibbonCommand {

    override fun run(): ClientHttpResponse {
        val context = RequestContext.currentContext

        val request: RQ = createRequest()

        val response: RS = this.client.executeWithLoadBalancer(request, config)
        context["ribbonResponse"] = response

        if (this.isResponseTimedOut) {
            response.close()
        }

        return RibbonHttpResponse(response)
    }

    override fun getFallback(): ClientHttpResponse {
        return fallbackProvider
                ?.let {
                    val cause = failedExecutionException ?: executionException
                    it.fallbackResponse(context.serviceId, cause)
                }
                ?: super.getFallback()
    }

    protected abstract fun createRequest(): RQ

    companion object {

        private val logger = LoggerFactory.getLogger(AbstractRibbonCommand::class.java)

        private fun createSetter(
            config: IClientConfig, commandKey: String, properties: ZuulProperties
        ): HystrixCommandProperties.Setter {
            val hystrixTimeout = getHystrixTimeout(config, commandKey)
            return HystrixCommandProperties.Setter()
                .withExecutionIsolationStrategy(properties.ribbonIsolationStrategy)
                .withExecutionTimeoutInMilliseconds(hystrixTimeout)
        }

        private fun getHystrixTimeout(config: IClientConfig, commandKey: String): Int {

            val ribbonTimeout = getRibbonTimeout(config, commandKey)
            val dynamicPropertyFactory = DynamicPropertyFactory.getInstance()
            val defaultHystrixTimeout = dynamicPropertyFactory.getIntProperty(
                "hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds",
                0).get()
            val commandHystrixTimeout = dynamicPropertyFactory.getIntProperty(
                "hystrix.command.$commandKey.execution.isolation.thread.timeoutInMilliseconds",
                0).get()
            val hystrixTimeout = when {
                commandHystrixTimeout > 0 -> commandHystrixTimeout
                defaultHystrixTimeout > 0 -> defaultHystrixTimeout
                else -> ribbonTimeout
            }
            if (hystrixTimeout < ribbonTimeout) {
                logger.warn("The Hystrix timeout of ${hystrixTimeout}ms " +
                        "for the command $commandKey is set lower than the combination of " +
                        "the Ribbon read and connect timeout, ${ribbonTimeout}ms.")
            }
            return hystrixTimeout
        }

        private fun getRibbonTimeout(config: IClientConfig?, commandKey: String): Int {
            val ribbonTimeout: Int
            if (config == null) {
                ribbonTimeout = RibbonClientConfiguration.DEFAULT_CONNECT_TIMEOUT +
                        RibbonClientConfiguration.DEFAULT_READ_TIMEOUT
            } else {
                val ribbonReadTimeout = getTimeout(config, commandKey, "ReadTimeout",
                    IClientConfigKey.Keys.ReadTimeout, RibbonClientConfiguration.DEFAULT_READ_TIMEOUT)
                val ribbonConnectTimeout = getTimeout(config, commandKey, "ConnectTimeout",
                    IClientConfigKey.Keys.ConnectTimeout, RibbonClientConfiguration.DEFAULT_CONNECT_TIMEOUT)
                val maxAutoRetries = getTimeout(config, commandKey, "MaxAutoRetries",
                    IClientConfigKey.Keys.MaxAutoRetries, DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES)
                val maxAutoRetriesNextServer = getTimeout(config, commandKey, "MaxAutoRetriesNextServer",
                    IClientConfigKey.Keys.MaxAutoRetriesNextServer, DefaultClientConfigImpl.DEFAULT_MAX_AUTO_RETRIES_NEXT_SERVER)
                ribbonTimeout = (ribbonReadTimeout + ribbonConnectTimeout) *
                        (maxAutoRetries + 1) * (maxAutoRetriesNextServer + 1)
            }
            return ribbonTimeout
        }

        private fun getTimeout(
            config: IClientConfig,
            commandKey: String,
            property: String,
            configKey: IClientConfigKey<Int>,
            defaultValue: Int
        ): Int = DynamicPropertyFactory.getInstance()
            .getIntProperty(
                "$commandKey.${config.nameSpace}.$property",
                config.get(configKey, defaultValue))
            .get()

        private fun getSetter(
            commandKey: String, properties: ZuulProperties, config: IClientConfig
        ): Setter {
            val commandSetter = Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey("RibbonCommand"))
                .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey))
            val setter = createSetter(config, commandKey, properties)

            if (properties.ribbonIsolationStrategy == HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE) {
                val name = "${ZuulConstants.ZUUL_EUREKA + commandKey}.semaphore.maxSemaphores"
                val maxSemaphores = DynamicPropertyFactory.getInstance()
                    .getIntProperty(name, properties.semaphore.maxSemaphores)
                    .get()
                setter.withExecutionIsolationSemaphoreMaxConcurrentRequests(maxSemaphores)
            } else if (properties.threadPool.isUseSeparateThreadPools) {
                val threadPoolKey = properties.threadPool.threadPoolKeyPrefix + commandKey
                commandSetter.andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(threadPoolKey))
            }
            return commandSetter.andCommandPropertiesDefaults(setter)
        }
    }
}
