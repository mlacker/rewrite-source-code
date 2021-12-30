package com.mlacker.samples.web.servlet.mvc.method

import com.mlacker.samples.web.servlet.HandlerMapping
import com.mlacker.samples.web.servlet.handler.AbstractHandlerMethodMapping
import org.springframework.web.servlet.mvc.method.RequestMappingInfo
import java.util.Comparator
import javax.servlet.http.HttpServletRequest

abstract class RequestMappingInfoHandlerMapping: AbstractHandlerMethodMapping<RequestMappingInfo>() {

    // 83
    override fun getMappingPathPatterns(mapping: RequestMappingInfo): Set<String> {
        return mapping.patternsCondition.patterns
    }

    // 94
    override fun getMatchingMapping(mapping: RequestMappingInfo, req: HttpServletRequest): RequestMappingInfo? {
        return mapping.getMatchingCondition(req)
    }

    // 102
    override fun getMappingComparator(req: HttpServletRequest): Comparator<RequestMappingInfo> {
        return Comparator { info1, info2 -> info1.compareTo(info2, req) }
    }

    // 124
    override fun handleMatch(mapping: RequestMappingInfo, lookupPath: String, req: HttpServletRequest) {
        super.handleMatch(mapping, lookupPath, req)

        val bestPattern: String
        val uriVariables: Map<String, String>

        val patterns = mapping.patternsCondition.patterns
        if (patterns.isEmpty()) {
            bestPattern = lookupPath
            uriVariables = emptyMap()
        } else {
            bestPattern = patterns.iterator().next()
            uriVariables = pathMatcher.extractUriTemplateVariables(bestPattern, lookupPath)
        }

        req.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, bestPattern)

        val decodePathVariables = urlPathHelper.decodePathVariables(req, uriVariables)
        req.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, decodePathVariables)
    }
}