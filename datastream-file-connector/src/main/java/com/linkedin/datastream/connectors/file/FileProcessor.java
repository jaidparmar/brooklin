/**
 *  Copyright 2019 LinkedIn Corporation. All rights reserved.
 *  Licensed under the BSD 2-Clause License. See the LICENSE file in the project root for license information.
 *  See the NOTICE file in the project root for additional information regarding copyright ownership.
 */
package com.linkedin.datastream.connectors.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.CountingInputStream;

import com.linkedin.datastream.common.BrooklinEnvelope;
import com.linkedin.datastream.common.BrooklinEnvelopeMetadataConstants;
import com.linkedin.datastream.connectors.file.diag.FilePositionKey;
import com.linkedin.datastream.connectors.file.diag.FilePositionValue;
import com.linkedin.datastream.server.DatastreamEventProducer;
import com.linkedin.datastream.server.DatastreamProducerRecordBuilder;
import com.linkedin.datastream.server.DatastreamTask;


class FileProcessor implements Runnable {
  private static final Logger LOG = LoggerFactory.getLogger(FileConnector.class);

  private static final int PARTITION = 0;
  private static final int POLL_WAIT_MS = 100;
  private static final Duration ACQUIRE_TIMEOUT = Duration.ofMinutes(5);

  private final DatastreamTask _task;
  private final String _fileName;
  private final DatastreamEventProducer _producer;
  private final BufferedReader _fileReader;
  private final CountingInputStream _inputStream;
  private final AtomicInteger _lineNo;
  private final FilePositionKey _positionKey;
  private final FilePositionValue _positionValue;
  private boolean _cancelRequested;
  private boolean _isStopped;

  public FileProcessor(DatastreamTask datastreamTask, DatastreamEventProducer producer) throws FileNotFoundException {
    _task = datastreamTask;
    _fileName = datastreamTask.getDatastreamSource().getConnectionString();
    _positionKey = new FilePositionKey(_task.getTaskPrefix(), _task.getDatastreamTaskName(), Instant.now(), _fileName);
    _positionValue = new FilePositionValue();

    // Set up input streams/readers
    final File file = new File(_fileName);
    _positionValue.setFileLengthBytes(file.length());
    _inputStream = new CountingInputStream(new FileInputStream(file));
    _fileReader = new BufferedReader(new InputStreamReader(_inputStream, StandardCharsets.UTF_8));
    _lineNo = new AtomicInteger();

    _producer = producer;
    _isStopped = false;
    _cancelRequested = false;
    LOG.info("Created FileProcessor for " + datastreamTask);
  }

  private int loadCheckpoint() throws IOException {
    int lineNo = 0;
    Map<Integer, String> savedCheckpoints = _task.getCheckpoints();
    String cpString = savedCheckpoints.getOrDefault(PARTITION, null);
    if (cpString != null && !cpString.isEmpty()) {
      // Resume from last saved line number
      lineNo = Integer.parseInt(cpString);

      // Must skip line by line as we can't get the actual file position of the
      // last newline character due to the buffering done by BufferedReader.
      for (int i = 0; i < lineNo; i++) {
        _fileReader.readLine();
      }

      LOG.info("Resumed from line " + lineNo);
    } else {
      LOG.info("Resumed from beginning");
    }

    return lineNo + 1;
  }

  @Override
  public void run() {
    try {
      _task.acquire(ACQUIRE_TIMEOUT);

      _lineNo.set(loadCheckpoint());
      _positionValue.setLinesRead((long) _lineNo.intValue());
      while (!_cancelRequested) {
        String text;
        try {
          text = _fileReader.readLine();
          _positionValue.setBytesRead(_inputStream.getCount());
        } catch (IOException e) {
          throw new RuntimeException("Reading file failed.", e);
        }
        if (text != null) {
          // Using the line# as the key

          HashMap<String, String> eventMetadata = new HashMap<>();
          long currentTimeMillis = System.currentTimeMillis();
          eventMetadata.put(BrooklinEnvelopeMetadataConstants.EVENT_TIMESTAMP, String.valueOf(currentTimeMillis));
          BrooklinEnvelope event =
              new BrooklinEnvelope(_lineNo.toString().getBytes(StandardCharsets.UTF_8),
                  text.getBytes(StandardCharsets.UTF_8), null, eventMetadata);

          LOG.info("sending event " + text);
          DatastreamProducerRecordBuilder builder = new DatastreamProducerRecordBuilder();
          builder.addEvent(event);
          builder.setEventsSourceTimestamp(currentTimeMillis);
          // If the destination is user managed, we will use the key to decide the partition.
          if (!_task.isUserManagedDestination()) {
            builder.setPartition(0);
          } else {
            builder.setPartitionKey(_lineNo.toString());
          }

          builder.setSourceCheckpoint(_lineNo.toString());
          _producer.send(builder.build(), (metadata, exception) -> {
            if (exception == null) {
              LOG.info("Sending event:{} succeeded, metadata:{}", text, metadata);
            } else {
              LOG.error(String.format("Sending event:{%s} failed, metadata:{%s}", text, metadata), exception);
            }
          });
          _positionValue.setLinesRead((long) _lineNo.incrementAndGet());
        } else {
          try {
            // Wait for new data
            Thread.sleep(POLL_WAIT_MS);
          } catch (InterruptedException e) {
            LOG.info("Interrupted");
            break;
          }
        }
      }

      _task.release();
      _isStopped = true;
      LOG.info("Stopped at line " + _lineNo + " task=" + _task);
    } catch (Throwable e) {
      LOG.error("File processor is quitting with exception, task=" + _task, e);
    }
  }

  public boolean isStopped() {
    return _isStopped;
  }

  public void stop() {
    _cancelRequested = true;
  }

  public Integer getLineNumber() {
    return _lineNo.get();
  }

  public String getFileName() {
    return _fileName;
  }

  public FilePositionKey getPositionKey() {
    return _positionKey;
  }

  public FilePositionValue getPositionValue() {
    return _positionValue;
  }
}
