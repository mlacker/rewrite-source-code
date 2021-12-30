package com.mlacker.samples.netflix.eureka.lease

/**
 * This class is responsible for creating/renewing and evicting a <em>lease</em>
 * for a particular instance.
 *
 * <p>
 * Leases determine what instances receive traffic. When there is no renewal
 * request from the client, the lease gets expired and the instances are evicted
 * out of {@link AbstractInstanceRegistry}. This is key to instances receiving traffic
 * or not.
 * <p>
 */
interface LeaseManager<T> {

    fun register(r: T, leaseDuration: Int, isReplication: Boolean)

    fun cancel(appName: String, id: String, isReplication: Boolean): Boolean

    fun renew(appName: String, id: String, isReplication: Boolean): Boolean

    fun evict()
}
