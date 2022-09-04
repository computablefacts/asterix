package com.computablefacts.asterix.ml.classification;

import com.computablefacts.asterix.ml.FeatureMatrix;
import com.computablefacts.asterix.ml.FeatureVector;
import org.junit.Assert;
import org.junit.Test;

public class MinMaxScalerTest {

  @Test
  public void testScaler() {

    FeatureMatrix matrix = new FeatureMatrix();
    matrix.addRow(new double[]{100.0, 0.001});
    matrix.addRow(new double[]{8.0, 0.05});
    matrix.addRow(new double[]{50.0, 0.005});
    matrix.addRow(new double[]{88.0, 0.07});
    matrix.addRow(new double[]{4.0, 0.1});

    AbstractScaler scaler = new MinMaxScaler();
    FeatureMatrix newMatrix = scaler.fitAndTransform(matrix);

    Assert.assertArrayEquals(new double[]{1.0, 0.0}, newMatrix.denseArray()[0], 0.000001);
    Assert.assertArrayEquals(new double[]{0.04166667, 0.49494949}, newMatrix.denseArray()[1], 0.000001);
    Assert.assertArrayEquals(new double[]{0.47916667, 0.04040404}, newMatrix.denseArray()[2], 0.000001);
    Assert.assertArrayEquals(new double[]{0.875, 0.6969697}, newMatrix.denseArray()[3], 0.000001);
    Assert.assertArrayEquals(new double[]{0.0, 1.0}, newMatrix.denseArray()[4], 0.000001);

    FeatureVector vector = new FeatureVector(new double[]{1.0, 1.0});
    FeatureVector newVector = scaler.transform(vector);

    Assert.assertArrayEquals(new double[]{-0.03125, 10.090909}, newVector.denseArray(), 0.000001);
  }
}
