/**
 *  Copyright 2019 LinkedIn Corporation. All rights reserved.
 *  Licensed under the BSD 2-Clause License. See the LICENSE file in the project root for license information.
 *  See the NOTICE file in the project root for additional information regarding copyright ownership.
 */
package com.linkedin.datastream.server.assignment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import com.linkedin.datastream.common.Datastream;
import com.linkedin.datastream.common.DatastreamMetadataConstants;
import com.linkedin.datastream.common.DatastreamRuntimeException;
import com.linkedin.datastream.connectors.DummyConnector;
import com.linkedin.datastream.server.DatastreamGroup;
import com.linkedin.datastream.server.DatastreamGroupPartitionsMetadata;
import com.linkedin.datastream.server.DatastreamTask;
import com.linkedin.datastream.server.DatastreamTaskImpl;
import com.linkedin.datastream.server.zk.ZkAdapter;
import com.linkedin.datastream.testutil.DatastreamTestUtils;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/**
 * Tests for {@link StickyPartitionAssignmentStrategy}
 */
public class TestStickyPartitionAssignment {

  private static final Logger LOG = LoggerFactory.getLogger(TestStickyPartitionAssignment.class.getName());

  @Test
  public void testCreateAssignmentAcrossAllTasks() {
    StickyPartitionAssignmentStrategy strategy = new StickyPartitionAssignmentStrategy(Optional.empty(),
        Optional.empty(), Optional.empty());
    Set<DatastreamTask> taskSet = new HashSet<>();
    List<DatastreamGroup> datastreams = generateDatastreams("ds", 1);
    Map<String, Set<DatastreamTask>> assignment = generateEmptyAssignment(datastreams, 1, 3, true);
    assignment.put("instance1", taskSet);

    List<String> partitions = ImmutableList.of("t-0", "t-1", "t1-0");

    DatastreamGroupPartitionsMetadata partitionsMetadata =
        new DatastreamGroupPartitionsMetadata(datastreams.get(0), partitions);

    assignment = strategy.assignPartitions(assignment, partitionsMetadata);

    for (DatastreamTask task : assignment.get("instance1")) {
      Assert.assertEquals(task.getPartitionsV2().size(), 1);
    }
  }


  @Test
  public void testAddPartitions() {
    StickyPartitionAssignmentStrategy strategy = new StickyPartitionAssignmentStrategy(Optional.empty(),
        Optional.empty(), Optional.empty());
    Set<DatastreamTask> taskSet = new HashSet<>();
    List<DatastreamGroup> datastreams = generateDatastreams("ds", 1);
    Map<String, Set<DatastreamTask>> assignment = generateEmptyAssignment(datastreams, 1, 3, true);
    assignment.put("instance1", taskSet);

    List<String> partitions = ImmutableList.of("t-0", "t-1", "t1-0");

    DatastreamGroupPartitionsMetadata partitionsMetadata =
        new DatastreamGroupPartitionsMetadata(datastreams.get(0), partitions);

    assignment = strategy.assignPartitions(assignment, partitionsMetadata);

    List<String> newPartitions = ImmutableList.of("t-0", "t-1", "t1-0", "t2-0", "t2-1", "t2-2");
    DatastreamGroupPartitionsMetadata newPartitionsMetadata =
        new DatastreamGroupPartitionsMetadata(datastreams.get(0), newPartitions);

    assignment = strategy.assignPartitions(assignment, newPartitionsMetadata);

    for (DatastreamTask task : assignment.get("instance1")) {
      Assert.assertEquals(task.getPartitionsV2().size(), 2);
    }
  }


  @Test(expectedExceptions = DatastreamRuntimeException.class)
  public void testCreateAssignmentFailureDueToUnlockedTask() {
    StickyPartitionAssignmentStrategy strategy = new StickyPartitionAssignmentStrategy(Optional.empty(),
        Optional.empty(), Optional.empty());
    Set<DatastreamTask> taskSet = new HashSet<>();
    List<DatastreamGroup> datastreams = generateDatastreams("ds", 1);
    Map<String, Set<DatastreamTask>> assignment = generateEmptyAssignment(datastreams, 1, 3, false);
    assignment.put("instance1", taskSet);

    List<String> partitions = ImmutableList.of("t-0", "t-1", "t1-0");

    DatastreamGroupPartitionsMetadata partitionsMetadata =
        new DatastreamGroupPartitionsMetadata(datastreams.get(0), partitions);

    assignment = strategy.assignPartitions(assignment, partitionsMetadata);

    // Generate partition assignment
    List<String> newPartitions = ImmutableList.of("t-0", "t-1", "t1-0", "t2-0");
    DatastreamGroupPartitionsMetadata newPartitionsMetadata =
        new DatastreamGroupPartitionsMetadata(datastreams.get(0), newPartitions);

    assignment = strategy.assignPartitions(assignment, newPartitionsMetadata);
  }


  @Test
  public void testMovePartition() {
    StickyPartitionAssignmentStrategy strategy = new StickyPartitionAssignmentStrategy(Optional.empty(),
        Optional.empty(), Optional.empty());
    List<DatastreamGroup> datastreams = generateDatastreams("ds", 2);
    Map<String, Set<DatastreamTask>> assignment = generateEmptyAssignment(datastreams, 3, 2, true);
    List<String> partitions = ImmutableList.of("t-0", "t-1", "t-2", "t-3", "t-4");
    DatastreamGroupPartitionsMetadata partitionsMetadata =
        new DatastreamGroupPartitionsMetadata(datastreams.get(0), partitions);
    // Generate partition assignment
    assignment = strategy.assignPartitions(assignment, partitionsMetadata);


    Map<String, Set<String>> targetAssignment = new HashMap<>();
    targetAssignment.put("instance2", ImmutableSet.of("t-3", "t-2", "t-1", "t-5"));
    targetAssignment.put("instance1", ImmutableSet.of("t-0"));

    assignment = strategy.movePartitions(assignment, targetAssignment, partitionsMetadata);

    Assert.assertTrue(getPartitionsFromTask(assignment.get("instance2")).contains("t-1"));
    Assert.assertTrue(getPartitionsFromTask(assignment.get("instance2")).contains("t-2"));
    Assert.assertTrue(getPartitionsFromTask(assignment.get("instance2")).contains("t-3"));

    Assert.assertEquals(getTotalPartitions(assignment), 5);
  }


  @Test(expectedExceptions = DatastreamRuntimeException.class)
  public void testMovePartitionToInstanceWithoutTask() {
    StickyPartitionAssignmentStrategy strategy = new StickyPartitionAssignmentStrategy(Optional.empty(),
        Optional.empty(), Optional.empty());
    List<DatastreamGroup> datastreams = generateDatastreams("ds", 2);
    Map<String, Set<DatastreamTask>> assignment = generateEmptyAssignment(datastreams, 3, 2, true);
    List<String> partitions = ImmutableList.of("t-0", "t-1", "t-2", "t-3", "t-4");
    DatastreamGroupPartitionsMetadata partitionsMetadata =
        new DatastreamGroupPartitionsMetadata(datastreams.get(0), partitions);
    // Generate partition assignment
    assignment = strategy.assignPartitions(assignment, partitionsMetadata);
    assignment.put("empty", new HashSet<DatastreamTask>());

    Map<String, Set<String>> targetAssignment = new HashMap<>();
    targetAssignment.put("empty", ImmutableSet.of("t-3", "t-2", "t-1", "t-5"));
    assignment = strategy.movePartitions(assignment, targetAssignment, partitionsMetadata);
  }


  @Test
  public void testRemovePartitions() {
    StickyPartitionAssignmentStrategy strategy = new StickyPartitionAssignmentStrategy(Optional.empty(),
        Optional.empty(), Optional.empty());
    List<DatastreamGroup> datastreams = generateDatastreams("ds", 1);
    Map<String, Set<DatastreamTask>> assignment = generateEmptyAssignment(datastreams, 3, 2, true);

    List<String> partitions = ImmutableList.of("t-0", "t-1", "t-2", "t-3", "t-4", "t-5", "t-6");
    DatastreamGroupPartitionsMetadata partitionsMetadata =
        new DatastreamGroupPartitionsMetadata(datastreams.get(0), partitions);

    // Generate partition assignment
    assignment = strategy.assignPartitions(assignment, partitionsMetadata);

    List<String> newPartitions = ImmutableList.of("t-1", "t-3", "t-4", "t-6");
    DatastreamGroupPartitionsMetadata newPartitionsMetadata =
        new DatastreamGroupPartitionsMetadata(datastreams.get(0), newPartitions);


    assignment = strategy.assignPartitions(assignment, newPartitionsMetadata);

    List<String> remainingPartitions = new ArrayList<>();
    for (String instance : assignment.keySet()) {
      for (DatastreamTask task : assignment.get(instance)) {
        remainingPartitions.addAll(task.getPartitionsV2());
      }
    }

    Assert.assertEquals(new HashSet<>(remainingPartitions), new HashSet<>(newPartitions));
  }

  private  Map<String, Set<DatastreamTask>> generateEmptyAssignment(List<DatastreamGroup> datastreams,
      int instanceNum, int taskNum, boolean isTaskLocked) {
    Map<String, Set<DatastreamTask>> assignment = new HashMap<>();
    for (int i = 0; i < instanceNum; ++i) {
      Set<DatastreamTask> set = new HashSet<>();
      for (int j = 0; j < taskNum; ++j) {
        DatastreamTaskImpl task = new DatastreamTaskImpl(datastreams.get(0).getDatastreams());
        ZkAdapter mockZkAdapter = mock(ZkAdapter.class);
        task.setZkAdapter(mockZkAdapter);
        when(mockZkAdapter.checkIsTaskLocked(anyString(), anyString())).thenReturn(isTaskLocked);
        set.add(task);
      }
      assignment.put("instance" + i, set);
    }
    return assignment;
  }

  private Set<String> getPartitionsFromTask(Set<DatastreamTask> tasks) {
    Set<String> partitions = new HashSet<>();
    tasks.forEach(t -> partitions.addAll(t.getPartitionsV2()));
    return partitions;
  }


  private int getTotalPartitions(Map<String, Set<DatastreamTask>> assignment) {
    int count = 0;
    for (Set<DatastreamTask> tasks : assignment.values()) {
      count += tasks.stream().map(t -> t.getPartitionsV2().size()).mapToInt(Integer::intValue).sum();
    }
    return count;
  }

  private List<DatastreamGroup> generateDatastreams(String namePrefix, int numberOfDatastreams) {
    List<DatastreamGroup> datastreams = new ArrayList<>();
    String type = DummyConnector.CONNECTOR_TYPE;
    for (int index = 0; index < numberOfDatastreams; index++) {
      Datastream ds = DatastreamTestUtils.createDatastream(type, namePrefix + index, "DummySource");
      ds.getMetadata().put(DatastreamMetadataConstants.OWNER_KEY, "person_" + index);
      ds.getMetadata().put(DatastreamMetadataConstants.TASK_PREFIX, DatastreamTaskImpl.getTaskPrefix(ds));
      datastreams.add(new DatastreamGroup(Collections.singletonList(ds)));
    }
    return datastreams;
  }

}
