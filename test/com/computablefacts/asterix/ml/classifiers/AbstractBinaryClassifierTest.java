package com.computablefacts.asterix.ml.classifiers;

import static com.computablefacts.asterix.ml.classifiers.AbstractBinaryClassifier.KO;
import static com.computablefacts.asterix.ml.classifiers.AbstractBinaryClassifier.OK;

import com.computablefacts.asterix.ml.FeatureVector;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Assert;
import org.junit.Test;

/**
 * This class trains a binary classifier that categorize numbers into ODD or EVEN.
 */
public abstract class AbstractBinaryClassifierTest {

  private final int EVEN = OK;
  private final int ODD = KO;

  @Test
  public void testClassify() {

    int[] actuals = new int[100];
    List<FeatureVector> train = new ArrayList<>();

    for (int i = 0; i < 100; i++) {
      train.add(vector(i));
      actuals[i] = i % 2 == 0 ? EVEN : ODD;
    }

    AbstractBinaryClassifier classifier = classifier();
    classifier.train(train, actuals);

    for (int i = 100; i < 1000; i++) {
      //System.out.println(
      //  "[CLASSIFY] i=" + i + ", expected=" + (i % 2 == 0 ? EVEN : ODD) + ", actual="
      //    + classifier.predict(vector(i)));
      Assert.assertEquals(i % 2 == 0 ? EVEN : ODD, classifier.predict(vector(i)));
    }
  }

  @Test
  public void testUpdate() {

    int[] actuals = new int[100];
    List<FeatureVector> train = new ArrayList<>();

    for (int i = 0; i < 100; i++) {
      train.add(vector(i));
      actuals[i] = i % 2 == 0 ? EVEN : ODD;
    }

    AbstractBinaryClassifier classifier = classifier();
    classifier.train(train, actuals);

    if (classifier.supportsIncrementalTraining()) {

      for (int i = 100; i < 200; i++) {
        classifier.update(vector(i), i % 2 == 0 ? EVEN : ODD);
      }

      for (int i = 200; i < 1000; i++) {
        //System.out.println(
        //  "[UPDATE] i=" + i + ", expected=" + (i % 2 == 0 ? EVEN : ODD) + ", actual="
        //    + classifier.predict(vector(i)));
        Assert.assertEquals(i % 2 == 0 ? EVEN : ODD, classifier.predict(vector(i)));
      }
    } else {
      try {
        classifier.update(vector(100), EVEN);
      } catch (Exception e) {
        Assert.assertEquals(NotImplementedException.class, e.getClass());
      }
    }
  }

  protected abstract AbstractBinaryClassifier classifier();

  protected FeatureVector vector(int i) {
    FeatureVector vector = new FeatureVector(2);
    vector.set(0, i % 2);
    vector.set(1, 1 - i % 2); // ensure at least one non-zero entry
    return vector;
  }
}
