package com.computablefacts.asterix.ml.standardization;

import com.computablefacts.asterix.ml.FeatureMatrix;
import com.computablefacts.asterix.ml.FeatureVector;
import org.junit.Assert;
import org.junit.Test;

public class StandardScalerTest {

  @Test
  public void testScaler() {

    FeatureMatrix matrix = new FeatureMatrix();
    matrix.addRow(new double[]{100.0, 0.001});
    matrix.addRow(new double[]{8.0, 0.05});
    matrix.addRow(new double[]{50.0, 0.005});
    matrix.addRow(new double[]{88.0, 0.07});
    matrix.addRow(new double[]{4.0, 0.1});

    AbstractScaler scaler = new StandardScaler();
    FeatureMatrix newMatrix = scaler.fitAndTransform(matrix);

    Assert.assertArrayEquals(new double[]{1.26398112, -1.16389967}, newMatrix.denseArray()[0], 0.000001);
    Assert.assertArrayEquals(new double[]{-1.06174414, 0.12639634}, newMatrix.denseArray()[1], 0.000001);
    Assert.assertArrayEquals(new double[]{0.0, -1.05856939}, newMatrix.denseArray()[2], 0.000001);
    Assert.assertArrayEquals(new double[]{0.96062565, 0.65304778}, newMatrix.denseArray()[3], 0.000001);
    Assert.assertArrayEquals(new double[]{-1.16286263, 1.44302493}, newMatrix.denseArray()[4], 0.000001);

    FeatureVector vector = new FeatureVector(new double[]{1.0, 1.0});
    FeatureVector newVector = scaler.transform(vector);

    Assert.assertArrayEquals(new double[]{-1.2387014937397334, 25.14233956072037}, newVector.denseArray(), 0.000001);
  }
}
