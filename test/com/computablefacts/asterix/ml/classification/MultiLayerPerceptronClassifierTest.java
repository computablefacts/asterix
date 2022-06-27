package com.computablefacts.asterix.ml.classification;

import org.junit.Assert;
import org.junit.Test;

public class MultiLayerPerceptronClassifierTest extends AbstractBinaryClassifierTest {

  @Test
  public void testSupportsIncrementalTraining() {
    Assert.assertTrue(classifier().supportsIncrementalTraining());
  }

  @Override
  protected AbstractBinaryClassifier classifier() {
    return new MultiLayerPerceptronClassifier();
  }
}