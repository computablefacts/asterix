package com.computablefacts.asterix.ml.classification;

import com.computablefacts.asterix.ml.standardization.NopScaler;
import org.junit.Assert;
import org.junit.Test;

public class LogisticRegressionClassifierTest extends AbstractBinaryClassifierTest {

  @Test
  public void testSupportsIncrementalTraining() {
    Assert.assertTrue(classifier().supportsIncrementalTraining());
  }

  @Override
  protected AbstractBinaryClassifier classifier() {
    return new LogisticRegressionClassifier(new NopScaler());
  }
}