package com.mlacker.samples.netflix.loadbalancer

import com.netflix.loadbalancer.Server

interface ServerOperation<T> : (Server) -> T