package com.mlacker.samples

import com.mlacker.samples.beans.factory.support.DefaultListableBeanFactory
import com.mlacker.samples.beans.factory.support.GenericBeanDefinition
import org.apache.commons.logging.LogFactory
import org.springframework.beans.factory.support.AbstractBeanDefinition
import org.springframework.context.annotation.*
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

fun main(args: Array<String>) {
    val log = LogFactory.getLog(Application::class.java)
    val applicationContext = AnnotationConfigApplicationContext(Application::class.java)

    val beanFactory = DefaultListableBeanFactory()

    for (beanName in applicationContext.beanDefinitionNames) {
        val obd = applicationContext.getBeanDefinition(beanName) as AbstractBeanDefinition
        val gbd = GenericBeanDefinition().apply {
            beanClassName = obd.beanClassName
            obd.scope?.let { scope = it }
            isLazyInit = obd.isLazyInit
            autowireMode = obd.autowireMode
            isAutowireCandidate = obd.isAutowireCandidate
            isPrimary = obd.isPrimary
        }
        if (beanName == "application") {
            gbd.beanClass = Application::class
            gbd.beanClassName = gbd.beanClass!!.qualifiedName
        }
        if (beanName == "beanA") {
            gbd.beanClass = BeanA::class
        }
        beanFactory.registerBeanDefinition(beanName, gbd)
    }
    beanFactory.preInstantiateSingletons()

    val beanC = applicationContext.getBean(BeanC::class.java)
    beanFactory.getBean(BeanC::class)

    log.debug(beanC)
    log.debug(beanC.beanA)
    log.debug(beanC.beanB)
}