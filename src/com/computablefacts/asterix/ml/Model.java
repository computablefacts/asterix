package com.computablefacts.asterix.ml;

import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.KO;
import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.OK;

import com.computablefacts.asterix.IO;
import com.computablefacts.asterix.SnippetExtractor;
import com.computablefacts.asterix.Span;
import com.computablefacts.asterix.View;
import com.computablefacts.asterix.ml.VectorsReducer.eCorrelation;
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
import com.google.common.annotations.Beta;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A named binary classifier.
 */
@CheckReturnValue
final public class Model extends AbstractStack {

  private final String label_;
  private Featurizer featurizer_;
  private AbstractBinaryClassifier classifier_;
  private AbstractScaler scaler_;

  public Model(String label) {
    label_ = Preconditions.checkNotNull(label, "label should not be null");
  }

  /**
   * Build a model from the ground up using only a given set of gold labels.
   * <ul>
   *   <li>{@code args[0]} the gold labels as a gzipped JSONL file.</li>
   *   <li>{@code args[1]} the list of labels to consider.</li>
   *   <li>{@code args[2]} the classifier name to train in `{dnb, fld, knn, mlp, svm, logit}`.</li>
   * </ul>
   */
  @Beta
  public static void main(String[] args) {

    File goldLabels = new File(args[0]);
    List<String> labels = Splitter.on(',').trimResults().omitEmptyStrings().splitToList(args.length < 3 ? "" : args[1]);
    List<String> classifiers = Splitter.on(',').trimResults().omitEmptyStrings()
        .splitToList(args.length < 3 ? "logit" : args[2]);

    Preconditions.checkState(goldLabels.exists(), "Missing gold labels : %s", goldLabels);
    Preconditions.checkArgument(!labels.isEmpty(), "labels should not be empty");

    System.out.printf("Gold labels stored in %s.\n", goldLabels);
    System.out.printf("Labels to consider are [%s].\n", Joiner.on(", ").join(labels));
    System.out.printf("Classifiers to consider are [%s].\n", Joiner.on(", ").join(classifiers));

    // Load vocabulary (unigrams)
    File fvocabulary = new File(String.format("%svocabulary-1grams.tsv.gz", goldLabels.getParent() + File.separator));

    Preconditions.checkState(fvocabulary.exists(), "Missing vocabulary : %s", fvocabulary);

    Vocabulary vocabulary = fvocabulary.exists() ? new Vocabulary(fvocabulary) : null;

    // Initialize tokenizers
    int chopAt = 6; // TODO : move as parameter?
    Set<String> includeTags = Sets.newHashSet("WORD", "NUMBER", "TERMINAL_MARK"); // TODO : move as parameter?
    Set<String> stopwords = Sets.newHashSet(vocabulary.stopwords(100));
    Predicate<Span> keepSpan = span -> !Sets.intersection(span.tags(), includeTags).isEmpty();
    Predicate<String> keepToken = tkn -> tkn.length() > 1 && vocabulary.index(tkn) != 0 /* UNK */
        && !stopwords.contains(tkn);
    Function<String, List<String>> tokenizer = Vocabulary.tokenizer(keepSpan, keepToken, null, 1, chopAt);

    // Train/test model
    double trainSizeInPercent = 0.75; // TODO : move as parameter?

    for (String label : labels) {

      System.out.println("================================================================================");
      System.out.printf("== %s\n", label);
      System.out.println("================================================================================");

      System.out.println("Assembling dataset...");
      Stopwatch stopwatch = Stopwatch.createStarted();

      Map.Entry<List<GoldLabel>, List<Integer>> dataset = GoldLabel.load(goldLabels, label).displayProgress(1000).unzip(
          goldLabel -> new SimpleImmutableEntry<>(goldLabel,
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
/*
      // Train categorizer
      System.out.println("Training text categorizer...");
      stopwatch.reset().start();

      TextCategorizer categorizer = TextCategorizer.trainTextCategorizer(
          View.of(dataset.getKey()).map(GoldLabel::data).toList(), dataset.getValue());

      stopwatch.stop();
      System.out.printf("Text categorizer trained in %d seconds.\n", stopwatch.elapsed(TimeUnit.SECONDS));
*/
      // Extract interesting ngrams
      System.out.println("Running DocSetLabeler...");
      stopwatch.reset().start();

      List<String> ok = View.of(dataset.getKey()).map(GoldLabel::data).zip(dataset.getValue())
          .filter(e -> e.getValue() == OK).map(Entry::getKey).toList();
      List<String> ko = View.of(dataset.getKey()).map(GoldLabel::data).zip(dataset.getValue())
          .filter(e -> e.getValue() == KO).map(Entry::getKey).toList();

      int nbCandidatesToConsider = 50; // TODO : move as parameter?
      int nbLabelsToReturn = 25; // TODO : move as parameter?
      List<Map.Entry<String, Double>> labelingFunctions = findInterestingNGrams(ok, ko, vocabulary, tokenizer,
          nbCandidatesToConsider, nbLabelsToReturn);

      stopwatch.stop();
      System.out.printf("DocSetLabeler ran in %d seconds.\n", stopwatch.elapsed(TimeUnit.SECONDS));
      System.out.printf("Labeling functions are : [\n  %s\n]\n",
          View.of(labelingFunctions).toString(Entry::getKey, ",\n  "));

      // Rewrite all LF as spans
      System.out.println("Capturing context around labeling functions...");
      stopwatch.reset().start();

      int windowLength = 7; // TODO : move as parameter?
      int windowCenter = (windowLength - 1) / 2;
      Set<String> lfs = View.of(labelingFunctions).map(Entry::getKey).toSet();

      Function<String, Multiset<String>> dicBuilder = text -> HashMultiset.create(View.of(text).map(tokenizer).flatten(
              tkns -> View.of(tkns).overlappingWindowWithStrictLength(windowLength)
                  .filter(ngram -> ngram.size() == windowLength && lfs.contains(ngram.get(windowCenter))))
          .map(ngram -> Joiner.on('_').join(ngram)).toList());

      Map<String, Double> dictionary = new HashMap<>();

      View.of(ok).map(tokenizer).flatten(tkns -> View.of(tkns).overlappingWindowWithStrictLength(windowLength)
              .filter(ngram -> ngram.size() == windowLength && lfs.contains(ngram.get(windowCenter))))
          .forEachRemaining(ngram -> {
            String pattern = Joiner.on('_').join(ngram);
            double weight = View.of(ngram).map(tkn -> lfs.contains(tkn) ? 1.0 : 0.0).reduce(0.0, Double::sum);
            dictionary.put(pattern, weight);
          });

      stopwatch.stop();
      System.out.printf("Context captured in %d seconds.\n", stopwatch.elapsed(TimeUnit.SECONDS));
      System.out.printf("Context is : [\n  %s\n]\n", View.of(dictionary.keySet()).join(x -> x, ",\n  "));

      List<Model> models = new ArrayList<>();

      for (String classifier : classifiers) {

        Model model = new Model(label + "/" + classifier);
        model.featurizer_ = new Featurizer(new DictionaryVectorizer(dictionary, dicBuilder));

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

        List<String> texts = View.of(dataset.getKey()).map(GoldLabel::data).toList();
        List<Integer> categories = dataset.getValue();
        List<String> testDataset = new ArrayList<>();
        List<Integer> testCategories = new ArrayList<>();

        ConfusionMatrix confusionMatrix;

        // Split data between train and test then train a classifier
        System.out.printf("Training model for classifier %s...\n", classifier);
        stopwatch.reset().start();

        if (!model.classifier_.supportsIncrementalTraining()) {

          Entry<List<Entry<String, Integer>>, List<Entry<String, Integer>>> entry = split(
              View.of(texts).zip(categories).toList(), trainSizeInPercent);
          Map.Entry<List<String>, List<Integer>> train = View.of(entry.getKey()).unzip(Function.identity());
          Map.Entry<List<String>, List<Integer>> test = View.of(entry.getValue()).unzip(Function.identity());

          model.train(train.getKey(), train.getValue());

          testDataset.addAll(test.getKey());
          testCategories.addAll(test.getValue());
        } else {
          View.of(texts).zip(categories).partition(1000 /* batch size */).forEachRemaining(list -> {

            Entry<List<Entry<String, Integer>>, List<Entry<String, Integer>>> entry = split(list, trainSizeInPercent);
            Map.Entry<List<String>, List<Integer>> train = View.of(entry.getKey()).unzip(Function.identity());
            Map.Entry<List<String>, List<Integer>> test = View.of(entry.getValue()).unzip(Function.identity());

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

        List<FeatureVector> vectors = View.of(texts).map(text -> model.featurizer_.transform(text)).toList();
        model.init(vectors, categories.stream().mapToInt(x -> x).toArray());

        if (Double.isFinite(model.confusionMatrix().matthewsCorrelationCoefficient())) {
          models.add(model);
        }
      }

      if (models.isEmpty()) {
        System.out.println("ERROR: at least one model is needed");
        continue;
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
/*
      View.of(dataset.getKey()).zip(dataset.getValue()).forEachRemaining(entry -> {

        String text = entry.getKey().data();
        int actual = entry.getValue();

        Preconditions.checkState(actual == KO || actual == OK,
            "invalid class: should be either 1 (in class) or 0 (not in class)");

        int prediction = stack.predict(text);

        if (prediction == OK) {
          System.out.println("================================================================================");
          System.out.printf("== %s - %s (actual) vs. %s (prediction)\n", entry.getKey().id(),
              actual == OK ? "OK" : "KO", prediction == OK ? "OK" : "KO");
          Set<String> snippets = stack.snippetBestEffort(text);
          if (snippets.isEmpty()) {
            System.out.println("================================================================================");
          } else {
            snippets.forEach(snippet -> {
              System.out.println("================================================================================");
              System.out.println(snippet);
            });
          }
        }
      });
*/
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
            "java.util.**", "smile.classification.**", "smile.math.**", "smile.base.**", "smile.data.**",
            "smile.neighbor.**"});

    return xStream;
  }

  private static Entry<List<Entry<String, Integer>>, List<Entry<String, Integer>>> split(
      List<Entry<String, Integer>> list, double trainSizeInPercent) {

    Preconditions.checkNotNull(list, "list should not be null");
    Preconditions.checkArgument(0.0 <= trainSizeInPercent && trainSizeInPercent <= 1.0,
        "trainSizeInPercent must be such as 0.0 <= trainSizeInPercent <= 1.0");

    List<Entry<String, Integer>> batchOk = View.of(list).filter(b -> b.getValue() == OK).toList();
    List<Entry<String, Integer>> batchKo = View.of(list).filter(b -> b.getValue() == KO).toList();

    Collections.shuffle(batchOk);
    Collections.shuffle(batchKo);

    int trainSizeOk = (int) (batchOk.size() * trainSizeInPercent);
    int trainSizeKo = (int) (batchKo.size() * trainSizeInPercent);

    List<Entry<String, Integer>> train = View.of(batchOk).take(trainSizeOk).concat(View.of(batchKo).take(trainSizeKo))
        .toList();
    List<Entry<String, Integer>> test = View.of(batchOk).drop(trainSizeOk).concat(View.of(batchKo).drop(trainSizeKo))
        .toList();

    Collections.shuffle(train);
    Collections.shuffle(test);

    return new SimpleImmutableEntry<>(train, test);
  }

  private static List<Map.Entry<String, Double>> findInterestingNGrams(List<String> ok, List<String> ko,
      Vocabulary wholeVocabulary, Function<String, List<String>> tokenizer, int nbCandidatesToConsider,
      int nbLabelsToReturn) {

    Preconditions.checkNotNull(ok, "ok should not be null");
    Preconditions.checkNotNull(ko, "ko should not be null");
    Preconditions.checkNotNull(wholeVocabulary, "wholeVocabulary should not be null");
    Preconditions.checkNotNull(tokenizer, "tokenizer should not be null");

    // Deduplicate datasets
    Set<String> newOk = Sets.newHashSet(ok);
    Set<String> newKo = Sets.difference(Sets.newHashSet(ko), Sets.newHashSet(ok));

    // Provide an implementation of the DocSetLabeler
    AbstractDocSetLabeler docSetLabeler = new AbstractDocSetLabeler() {

      private final Map<String, Multiset<String>> cache_ = new ConcurrentHashMap<>();

      @Override
      protected Multiset<String> candidates(String text) {
        if (!cache_.containsKey(text)) {
          cache_.put(text, HashMultiset.create(View.of(text).map(tokenizer).flatten(View::of).toList()));
        }
        return cache_.get(text);
      }

      @Override
      protected double computeX(String text, String candidate, int count) {
        return (double) count / (double) cache_.get(text).size();
      }

      @Override
      protected double computeY(String text, String candidate, int count) {
        return 1.0 / (1.0 + Math.exp(-1.0 * wholeVocabulary.tfIdf(candidate, count)));
      }
    };
    return docSetLabeler.labels(Lists.newArrayList(newOk), Lists.newArrayList(newKo), nbCandidatesToConsider,
        nbLabelsToReturn);
  }

  @Override
  public String toString() {
    return label_;
  }

  @Override
  public int predict(String text) {
    return predict(featurizer_.transform(text));
  }

  @Override
  public int predict(FeatureVector vector) {
    return classifier_.predict(vector);
  }

  @Override
  public Set<String> snippetBestEffort(String text) {

    Preconditions.checkNotNull(text, "text should not be null");
    Preconditions.checkState(featurizer_ != null, "missing featurizer");
    Preconditions.checkState(classifier_ != null, "missing classifier");

    return featurizer_.snippetBestEffort(text);
  }

  private void train(List<String> texts, List<Integer> categories) {

    Preconditions.checkNotNull(texts, "texts should not be null");
    Preconditions.checkNotNull(categories, "categories should not be null");
    Preconditions.checkArgument(texts.size() == categories.size(),
        "mismatch between the number of texts and the number of categories");
    Preconditions.checkState(featurizer_ != null, "missing featurizer");
    Preconditions.checkState(classifier_ != null, "missing classifier");

    if (!classifier_.isTrained()) {

      FeatureMatrix matrix = new FeatureMatrix(featurizer_.fitAndTransform(texts));
      int[] actuals = categories.stream().mapToInt(x -> x).toArray();

      classifier_.train(matrix, actuals);
      return;
    }

    Preconditions.checkState(classifier_.supportsIncrementalTraining(),
        "classifier does not support incremental training");

    View.of(texts).zip(categories).map(e -> {

      String text = e.getKey();
      int category = e.getValue();
      FeatureVector vector = featurizer_.transform(text);

      return new SimpleImmutableEntry<>(vector, category);
    }).forEachRemaining(e -> classifier_.update(e.getKey(), e.getValue()));
  }

  private ConfusionMatrix test(List<String> texts, List<Integer> categories) {

    Preconditions.checkNotNull(texts, "texts should not be null");
    Preconditions.checkNotNull(categories, "categories should not be null");
    Preconditions.checkArgument(texts.size() == categories.size(),
        "mismatch between the number of texts entries and the number of categories");
    Preconditions.checkState(featurizer_ != null, "missing featurizer");
    Preconditions.checkState(classifier_ != null, "classifier should be trained first");

    ConfusionMatrix confusionMatrix = new ConfusionMatrix();

    View.of(texts).zip(categories).forEachRemaining(e -> {

      String text = e.getKey();
      int actual = e.getValue();

      Preconditions.checkState(actual == KO || actual == OK,
          "invalid class: should be either 1 (in class) or 0 (not in class)");

      FeatureVector vector = featurizer_.transform(text);
      int prediction = classifier_.predict(vector);

      confusionMatrix.add(actual, prediction);
    });
    return confusionMatrix;
  }

  private static class Featurizer {

    private final TextNormalizer normalizer_ = new TextNormalizer(true);
    private final List<DictionaryVectorizer> vectorizers_;
    private final Reducer reducer_ = new Reducer();
    private final RegexVectorizer snippeter_;

    public Featurizer(DictionaryVectorizer vectorizer) {

      Preconditions.checkNotNull(vectorizer, "missing vectorizer");

      vectorizers_ = Lists.newArrayList(vectorizer);

      Pattern regex = Pattern.compile(
          View.of(vectorizer.dicKeys()).map(key -> Pattern.quote(key).replace("_", ".*")).join(x -> x, ")|(", "(", ")"),
          Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

      List<Double> weights = View.iterate(1.0, x -> 1.0).take(vectorizer.dicKeys().size()).toList();

      snippeter_ = new RegexVectorizer(regex, weights);
    }

    public FeatureVector transform(String text) {
      return reducer_.apply(Lists.newArrayList(apply(text))).get(0);
    }

    public List<FeatureVector> fitAndTransform(List<String> texts) {
      return reducer_.apply(View.of(texts).map(this::apply).toList());
    }

    @Beta
    public Set<String> snippetBestEffort(String text) {

      Preconditions.checkState(vectorizers_.size() == 1);

      String newText = normalizer_.apply(Strings.nullToEmpty(text));
      List<Set<Span>> matchedGroups = snippeter_.findGroupMatches(newText);

      return View.of(matchedGroups).flatten(View::of)
          .map(span -> SnippetExtractor.extract(Lists.newArrayList(span.text().trim()), newText, 300, 50, "")).toSet();
    }

    private FeatureVector apply(String text) {

      // Vectorize text
      String newText = normalizer_.apply(Strings.nullToEmpty(text));
      List<FeatureVector> vectors = new ArrayList<>(vectorizers_.size());

      for (int i = 0; i < vectorizers_.size(); i++) {
        vectors.add(vectorizers_.get(i).apply(newText));
      }

      // Concat vectors
      int length = vectors.stream().mapToInt(FeatureVector::length).sum();
      FeatureVector vector = new FeatureVector(length);

      @Var int prevLength = 0;

      for (FeatureVector vect : vectors) {
        int disp = prevLength;
        vect.nonZeroEntries().forEach(i -> vector.set(disp + i, vect.get(i)));
        prevLength += vect.length();
      }
      return vector;
    }
  }

  private static class Reducer implements Function<List<FeatureVector>, List<FeatureVector>> {

    private final VectorsReducer pruneMeaninglessFeatures_ = new VectorsReducer();
    private final VectorsReducer pruneCorrelatedFeatures_ = new VectorsReducer(eCorrelation.KENDALL);

    public Reducer() {
    }

    @Override
    public List<FeatureVector> apply(List<FeatureVector> vectors) {
      return pruneMeaninglessFeatures_.andThen(pruneCorrelatedFeatures_).apply(vectors);
    }
  }
}
