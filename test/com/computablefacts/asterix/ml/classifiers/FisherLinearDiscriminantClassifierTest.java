package com.computablefacts.asterix.ml.classifiers;

import com.computablefacts.asterix.ml.FeatureVector;
import org.junit.Assert;
import org.junit.Test;

public class FisherLinearDiscriminantClassifierTest extends AbstractBinaryClassifierTest {

  @Test
  public void testSupportsIncrementalTraining() {
    Assert.assertFalse(classifier().supportsIncrementalTraining());
  }

  @Override
  protected AbstractBinaryClassifier classifier() {
    return new FisherLinearDiscriminantClassifier();
  }

  @Override
  protected FeatureVector vector(int i) {

    FeatureVector vector = new FeatureVector(100);
    vector.set(0, i % 2);
    vector.set(1, 1 - i % 2); // ensure at least one non-zero entry

    // One complication in applying FLD (and LDA) to real data occurs when the number of variables/features does not exceed the number of samples.
    // In this case, the covariance estimates do not have full rank, and so cannot be inverted.
    // This is known as small sample size problem.
    for (int j = 2; j < 100; j++) {
      vector.set(j, j);
    }
    return vector;
  }
}