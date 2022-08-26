package com.computablefacts.asterix.ml;

import org.junit.Assert;
import org.junit.Test;

public class MinMaxNormalizerTest {

  @Test
  public void testNormalizeEmptyVector() {

    MinMaxNormalizer normalizer = new MinMaxNormalizer();
    FeatureVector vector = new FeatureVector(new double[]{});
    FeatureVector newVector = normalizer.apply(vector);

    Assert.assertEquals(vector.length(), newVector.length());
    Assert.assertArrayEquals(new double[]{}, newVector.denseArray(), 0.00001);
  }

  @Test
  public void testNormalizeVector() {

    MinMaxNormalizer normalizer = new MinMaxNormalizer();
    FeatureVector vector = new FeatureVector(new double[]{3, 3, 1, 3});
    FeatureVector newVector = normalizer.apply(vector);

    Assert.assertEquals(vector.length(), newVector.length());
    Assert.assertArrayEquals(new double[]{1.0, 1.0, 0.0, 1.0}, newVector.denseArray(), 0.000001);
  }
}
