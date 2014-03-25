/*
 * Copyright (c) 2014 DataTorrent, Inc. ALL Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datatorrent.lib.statistics;

import java.util.Map;

import javax.annotation.Nonnull;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import com.datatorrent.api.BaseOperator;
import com.datatorrent.api.DefaultOutputPort;
import com.datatorrent.api.Operator;

/**
 * A {@link Unifier} implementation for {@link DimensionsComputation}.<br/>
 * The events to the unifier should contain the dimension key which is why this class is abstract
 * and any concrete implementations should provide {@link #getDimensionKey(Object)} implementation.
 *
 * @param <EVENT>
 */
public abstract class DimensionsComputationUnifierImpl<EVENT> extends BaseOperator implements Operator.Unifier<EVENT>
{
  @Nonnull
  private final Map<Integer,DimensionsComputation.Aggregator<EVENT>> aggregators;
  @Nonnull
  private final Map<EVENT, EVENT> aggregates;

  public final transient DefaultOutputPort<EVENT> output = new DefaultOutputPort<EVENT>();

  private DimensionsComputationUnifierImpl()
  {
    /** for kryo serialization */
    aggregators = null;
    aggregates = null;
  }

  DimensionsComputationUnifierImpl(Map<Integer, DimensionsComputation.Aggregator<EVENT>> aggregators)
  {
    this.aggregators = Preconditions.checkNotNull(aggregators, "aggregators");
    this.aggregates = Maps.newHashMap();
  }

  @Override
  public void process(EVENT tuple)
  {
    EVENT destination = aggregates.get(tuple);
    if (destination == null) {
      aggregates.put(tuple, tuple);
    }
    else {
      int dimensionKey = getDimensionKey(tuple);
      aggregators.get(dimensionKey).aggregate(destination, tuple);
    }
  }

  public void endWindow()
  {
    for (EVENT value : aggregates.values()) {
      output.emit(value);
    }
    aggregates.clear();
  }

  protected abstract int getDimensionKey(EVENT tuple);
}
