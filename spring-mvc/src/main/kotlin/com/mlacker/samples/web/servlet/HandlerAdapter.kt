package com.mlacker.samples.web.servlet

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

interface HandlerAdapter {

    fun supports(handler: Any): Boolean

    fun handle(req: HttpServletRequest, resp: HttpServletResponse, handler: Any)
}