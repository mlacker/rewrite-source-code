package com.mlacker.samples

import org.apache.commons.logging.LogFactory
import org.springframework.context.annotation.*
import org.springframework.stereotype.Component

@Configuration
@ComponentScan(basePackageClasses = [Application::class])
class Application {

    @Bean
    fun beanA(): BeanA {
        return BeanA()
    }
}

class BeanA

@Component
class BeanB

fun main(args: Array<String>) {
    val log = LogFactory.getLog(Application::class.java)
    val applicationContext = AnnotationConfigApplicationContext(Application::class.java)

    val beanA = applicationContext.getBean(BeanA::class.java)
    val beanB = applicationContext.getBean(BeanB::class.java)

    log.info(beanA)
    log.info(beanB)
}