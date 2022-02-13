package com.mlacker.samples.kafka.consumer

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.*

class ConsumerApplication {

    private val brokers = listOf("kafka-1:9092")
    private val topic = "topic-sample"

    private val logger = LoggerFactory.getLogger(javaClass)

    fun run() {
        val deserializer = StringDeserializer()
        val properties = Properties().apply {
            this[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = brokers
            this[ConsumerConfig.GROUP_ID_CONFIG] = "consumer.group"
        }
        val consumer = KafkaConsumer(properties, deserializer, deserializer)
        consumer.subscribe(listOf(topic))

        while (true) {
            val records = consumer.poll(Duration.ofSeconds(2))

            for (record in records) {
                logger.info("received message: ${record.value()}")
            }
        }
    }
}

fun main() {
    ConsumerApplication()
        .run()
}