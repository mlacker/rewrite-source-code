package com.mlacker.samples.web.servlet.handler

import com.mlacker.samples.web.servlet.HandlerMapping
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.InitializingBean
import org.springframework.core.MethodIntrospector
import org.springframework.util.ClassUtils
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.method.HandlerMethod
import java.lang.reflect.Method
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.servlet.http.HttpServletRequest

abstract class AbstractHandlerMethodMapping<T> : AbstractHandlerMapping(), InitializingBean {

    private val mappingRegistry = MappingRegistry()

    // 204
    override fun afterPropertiesSet() {
        initHandlerMethods()
    }

    // 214
    private fun initHandlerMethods() {
        val beanNames = obtainApplicationContext().getBeanNamesForType(Any::class.java)
        for (beanName in beanNames) {
            var beanType: Class<*>? = null
            try {
                beanType = obtainApplicationContext().getType(beanName)
            } catch (ex: Throwable) {
            }
            if (beanType != null && isHandler(beanType)) {
                detectHandlerMethods(beanName)
            }
        }
    }

    // 267
    private fun detectHandlerMethods(handler: String) {
        val handlerType = obtainApplicationContext().getType(handler)

        if (handlerType != null) {
            val userType = ClassUtils.getUserClass(handlerType)
            val methods = MethodIntrospector.selectMethods(userType,
                    MethodIntrospector.MetadataLookup { method ->
                        try {
                            return@MetadataLookup getMappingForMethod(method, userType)
                        } catch (ex: Throwable) {
                            throw IllegalStateException("Invalid mapping on handler class [${userType.name}: $method", ex)
                        }
                    })
            methods.forEach { (method, mapping) ->
                val invocableMethod = AopUtils.selectInvocableMethod(method, userType)
                registerHandlerMethod(handler, invocableMethod, mapping)
            }
        }
    }

    // 317
    private fun registerHandlerMethod(handler: Any, method: Method, mapping: T) {
        this.mappingRegistry.register(mapping, handler, method)
    }

    // 327
    private fun createHandlerMethod(handler: Any, method: Method): HandlerMethod {
        if (handler is String) {
            return HandlerMethod(handler,
                    obtainApplicationContext().autowireCapableBeanFactory, method)
        }
        return HandlerMethod(handler, method)
    }

    //  362
    override fun getHandlerInternal(req: HttpServletRequest): HandlerMethod? {
        val lookupPath = urlPathHelper.getLookupPathForRequest(req)
        req.setAttribute(HandlerMapping.LOOKUP_PATH, lookupPath)
        this.mappingRegistry.acquireReadLock()
        try {
            return lookupHandlerMethod(lookupPath, req)?.createWithResolvedBean()
        } finally {
            this.mappingRegistry.releaseReadLock()
        }
    }

    // 385
    private fun lookupHandlerMethod(lookupPath: String, req: HttpServletRequest): HandlerMethod? {
        val matches = mutableListOf<Match>()
        val directPathMatches = this.mappingRegistry.getMappingsByUrl(lookupPath)
        if (directPathMatches != null) {
            addMatchingMappings(directPathMatches, matches, req)
        }
        if (matches.isEmpty()) {
            addMatchingMappings(this.mappingRegistry.mappings.keys, matches, req)
        }

        if (matches.isNotEmpty()) {
            var bestMatch = matches.first()
            if (matches.size > 1) {
                val comparator = MatchComparator(getMappingComparator(req))
                matches.sortWith(comparator)
                bestMatch = matches.first()
                val secondBestMatch = matches.component2()
                if (comparator.compare(bestMatch, secondBestMatch) == 0) {
                    val m1 = bestMatch.handlerMethod.method
                    val m2 = secondBestMatch.handlerMethod.method
                    val uri = req.requestURI
                    throw IllegalStateException(
                            "Ambiguous handler methods mapped for '$uri': {$m1, $m2}"
                    )
                }
            }
            req.setAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE, bestMatch.handlerMethod)
            handleMatch(bestMatch.mapping, lookupPath, req)
            return bestMatch.handlerMethod
        } else {
            return handleNoMatch(this.mappingRegistry.mappings.keys, lookupPath, req)
        }
    }

    // 426
    private fun addMatchingMappings(mappings: Collection<T>, matches: MutableList<Match>, req: HttpServletRequest) {
        for (mapping in mappings) {
            val match = getMatchingMapping(mapping, req)
            if (match != null) {
                matches.add(Match(match, this.mappingRegistry.mappings[mapping]!!))
            }
        }
    }

    // 441
    protected open fun handleMatch(mapping: T, lookupPath: String, req: HttpServletRequest) {
        req.setAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE, lookupPath)
    }

    // 453
    protected open fun handleNoMatch(mappings: Set<T>, lookupPath: String, req: HttpServletRequest): HandlerMethod? {
        return null
    }

    // 489
    protected abstract fun isHandler(beanType: Class<*>): Boolean

    // 500
    protected abstract fun getMappingForMethod(method: Method, handlerType: Class<*>): T?

    // 505
    protected abstract fun getMappingPathPatterns(mapping: T): Set<String>

    // 515
    protected abstract fun getMatchingMapping(mapping: T, req: HttpServletRequest): T?

    // 523
    protected abstract fun getMappingComparator(req: HttpServletRequest): java.util.Comparator<T>

    private inner class MappingRegistry {

        val mappings: MutableMap<T, HandlerMethod> = LinkedHashMap()

        private val urlLookup: MultiValueMap<String, T> = LinkedMultiValueMap()

        private val readWriteLock = ReentrantReadWriteLock()

        fun getMappingsByUrl(lookupPath: String): MutableList<T>? =
                urlLookup[lookupPath]

        fun acquireReadLock() = this.readWriteLock.readLock().lock()

        fun releaseReadLock() = this.readWriteLock.readLock().unlock()

        fun register(mapping: T, handler: Any, method: Method) {
            this.readWriteLock.writeLock().lock()
            try {
                val handlerMethod = createHandlerMethod(handler, method)
                this.mappings[mapping] = handlerMethod

                val directUrls = getDirectUrls(mapping)
                for (url in directUrls) {
                    this.urlLookup.add(url, mapping)
                }
            } finally {
                this.readWriteLock.writeLock().unlock()
            }
        }

        private fun getDirectUrls(mapping: T): List<String> {
            return getMappingPathPatterns(mapping).filter { !pathMatcher.isPattern(it) }
        }
    }

    private inner class Match(
            val mapping: T,
            val handlerMethod: HandlerMethod
    ) {
        override fun toString() = this.mapping.toString()
    }

    private inner class MatchComparator(
            private val comparator: Comparator<T>
    ) : Comparator<Match> {

        override fun compare(o1: Match, o2: Match): Int {
            return this.comparator.compare(o1.mapping, o2.mapping)
        }
    }
}