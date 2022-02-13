package com.mlacker.samples.kafka.producer

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

class ProducerApplication {

    private val brokers = listOf("192.168.20.20:9092")
    private val topic = "topic-sample"

    fun run() {
        val properties = Properties().apply {
            this[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = brokers
            // this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.qualifiedName
            // this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.qualifiedName
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