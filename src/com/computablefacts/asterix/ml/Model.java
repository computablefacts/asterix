package com.computablefacts.asterix.ml;

import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.KO;
import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.OK;

import com.computablefacts.asterix.Document;
import com.computablefacts.asterix.IO;
import com.computablefacts.asterix.Span;
import com.computablefacts.asterix.SpanSequence;
import com.computablefacts.asterix.View;
import com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier;
import com.computablefacts.asterix.ml.classification.DiscreteNaiveBayesClassifier;
import com.computablefacts.asterix.ml.classification.FisherLinearDiscriminantClassifier;
import com.computablefacts.asterix.ml.classification.KNearestNeighborClassifier;
import com.computablefacts.asterix.ml.classification.LogisticRegressionClassifier;
import com.computablefacts.asterix.ml.classification.MultiLayerPerceptronClassifier;
import com.computablefacts.asterix.ml.classification.SvmClassifier;
import com.computablefacts.asterix.ml.stacking.AbstractStack;
import com.computablefacts.asterix.ml.stacking.Stack;
import com.google.common.annotations.Beta;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;
import java.io.File;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A named binary classifier.
 */
@CheckReturnValue
final public class Model extends AbstractStack {

  private final String label_;
  private Function<String, String> normalizer_;
  private Function<String, SpanSequence> tokenizer_;
  private List<Function<SpanSequence, FeatureVector>> vectorizers_;
  private AbstractBinaryClassifier classifier_;

  public Model(String label) {
    label_ = Preconditions.checkNotNull(label, "label should not be null");
  }

  /**
   * Build a model from the ground up using only a given set of gold labels.
   * <ul>
   *   <li>{@code args[0]} the corpus of documents as a gzipped JSONL file.</li>
   *   <li>{@code args[1]} the gold labels as a gzipped JSONL file.</li>
   *   <li>{@code args[2]} the list of labels to consider.</li>
   *   <li>{@code args[3]} the classifier name to train in `{dnb, fld, knn, mlp, svm, logit}`.</li>
   * </ul>
   */
  @Beta
  public static void main(String[] args) {

    File documents = new File(args[0]);
    File goldLabels = new File(args[1]);
    List<String> labels = Splitter.on(',').trimResults().omitEmptyStrings()
        .splitToList(args.length < 3 ? "" : args[2]);
    List<String> classifiers = Splitter.on(',').trimResults().omitEmptyStrings()
        .splitToList(args.length < 4 ? "logit" : args[3]);

    Preconditions.checkState(documents.exists(), "Missing documents : %s", documents);
    Preconditions.checkState(goldLabels.exists(), "Missing gold labels : %s", goldLabels);
    Preconditions.checkArgument(!labels.isEmpty(), "labels should not be empty");

    System.out.printf("Documents stored in %s.\n", documents);
    System.out.printf("Gold labels stored in %s.\n", goldLabels);
    System.out.printf("Labels to consider are [%s].\n", Joiner.on(", ").join(labels));
    System.out.printf("Classifier to consider are [%s].\n", Joiner.on(", ").join(classifiers));

    TextNormalizer normalizer = new TextNormalizer();
    TextTokenizer tokenizer = new TextTokenizer();

    // Build vocabulary: unigrams, bigrams and trigrams
    double minDocFreq = 0.01; // TODO : move as parameter?
    double maxDocFreq = 0.99; // TODO : move as parameter?
    int maxVocabSize = 100_000; // TODO : move as parameter?
    Set<String> includeTags = Sets.newHashSet("WORD", "NUMBER",
        "TERMINAL_MARK"); // TODO : move as parameter?

    @Var Vocabulary unigrams = null;
    @Var Vocabulary bigrams = null;
    @Var Vocabulary trigrams = null;

    File funigrams = new File(
        String.format("%svocabulary-unigrams.tsv.gz", goldLabels.getParent() + File.separator));
    File fbigrams = new File(
        String.format("%svocabulary-bigrams.tsv.gz", goldLabels.getParent() + File.separator));
    File ftrigrams = new File(
        String.format("%svocabulary-trigrams.tsv.gz", goldLabels.getParent() + File.separator));

    boolean exists = funigrams.exists() && fbigrams.exists() /*&& ftrigrams.exists() */;

    Preconditions.checkState(
        exists || (!funigrams.exists() && !fbigrams.exists() /* && !ftrigrams.exists() */));

    if (exists) {
      unigrams = funigrams.exists() ? new Vocabulary(funigrams) : null;
      bigrams = fbigrams.exists() ? new Vocabulary(fbigrams) : null;
      trigrams = ftrigrams.exists() ? new Vocabulary(ftrigrams) : null;
    } else {

      System.out.println("Sampling documents...");
      Stopwatch stopwatch = Stopwatch.createStarted();

      List<String> sample = Document.of(documents, true).map(doc -> (String) doc.text())
          .displayProgress(10_000).sample(30_000);

      stopwatch.stop();
      System.out.printf("Documents sampled in %d seconds.\n", stopwatch.elapsed(TimeUnit.SECONDS));

      for (int i = 0; i < 3; i++) {

        int length = i + 1;

        View<List<String>> tokens = View.of(sample).map(normalizer).map(tokenizer).map(
            spans -> View.of(spans)
                .filter(span -> !Sets.intersection(includeTags, span.tags()).isEmpty())
                .map(Span::text).overlappingWindowWithStrictLength(length)
                .map(tks -> Joiner.on('_').join(tks)).toList()).displayProgress(10_000);

        if (length == 1 && !funigrams.exists()) {

          System.out.println("Building vocabulary for unigrams...");
          stopwatch.reset().start();

          unigrams = Vocabulary.of(tokens, minDocFreq, maxDocFreq, maxVocabSize);
          unigrams.save(funigrams);
          unigrams = null;

          stopwatch.stop();
          System.out.printf("Vocabulary for unigrams built in %d seconds.\n",
              stopwatch.elapsed(TimeUnit.SECONDS));
        } else if (length == 2 && !fbigrams.exists()) {

          System.out.println("Building vocabulary for bigrams...");
          stopwatch.reset().start();

          bigrams = Vocabulary.of(tokens, minDocFreq, maxDocFreq, maxVocabSize);
          bigrams.save(fbigrams);
          bigrams = null;

          stopwatch.stop();
          System.out.printf("Vocabulary for bigrams built in %d seconds.\n",
              stopwatch.elapsed(TimeUnit.SECONDS));
        } else if (length == 3 && !ftrigrams.exists()) {

          System.out.println("Building vocabulary for trigrams...");
          stopwatch.reset().start();

          trigrams = Vocabulary.of(tokens, minDocFreq, maxDocFreq, maxVocabSize);
          trigrams.save(ftrigrams);
          trigrams = null;

          stopwatch.stop();
          System.out.printf("Vocabulary for trigrams built in %d seconds.\n",
              stopwatch.elapsed(TimeUnit.SECONDS));
        }
      }

      unigrams = funigrams.exists() ? new Vocabulary(funigrams) : null;
      bigrams = fbigrams.exists() ? new Vocabulary(fbigrams) : null;
      trigrams = ftrigrams.exists() ? new Vocabulary(ftrigrams) : null;
    }

    // Train/test model
    double trainSizeInPercent = 0.75; // TODO : move as parameter?

    for (String label : labels) {

      System.out.println(
          "================================================================================");
      System.out.printf("== %s\n", label);
      System.out.println(
          "================================================================================");

      System.out.println("Assembling dataset...");
      Stopwatch stopwatch = Stopwatch.createStarted();

      Map.Entry<List<String>, List<Integer>> dataset = GoldLabel.load(goldLabels, label)
          .displayProgress(1000).unzip(goldLabel -> new SimpleImmutableEntry<>(goldLabel.data(),
              goldLabel.isTruePositive() || goldLabel.isFalseNegative() ? OK : KO));

      stopwatch.stop();
      System.out.printf("Dataset assembled in %d seconds.\n", stopwatch.elapsed(TimeUnit.SECONDS));

      List<Model> models = new ArrayList<>();

      for (String classifier : classifiers) {

        Model model = new Model(label + "/" + classifier);
        model.normalizer_ = normalizer;
        model.tokenizer_ = tokenizer;
        model.vectorizers_ = new ArrayList<>();

        if (unigrams != null) {
          model.vectorizers_.add(new TfIdfVectorizer(unigrams));
        }
        if (bigrams != null) {
          model.vectorizers_.add(new TfIdfVectorizer(bigrams));
        }
        if (trigrams != null) {
          model.vectorizers_.add(new TfIdfVectorizer(trigrams));
        }

        if ("dnb".equals(classifier)) {
          model.classifier_ = new DiscreteNaiveBayesClassifier();
        } else if ("fld".equals(classifier)) {
          model.classifier_ = new FisherLinearDiscriminantClassifier();
        } else if ("knn".equals(classifier)) {
          model.classifier_ = new KNearestNeighborClassifier();
        } else if ("mlp".equals(classifier)) {
          model.classifier_ = new MultiLayerPerceptronClassifier();
        } else if ("svm".equals(classifier)) {
          model.classifier_ = new SvmClassifier();
        } else {
          model.classifier_ = new LogisticRegressionClassifier();
        }

        List<String> texts = dataset.getKey();
        List<Integer> categories = dataset.getValue();
        List<String> testDataset = new ArrayList<>();
        List<Integer> testCategories = new ArrayList<>();

        ConfusionMatrix confusionMatrix;

        // Split data between train and test then train a classifier
        System.out.printf("Training model for classifier %s...\n", classifier);
        stopwatch.reset().start();

        if (!model.classifier_.supportsIncrementalTraining()) {

          List<Entry<String, Integer>> batch = View.of(texts).zip(View.of(categories)).toList();

          Collections.shuffle(batch);

          int trainSize = (int) (batch.size() * trainSizeInPercent);

          Map.Entry<List<String>, List<Integer>> train = View.of(batch).take(trainSize)
              .unzip(Function.identity());
          Map.Entry<List<String>, List<Integer>> test = View.of(batch).drop(trainSize)
              .unzip(Function.identity());

          model.train(train.getKey(), train.getValue());

          testDataset.addAll(test.getKey());
          testCategories.addAll(test.getValue());
        } else {
          View.of(texts).zip(View.of(categories)).partition(1000 /* batch size */)
              .forEachRemaining(list -> {

                List<Map.Entry<String, Integer>> batch = new ArrayList<>(list);
                Collections.shuffle(batch);

                int trainSize = (int) (batch.size() * trainSizeInPercent);

                Map.Entry<List<String>, List<Integer>> train = View.of(batch).take(trainSize)
                    .unzip(Function.identity());
                Map.Entry<List<String>, List<Integer>> test = View.of(batch).drop(trainSize)
                    .unzip(Function.identity());

                model.train(train.getKey(), train.getValue());

                testDataset.addAll(test.getKey());
                testCategories.addAll(test.getValue());
              });
        }

        stopwatch.stop();
        System.out.printf("Model trained in %d seconds.\n", stopwatch.elapsed(TimeUnit.SECONDS));

        // Test the classifier
        System.out.printf("Testing model for classifier %s...\n", classifier);
        stopwatch.reset().start();

        confusionMatrix = model.test(testDataset, testCategories);

        stopwatch.stop();
        System.out.println(confusionMatrix);
        System.out.printf("Model tested in %d seconds.\n", stopwatch.elapsed(TimeUnit.SECONDS));

        // Save model
        System.out.printf("Saving model for classifier %s...\n", classifier);

        List<FeatureVector> vectors = texts.stream().map(model.featurizer())
            .collect(Collectors.toList());

        model.init(vectors, categories.stream().mapToInt(x -> x).toArray());

        FeatureVector actuals = model.actuals_;
        FeatureVector predictions = model.predictions_;

        model.actuals_ = null;
        model.predictions_ = null;

        save(new File(String.format("%smodel-%s-%s.xml.gz", goldLabels.getParent() + File.separator,
            classifier, label)), model);

        System.out.println("Model saved.");

        model.actuals_ = actuals;
        model.predictions_ = predictions;

        models.add(model);
      }

      System.out.println("Building ensemble model...");
      stopwatch.reset().start();

      Stack stack = new Stack(
          models.stream().map(model -> (AbstractStack) model).collect(Collectors.toList()));

      System.out.printf("Ensemble model is %s.\n", stack);
      System.out.println(stack.confusionMatrix());
      System.out.printf("Ensemble model built in %d seconds.\n",
          stopwatch.elapsed(TimeUnit.SECONDS));

      // TODO : save ensemble model
    }
  }

  public static void save(File file, Model model) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkNotNull(model, "model should not be null");

    Preconditions.checkState(IO.writeCompressedText(file, xStream().toXML(model), false),
        "%s cannot be written", file.getAbsolutePath());
  }

  @SuppressWarnings("unchecked")
  public static Model load(File file) {

    Preconditions.checkNotNull(file, "file should not be null");

    return (Model) xStream().fromXML(String.join("\n", View.of(file, true).toList()));
  }

  private static XStream xStream() {

    XStream xStream = new XStream();
    xStream.addPermission(NoTypePermission.NONE);
    xStream.addPermission(NullPermission.NULL);
    xStream.addPermission(PrimitiveTypePermission.PRIMITIVES);
    xStream.allowTypeHierarchy(Collection.class);
    xStream.allowTypesByWildcard(
        new String[]{"com.computablefacts.asterix.**", "com.google.common.collect.**",
            "java.lang.**", "java.util.**", "smile.classification.**"});

    return xStream;
  }

  @Override
  public String toString() {
    return label_;
  }

  @Override
  public int predict(FeatureVector vector) {
    return classifier_.predict(vector);
  }

  private Function<String, FeatureVector> featurizer() {

    Preconditions.checkState(normalizer_ != null, "missing normalizer");
    Preconditions.checkState(tokenizer_ != null, "missing tokenizer");
    Preconditions.checkState(vectorizers_ != null, "missing vectorizer");

    VectorsMerger merger = new VectorsMerger();
    return (text) -> {

      SpanSequence spans = tokenizer_.apply(normalizer_.apply(text));
      List<FeatureVector> vectors = new ArrayList<>(vectorizers_.size());

      for (Function<SpanSequence, FeatureVector> vectorizer : vectorizers_) {
        vectors.add(vectorizer.apply(spans));
      }
      return merger.apply(vectors);
    };
  }

  private void train(List<String> texts, List<Integer> categories) {

    Preconditions.checkNotNull(texts, "texts should not be null");
    Preconditions.checkNotNull(categories, "categories should not be null");
    Preconditions.checkArgument(texts.size() == categories.size(),
        "mismatch between the number of texts entries and the number of categories");
    Preconditions.checkState(tokenizer_ != null, "missing tokenizer");
    Preconditions.checkState(vectorizers_ != null && vectorizers_.size() > 0, "missing vectorizer");
    Preconditions.checkState(classifier_ != null, "missing classifier");

    Function<String, FeatureVector> featurizer = featurizer();

    if (!classifier_.isTrained()) {

      List<FeatureVector> vectors = texts.stream().map(featurizer).collect(Collectors.toList());
      int[] actuals = categories.stream().mapToInt(x -> x).toArray();

      classifier_.train(vectors, actuals);
      return;
    }

    Preconditions.checkState(classifier_.supportsIncrementalTraining(),
        "classifier does not support incremental training");

    View.of(texts).zip(View.of(categories)).map(e -> {

      FeatureVector vector = featurizer.apply(e.getKey());
      int category = e.getValue();

      return new SimpleImmutableEntry<>(vector, category);
    }).forEachRemaining(e -> classifier_.update(e.getKey(), e.getValue()));
  }

  private ConfusionMatrix test(List<String> texts, List<Integer> categories) {

    Preconditions.checkNotNull(texts, "texts should not be null");
    Preconditions.checkNotNull(categories, "categories should not be null");
    Preconditions.checkArgument(texts.size() == categories.size(),
        "mismatch between the number of texts entries and the number of categories");
    Preconditions.checkState(tokenizer_ != null, "missing tokenizer");
    Preconditions.checkState(vectorizers_ != null && vectorizers_.size() > 0, "missing vectorizer");
    Preconditions.checkState(classifier_ != null, "classifier should be trained first");

    ConfusionMatrix confusionMatrix = new ConfusionMatrix();
    Function<String, FeatureVector> featurizer = featurizer();

    View.of(texts).zip(View.of(categories)).forEachRemaining(e -> {

      @Var String text = e.getKey();
      int actual = e.getValue();

      Preconditions.checkState(actual == KO || actual == OK,
          "invalid class: should be either 1 (in class) or 0 (not in class)");

      FeatureVector vector = featurizer.apply(text);
      int prediction = classifier_.predict(vector);

      if (actual == OK) {
        if (prediction == OK) {
          confusionMatrix.incrementTruePositives();
        } else {
          confusionMatrix.incrementFalseNegatives();
        }
      } else {
        if (prediction == OK) {
          confusionMatrix.incrementFalsePositives();
        } else {
          confusionMatrix.incrementTrueNegatives();
        }
      }
    });
    return confusionMatrix;
  }
}
