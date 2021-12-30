package com.mlacker.samples.netflix.eureka.registry

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.mlacker.samples.netflix.discovery.shared.Application
import com.mlacker.samples.netflix.discovery.shared.Applications
import com.netflix.appinfo.EurekaAccept
import com.netflix.eureka.EurekaServerConfig
import com.netflix.eureka.Version
import com.netflix.eureka.registry.Key
import com.netflix.eureka.resources.CurrentRequestVersion
import com.netflix.eureka.resources.ServerCodecs
import java.lang.Exception
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit

/**
 * The class that is responsible for caching registry information that will be
 * queried by clients.
 *
 * <p>
 *     The cache is maintained in compressed and non-compressed form for three
 *     categories of requests - all applications, delta changes and for individual
 *     applications, The compressed form is probably the most efficient in terms of
 *     network traffic especially when querying all applications.
 * </p>
 */
class ResponseCacheImpl(
    serverConfig: EurekaServerConfig,
    private val serverCodecs: ServerCodecs,
    private val registry: AbstractInstanceRegistry,
) : ResponseCache {

    private val timer: Timer = Timer("Eureka-CacheFillTimer", true)

    private val readOnlyCacheMap: ConcurrentMap<Key, String> = ConcurrentHashMap()
    private val readWriteCacheMap: LoadingCache<Key, String>
    private val shouldUseReadOnlyResponseCache: Boolean = serverConfig.shouldUseReadOnlyResponseCache()

    init {
        // 30s
        val responseCacheUpdateIntervalMs = serverConfig.responseCacheUpdateIntervalMs
        this.readWriteCacheMap =
            CacheBuilder.newBuilder()
                .initialCapacity(serverConfig.initialCapacityOfResponseCache) // 1000
                .expireAfterWrite(serverConfig.responseCacheAutoExpirationInSeconds, TimeUnit.SECONDS) // 180
                .build(object : CacheLoader<Key, String>() {
                    override fun load(key: Key): String {
                        return generatePayload(key)
                    }
                })
        if (shouldUseReadOnlyResponseCache) {
            timer.schedule(getCacheUpdateTask(),
                Date(((System.currentTimeMillis() / responseCacheUpdateIntervalMs) * responseCacheUpdateIntervalMs)
                        + responseCacheUpdateIntervalMs),
                responseCacheUpdateIntervalMs)
        }
    }

    private fun getCacheUpdateTask(): TimerTask {
        return object : TimerTask() {
            override fun run() {
                for (key in readOnlyCacheMap.keys) {
                    try {
                        CurrentRequestVersion.set(key.version)
                        val cacheValue = readWriteCacheMap[key]
                        val currentCacheValue = readOnlyCacheMap[key]
                        if (cacheValue != currentCacheValue) {
                            readOnlyCacheMap[key] = cacheValue
                        }
                    } catch (th: Throwable) {
                    } finally {
                        CurrentRequestVersion.remove()
                    }
                }
            }
        }
    }

    override fun get(key: Key): String? {
        return get(key, shouldUseReadOnlyResponseCache)
    }

    private fun get(key: Key, useReadOnlyCache: Boolean): String? {
        val payload = getValue(key, useReadOnlyCache)
        return if (payload == "") {
            null
        } else {
            payload
        }
    }

    override fun stop() {
        timer.cancel()
    }

    override fun invalidate(appName: String, vipAddress: String?) {
        invalidate(Key(Key.EntityType.Application, appName, Key.KeyType.JSON, Version.V2, EurekaAccept.full))
        invalidate(Key(Key.EntityType.Application, "ALL_APPS", Key.KeyType.JSON, Version.V2, EurekaAccept.full))
        if (vipAddress != null) {
            invalidate(Key(Key.EntityType.VIP, appName, Key.KeyType.JSON, Version.V2, EurekaAccept.full))
        }
    }

    private fun invalidate(vararg keys: Key) {
        for (key in keys) {
            readWriteCacheMap.invalidate(key)
        }
    }

    private fun getValue(key: Key, useReadOnlyCache: Boolean): String? {
        var payload: String? = null
        try {
            if (useReadOnlyCache) {
                val currentPayload = readOnlyCacheMap[key]
                if (currentPayload != null) {
                    payload = currentPayload
                } else {
                    payload = readWriteCacheMap[key]
                    readOnlyCacheMap[key] = payload
                }
            } else {
                payload = readWriteCacheMap[key]
            }
        } catch (t: Throwable) {
        }
        return payload
    }

    private fun getPayload(key: Key, apps: Applications): String {
        val encoder = serverCodecs.getEncoder(Key.KeyType.JSON, EurekaAccept.full)
        return try {
            encoder.encode(apps as com.netflix.discovery.shared.Applications)
        } catch (ex: Exception) {
            ""
        }
    }

    private fun getPayload(key: Key, app: Application?): String {
        if (app == null) {
            return ""
        }
        val encoder = serverCodecs.getEncoder(Key.KeyType.JSON, EurekaAccept.full)
        return try {
            encoder.encode(app)
        } catch (e: Exception) {
            return ""
        }
    }

    private fun generatePayload(key: Key): String {
        return when (key.entityType) {
            Key.EntityType.Application -> {
                if ("ALL_APPS" == key.name) {
                    getPayload(key, registry.getApplications())
                } else {
                    getPayload(key, registry.getApplication(key.name))
                }
            }
            Key.EntityType.VIP -> {
                getPayload(key, getApplicationsForVip(key, registry))
            }
            else -> ""
        }
    }

    private fun getApplicationsForVip(key: Key, registry: AbstractInstanceRegistry): Applications {
        val toReturn = Applications()
        val applications = registry.getApplications()
        for (application in applications.registeredApplications) {
            var appToAdd: Application? = null
            for (instanceInfo in application.instances) {
                val vipAddress: String?
                if (Key.EntityType.VIP == key.entityType) {
                    vipAddress = instanceInfo.vipAddress
                } else {
                    continue
                }

                if (vipAddress != null) {
                    val contains = vipAddress.split(",")
                        .sorted()
                        .contains(key.name)
                    if (contains) {
                        if (appToAdd == null) {
                            appToAdd = Application(application.name)
                            toReturn.addApplication(appToAdd)
                        }
                        appToAdd.addInstance(instanceInfo)
                    }
                }
            }
        }
        toReturn.appsHashCode =toReturn.getReconcileHashCode()
        return toReturn
    }
}