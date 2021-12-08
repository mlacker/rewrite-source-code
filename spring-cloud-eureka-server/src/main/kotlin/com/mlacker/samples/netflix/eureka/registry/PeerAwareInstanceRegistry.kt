package com.mlacker.samples.netflix.eureka.registry

import com.mlacker.samples.netflix.appinfo.ApplicationInfoManager
import com.mlacker.samples.netflix.appinfo.InstanceInfo
import com.mlacker.samples.netflix.appinfo.InstanceStatus
import com.mlacker.samples.netflix.discovery.shared.Application
import com.mlacker.samples.netflix.discovery.shared.LookupService
import com.mlacker.samples.netflix.eureka.lease.LeaseManager
import com.netflix.eureka.registry.ResponseCache

interface PeerAwareInstanceRegistry : LeaseManager<InstanceInfo>, LookupService {

    fun openForTraffic(applicationInfoManager: ApplicationInfoManager, count: Int)

    fun shutdown()

    fun storeOverriddenStatusIfRequired(appName: String, id: String, overriddenStatus: InstanceStatus)

    fun statusUpdate(appName: String, id: String, newStatus: InstanceStatus, lastDirtyTimestamp: String, isReplication: Boolean): Boolean

    fun deleteStatusOverride(appName: String, id: String, newStatus: InstanceStatus, lastDirtyTimestamp: String, isReplication: Boolean): Boolean

    fun overriddenInstanceStatusesSnapshot(): Map<String, InstanceStatus>

    fun getStoredApplications(): List<Application>

    fun getInstanceByAppAndId(appName: String, id: String): InstanceInfo

    fun clearRegistry()

    fun initializedResponseCache()

    fun getResponseCache(): ResponseCache

    fun getNumOfRenewsInLastMin(): Long

    fun getNumOfRenewsPerMinThreshold(): Int

    fun isBelowRenewThreshold()

    fun getLastNRegisteredInstances(): List<Pair<Long, String>>

    fun getLastNCanceledInstances(): List<Pair<Long, String>>

    fun isLeaseExpirationEnabled(): Boolean

    fun isSelfPreservationModeEnabled(): Boolean
}
