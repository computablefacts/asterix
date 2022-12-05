package com.computablefacts.asterix.ml.classification;

import com.computablefacts.asterix.ml.standardization.NopScaler;
import org.junit.Assert;
import org.junit.Test;

public class SvmClassifierTest extends AbstractBinaryClassifierTest {

  @Test
  public void testSupportsIncrementalTraining() {
    Assert.assertFalse(classifier().supportsIncrementalTraining());
  }

  @Override
  protected AbstractBinaryClassifier classifier() {
    return new SvmClassifier(new NopScaler());
  }
}