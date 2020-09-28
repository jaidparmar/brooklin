/**
 *  Copyright 2019 LinkedIn Corporation. All rights reserved.
 *  Licensed under the BSD 2-Clause License. See the LICENSE file in the project root for license information.
 *  See the NOTICE file in the project root for additional information regarding copyright ownership.
 */
package com.linkedin.datastream.connectors.kafka;

import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;

import com.linkedin.datastream.kafka.factory.KafkaConsumerFactory;
import com.linkedin.kafka.clients.consumer.LiKafkaConsumerImpl;


/**
 * Factory for creating Kafka {@link Consumer} instances with {@code byte[]} keys and values
 */
public class LiKafkaConsumerFactory implements KafkaConsumerFactory<byte[], byte[]> {

  @Override
  public Consumer<byte[], byte[]> createConsumer(Properties properties) {
    properties.put("key.deserializer", ByteArrayDeserializer.class.getCanonicalName());
    properties.put("value.deserializer", ByteArrayDeserializer.class.getCanonicalName());
    return new LiKafkaConsumerImpl<>(properties);
  }
}
