package com.mlacker.samples.cloud.netflix.zuul.filters.route

import com.netflix.hystrix.HystrixExecutable
import org.springframework.http.client.ClientHttpResponse

interface RibbonCommand: HystrixExecutable<ClientHttpResponse>
