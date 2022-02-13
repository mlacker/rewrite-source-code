package com.mlacker.samples.kafka.producer

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

class ProducerApplication {

    private val brokers = listOf("kafka-1:9092")
    private val topic = "topic-sample"

    fun run() {
        val properties = Properties().apply {
            this[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = brokers
        }
        val serializer = StringSerializer()
        val producer = KafkaProducer(properties, serializer, serializer)

        val record = ProducerRecord<String, String>(topic, "Hello, Kafka!")

        producer.send(record)

        producer.close()
    }
}

fun main() {
    ProducerApplication().run()
}