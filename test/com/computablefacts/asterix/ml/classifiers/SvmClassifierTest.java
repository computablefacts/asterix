package com.computablefacts.asterix.ml.classifiers;

import org.junit.Assert;
import org.junit.Test;

public class SvmClassifierTest extends AbstractBinaryClassifierTest {

  @Test
  public void testSupportsIncrementalTraining() {
    Assert.assertFalse(classifier().supportsIncrementalTraining());
  }

  @Override
  protected AbstractBinaryClassifier classifier() {
    return new SvmClassifier();
  }
}