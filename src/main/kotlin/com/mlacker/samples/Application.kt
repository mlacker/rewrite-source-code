package com.mlacker.samples

import org.apache.commons.logging.LogFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component

@Configuration
@ComponentScan(basePackageClasses = [Application::class])
class Application() {

    @Bean
    fun beanA(): BeanA {
        return BeanA()
    }
}

class BeanA

@Component
class BeanB

@Component
class BeanC(
        val beanA: BeanA,
        val beanB: BeanB
)

fun main() {
    val log = LogFactory.getLog(Application::class.java)
    val applicationContext = AnnotationConfigApplicationContext(Application::class.java)

    val beanC = applicationContext.getBean(BeanC::class.java)
    applicationContext.getBean(BeanC::class.java)

    log.debug(beanC)
    log.debug(beanC.beanA)
    log.debug(beanC.beanB)
}