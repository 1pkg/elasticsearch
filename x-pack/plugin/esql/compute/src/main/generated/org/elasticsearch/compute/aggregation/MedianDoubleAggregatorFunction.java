// Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
// or more contributor license agreements. Licensed under the Elastic License
// 2.0; you may not use this file except in compliance with the Elastic License
// 2.0.
package org.elasticsearch.compute.aggregation;

import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import org.elasticsearch.compute.data.AggregatorStateVector;
import org.elasticsearch.compute.data.Block;
import org.elasticsearch.compute.data.DoubleBlock;
import org.elasticsearch.compute.data.DoubleVector;
import org.elasticsearch.compute.data.ElementType;
import org.elasticsearch.compute.data.IntVector;
import org.elasticsearch.compute.data.Page;
import org.elasticsearch.compute.data.Vector;

/**
 * {@link AggregatorFunction} implementation for {@link MedianDoubleAggregator}.
 * This class is generated. Do not edit it.
 */
public final class MedianDoubleAggregatorFunction implements AggregatorFunction {
  private final QuantileStates.SingleState state;

  private final int channel;

  public MedianDoubleAggregatorFunction(int channel, QuantileStates.SingleState state) {
    this.channel = channel;
    this.state = state;
  }

  public static MedianDoubleAggregatorFunction create(int channel) {
    return new MedianDoubleAggregatorFunction(channel, MedianDoubleAggregator.initSingle());
  }

  @Override
  public void addRawInput(Page page) {
    assert channel >= 0;
    ElementType type = page.getBlock(channel).elementType();
    if (type == ElementType.NULL) {
      return;
    }
    DoubleBlock block = page.getBlock(channel);
    DoubleVector vector = block.asVector();
    if (vector != null) {
      addRawVector(vector);
    } else {
      addRawBlock(block);
    }
  }

  private void addRawVector(DoubleVector vector) {
    for (int i = 0; i < vector.getPositionCount(); i++) {
      MedianDoubleAggregator.combine(state, vector.getDouble(i));
    }
  }

  private void addRawBlock(DoubleBlock block) {
    for (int p = 0; p < block.getTotalValueCount(); p++) {
      if (block.isNull(p) == false) {
        int i = block.getFirstValueIndex(p);
        MedianDoubleAggregator.combine(state, block.getDouble(i));
      }
    }
  }

  @Override
  public void addIntermediateInput(Block block) {
    assert channel == -1;
    Vector vector = block.asVector();
    if (vector == null || vector instanceof AggregatorStateVector == false) {
      throw new RuntimeException("expected AggregatorStateBlock, got:" + block);
    }
    @SuppressWarnings("unchecked") AggregatorStateVector<QuantileStates.SingleState> blobVector = (AggregatorStateVector<QuantileStates.SingleState>) vector;
    QuantileStates.SingleState tmpState = new QuantileStates.SingleState();
    for (int i = 0; i < block.getPositionCount(); i++) {
      blobVector.get(i, tmpState);
      MedianDoubleAggregator.combineStates(state, tmpState);
    }
  }

  @Override
  public Block evaluateIntermediate() {
    AggregatorStateVector.Builder<AggregatorStateVector<QuantileStates.SingleState>, QuantileStates.SingleState> builder =
        AggregatorStateVector.builderOfAggregatorState(QuantileStates.SingleState.class, state.getEstimatedSize());
    builder.add(state, IntVector.range(0, 1));
    return builder.build().asBlock();
  }

  @Override
  public Block evaluateFinal() {
    return MedianDoubleAggregator.evaluateFinal(state);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append("[");
    sb.append("channel=").append(channel);
    sb.append("]");
    return sb.toString();
  }
}
