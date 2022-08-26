package com.computablefacts.asterix.ml;

import org.junit.Assert;
import org.junit.Test;

public class EuclideanNormNormalizerTest {

  @Test
  public void testNormalizeEmptyVector() {

    EuclideanNormNormalizer normalizer = new EuclideanNormNormalizer();
    FeatureVector vector = new FeatureVector(new double[]{});
    FeatureVector newVector = normalizer.apply(vector);

    Assert.assertEquals(vector.length(), newVector.length());
    Assert.assertArrayEquals(new double[]{}, newVector.denseArray(), 0.00001);
  }

  @Test
  public void testNormalizeVector() {

    EuclideanNormNormalizer normalizer = new EuclideanNormNormalizer();
    FeatureVector vector = new FeatureVector(new double[]{3, 3, 1, 3});
    FeatureVector newVector = normalizer.apply(vector);

    Assert.assertEquals(vector.length(), newVector.length());
    Assert.assertArrayEquals(
        new double[]{0.5669467095138409, 0.5669467095138409, 0.1889822365046136,
            0.5669467095138409}, newVector.denseArray(), 0.000001);
  }
}
