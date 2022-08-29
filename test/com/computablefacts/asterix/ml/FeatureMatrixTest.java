package com.computablefacts.asterix.ml;

import com.google.common.collect.Lists;
import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Assert;
import org.junit.Test;

public class FeatureMatrixTest {

  @Test
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(FeatureMatrix.class).withNonnullFields("rows_").verify();
  }

  @Test
  public void testToString() {

    FeatureMatrix matrix = new FeatureMatrix();
    matrix.addRow(new double[]{1.0, 0.0, 0.0});
    matrix.addRow(new double[]{0.0, 1.0, 0.0});
    matrix.addRow(new double[]{0.0, 0.0, 1.0});

    Assert.assertEquals("[1.0, 0.0, 0.0]\n[0.0, 1.0, 0.0]\n[0.0, 0.0, 1.0]", matrix.toString());
  }

  @Test
  public void testGetRows() {

    List<FeatureVector> vectors = Lists.newArrayList(new FeatureVector(new double[]{0.0, 0.0, 0.0}),
        new FeatureVector(new double[]{0.5, 0.5, 0.5}), new FeatureVector(new double[]{1.0, 1.0, 1.0}));
    FeatureMatrix matrix = new FeatureMatrix(vectors);

    Assert.assertEquals(vectors, matrix.rows());
  }

  @Test
  public void testGetColumns() {

    List<FeatureVector> vectors = Lists.newArrayList(new FeatureVector(new double[]{0.0, 0.0, 0.0}),
        new FeatureVector(new double[]{0.5, 0.5, 0.5}), new FeatureVector(new double[]{1.0, 1.0, 1.0}));
    FeatureMatrix matrix = new FeatureMatrix(vectors);

    List<FeatureVector> columns = Lists.newArrayList(new FeatureVector(new double[]{0.0, 0.5, 1.0}),
        new FeatureVector(new double[]{0.0, 0.5, 1.0}), new FeatureVector(new double[]{0.0, 0.5, 1.0}));

    Assert.assertEquals(columns, matrix.columns());
  }

  @Test
  public void testGetRow() {

    FeatureMatrix matrix = new FeatureMatrix();
    matrix.addRow(new double[]{1.0, 0.0, 0.0});
    matrix.addRow(new double[]{0.0, 1.0, 0.0});
    matrix.addRow(new double[]{0.0, 0.0, 1.0});

    Assert.assertArrayEquals(new double[]{1.0, 0.0, 0.0}, matrix.row(0).denseArray(), 0.0000001);
    Assert.assertArrayEquals(new double[]{0.0, 1.0, 0.0}, matrix.row(1).denseArray(), 0.0000001);
    Assert.assertArrayEquals(new double[]{0.0, 0.0, 1.0}, matrix.row(2).denseArray(), 0.0000001);
  }

  @Test
  public void testGetColumn() {

    FeatureMatrix matrix = new FeatureMatrix();
    matrix.addRow(new double[]{1.0, 0.0, 0.0});
    matrix.addRow(new double[]{0.0, 1.0, 0.0});
    matrix.addRow(new double[]{0.0, 0.0, 1.0});

    Assert.assertArrayEquals(new double[]{1.0, 0.0, 0.0}, matrix.column(0).denseArray(), 0.0000001);
    Assert.assertArrayEquals(new double[]{0.0, 1.0, 0.0}, matrix.column(1).denseArray(), 0.0000001);
    Assert.assertArrayEquals(new double[]{0.0, 0.0, 1.0}, matrix.column(2).denseArray(), 0.0000001);
  }

  @Test
  public void testAddRow() {

    // Vectors
    FeatureMatrix matrix1 = new FeatureMatrix();
    matrix1.addRow(new FeatureVector(new double[]{1.0, 0.0, 0.0}));
    matrix1.addRow(new FeatureVector(new double[]{0.0, 1.0, 0.0}));
    matrix1.addRow(new FeatureVector(new double[]{0.0, 0.0, 1.0}));

    Assert.assertEquals(3, matrix1.nbRows());
    Assert.assertEquals(3, matrix1.nbColumns());
    Assert.assertArrayEquals(new double[][]{{1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, 1.0}}, matrix1.denseArray());

    // Array
    FeatureMatrix matrix2 = new FeatureMatrix();
    matrix2.addRow(new double[]{1.0, 0.0, 0.0});
    matrix2.addRow(new double[]{0.0, 1.0, 0.0});
    matrix2.addRow(new double[]{0.0, 0.0, 1.0});

    Assert.assertEquals(3, matrix2.nbRows());
    Assert.assertEquals(3, matrix2.nbColumns());
    Assert.assertArrayEquals(new double[][]{{1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, 1.0}}, matrix2.denseArray());
  }

  @Test
  public void testAddRows() {

    // Vectors
    List<FeatureVector> vectors = Lists.newArrayList(new FeatureVector(new double[]{1.0, 0.0, 0.0}),
        new FeatureVector(new double[]{0.0, 1.0, 0.0}), new FeatureVector(new double[]{0.0, 0.0, 1.0}));
    FeatureMatrix matrix1 = new FeatureMatrix(vectors);

    Assert.assertEquals(3, matrix1.nbRows());
    Assert.assertEquals(3, matrix1.nbColumns());
    Assert.assertArrayEquals(new double[][]{{1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, 1.0}}, matrix1.denseArray());

    // Array
    double[][] array = new double[][]{{1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, 1.0}};
    FeatureMatrix matrix2 = new FeatureMatrix(array);

    Assert.assertEquals(3, matrix2.nbRows());
    Assert.assertEquals(3, matrix2.nbColumns());
    Assert.assertArrayEquals(new double[][]{{1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, 1.0}}, matrix2.denseArray());
  }

  @Test
  public void testPrependColumn() {

    FeatureMatrix matrix1 = new FeatureMatrix();
    matrix1.addRow(new double[]{0.0, 0.0});
    matrix1.addRow(new double[]{1.0, 0.0});
    matrix1.addRow(new double[]{0.0, 1.0});

    matrix1.prependColumn(new FeatureVector(new double[]{1.0, 0.0, 0.0})); // vector

    Assert.assertEquals(3, matrix1.nbRows());
    Assert.assertEquals(3, matrix1.nbColumns());
    Assert.assertArrayEquals(new double[][]{{1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, 1.0}}, matrix1.denseArray());

    FeatureMatrix matrix2 = new FeatureMatrix();
    matrix2.addRow(new double[]{0.0, 0.0});
    matrix2.addRow(new double[]{1.0, 0.0});
    matrix2.addRow(new double[]{0.0, 1.0});

    matrix2.prependColumn(new double[]{1.0, 0.0, 0.0}); // array

    Assert.assertEquals(3, matrix2.nbRows());
    Assert.assertEquals(3, matrix2.nbColumns());
    Assert.assertArrayEquals(new double[][]{{1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, 1.0}}, matrix2.denseArray());
  }

  @Test
  public void testAppendColumn() {

    FeatureMatrix matrix1 = new FeatureMatrix();
    matrix1.addRow(new double[]{1.0, 0.0});
    matrix1.addRow(new double[]{0.0, 1.0});
    matrix1.addRow(new double[]{0.0, 0.0});

    matrix1.appendColumn(new FeatureVector(new double[]{0.0, 0.0, 1.0})); // vector

    Assert.assertEquals(3, matrix1.nbRows());
    Assert.assertEquals(3, matrix1.nbColumns());
    Assert.assertArrayEquals(new double[][]{{1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, 1.0}}, matrix1.denseArray());

    FeatureMatrix matrix2 = new FeatureMatrix();
    matrix2.addRow(new double[]{1.0, 0.0});
    matrix2.addRow(new double[]{0.0, 1.0});
    matrix2.addRow(new double[]{0.0, 0.0});

    matrix2.appendColumn(new double[]{0.0, 0.0, 1.0}); // array

    Assert.assertEquals(3, matrix2.nbRows());
    Assert.assertEquals(3, matrix2.nbColumns());
    Assert.assertArrayEquals(new double[][]{{1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, 1.0}}, matrix2.denseArray());
  }

  @Test
  public void testReplaceRow() {

    FeatureMatrix matrix1 = new FeatureMatrix();
    matrix1.addRow(new double[]{1.0, 0.0, 0.0});
    matrix1.addRow(new double[]{0.0, 0.0, 0.0});
    matrix1.addRow(new double[]{0.0, 0.0, 1.0});

    matrix1.replaceRow(1, new FeatureVector(new double[]{0.0, 1.0, 0.0})); // vector

    Assert.assertArrayEquals(new double[][]{{1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, 1.0}}, matrix1.denseArray());

    FeatureMatrix matrix2 = new FeatureMatrix();
    matrix2.addRow(new double[]{1.0, 0.0, 0.0});
    matrix2.addRow(new double[]{0.0, 0.0, 0.0});
    matrix2.addRow(new double[]{0.0, 0.0, 1.0});

    matrix2.replaceRow(1, new double[]{0.0, 1.0, 0.0}); // array

    Assert.assertArrayEquals(new double[][]{{1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, 1.0}}, matrix2.denseArray());
  }

  @Test
  public void testReplaceColumn() {

    FeatureMatrix matrix1 = new FeatureMatrix();
    matrix1.addRow(new double[]{1.0, 0.0, 0.0});
    matrix1.addRow(new double[]{0.0, 0.0, 0.0});
    matrix1.addRow(new double[]{0.0, 0.0, 1.0});

    matrix1.replaceColumn(1, new FeatureVector(new double[]{0.0, 1.0, 0.0})); // vector

    Assert.assertArrayEquals(new double[][]{{1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, 1.0}}, matrix1.denseArray());

    FeatureMatrix matrix2 = new FeatureMatrix();
    matrix2.addRow(new double[]{1.0, 0.0, 0.0});
    matrix2.addRow(new double[]{0.0, 0.0, 0.0});
    matrix2.addRow(new double[]{0.0, 0.0, 1.0});

    matrix2.replaceColumn(1, new double[]{0.0, 1.0, 0.0}); // array

    Assert.assertArrayEquals(new double[][]{{1.0, 0.0, 0.0}, {0.0, 1.0, 0.0}, {0.0, 0.0, 1.0}}, matrix2.denseArray());
  }

  @Test
  public void testNormalizeColumnUsingEuclideanNorm() {

    FeatureMatrix matrix = new FeatureMatrix();
    matrix.addRow(new double[]{0.0, 0.0, 0.0});
    matrix.addRow(new double[]{0.0, 0.640, 0.0});
    matrix.addRow(new double[]{0.0, 0.0, 0.0});
    matrix.addRow(new double[]{0.0, 0.0, 0.0});
    matrix.addRow(new double[]{0.0, 0.845, 0.0});

    matrix.normalizeColumnUsingEuclideanNorm(1);

    Assert.assertArrayEquals(new double[]{0.0, 0.604, 0.0, 0.0, 0.79}, matrix.column(1).denseArray(), 0.01);
  }

  @Test
  public void testNormalizeColumnUsingMinMax() {

    FeatureMatrix matrix = new FeatureMatrix();
    matrix.addRow(new double[]{0.0, 8.0, 0.0});
    matrix.addRow(new double[]{0.0, 10.0, 0.0});
    matrix.addRow(new double[]{0.0, 15.0, 0.0});
    matrix.addRow(new double[]{0.0, 20.0, 0.0});

    matrix.normalizeColumnUsingMinMax(1);

    Assert.assertArrayEquals(new double[]{0.0, 0.16, 0.58, 1.0}, matrix.column(1).denseArray(), 0.01);
  }
}
