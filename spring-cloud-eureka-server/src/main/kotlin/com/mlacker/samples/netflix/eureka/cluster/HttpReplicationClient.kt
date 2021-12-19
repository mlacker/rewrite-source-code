package com.mlacker.samples.netflix.eureka.cluster

import com.mlacker.samples.netflix.discovery.shared.transport.RegistrationClient
import com.netflix.discovery.shared.transport.EurekaHttpResponse
import com.netflix.eureka.cluster.protocol.ReplicationList
import com.netflix.eureka.cluster.protocol.ReplicationListResponse

interface HttpReplicationClient : RegistrationClient {

    fun submitBatchUpdates(replicationList: ReplicationList): EurekaHttpResponse<ReplicationListResponse>

    fun shutdown()
}