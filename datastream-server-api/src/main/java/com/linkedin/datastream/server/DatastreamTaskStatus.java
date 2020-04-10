/**
 *  Copyright 2019 LinkedIn Corporation. All rights reserved.
 *  Licensed under the BSD 2-Clause License. See the LICENSE file in the project root for license information.
 *  See the NOTICE file in the project root for additional information regarding copyright ownership.
 */
package com.linkedin.datastream.server;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Represent the status of a DatastreamTask with a code and message.
 */
public class DatastreamTaskStatus {
  private static final Logger LOG = LoggerFactory.getLogger(DatastreamTaskStatus.class);

  /**
   * DatastreamTask status code
   */
  public enum Code { OK, ERROR, COMPLETE, PAUSED }

  private Code _code;
  private String _message;
  private long _timeStamp = System.currentTimeMillis();
  private String _hostName;

  /**
   * Constructor for DatastreamTaskStatus (needed for JSON deserialization)
   */
  public DatastreamTaskStatus() {
    _hostName = "";
  }

  /**
   * Construct an instance of DatastreamTaskStatus
   */
  public DatastreamTaskStatus(Code code, String message) {
    if (code != Code.ERROR) {
      Validate.notEmpty(message, "must provide a message for ERROR status.");
    }
    _code = code;
    _message = message;
    _hostName = localHostName();
  }

  /**
   * Helper method to create an OK status.
   * @return OK task status
   */
  public static DatastreamTaskStatus ok() {
    return new DatastreamTaskStatus(Code.OK, "OK");
  }

  /**
   * Helper method to create an OK status.
   * @param message message to return
   * @return OK task status
   */
  public static DatastreamTaskStatus ok(String message) {
    return new DatastreamTaskStatus(Code.OK, message);
  }

  /**
   * Helper method to create an ERROR status
   * @return ERROR task status
   */
  public static DatastreamTaskStatus error(String message) {
    return new DatastreamTaskStatus(Code.ERROR, message);
  }

  /**
   * Helper method to create a COMPLETE status
   * @return COMPLETE task status
   */
  public static DatastreamTaskStatus complete() {
    return new DatastreamTaskStatus(Code.COMPLETE, "Completed.");
  }

  /**
   * Helper method to create a Paused status.
   * @return PAUSED task status
   */
  public static DatastreamTaskStatus paused() {
    return new DatastreamTaskStatus(Code.PAUSED, "Paused");
  }

  private String localHostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      LOG.warn("Couldn't get the hostname");
      return "unknown";
    }
  }

  /**
   * Get the timestamp of the status
   */
  public long getTimeStamp() {
    return _timeStamp;
  }

  /**
   * Set the timestamp. Needed for JsonUtils.
   * @param timeStamp timestamp
   */
  public void setTimeStamp(long timeStamp) {
    _timeStamp = timeStamp;
  }

  /**
   * Get the hostname where the last status was written
   */
  public String getHostName() {
    return _hostName;
  }

  /**
   * Set the hostname. Needed for JsonUtils.
   * @param hostName hostName
   */
  public void setHostName(String hostName) {
    _hostName = hostName;
  }

  /**
   * Get kind of the status
   */
  public Code getCode() {
    return _code;
  }

  /**
   * Set Code of the status. Needed for JsonUtils.
   * @param code status code
   */
  public void setCode(Code code) {
    _code = code;
  }

  /**
   * Get message associated with the status
   */
  public String getMessage() {
    return _message;
  }

  /**
   * Set message of the status. Needed for JsonUtils.
   * @param message status message
   */
  public void setMessage(String message) {
    _message = message;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof DatastreamTaskStatus)) {
      return false;
    }

    DatastreamTaskStatus that = (DatastreamTaskStatus) obj;
    return Objects.equals(this.getCode(), that.getCode()) || Objects.equals(this.getMessage(), that.getMessage());
  }

  @Override
  public int hashCode() {
    return Objects.hash(this.getCode(), this.getMessage());
  }

  @Override
  public String toString() {
    return String.format("TaskStatus: Timestamp=%d, hostname=%s, code=%s, msg=%s, ", _timeStamp, _hostName, _code,
        _message);
  }
}
