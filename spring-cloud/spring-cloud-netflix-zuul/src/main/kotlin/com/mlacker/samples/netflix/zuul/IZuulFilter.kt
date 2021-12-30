package com.mlacker.samples.netflix.zuul

interface IZuulFilter {

    val shouldFilter: Boolean

    fun run(): Any
}