package com.computablefacts.asterix.ml;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class VectorsReducerTest {

  @Test
  public void testReduceDenseVectors() {

    double[] vec1 = new double[]{0.0, 0.640, 0.0, 0.276, 0.845};
    double[] vec2 = new double[]{1.0, 0.0, 3.0, 0.0, 5.0};

    FeatureVector vector1 = new FeatureVector(vec1);
    FeatureVector vector2 = new FeatureVector(vec2);

    List<FeatureVector> vectors = Lists.newArrayList(vector1, vector2);
    List<FeatureVector> newVectors = new VectorsReducer().apply(vectors);

    Assert.assertEquals(vector1, newVectors.get(0));
    Assert.assertEquals(vector2, newVectors.get(1));
  }

  @Test
  public void testReduceSparseVectors() {

    double[] vec1 = new double[]{0.0, 0.640, 0.0, 0.0, 0.845};
    double[] vec2 = new double[]{0.0, 0.0, 3.0, 0.0, 5.0};

    FeatureVector vector1 = new FeatureVector(vec1);
    FeatureVector vector2 = new FeatureVector(vec2);

    List<FeatureVector> vectors = Lists.newArrayList(vector1, vector2);
    List<FeatureVector> newVectors = new VectorsReducer().apply(vectors);

    Assert.assertEquals(new FeatureVector(new double[]{0.640, 0.0, 0.845}), newVectors.get(0));
    Assert.assertEquals(new FeatureVector(new double[]{0.0, 3.0, 5.0}), newVectors.get(1));
  }
}
