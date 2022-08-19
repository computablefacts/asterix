package com.computablefacts.asterix.ml;

import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class VectorsMergerTest {

  @Test
  public void testMergeVectorsOfSameLengths() {

    double[] vec1 = new double[]{0.0, 0.640, 0.0, 0.0, 0.845};
    double[] vec2 = new double[]{1.0, 0.0, 3.0, 0.0, 5.0};
    double[] vec3 = new double[]{0.0, 2.0, 0.0, 4.0, 0.0};

    FeatureVector vector1 = new FeatureVector(vec1);
    FeatureVector vector2 = new FeatureVector(vec2);
    FeatureVector vector3 = new FeatureVector(vec3);

    List<FeatureVector> vectors = Lists.newArrayList(vector1, vector2, vector3);
    FeatureVector vector = new VectorsMerger().apply(vectors);

    Assert.assertEquals(new FeatureVector(
        new double[]{0.0, 0.640, 0.0, 0.0, 0.845, 1.0, 0.0, 3.0, 0.0, 5.0, 0.0, 2.0, 0.0, 4.0,
            0.0}), vector);
  }

  @Test
  public void testMergeVectorsOfDifferentLengths() {

    double[] vec1 = new double[]{0.0, 0.640, 0.0, 0.0, 0.845};
    double[] vec2 = new double[]{1.0, 3.0, 5.0};
    double[] vec3 = new double[]{2.0, 4.0};

    FeatureVector vector1 = new FeatureVector(vec1);
    FeatureVector vector2 = new FeatureVector(vec2);
    FeatureVector vector3 = new FeatureVector(vec3);

    List<FeatureVector> vectors = Lists.newArrayList(vector1, vector2, vector3);
    FeatureVector vector = new VectorsMerger().apply(vectors);

    Assert.assertEquals(
        new FeatureVector(new double[]{0.0, 0.640, 0.0, 0.0, 0.845, 1.0, 3.0, 5.0, 2.0, 4.0}),
        vector);
  }
}
