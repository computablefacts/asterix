package com.computablefacts.asterix.ml.classification;

import org.junit.Assert;
import org.junit.Test;

public class GradientBoostedTreesClassifierTest extends AbstractBinaryClassifierTest {

  @Test
  public void testSupportsIncrementalTraining() {
    Assert.assertFalse(classifier().supportsIncrementalTraining());
  }

  @Override
  protected AbstractBinaryClassifier classifier() {
    return new GradientBoostedTreesClassifier();
  }
}