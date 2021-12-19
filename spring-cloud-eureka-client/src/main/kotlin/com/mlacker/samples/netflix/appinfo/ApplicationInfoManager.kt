package com.mlacker.samples.netflix.appinfo

import javax.inject.Singleton

@Singleton
class ApplicationInfoManager(
    private val instanceInfo: InstanceInfo
) {
    val info: InstanceInfo = instanceInfo

    init {
        instance = this
    }

    fun setInstanceStatus(status: InstanceStatus) {
        instanceInfo.setStatus(status)
    }

    companion object {
        @Deprecated("Deprecated please use DI instead")
        lateinit var instance: ApplicationInfoManager
    }
}