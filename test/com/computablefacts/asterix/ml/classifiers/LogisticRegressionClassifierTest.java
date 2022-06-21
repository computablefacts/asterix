package com.computablefacts.asterix.ml.classifiers;

import org.junit.Assert;
import org.junit.Test;

public class LogisticRegressionClassifierTest extends AbstractBinaryClassifierTest {

  @Test
  public void testSupportsIncrementalTraining() {
    Assert.assertTrue(classifier().supportsIncrementalTraining());
  }

  @Override
  protected AbstractBinaryClassifier classifier() {
    return new LogisticRegressionClassifier();
  }
}