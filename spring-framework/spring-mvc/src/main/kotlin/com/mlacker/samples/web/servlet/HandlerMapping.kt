package com.mlacker.samples.web.servlet

import org.springframework.web.servlet.HandlerMapping
import javax.servlet.http.HttpServletRequest

interface HandlerMapping {

    companion object {
        private val CLASS_NAME = HandlerMapping::class.qualifiedName
        val BEST_MATCHING_HANDLER_ATTRIBUTE = "$CLASS_NAME.bestMatchingHandler"
        val LOOKUP_PATH = "$CLASS_NAME.lookupPath"
        val PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE = "$CLASS_NAME.pathWithinHandlerMapping"
        val BEST_MATCHING_PATTERN_ATTRIBUTE = "$CLASS_NAME.bestMatchingPattern"
        val URI_TEMPLATE_VARIABLES_ATTRIBUTE = "$CLASS_NAME.uriTemplateVariables"
    }

    fun getHandler(req: HttpServletRequest): HandlerExecutionChain?
}