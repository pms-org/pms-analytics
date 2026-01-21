// package com.pms.analytics.config;

// import java.util.HashMap;
// import java.util.Map;

// import org.apache.kafka.clients.admin.NewTopic;
// import org.apache.kafka.clients.consumer.ConsumerConfig;
// import org.apache.kafka.clients.producer.ProducerConfig;
// import org.apache.kafka.common.serialization.StringDeserializer;
// import org.apache.kafka.common.serialization.StringSerializer;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.context.annotation.Primary;
// import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
// import org.springframework.kafka.config.TopicBuilder;
// import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
// import org.springframework.kafka.core.DefaultKafkaProducerFactory;
// import org.springframework.kafka.core.KafkaTemplate;
// import org.springframework.kafka.core.ProducerFactory;
// import org.springframework.kafka.listener.ContainerProperties;
// import org.springframework.kafka.listener.DefaultErrorHandler;
// import org.springframework.util.backoff.FixedBackOff;

// import com.pms.analytics.dto.RiskEventOuterClass;
// import com.pms.analytics.dto.TransactionOuterClass.Transaction;

// import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
// import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;

// @Configuration
// public class KafkaConfig {

//     @Value("${spring.kafka.bootstrap-servers}")
//     private String bootstrapServers;

//     @Value("${spring.kafka.properties.schema.registry.url}")
//     private String schemaRegistryUrl;

//     @Value("${spring.kafka.consumer.group-id}")
//     private String consumerGroupId;

//     @Value("${app.kafka-topic}")
//     private String transactionsTopic;

//     @Bean
//     public NewTopic transactionsTopic() {
//         return TopicBuilder.name(transactionsTopic)
//                 .partitions(5)
//                 .replicas(1)
//                 .build();
//     }

//     @Bean
//     public ProducerFactory<String, Transaction> producerFactory() {
//         Map<String, Object> props = new HashMap<>();
//         props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//         props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//         props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class);
//         props.put("schema.registry.url", schemaRegistryUrl);

//         props.put(ProducerConfig.RETRIES_CONFIG, 5);
//         props.put(ProducerConfig.ACKS_CONFIG, "all");
//         props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
//         props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
//         props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 30000);
//         props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 15000);

//         return new DefaultKafkaProducerFactory<>(props);
//     }

//     @Bean
//     @Primary
//     public KafkaTemplate<String, Transaction> kafkaTemplate() {
//         return new KafkaTemplate<>(producerFactory());
//     }

//     @Bean(name = "protobufKafkaListenerContainerFactory")
//     public ConcurrentKafkaListenerContainerFactory<String, Transaction> protobufKafkaListenerContainerFactory() {
//         Map<String, Object> props = new HashMap<>();
//         props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//         props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
//         props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
//         props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaProtobufDeserializer.class);
//         props.put("schema.registry.url", schemaRegistryUrl);
//         props.put("specific.protobuf.value.type", Transaction.class.getName());
//         props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
//         props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 20000);
//         props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 20000);

//         DefaultKafkaConsumerFactory<String, Transaction> consumerFactory
//                 = new DefaultKafkaConsumerFactory<>(props);

//         ConcurrentKafkaListenerContainerFactory<String, Transaction> factory
//                 = new ConcurrentKafkaListenerContainerFactory<>();

//         factory.setConsumerFactory(consumerFactory);
//         factory.setBatchListener(true);
//         factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
//         factory.setCommonErrorHandler(errorHandler());

//         return factory;
//     }

//     @Bean
//     public ProducerFactory<String, RiskEventOuterClass.RiskEvent> riskEventProducerFactory() {
//         Map<String, Object> props = new HashMap<>();
//         props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
//         props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//         props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
//                 io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer.class);
//         props.put("schema.registry.url", schemaRegistryUrl);
//         return new DefaultKafkaProducerFactory<>(props);
//     }

//     @Bean
//     public KafkaTemplate<String, RiskEventOuterClass.RiskEvent> riskEventKafkaTemplate() {
//         return new KafkaTemplate<>(riskEventProducerFactory());
//     }

//     @Bean
//     public DefaultErrorHandler errorHandler() {

//         FixedBackOff backOff = new FixedBackOff(
//                 2000L, // wait 2 seconds between retries
//                 3 // retry 3 times
//         );

//         DefaultErrorHandler handler
//                 = new DefaultErrorHandler(
//                         (record, ex) -> {
//                             // retries exhausted
//                             // do nothing
//                         },
//                         backOff
//                 );

//         handler.setAckAfterHandle(false); // DO NOT COMMIT OFFSET
//         return handler;
//     }

// }

package com.pms.analytics.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import com.pms.analytics.dto.RiskEventOuterClass.RiskEvent;
import com.pms.analytics.dto.TransactionOuterClass.Transaction;

import io.confluent.kafka.serializers.protobuf.KafkaProtobufDeserializer;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroupId;

    @Value("${app.kafka.consumer-topic}")
    private String consumerTopic;

    @Value("${spring.kafka.producer.properties.delivery.timeout.ms}")
    private int deliveryTimeoutMs;

    @Value("${outbox.delay.system-failure-ms}")
    private long retryBackoffMs;

    @Value("${outbox.delay.retry-attempts}")
    private long retryAttempts;

    // ----------------------------
    // Topic
    // ----------------------------
    @Bean
    public NewTopic consumerTopic() {
        return TopicBuilder.name(consumerTopic)
                .partitions(5)
                .replicas(1)
                .build();
    }

    // ----------------------------
    // Producer (Transaction)
    // ----------------------------
    @Bean
    public ProducerFactory<String, Transaction> producerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class);
        props.put("schema.registry.url", schemaRegistryUrl);

        props.put(ProducerConfig.RETRIES_CONFIG, 5);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, deliveryTimeoutMs);
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 15000);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    @Primary
    public KafkaTemplate<String, Transaction> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ----------------------------
    // Consumer
    // ----------------------------
    @Bean(name = "protobufKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Transaction>
    protobufKafkaListenerContainerFactory() {

        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaProtobufDeserializer.class);
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("specific.protobuf.value.type", Transaction.class.getName());
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 20000);
        props.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 20000);


        DefaultKafkaConsumerFactory<String, Transaction> consumerFactory =
                new DefaultKafkaConsumerFactory<>(props);

        ConcurrentKafkaListenerContainerFactory<String, Transaction> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setCommonErrorHandler(errorHandler());

        return factory;
    }

    // ----------------------------
    // Producer (RiskEvent)
    // ----------------------------
    @Bean
    public ProducerFactory<String, RiskEvent> riskEventProducerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class);
        props.put("schema.registry.url", schemaRegistryUrl);

        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, RiskEvent> riskEventKafkaTemplate() {
        return new KafkaTemplate<>(riskEventProducerFactory());
    }

    // ----------------------------
    // Error Handling
    // ----------------------------
    @Bean
    public DefaultErrorHandler errorHandler() {
        FixedBackOff backOff = new FixedBackOff(retryBackoffMs, retryAttempts);
        DefaultErrorHandler handler
                = new DefaultErrorHandler(
                        (record, ex) -> {
                            // retries exhausted
                            // do nothing
                        },
                        backOff
                );        
        handler.setAckAfterHandle(false);
        return handler;
    }
}
