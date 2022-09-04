package com.computablefacts.asterix.ml.classification;

import com.computablefacts.asterix.ml.FeatureMatrix;
import com.computablefacts.asterix.ml.FeatureVector;
import org.junit.Assert;
import org.junit.Test;

public class NopScalerTest {

  @Test
  public void testScaler() {

    FeatureMatrix matrix = new FeatureMatrix();
    matrix.addRow(new double[]{100.0, 0.001});
    matrix.addRow(new double[]{8.0, 0.05});
    matrix.addRow(new double[]{50.0, 0.005});
    matrix.addRow(new double[]{88.0, 0.07});
    matrix.addRow(new double[]{4.0, 0.1});

    AbstractScaler scaler = new NopScaler();
    FeatureMatrix newMatrix = scaler.train(matrix);

    Assert.assertEquals(matrix, newMatrix);

    FeatureVector vector = new FeatureVector(new double[]{1.0, 1.0});
    FeatureVector newVector = scaler.predict(vector);

    Assert.assertEquals(vector, newVector);
  }
}
