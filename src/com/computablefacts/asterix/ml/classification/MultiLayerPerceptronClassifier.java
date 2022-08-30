package com.computablefacts.asterix.ml.classification;

import com.computablefacts.asterix.ml.FeatureMatrix;
import com.computablefacts.asterix.ml.FeatureVector;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import smile.base.mlp.Layer;
import smile.base.mlp.OutputFunction;
import smile.classification.MLP;
import smile.math.MathEx;
import smile.math.TimeFunction;

@CheckReturnValue
final public class MultiLayerPerceptronClassifier implements AbstractBinaryClassifier {
  
  private final AbstractScaler scaler_;
  private final int nbEpochs_;
  private final int nbHiddenNeurons_;
  private MLP classifier_;

  public MultiLayerPerceptronClassifier(AbstractScaler scaler) {
    this(scaler, 10, 3);
  }

  public MultiLayerPerceptronClassifier(AbstractScaler scaler, int nbEpochs, int nbHiddenNeurons) {

    Preconditions.checkNotNull(scaler, "scaler should not be null");
    Preconditions.checkArgument(nbEpochs > 0, "the number of epochs must be > 0");
    Preconditions.checkArgument(nbHiddenNeurons > 0, "the number of hidden neurons must be > 0");

    scaler_ = scaler;
    nbEpochs_ = nbEpochs;
    nbHiddenNeurons_ = nbHiddenNeurons;
  }

  @Override
  public boolean isTrained() {
    return classifier_ != null;
  }

  @Override
  public int predict(FeatureVector vector) {

    Preconditions.checkNotNull(vector, "vector should not be null");

    return classifier_.predict(scaler_.predict(vector).denseArray());
  }

  @Override
  public void train(FeatureMatrix matrix, int[] actuals) {

    Preconditions.checkNotNull(matrix, "matrix should not be null");
    Preconditions.checkNotNull(actuals, "actuals should not be null");
    Preconditions.checkArgument(matrix.nbRows() == actuals.length,
        "mismatch between the number of rows and the number of actuals");
    Preconditions.checkState(classifier_ == null, "classifier has already been trained");

    double[][] newMatrix = scaler_.train(matrix).denseArray();

    classifier_ = new MLP(newMatrix[0].length, Layer.sigmoid(nbHiddenNeurons_), Layer.mle(1, OutputFunction.SIGMOID));
    classifier_.setLearningRate(TimeFunction.linear(0.02, 10000, 0.01));
    classifier_.setMomentum(TimeFunction.constant(0.01));

    for (int epoch = 0; epoch < nbEpochs_; epoch++) {
      for (int i : MathEx.permutate(newMatrix.length)) {
        classifier_.update(newMatrix[i], actuals[i]);
      }
    }
  }

  @Override
  public void update(FeatureVector vector, int actual) {

    Preconditions.checkNotNull(vector, "vector should not be null");
    Preconditions.checkArgument(actual == KO || actual == OK,
        "invalid class: should be either 1 (in class) or 0 (not in class)");
    Preconditions.checkState(classifier_ != null, "classifier should be trained first");

    classifier_.update(scaler_.predict(vector).denseArray(), actual);
  }

  @Override
  public boolean supportsIncrementalTraining() {
    return true;
  }
}
