/**
 *  Copyright 2019 LinkedIn Corporation. All rights reserved.
 *  Licensed under the BSD 2-Clause License. See the LICENSE file in the project root for license information.
 *  See the NOTICE file in the project root for additional information regarding copyright ownership.
 */
package com.linkedin.datastream.common;

/**
 * Constants used in the metadata field of {@link BrooklinEnvelope}
 */
public class BrooklinEnvelopeMetadataConstants {

  /**
   * Codes to identify the different data change operations
   */
  public enum OpCode {
    INSERT,
    UPDATE,
    DELETE,
  }

  // Event opcode
  public static final String OPCODE = "OpCode";

  // Scn of the event
  public static final String SCN = "Scn";

  // Database to which the event belongs
  public static final String DATABASE = "Database";

  // Table for which the event belongs
  public static final String TABLE = "Table";

  // Timestamp of when the event was last modified in the source where the event was generated
  public static final String EVENT_TIMESTAMP = "EventTimestamp";

  // Timestamp of the event when it was written in the last leg (i.e. the Source of the connector)
  public static final String SOURCE_TIMESTAMP = "SourceTimestamp";

  // Source partition number from where the event was generated
  public static final String SOURCE_PARTITION = "SourcePartition";
}
