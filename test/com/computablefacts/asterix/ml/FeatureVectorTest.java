package com.computablefacts.asterix.ml;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Assert;
import org.junit.Test;
import smile.util.SparseArray;

public class FeatureVectorTest {

  @Test
  public void testEqualsAndHashcode() {
    EqualsVerifier.forClass(FeatureVector.class).verify();
  }

  @Test
  public void testCreateEmptyVector() {

    FeatureVector vector = new FeatureVector(10);

    Assert.assertEquals(10, vector.length());
    Assert.assertTrue(vector.isEmpty());
  }

  @Test
  public void testCreateVectorFromArrayOfDoubles() {

    double[] array = new double[]{1.0, 0.0, 3.0, 0.0, 5.0};
    FeatureVector vector = new FeatureVector(array);

    Assert.assertEquals(5, vector.length());
    Assert.assertEquals(1.0, vector.get(0), 0.0);
    Assert.assertEquals(0.0, vector.get(1), 0.0);
    Assert.assertEquals(3.0, vector.get(2), 0.0);
    Assert.assertEquals(0.0, vector.get(3), 0.0);
    Assert.assertEquals(5.0, vector.get(4), 0.0);
  }

  @Test
  public void testCreateVectorFromListOfDoubles() {

    List<Double> list = Lists.newArrayList(1.0, 0.0, 3.0, 0.0, 5.0);
    FeatureVector vector = new FeatureVector(list);

    Assert.assertEquals(5, vector.length());
    Assert.assertEquals(1.0, vector.get(0), 0.0);
    Assert.assertEquals(0.0, vector.get(1), 0.0);
    Assert.assertEquals(3.0, vector.get(2), 0.0);
    Assert.assertEquals(0.0, vector.get(3), 0.0);
    Assert.assertEquals(5.0, vector.get(4), 0.0);
  }

  @Test
  public void testGetSparseArray() {

    double[] array = new double[]{1.0, 0.0, 3.0, 0.0, 5.0};
    FeatureVector vector = new FeatureVector(array);
    SparseArray sparseArray = vector.sparseArray();

    Assert.assertEquals(3 /* # non-zero entries */, sparseArray.size());
    Assert.assertEquals(1.0, sparseArray.get(0), 0.0);
    Assert.assertEquals(0.0, sparseArray.get(1), 0.0);
    Assert.assertEquals(3.0, sparseArray.get(2), 0.0);
    Assert.assertEquals(0.0, sparseArray.get(3), 0.0);
    Assert.assertEquals(5.0, sparseArray.get(4), 0.0);
  }

  @Test
  public void testGetDenseArray() {

    double[] array = new double[]{1.0, 0.0, 3.0, 0.0, 5.0};
    FeatureVector vector = new FeatureVector(array);
    double[] denseArray = vector.denseArray();

    Assert.assertEquals(5, denseArray.length);
    Assert.assertEquals(1.0, denseArray[0], 0.0);
    Assert.assertEquals(0.0, denseArray[1], 0.0);
    Assert.assertEquals(3.0, denseArray[2], 0.0);
    Assert.assertEquals(0.0, denseArray[3], 0.0);
    Assert.assertEquals(5.0, denseArray[4], 0.0);
  }

  @Test
  public void testGetAndSetValueAtPos() {

    double[] array = new double[]{1.0, 0.0, 3.0, 0.0, 5.0};
    FeatureVector vector = new FeatureVector(array);

    Assert.assertEquals(5, vector.length());
    Assert.assertEquals(1.0, vector.get(0), 0.0);
    Assert.assertEquals(0.0, vector.get(1), 0.0);
    Assert.assertEquals(3.0, vector.get(2), 0.0);
    Assert.assertEquals(0.0, vector.get(3), 0.0);
    Assert.assertEquals(5.0, vector.get(4), 0.0);

    for (int i = 0; i < array.length; i++) {
      vector.set(i, array[i] - 1);
    }

    Assert.assertEquals(5, vector.length());
    Assert.assertEquals(0.0, vector.get(0), 0.0);
    Assert.assertEquals(-1.0, vector.get(1), 0.0);
    Assert.assertEquals(2.0, vector.get(2), 0.0);
    Assert.assertEquals(-1.0, vector.get(3), 0.0);
    Assert.assertEquals(4.0, vector.get(4), 0.0);
  }

  @Test
  public void testRemove() {

    double[] array = new double[]{1.0, 0.0, 3.0, 0.0, 5.0};
    FeatureVector vector = new FeatureVector(array);

    Assert.assertFalse(vector.isEmpty());
    Assert.assertEquals(5, vector.length());
    Assert.assertEquals(1.0, vector.get(0), 0.0);
    Assert.assertEquals(0.0, vector.get(1), 0.0);
    Assert.assertEquals(3.0, vector.get(2), 0.0);
    Assert.assertEquals(0.0, vector.get(3), 0.0);
    Assert.assertEquals(5.0, vector.get(4), 0.0);

    vector.remove(0);
    vector.remove(2);
    vector.remove(4);

    Assert.assertTrue(vector.isEmpty());
    Assert.assertEquals(5, vector.length());
    Assert.assertEquals(0.0, vector.get(0), 0.0);
    Assert.assertEquals(0.0, vector.get(1), 0.0);
    Assert.assertEquals(0.0, vector.get(2), 0.0);
    Assert.assertEquals(0.0, vector.get(3), 0.0);
    Assert.assertEquals(0.0, vector.get(4), 0.0);
  }

  @Test
  public void testRemoveAll() {

    double[] array = new double[]{1.0, 0.0, 3.0, 0.0, 5.0};
    FeatureVector vector = new FeatureVector(array);

    Assert.assertFalse(vector.isEmpty());
    Assert.assertEquals(5, vector.length());
    Assert.assertEquals(1.0, vector.get(0), 0.0);
    Assert.assertEquals(0.0, vector.get(1), 0.0);
    Assert.assertEquals(3.0, vector.get(2), 0.0);
    Assert.assertEquals(0.0, vector.get(3), 0.0);
    Assert.assertEquals(5.0, vector.get(4), 0.0);

    vector.remove(Sets.newHashSet(0, 2, 4));

    Assert.assertTrue(vector.isEmpty());
    Assert.assertEquals(5, vector.length());
    Assert.assertEquals(0.0, vector.get(0), 0.0);
    Assert.assertEquals(0.0, vector.get(1), 0.0);
    Assert.assertEquals(0.0, vector.get(2), 0.0);
    Assert.assertEquals(0.0, vector.get(3), 0.0);
    Assert.assertEquals(0.0, vector.get(4), 0.0);
  }

  @Test
  public void testGetZeroes() {

    double[] array = new double[]{1.0, 0.0, 3.0, 0.0, 5.0};
    FeatureVector vector = new FeatureVector(array);

    Assert.assertEquals(Lists.newArrayList(1, 3), vector.zeroes());
  }
}