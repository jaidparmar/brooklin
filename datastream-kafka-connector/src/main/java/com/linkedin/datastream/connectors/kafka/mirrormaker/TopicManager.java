/**
 *  Copyright 2019 LinkedIn Corporation. All rights reserved.
 *  Licensed under the BSD 2-Clause License. See the LICENSE file in the project root for license information.
 *  See the NOTICE file in the project root for additional information regarding copyright ownership.
 */
package com.linkedin.datastream.connectors.kafka.mirrormaker;

import java.util.Collection;

import org.apache.kafka.common.TopicPartition;


/**
 * The interface can be used to implement any topics-related management that mirror maker needs to do.
 */
public interface TopicManager {

  String COUNTER = "Counter";
  String GAUGE = "Gauge";

  /**
   * This gets called as part of Kafka's onPartitionsAssigned() callback, in case any topic management needs to be done.
   * @return List of topic partitions that need to be paused as result of topic management
   * (for example, in case topic couldn't be created on destination side).
   */
  Collection<TopicPartition> onPartitionsAssigned(Collection<TopicPartition> partitions);

  /**
   * This gets called as part of Kafka's onPartitionsRevoked() callback, in case any topic management needs to be done
   * when partitions are revoked by Kafka.
   */
  void onPartitionsRevoked(Collection<TopicPartition> partitions);

  /**
   * Method that mirror maker calls to check if a partition that Topic Manager deemed should be paused previously
   * should now be unpaused.
   * @return true if partition should be unpaused, false otherwise.
   */
  boolean shouldResumePartition(TopicPartition tp);

  /**
   * Method that mirror maker task calls whenever it stops. TopicManager can perform any cleanups it needs to do at this
   * point.
   */
  void stop();
}
