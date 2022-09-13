package com.computablefacts.asterix.ml;

import static com.computablefacts.asterix.ml.AbstractDocSetLabeler.findInterestingNGrams;
import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.KO;
import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.OK;

import com.computablefacts.asterix.IO;
import com.computablefacts.asterix.View;
import com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier;
import com.computablefacts.asterix.ml.classification.AbstractScaler;
import com.computablefacts.asterix.ml.classification.AdaBoostClassifier;
import com.computablefacts.asterix.ml.classification.DecisionTreeClassifier;
import com.computablefacts.asterix.ml.classification.DiscreteNaiveBayesClassifier;
import com.computablefacts.asterix.ml.classification.FisherLinearDiscriminantClassifier;
import com.computablefacts.asterix.ml.classification.GradientBoostedTreesClassifier;
import com.computablefacts.asterix.ml.classification.KNearestNeighborClassifier;
import com.computablefacts.asterix.ml.classification.LogisticRegressionClassifier;
import com.computablefacts.asterix.ml.classification.MultiLayerPerceptronClassifier;
import com.computablefacts.asterix.ml.classification.RandomForestClassifier;
import com.computablefacts.asterix.ml.classification.StandardScaler;
import com.computablefacts.asterix.ml.classification.SvmClassifier;
import com.computablefacts.asterix.ml.stacking.AbstractStack;
import com.computablefacts.asterix.ml.stacking.Stack;
import com.computablefacts.asterix.ml.textcategorization.TextCategorizer;
import com.google.common.annotations.Beta;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import com.google.re2j.Pattern;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.ArrayTypePermission;
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

  private static final long serialVersionUID = 42L;

  private final String label_;
  private List<? extends Function<String, FeatureVector>> vectorizers_;
  private AbstractBinaryClassifier classifier_;
  private AbstractScaler scaler_;

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
    List<String> labels = Splitter.on(',').trimResults().omitEmptyStrings().splitToList(args.length < 3 ? "" : args[2]);
    List<String> classifiers = Splitter.on(',').trimResults().omitEmptyStrings()
        .splitToList(args.length < 4 ? "logit" : args[3]);

    Preconditions.checkState(documents.exists(), "Missing documents : %s", documents);
    Preconditions.checkState(goldLabels.exists(), "Missing gold labels : %s", goldLabels);
    Preconditions.checkArgument(!labels.isEmpty(), "labels should not be empty");

    System.out.printf("Documents stored in %s.\n", documents);
    System.out.printf("Gold labels stored in %s.\n", goldLabels);
    System.out.printf("Labels to consider are [%s].\n", Joiner.on(", ").join(labels));
    System.out.printf("Classifiers to consider are [%s].\n", Joiner.on(", ").join(classifiers));

    // Load vocabulary: unigrams, bigrams and trigrams, etc.
    File funigrams = new File(String.format("%svocabulary-1grams.tsv.gz", goldLabels.getParent() + File.separator));
    File fbigrams = new File(String.format("%svocabulary-2grams.tsv.gz", goldLabels.getParent() + File.separator));
    File ftrigrams = new File(String.format("%svocabulary-3grams.tsv.gz", goldLabels.getParent() + File.separator));
    File fquadgrams = new File(String.format("%svocabulary-4grams.tsv.gz", goldLabels.getParent() + File.separator));
    File fquintgrams = new File(String.format("%svocabulary-5grams.tsv.gz", goldLabels.getParent() + File.separator));
    File fsextgrams = new File(String.format("%svocabulary-6grams.tsv.gz", goldLabels.getParent() + File.separator));

    Vocabulary unigrams = funigrams.exists() ? new Vocabulary(funigrams) : null;
    Vocabulary bigrams = fbigrams.exists() ? new Vocabulary(fbigrams) : null;
    Vocabulary trigrams = ftrigrams.exists() ? new Vocabulary(ftrigrams) : null;
    Vocabulary quadgrams = fquadgrams.exists() ? new Vocabulary(fquadgrams) : null;
    Vocabulary quintgrams = fquintgrams.exists() ? new Vocabulary(fquintgrams) : null;
    Vocabulary sextgrams = fsextgrams.exists() ? new Vocabulary(fsextgrams) : null;

    // Train/test model
    double trainSizeInPercent = 0.75; // TODO : move as parameter?

    for (String label : labels) {

      System.out.println("================================================================================");
      System.out.printf("== %s\n", label);
      System.out.println("================================================================================");

      System.out.println("Assembling dataset...");
      Stopwatch stopwatch = Stopwatch.createStarted();

      Map.Entry<List<String>, List<Integer>> dataset = GoldLabel.load(goldLabels, label).displayProgress(1000).unzip(
          goldLabel -> new SimpleImmutableEntry<>(goldLabel.data(),
              goldLabel.isTruePositive() || goldLabel.isFalseNegative() ? OK : KO));

      stopwatch.stop();
      System.out.printf("Dataset assembled in %d seconds.\n", stopwatch.elapsed(TimeUnit.SECONDS));

      if (dataset.getKey().size() < 10) {
        System.out.println("ERROR: dataset size has less than 10 entries.");
        continue;
      }

      Multiset<Integer> count = HashMultiset.create(dataset.getValue());

      if (count.count(OK) < 5 || count.count(KO) < 5) {
        System.out.println("ERROR: dataset must contain at least 5 positive and 5 negative entries.");
        continue;
      }

      System.out.printf("The number of POSITIVE entries is %d.\n", count.count(OK));
      System.out.printf("The number of NEGATIVE entries is %d.\n", count.count(KO));
      System.out.println("Running DocSetLabeler...");
      stopwatch.reset().start();

      List<String> ok = View.of(dataset.getKey()).zip(dataset.getValue()).filter(e -> e.getValue() == OK)
          .map(Entry::getKey).toList();
      List<String> ko = View.of(dataset.getKey()).zip(dataset.getValue()).filter(e -> e.getValue() == KO)
          .map(Entry::getKey).toList();

      Set<String> includeTags = Sets.newHashSet("WORD", "NUMBER", "TERMINAL_MARK"); // TODO : move as parameter?
      List<String> patterns = new ArrayList<>();
      List<List<Double>> weights = new ArrayList<>();

      if (unigrams != null) {
        Function<String, List<String>> tokenizer = Vocabulary.tokenizer(includeTags, 1);
        List<Map.Entry<String, Double>> lfs = findInterestingNGrams(tokenizer, unigrams, ok, ko);
        String pattern = "(" + Joiner.on(")|(").join(View.of(lfs).map(lf -> Pattern.quote(lf.getKey()))) + ")";
        List<Double> vector = View.of(lfs).map(Entry::getValue).toList();
        patterns.add(pattern);
        weights.add(vector);
        System.out.printf("Labeling functions for unigrams are : %s\n", pattern);
      }
      if (bigrams != null) {
        Function<String, List<String>> tokenizer = Vocabulary.tokenizer(includeTags, 2);
        List<Map.Entry<String, Double>> lfs = findInterestingNGrams(tokenizer, bigrams, ok, ko);
        String pattern =
            "(" + Joiner.on(")|(").join(View.of(lfs).map(lf -> Pattern.quote(lf.getKey()).replace("_", ".*"))) + ")";
        List<Double> vector = View.of(lfs).map(Entry::getValue).toList();
        patterns.add(pattern);
        weights.add(vector);
        System.out.printf("Labeling functions for bigrams are : %s\n", pattern);
      }
      if (trigrams != null) {
        Function<String, List<String>> tokenizer = Vocabulary.tokenizer(includeTags, 3);
        List<Map.Entry<String, Double>> lfs = findInterestingNGrams(tokenizer, trigrams, ok, ko);
        String pattern =
            "(" + Joiner.on(")|(").join(View.of(lfs).map(lf -> Pattern.quote(lf.getKey()).replace("_", ".*"))) + ")";
        List<Double> vector = View.of(lfs).map(Entry::getValue).toList();
        patterns.add(pattern);
        weights.add(vector);
        System.out.printf("Labeling functions for trigrams are : %s\n", pattern);
      }
      if (quadgrams != null) {
        Function<String, List<String>> tokenizer = Vocabulary.tokenizer(includeTags, 4);
        List<Map.Entry<String, Double>> lfs = findInterestingNGrams(tokenizer, quadgrams, ok, ko);
        String pattern =
            "(" + Joiner.on(")|(").join(View.of(lfs).map(lf -> Pattern.quote(lf.getKey()).replace("_", ".*"))) + ")";
        List<Double> vector = View.of(lfs).map(Entry::getValue).toList();
        patterns.add(pattern);
        weights.add(vector);
        System.out.printf("Labeling functions for quadgrams are : %s\n", pattern);
      }
      if (quintgrams != null) {
        Function<String, List<String>> tokenizer = Vocabulary.tokenizer(includeTags, 5);
        List<Map.Entry<String, Double>> lfs = findInterestingNGrams(tokenizer, quintgrams, ok, ko);
        String pattern =
            "(" + Joiner.on(")|(").join(View.of(lfs).map(lf -> Pattern.quote(lf.getKey()).replace("_", ".*"))) + ")";
        List<Double> vector = View.of(lfs).map(Entry::getValue).toList();
        patterns.add(pattern);
        weights.add(vector);
        System.out.printf("Labeling functions for quintgrams are : %s\n", pattern);
      }
      if (sextgrams != null) {
        Function<String, List<String>> tokenizer = Vocabulary.tokenizer(includeTags, 6);
        List<Map.Entry<String, Double>> lfs = findInterestingNGrams(tokenizer, sextgrams, ok, ko);
        String pattern =
            "(" + Joiner.on(")|(").join(View.of(lfs).map(lf -> Pattern.quote(lf.getKey()).replace("_", ".*"))) + ")";
        List<Double> vector = View.of(lfs).map(Entry::getValue).toList();
        patterns.add(pattern);
        weights.add(vector);
        System.out.printf("Labeling functions for sextgrams are : %s\n", pattern);
      }

      stopwatch.stop();
      System.out.printf("DocSetLabeler ran in %d seconds.\n", stopwatch.elapsed(TimeUnit.SECONDS));

      List<Model> models = new ArrayList<>();

      for (String classifier : classifiers) {

        Model model = new Model(label + "/" + classifier);
        model.vectorizers_ = View.of(patterns).map(pattern -> new RegexVectorizer(
            Pattern.compile(pattern, Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE))).toList();

        if ("dnb".equals(classifier)) {
          model.classifier_ = new DiscreteNaiveBayesClassifier();
        } else if ("fld".equals(classifier)) {
          model.classifier_ = new FisherLinearDiscriminantClassifier();
        } else if ("knn".equals(classifier)) {
          model.scaler_ = new StandardScaler();
          model.classifier_ = new KNearestNeighborClassifier(model.scaler_);
        } else if ("mlp".equals(classifier)) {
          model.scaler_ = new StandardScaler();
          model.classifier_ = new MultiLayerPerceptronClassifier(model.scaler_);
        } else if ("svm".equals(classifier)) {
          model.scaler_ = new StandardScaler();
          model.classifier_ = new SvmClassifier(model.scaler_);
        } else if ("rf".equals(classifier)) {
          model.classifier_ = new RandomForestClassifier();
        } else if ("dt".equals(classifier)) {
          model.classifier_ = new DecisionTreeClassifier();
        } else if ("gbt".equals(classifier)) {
          model.classifier_ = new GradientBoostedTreesClassifier();
        } else if ("ab".equals(classifier)) {
          model.classifier_ = new AdaBoostClassifier();
        } else {
          model.scaler_ = new StandardScaler();
          model.classifier_ = new LogisticRegressionClassifier(model.scaler_);
        }

        List<String> texts = dataset.getKey();
        List<Integer> categories = dataset.getValue();
        List<String> testDataset = new ArrayList<>();
        List<Integer> testCategories = new ArrayList<>();

        ConfusionMatrix confusionMatrix;

        // Train categorizer
        System.out.printf("Training text categorizer for classifier %s...\n", classifier);
        stopwatch.reset().start();

        TextCategorizer categorizer = TextCategorizer.trainTextCategorizer(texts, categories);

        stopwatch.stop();
        System.out.printf("Text categorizer trained in %d seconds.\n", stopwatch.elapsed(TimeUnit.SECONDS));

        // Split data between train and test then train a classifier
        System.out.printf("Training model for classifier %s...\n", classifier);
        stopwatch.reset().start();

        if (!model.classifier_.supportsIncrementalTraining()) {

          List<Entry<String, Integer>> batchOk = View.of(texts).zip(categories).filter(b -> b.getValue() == OK)
              .toList();
          List<Entry<String, Integer>> batchKo = View.of(texts).zip(categories).filter(b -> b.getValue() == KO)
              .toList();

          Collections.shuffle(batchOk);
          Collections.shuffle(batchKo);

          int trainSizeOk = (int) (batchOk.size() * trainSizeInPercent);
          int trainSizeKo = (int) (batchKo.size() * trainSizeInPercent);

          List<Entry<String, Integer>> train = View.of(batchOk).take(trainSizeOk)
              .concat(View.of(batchKo).take(trainSizeKo)).toList();
          List<Entry<String, Integer>> test = View.of(batchOk).drop(trainSizeOk)
              .concat(View.of(batchKo).drop(trainSizeKo)).toList();

          Collections.shuffle(train);
          Collections.shuffle(test);

          Map.Entry<List<String>, List<Integer>> trainn = View.of(train).unzip(Function.identity());
          Map.Entry<List<String>, List<Integer>> testt = View.of(test).unzip(Function.identity());

          model.train(trainn.getKey(), trainn.getValue(), categorizer);

          testDataset.addAll(testt.getKey());
          testCategories.addAll(testt.getValue());
        } else {
          View.of(texts).zip(categories).partition(1000 /* batch size */).forEachRemaining(list -> {

            List<Entry<String, Integer>> batchOk = View.of(list).filter(b -> b.getValue() == OK).toList();
            List<Entry<String, Integer>> batchKo = View.of(list).filter(b -> b.getValue() == KO).toList();

            Collections.shuffle(batchOk);
            Collections.shuffle(batchKo);

            int trainSizeOk = (int) (batchOk.size() * trainSizeInPercent);
            int trainSizeKo = (int) (batchKo.size() * trainSizeInPercent);

            List<Entry<String, Integer>> train = View.of(batchOk).take(trainSizeOk)
                .concat(View.of(batchKo).take(trainSizeKo)).toList();
            List<Entry<String, Integer>> test = View.of(batchOk).drop(trainSizeOk)
                .concat(View.of(batchKo).drop(trainSizeKo)).toList();

            Collections.shuffle(train);
            Collections.shuffle(test);

            Map.Entry<List<String>, List<Integer>> trainn = View.of(train).unzip(Function.identity());
            Map.Entry<List<String>, List<Integer>> testt = View.of(test).unzip(Function.identity());

            model.train(trainn.getKey(), trainn.getValue(), categorizer);

            testDataset.addAll(testt.getKey());
            testCategories.addAll(testt.getValue());
          });
        }

        stopwatch.stop();
        System.out.printf("Model trained in %d seconds.\n", stopwatch.elapsed(TimeUnit.SECONDS));

        // Test the classifier
        System.out.printf("Testing model for classifier %s...\n", classifier);
        stopwatch.reset().start();

        confusionMatrix = model.test(testDataset, testCategories, categorizer);

        stopwatch.stop();
        System.out.println(confusionMatrix);
        System.out.printf("Model tested in %d seconds.\n", stopwatch.elapsed(TimeUnit.SECONDS));

        // Save model
        System.out.printf("Saving model for classifier %s...\n", classifier);

        List<FeatureVector> vectors = texts.stream().map(model.featurizer(categorizer)).collect(Collectors.toList());

        model.init(vectors, categories.stream().mapToInt(x -> x).toArray());

        FeatureVector actuals = model.actuals_;
        FeatureVector predictions = model.predictions_;

        model.actuals_ = null;
        model.predictions_ = null;

        save(
            new File(String.format("%smodel-%s-%s.xml.gz", goldLabels.getParent() + File.separator, classifier, label)),
            model);

        System.out.println("Model saved.");

        model.actuals_ = actuals;
        model.predictions_ = predictions;

        models.add(model);
      }

      System.out.println("Building stack...");
      stopwatch.reset().start();

      Stack stack = new Stack(models.stream().map(model -> (AbstractStack) model).collect(Collectors.toList()));

      System.out.printf("Stack is %s.\n", stack);
      System.out.println(stack.confusionMatrix());
      System.out.printf("Stack built in %d seconds.\n", stopwatch.elapsed(TimeUnit.SECONDS));

      // Save ensemble model
      System.out.println("Saving stack...");

      save(new File(String.format("%sstack-%s.xml.gz", goldLabels.getParent() + File.separator, label)), stack);

      System.out.println("Stack saved.");
    }
  }

  public static <T> void save(File file, T model) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkNotNull(model, "model should not be null");

    Preconditions.checkState(IO.writeCompressedText(file, xStream().toXML(model), false), "%s cannot be written",
        file.getAbsolutePath());
  }

  @SuppressWarnings("unchecked")
  public static <T> T load(File file) {

    Preconditions.checkNotNull(file, "file should not be null");

    return (T) xStream().fromXML(String.join("\n", View.of(file, true).toList()));
  }

  private static XStream xStream() {

    XStream xStream = new XStream();
    xStream.addPermission(NoTypePermission.NONE);
    xStream.addPermission(NullPermission.NULL);
    xStream.addPermission(PrimitiveTypePermission.PRIMITIVES);
    xStream.addPermission(ArrayTypePermission.ARRAYS);
    xStream.allowTypeHierarchy(Collection.class);
    xStream.allowTypesByWildcard(
        new String[]{"com.computablefacts.asterix.**", "com.google.common.collect.**", "java.io.**", "java.lang.**",
            "java.util.**", "smile.classification.**", "smile.math.**", "smile.base.**", "smile.data.**"});

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

  private Function<String, FeatureVector> featurizer(TextCategorizer categorizer) {

    Preconditions.checkState(vectorizers_ != null, "missing vectorizer");

    TextNormalizer normalizer = new TextNormalizer(true);
    return text -> {

      // Vectorize text
      String newText = normalizer.apply(Strings.nullToEmpty(text));
      List<FeatureVector> vectors = new ArrayList<>(vectorizers_.size());

      for (int i = 0; i < vectorizers_.size(); i++) {
        vectors.add(vectorizers_.get(i).apply(newText));
      }

      // Concat vectors
      int length = vectors.stream().mapToInt(FeatureVector::length).sum();
      FeatureVector vector = new FeatureVector(length);

      @Var int prevLength = 0;

      for (FeatureVector vect : vectors) {
        for (int i = 0; i < vect.length(); i++) {
          vector.set(prevLength + i, vect.get(i));
        }
        prevLength += vect.length();
      }

      if (categorizer != null) {
        vector.append("OK".equals(categorizer.categorize(Strings.nullToEmpty(text))) ? OK : KO);
      }
      return vector;
    };
  }

  private void train(List<String> texts, List<Integer> categories, TextCategorizer categorizer) {

    Preconditions.checkNotNull(texts, "texts should not be null");
    Preconditions.checkNotNull(categories, "categories should not be null");
    Preconditions.checkArgument(texts.size() == categories.size(),
        "mismatch between the number of texts and the number of categories");
    Preconditions.checkState(vectorizers_ != null && vectorizers_.size() > 0, "missing vectorizer");
    Preconditions.checkState(classifier_ != null, "missing classifier");

    Function<String, FeatureVector> featurizer = featurizer(categorizer);

    if (!classifier_.isTrained()) {

      FeatureMatrix matrix = new FeatureMatrix(texts.stream().map(featurizer).collect(Collectors.toList()));
      int[] actuals = categories.stream().mapToInt(x -> x).toArray();

      classifier_.train(matrix, actuals);
      return;
    }

    Preconditions.checkState(classifier_.supportsIncrementalTraining(),
        "classifier does not support incremental training");

    View.of(texts).zip(categories).map(e -> {

      String text = e.getKey();
      int category = e.getValue();
      FeatureVector vector = featurizer.apply(text);

      return new SimpleImmutableEntry<>(vector, category);
    }).forEachRemaining(e -> classifier_.update(e.getKey(), e.getValue()));
  }

  private ConfusionMatrix test(List<String> texts, List<Integer> categories, TextCategorizer categorizer) {

    Preconditions.checkNotNull(texts, "texts should not be null");
    Preconditions.checkNotNull(categories, "categories should not be null");
    Preconditions.checkArgument(texts.size() == categories.size(),
        "mismatch between the number of texts entries and the number of categories");
    Preconditions.checkState(vectorizers_ != null && vectorizers_.size() > 0, "missing vectorizer");
    Preconditions.checkState(classifier_ != null, "classifier should be trained first");

    ConfusionMatrix confusionMatrix = new ConfusionMatrix();
    Function<String, FeatureVector> featurizer = featurizer(categorizer);

    View.of(texts).zip(categories).forEachRemaining(e -> {

      String text = e.getKey();
      int actual = e.getValue();

      Preconditions.checkState(actual == KO || actual == OK,
          "invalid class: should be either 1 (in class) or 0 (not in class)");

      FeatureVector vector = featurizer.apply(text);
      int prediction = classifier_.predict(vector);

      confusionMatrix.add(actual, prediction);
    });
    return confusionMatrix;
  }
}
