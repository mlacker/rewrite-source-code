package com.mlacker.samples

import com.mlacker.samples.beans.factory.support.DefaultListableBeanFactory
import com.mlacker.samples.beans.factory.support.RootBeanDefinition
import org.apache.commons.logging.LogFactory
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

    val beanFactory = DefaultListableBeanFactory()
    beanFactory.registerBeanDefinition("application", RootBeanDefinition().apply { beanClass = Application::class })
    beanFactory.registerBeanDefinition("beanA", RootBeanDefinition().apply { beanClass = BeanA::class })
    beanFactory.registerBeanDefinition("beanB", RootBeanDefinition().apply { beanClass = BeanB::class })
    beanFactory.registerBeanDefinition("beanC", RootBeanDefinition().apply { beanClass = BeanC::class })

    val beanC = beanFactory.getBean<BeanC>("beanC")

    log.debug(beanC)
    log.debug(beanC.beanA)
    log.debug(beanC.beanB)
}