package com.computablefacts.asterix.ml.stacking;

import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.KO;
import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.OK;

import com.computablefacts.asterix.ml.FeatureVector;
import com.computablefacts.asterix.ml.classification.LogisticRegressionClassifier;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class StackTest {

  @Test
  public void testSimpleStack() {

    List<FeatureVector> dataset = new ArrayList<>(100);
    int[] actuals2 = new int[100];
    int[] actuals5 = new int[100];
    int[] actuals10 = new int[100];

    for (int i = 0; i < 100; i++) {
      actuals2[i] = i % 2 == 0 ? OK : KO;
      actuals5[i] = i % 5 == 0 ? OK : KO;
      actuals10[i] = i % 10 == 0 ? OK : KO;
      dataset.add(vector(i));
    }

    // Train model to detect numbers divisible by 2
    IsDivisibleByTwo isDivisibleByTwo = new IsDivisibleByTwo();
    isDivisibleByTwo.train(dataset, actuals2);

    // Train model to detect numbers divisible by 5
    IsDivisibleByFive isDivisibleByFive = new IsDivisibleByFive();
    isDivisibleByFive.train(dataset, actuals5);

    // Create stack to detect number divisible by 10 i.e. both 2 and 5
    isDivisibleByTwo.init(dataset, actuals10);
    isDivisibleByFive.init(dataset, actuals10);

    Stack stack = new Stack(Lists.newArrayList(isDivisibleByTwo, isDivisibleByFive));

    Assert.assertEquals("(IsDivisibleByTwo AND IsDivisibleByFive)", stack.toString());
    Assert.assertEquals(1.0, stack.confusionMatrix().matthewsCorrelationCoefficient(), 0.0);
    Assert.assertEquals(1.0, stack.confusionMatrix().f1Score(), 0.0);

    for (int i = 100; i < 1000; i++) {
      Assert.assertEquals(i % 10 == 0 ? OK : KO, stack.predict(vector(i)));
    }
  }

  @Test
  public void testComplexStack() {

    List<FeatureVector> dataset = new ArrayList<>(100);
    int[] actuals2 = new int[100];
    int[] actuals3 = new int[100];
    int[] actuals5 = new int[100];
    int[] actuals3Or10 = new int[100];

    for (int i = 0; i < 100; i++) {
      actuals2[i] = i % 2 == 0 ? OK : KO;
      actuals3[i] = i % 3 == 0 ? OK : KO;
      actuals5[i] = i % 5 == 0 ? OK : KO;
      actuals3Or10[i] = i % 3 == 0 || i % 10 == 0 ? OK : KO;
      dataset.add(vector(i));
    }

    // Train model to detect numbers divisible by 2
    IsDivisibleByTwo isDivisibleByTwo = new IsDivisibleByTwo();
    isDivisibleByTwo.train(dataset, actuals2);

    // Train model to detect numbers divisible by 3
    IsDivisibleByThree isDivisibleByThree = new IsDivisibleByThree();
    isDivisibleByThree.train(dataset, actuals3);

    // Train model to detect numbers divisible by 5
    IsDivisibleByFive isDivisibleByFive = new IsDivisibleByFive();
    isDivisibleByFive.train(dataset, actuals5);

    // Create stack to detect number divisible by 3 or 10
    isDivisibleByTwo.init(dataset, actuals3Or10);
    isDivisibleByThree.init(dataset, actuals3Or10);
    isDivisibleByFive.init(dataset, actuals3Or10);

    Stack stack = new Stack(
        Lists.newArrayList(isDivisibleByTwo, isDivisibleByThree, isDivisibleByFive));

    Assert.assertEquals("((IsDivisibleByTwo AND IsDivisibleByFive) OR IsDivisibleByThree)",
        stack.toString());
    Assert.assertEquals(1.0, stack.confusionMatrix().matthewsCorrelationCoefficient(), 0.0);
    Assert.assertEquals(1.0, stack.confusionMatrix().f1Score(), 0.0);

    for (int i = 100; i < 1000; i++) {
      Assert.assertEquals(i % 3 == 0 || i % 10 == 0 ? OK : KO, stack.predict(vector(i)));
    }
  }

  private FeatureVector vector(int i) {
    FeatureVector vector = new FeatureVector(4);
    vector.set(0, i % 2 == 0 ? 1 : 0);
    vector.set(1, i % 3 == 0 ? 1 : 0);
    vector.set(2, i % 5 == 0 ? 1 : 0);
    vector.set(3, i % 7 == 0 ? 1 : 0);
    return vector;
  }

  @CheckReturnValue
  private static class IsDivisibleByFive extends AbstractModel {

    private final LogisticRegressionClassifier classifier_ = new LogisticRegressionClassifier();

    public IsDivisibleByFive() {
      super("IsDivisibleByFive");
    }

    @Override
    public int predict(FeatureVector vector) {
      return classifier_.predict(vector);
    }

    public void train(List<FeatureVector> vectors, int[] actuals) {
      classifier_.train(vectors, actuals);
    }
  }

  @CheckReturnValue
  private static class IsDivisibleByThree extends AbstractModel {

    private final LogisticRegressionClassifier classifier_ = new LogisticRegressionClassifier();

    public IsDivisibleByThree() {
      super("IsDivisibleByThree");
    }

    @Override
    public int predict(FeatureVector vector) {
      return classifier_.predict(vector);
    }

    public void train(List<FeatureVector> vectors, int[] actuals) {
      classifier_.train(vectors, actuals);
    }
  }

  @CheckReturnValue
  private static class IsDivisibleByTwo extends AbstractModel {

    private final LogisticRegressionClassifier classifier_ = new LogisticRegressionClassifier();

    public IsDivisibleByTwo() {
      super("IsDivisibleByTwo");
    }

    @Override
    public int predict(FeatureVector vector) {
      return classifier_.predict(vector);
    }

    public void train(List<FeatureVector> vectors, int[] actuals) {
      classifier_.train(vectors, actuals);
    }
  }
}
