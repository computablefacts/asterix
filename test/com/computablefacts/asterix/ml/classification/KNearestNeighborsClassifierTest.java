package com.computablefacts.asterix.ml.classification;

import org.junit.Assert;
import org.junit.Test;

public class KNearestNeighborsClassifierTest extends AbstractBinaryClassifierTest {

  @Test
  public void testSupportsIncrementalTraining() {
    Assert.assertFalse(classifier().supportsIncrementalTraining());
  }

  @Override
  protected AbstractBinaryClassifier classifier() {
    return new KNearestNeighborClassifier(new NopScaler());
  }
}