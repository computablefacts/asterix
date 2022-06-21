package com.computablefacts.asterix.ml.classifiers;

import com.computablefacts.asterix.ml.FeatureVector;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.List;
import smile.base.mlp.Layer;
import smile.base.mlp.OutputFunction;
import smile.classification.MLP;
import smile.math.MathEx;
import smile.math.TimeFunction;

@CheckReturnValue
final public class MultiLayerPerceptronClassifier implements AbstractBinaryClassifier {

  private final int nbEpochs_;
  private final int nbHiddenNeurons_;
  private MLP classifier_;

  public MultiLayerPerceptronClassifier() {
    this(10, 3);
  }

  public MultiLayerPerceptronClassifier(int nbEpochs, int nbHiddenNeurons) {

    Preconditions.checkArgument(nbEpochs > 0, "the number of epochs must be > 0");
    Preconditions.checkArgument(nbHiddenNeurons > 0, "the number of hidden neurons must be > 0");

    nbEpochs_ = nbEpochs;
    nbHiddenNeurons_ = nbHiddenNeurons;
  }

  @Override
  public int predict(FeatureVector vector) {
    return classifier_.predict(vector.denseArray());
  }

  @Override
  public void train(List<FeatureVector> vectors, int[] actuals) {

    Preconditions.checkNotNull(vectors, "vectors should not be null");
    Preconditions.checkNotNull(actuals, "actuals should not be null");
    Preconditions.checkArgument(vectors.size() == actuals.length,
        "mismatch between the number of vectors and the number of actuals");
    Preconditions.checkState(classifier_ == null, "classifier has already been trained");

    double[][] newVectors = new double[vectors.size()][vectors.get(0).length()];

    for (int i = 0; i < vectors.size(); i++) {
      newVectors[i] = vectors.get(i).denseArray();
    }

    classifier_ = new MLP(newVectors[0].length, Layer.sigmoid(nbHiddenNeurons_),
        Layer.mle(1, OutputFunction.SIGMOID));
    classifier_.setLearningRate(TimeFunction.linear(0.02, 10000, 0.01));
    classifier_.setMomentum(TimeFunction.constant(0.01));

    for (int epoch = 0; epoch < nbEpochs_; epoch++) {
      for (int i : MathEx.permutate(newVectors.length)) {
        classifier_.update(newVectors[i], actuals[i]);
      }
    }
  }

  @Override
  public void update(FeatureVector vector, int actual) {

    Preconditions.checkNotNull(vector, "vector should not be null");
    Preconditions.checkArgument(actual == KO || actual == OK,
        "invalid class: should be either 1 (in class) or 0 (not in class)");
    Preconditions.checkState(classifier_ != null, "classifier should be trained first");

    classifier_.update(vector.denseArray(), actual);
  }

  @Override
  public boolean supportsIncrementalTraining() {
    return true;
  }
}
