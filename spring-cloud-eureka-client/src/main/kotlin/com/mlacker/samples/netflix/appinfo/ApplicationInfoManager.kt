package com.mlacker.samples.netflix.appinfo

import javax.inject.Singleton

@Singleton
class ApplicationInfoManager(
    private val instanceInfo: InstanceInfo
) {

    val info: InstanceInfo = instanceInfo

    fun setInstanceStatus(status: InstanceInfo.InstanceStatus) {
        instanceInfo.setStatus(status)
    }
}