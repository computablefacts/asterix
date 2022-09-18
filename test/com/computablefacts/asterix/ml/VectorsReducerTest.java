package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.ml.VectorsReducer.eCorrelation;
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

  @Test
  public void testReduceUsingKendallCorrelationWhenFeaturesAreNotCorrelated() {

    double[] vec1 = new double[]{1.0, 8.0};
    double[] vec2 = new double[]{2.0, 7.0};
    double[] vec3 = new double[]{3.0, 6.0};
    double[] vec4 = new double[]{4.0, 5.0};
    double[] vec5 = new double[]{5.0, 4.0};
    double[] vec6 = new double[]{6.0, 3.0};
    double[] vec7 = new double[]{7.0, 2.0};
    double[] vec8 = new double[]{8.0, 1.0};

    FeatureVector vector1 = new FeatureVector(vec1);
    FeatureVector vector2 = new FeatureVector(vec2);
    FeatureVector vector3 = new FeatureVector(vec3);
    FeatureVector vector4 = new FeatureVector(vec4);
    FeatureVector vector5 = new FeatureVector(vec5);
    FeatureVector vector6 = new FeatureVector(vec6);
    FeatureVector vector7 = new FeatureVector(vec7);
    FeatureVector vector8 = new FeatureVector(vec8);

    List<FeatureVector> vectors = Lists.newArrayList(vector1, vector2, vector3, vector4, vector5, vector6, vector7,
        vector8);
    List<FeatureVector> newVectors = new VectorsReducer(eCorrelation.KENDALL).apply(vectors);

    Assert.assertEquals(vector1, newVectors.get(0));
    Assert.assertEquals(vector2, newVectors.get(1));
    Assert.assertEquals(vector3, newVectors.get(2));
    Assert.assertEquals(vector4, newVectors.get(3));
    Assert.assertEquals(vector5, newVectors.get(4));
    Assert.assertEquals(vector6, newVectors.get(5));
    Assert.assertEquals(vector7, newVectors.get(6));
    Assert.assertEquals(vector8, newVectors.get(7));
  }

  @Test
  public void testReduceUsingKendallCorrelationWhenFeaturesAreCorrelated() {

    double[] vec1 = new double[]{1.0, 1.0};
    double[] vec2 = new double[]{2.0, 2.0};
    double[] vec3 = new double[]{3.0, 3.0};
    double[] vec4 = new double[]{4.0, 5.0};
    double[] vec5 = new double[]{5.0, 4.0};
    double[] vec6 = new double[]{6.0, 7.0};
    double[] vec7 = new double[]{7.0, 6.0};
    double[] vec8 = new double[]{8.0, 8.0};
    double[] vec9 = new double[]{9.0, 10.0};
    double[] vec10 = new double[]{10.0, 9.0};
    double[] vec11 = new double[]{11.0, 11.0};
    double[] vec12 = new double[]{12.0, 12.0};

    FeatureVector vector1 = new FeatureVector(vec1);
    FeatureVector vector2 = new FeatureVector(vec2);
    FeatureVector vector3 = new FeatureVector(vec3);
    FeatureVector vector4 = new FeatureVector(vec4);
    FeatureVector vector5 = new FeatureVector(vec5);
    FeatureVector vector6 = new FeatureVector(vec6);
    FeatureVector vector7 = new FeatureVector(vec7);
    FeatureVector vector8 = new FeatureVector(vec8);
    FeatureVector vector9 = new FeatureVector(vec9);
    FeatureVector vector10 = new FeatureVector(vec10);
    FeatureVector vector11 = new FeatureVector(vec11);
    FeatureVector vector12 = new FeatureVector(vec12);

    List<FeatureVector> vectors = Lists.newArrayList(vector1, vector2, vector3, vector4, vector5, vector6, vector7,
        vector8, vector9, vector10, vector11, vector12);
    List<FeatureVector> newVectors = new VectorsReducer(eCorrelation.KENDALL).apply(vectors);

    Assert.assertEquals(new FeatureVector(new double[]{1.0}), newVectors.get(0));
    Assert.assertEquals(new FeatureVector(new double[]{2.0}), newVectors.get(1));
    Assert.assertEquals(new FeatureVector(new double[]{3.0}), newVectors.get(2));
    Assert.assertEquals(new FeatureVector(new double[]{5.0}), newVectors.get(3));
    Assert.assertEquals(new FeatureVector(new double[]{4.0}), newVectors.get(4));
    Assert.assertEquals(new FeatureVector(new double[]{7.0}), newVectors.get(5));
    Assert.assertEquals(new FeatureVector(new double[]{6.0}), newVectors.get(6));
    Assert.assertEquals(new FeatureVector(new double[]{8.0}), newVectors.get(7));
    Assert.assertEquals(new FeatureVector(new double[]{10.0}), newVectors.get(8));
    Assert.assertEquals(new FeatureVector(new double[]{9.0}), newVectors.get(9));
    Assert.assertEquals(new FeatureVector(new double[]{11.0}), newVectors.get(10));
    Assert.assertEquals(new FeatureVector(new double[]{12.0}), newVectors.get(11));
  }
}
