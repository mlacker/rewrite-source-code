package com.mlacker.samples.netflix.appinfo

import com.fasterxml.jackson.annotation.JsonIgnore
import com.netflix.appinfo.LeaseInfo
import java.util.concurrent.ConcurrentHashMap

data class InstanceInfo(
    val instanceId: String,
    val appName: String,
    val ipAddr: String,
    val port: Int,
    val homePageUrl: String,
    val healthCheckUrl: String,
    val vipAddress: String?,
    val hostName: String,
    var status: InstanceStatus = InstanceStatus.UP,
    val overriddenStatus: InstanceStatus = InstanceStatus.UNKNOWN,
    val leaseInfo: LeaseInfo,
    val metadata: Map<String, String> = ConcurrentHashMap(),
    val lastUpdatedTimestamp: Long = System.currentTimeMillis(),
    var lastDirtyTimestamp: Long = lastUpdatedTimestamp,
    val actionType: ActionType? = null,
) {
    private var isInstanceInfoDirty: Boolean = false

    val id: String
        get() = instanceId

    fun setStatus(status: InstanceStatus): InstanceStatus? {
        if (this.status != status) {
            val prev = this.status
            this.status = status
            setIsDirty()
            return prev
        }
        return null
    }

    val isDirty: Boolean
        @JsonIgnore get() = isInstanceInfoDirty

    val isDirtyWithTime: Long?
        get() = if (isInstanceInfoDirty) lastDirtyTimestamp else null

    fun setIsDirty() {
        isInstanceInfoDirty = true
        lastDirtyTimestamp = System.currentTimeMillis()
    }

    fun setIsDirtyWithTime(): Long {
        setIsDirty()
        return lastDirtyTimestamp
    }

    fun unsetIsDirty(unsetDirtyTimestamp: Long) {
        if (lastDirtyTimestamp <= unsetDirtyTimestamp) {
            isInstanceInfoDirty = false
        }
    }

    override fun hashCode(): Int {
        return instanceId.hashCode() + 31
    }

    override fun equals(other: Any?): Boolean {
        return when {
            this === other -> true
            null == other -> false
            other is InstanceInfo -> this.id === other.id
            else -> false
        }
    }

}