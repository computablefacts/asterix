package com.computablefacts.asterix.ml;

import static com.computablefacts.asterix.ml.FeatureVector.findCorrelatedEntries;
import static com.computablefacts.asterix.ml.FeatureVector.findNonZeroedEntries;
import static com.computablefacts.asterix.ml.FeatureVector.findZeroedEntries;

import com.computablefacts.asterix.ml.FeatureVector.eCorrelation;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Assert;
import org.junit.Test;
import smile.util.SparseArray;

public class FeatureVectorTest {

  @Test
  public void testEqualsAndHashcode() {
    EqualsVerifier.forClass(FeatureVector.class).suppress(Warning.NONFINAL_FIELDS).verify();
  }

  @Test
  public void testCopyConstructor() {

    double[] array = new double[]{1.0, 0.0, 3.0, 0.0, 5.0};
    FeatureVector vector = new FeatureVector(array);

    FeatureVector vectorCopy1 = new FeatureVector(array);
    FeatureVector vectorCopy2 = new FeatureVector(array);

    Assert.assertEquals(vector, vectorCopy1);
    Assert.assertEquals(vector, vectorCopy2);

    vector.mapValues(x -> 0.0);

    Assert.assertNotEquals(vector, vectorCopy1);
    Assert.assertNotEquals(vector, vectorCopy2);

    Assert.assertEquals(vectorCopy1, vectorCopy2);
  }

  @Test
  public void testCreateEmptyVector() {

    FeatureVector vector = new FeatureVector(10);

    Assert.assertEquals(10, vector.length());
    Assert.assertTrue(vector.isEmpty());
    Assert.assertEquals("[0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0]", vector.toString());
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
    Assert.assertEquals("[1.0, 0.0, 3.0, 0.0, 5.0]", vector.toString());
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
    Assert.assertEquals("[1.0, 0.0, 3.0, 0.0, 5.0]", vector.toString());
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
  public void testGetZeroEntries() {

    double[] array = new double[]{1.0, 0.0, 3.0, 0.0, 5.0};
    FeatureVector vector = new FeatureVector(array);

    Assert.assertEquals(Sets.newHashSet(1, 3), vector.zeroEntries());
  }

  @Test
  public void testGetNonZeroEntries() {

    double[] array = new double[]{1.0, 0.0, 3.0, 0.0, 5.0};
    FeatureVector vector = new FeatureVector(array);

    Assert.assertEquals(Sets.newHashSet(0, 2, 4), vector.nonZeroEntries());
  }

  @Test
  public void testFindZeroedEntries() {

    double[] array1 = new double[]{0.0, 0.0, 0.0, 0.0, 0.0};
    FeatureVector vector1 = new FeatureVector(array1);

    double[] array2 = new double[]{0.0, 1.0, 2.0, 3.0, 0.0};
    FeatureVector vector2 = new FeatureVector(array2);

    double[] array3 = new double[]{0.0, 1.0, 0.0, 3.0, 4.0};
    FeatureVector vector3 = new FeatureVector(array3);

    Assert.assertEquals(Sets.newHashSet(0), findZeroedEntries(Lists.newArrayList(vector1, vector2, vector3)));
  }

  @Test
  public void testFindNonZeroedEntries() {

    double[] array1 = new double[]{0.0, 0.0, 0.0, 0.0, 0.0};
    FeatureVector vector1 = new FeatureVector(array1);

    double[] array2 = new double[]{0.0, 1.0, 2.0, 3.0, 0.0};
    FeatureVector vector2 = new FeatureVector(array2);

    double[] array3 = new double[]{0.0, 1.0, 0.0, 3.0, 4.0};
    FeatureVector vector3 = new FeatureVector(array3);

    Assert.assertEquals(Sets.newHashSet(1, 2, 3, 4),
        findNonZeroedEntries(Lists.newArrayList(vector1, vector2, vector3)));
  }

  @Test
  public void testFindCorrelatedEntries() {

    double[] array1 = new double[]{1, 1, 0};
    FeatureVector vector1 = new FeatureVector(array1);

    double[] array2 = new double[]{2, 2, 0};
    FeatureVector vector2 = new FeatureVector(array2);

    double[] array3 = new double[]{3, 4, 0};
    FeatureVector vector3 = new FeatureVector(array3);

    double[] array4 = new double[]{4, 3, 0};
    FeatureVector vector4 = new FeatureVector(array4);

    double[] array5 = new double[]{5, 6, 0};
    FeatureVector vector5 = new FeatureVector(array5);

    double[] array6 = new double[]{6, 5, 0};
    FeatureVector vector6 = new FeatureVector(array6);

    double[] array7 = new double[]{7, 8, 0};
    FeatureVector vector7 = new FeatureVector(array7);

    double[] array8 = new double[]{8, 7, 0};
    FeatureVector vector8 = new FeatureVector(array8);

    double[] array9 = new double[]{9, 10, 0};
    FeatureVector vector9 = new FeatureVector(array9);

    double[] array10 = new double[]{10, 9, 0};
    FeatureVector vector10 = new FeatureVector(array10);

    double[] array11 = new double[]{11, 12, 0};
    FeatureVector vector11 = new FeatureVector(array11);

    double[] array12 = new double[]{12, 11, 0};
    FeatureVector vector12 = new FeatureVector(array12);

    Assert.assertEquals(Sets.newHashSet(new SimpleImmutableEntry<>(0, 1)), findCorrelatedEntries(
        Lists.newArrayList(vector1, vector2, vector3, vector4, vector5, vector6, vector7, vector8, vector9, vector10,
            vector11, vector12), eCorrelation.KENDALL, 0.8, 10));

    Assert.assertEquals(Sets.newHashSet(new SimpleImmutableEntry<>(0, 1)), findCorrelatedEntries(
        Lists.newArrayList(vector1, vector2, vector3, vector4, vector5, vector6, vector7, vector8, vector9, vector10,
            vector11, vector12), eCorrelation.KENDALL, 0.85, 10));

    Assert.assertEquals(Sets.newHashSet(), findCorrelatedEntries(
        Lists.newArrayList(vector1, vector2, vector3, vector4, vector5, vector6, vector7, vector8, vector9, vector10,
            vector11, vector12), eCorrelation.KENDALL, 0.90, 10));
  }

  @Test
  public void testAppend() {

    double[] array = new double[]{1.0, 0.0, 3.0, 0.0};
    FeatureVector vector = new FeatureVector(array);

    Assert.assertEquals(Sets.newHashSet(0, 2), vector.nonZeroEntries());

    vector.append(5.0);

    Assert.assertEquals(Sets.newHashSet(0, 2, 4), vector.nonZeroEntries());
  }

  @Test
  public void testPrepend() {

    double[] array = new double[]{0.0, 3.0, 0.0, 5.0};
    FeatureVector vector = new FeatureVector(array);

    Assert.assertEquals(Sets.newHashSet(1, 3), vector.nonZeroEntries());

    vector.prepend(1.0);

    Assert.assertEquals(Sets.newHashSet(0, 2, 4), vector.nonZeroEntries());
  }
}
