/**
 *  Copyright 2019 LinkedIn Corporation. All rights reserved.
 *  Licensed under the BSD 2-Clause License. See the LICENSE file in the project root for license information.
 *  See the NOTICE file in the project root for additional information regarding copyright ownership.
 */
package com.linkedin.datastream.connectors.kafka;

import java.util.Comparator;
import java.util.Objects;


/**
 * The object representation of a Kafka Broker
 */
public class KafkaBrokerAddress {
  public static final Comparator<KafkaBrokerAddress> BY_URL =
      Comparator.comparing((KafkaBrokerAddress o) -> o._hostName).thenComparingInt(o -> o._portNumber);

  private final String _hostName;
  private final int _portNumber;

  /**
   * Construct a KafkaBrokerAddress
   * @param hostName Kafka broker hostname
   * @param portNumber Port number on Kafka broker
   */
  public KafkaBrokerAddress(String hostName, int portNumber) {
    validatePortNumber(portNumber);
    this._hostName = validateHostname(hostName);
    this._portNumber = portNumber;
  }

  public String getHostName() {
    return _hostName;
  }

  public int getPortNumber() {
    return _portNumber;
  }

  @Override
  public String toString() {
    return _hostName + ":" + _portNumber;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    KafkaBrokerAddress that = (KafkaBrokerAddress) o;
    return _portNumber == that._portNumber && Objects.equals(_hostName, that._hostName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_hostName, _portNumber);
  }

  /**
   * Create a KafkaBrokerAddress by parsing hostname and port from {@code brokerAddress}
   * @param brokerAddress
   *  brokerAddress in the form hostname:port
   */
  public static KafkaBrokerAddress valueOf(String brokerAddress) throws IllegalArgumentException {
    if (brokerAddress == null) {
      badArg(null);
    }
    String str = brokerAddress.trim();
    if (str.isEmpty()) {
      badArg(brokerAddress);
    }
    int lastColon = str.lastIndexOf(":");
    if (lastColon == -1) {
      badArg(brokerAddress); //at least host:port
    }
    int portNumber = -1;
    try {
      portNumber = Integer.parseInt(str.substring(lastColon + 1).trim());
      validatePortNumber(portNumber);
    } catch (NumberFormatException e) {
      badArg(brokerAddress);
    }
    String hostName = str.substring(0, lastColon);
    hostName = validateHostname(hostName);

    return new KafkaBrokerAddress(hostName, portNumber);
  }

  private static void validatePortNumber(int portNumber) throws IllegalArgumentException {
    if (portNumber <= 0 || portNumber > 65535) {
      throw new IllegalArgumentException(portNumber + " is not a valid port number");
    }
  }

  private static String validateHostname(String hostName) throws IllegalArgumentException {
    if (hostName == null) {
      throw new IllegalArgumentException("null host name");
    }
    String trimmed = hostName.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("empty host name");
    }
    return trimmed;
  }

  private static void badArg(String arg) throws IllegalArgumentException {
    throw new IllegalArgumentException(arg + " is not a valid broker address");
  }
}
