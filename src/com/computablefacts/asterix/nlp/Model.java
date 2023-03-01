package com.computablefacts.asterix.nlp;

import static com.computablefacts.asterix.ml.FeatureVector.findZeroedEntries;
import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.KO;
import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.OK;

import com.computablefacts.asterix.Result;
import com.computablefacts.asterix.View;
import com.computablefacts.asterix.XStream;
import com.computablefacts.asterix.console.AsciiTable;
import com.computablefacts.asterix.console.Observations;
import com.computablefacts.asterix.ml.ConfusionMatrix;
import com.computablefacts.asterix.ml.FeatureMatrix;
import com.computablefacts.asterix.ml.FeatureVector;
import com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier;
import com.computablefacts.asterix.ml.classification.AdaBoostClassifier;
import com.computablefacts.asterix.ml.classification.DecisionTreeClassifier;
import com.computablefacts.asterix.ml.classification.DiscreteNaiveBayesClassifier;
import com.computablefacts.asterix.ml.classification.FisherLinearDiscriminantClassifier;
import com.computablefacts.asterix.ml.classification.GradientBoostedTreesClassifier;
import com.computablefacts.asterix.ml.classification.KNearestNeighborClassifier;
import com.computablefacts.asterix.ml.classification.LogisticRegressionClassifier;
import com.computablefacts.asterix.ml.classification.MultiLayerPerceptronClassifier;
import com.computablefacts.asterix.ml.classification.RandomForestClassifier;
import com.computablefacts.asterix.ml.classification.SvmClassifier;
import com.computablefacts.asterix.ml.stacking.AbstractStack;
import com.computablefacts.asterix.ml.stacking.Stack;
import com.computablefacts.asterix.ml.standardization.StandardScaler;
import com.computablefacts.logfmt.LogFormatter;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CheckReturnValue
final public class Model extends AbstractStack {

  private static final Logger logger_ = LoggerFactory.getLogger(Model.class);
  private static final Function<GoldLabel, Integer> mapToClassId_ = gl -> gl.isTruePositive() || gl.isFalseNegative()
      ? OK : KO;
  private static final Function<GoldLabel, String> mapToClassLabel_ = gl -> gl.isTruePositive() || gl.isFalseNegative()
      ? "OK" : "KO";

  private final String name_;
  private final Vocabulary vocabulary_;
  private final Set<String> stopwords_;
  private final Set<String> includeTags_;
  private final Map<String, Double> keywords_;
  private final Set<String> whitelist_;
  private AbstractBinaryClassifier classifier_;
  private Function<String, View<List<Span>>> tokenizer_;
  private Function<String, View<List<Span>>> tokenizerOnNormalizedText_;
  private Function<View<List<Span>>, FeatureVector> featurizer_;

  public Model(String name, Vocabulary vocabulary, Set<String> stopwords, Set<String> includeTags,
      Map<String, Double> keywords, Set<String> whitelist) {

    Preconditions.checkNotNull(name, "name should not be null");
    Preconditions.checkNotNull(vocabulary, "vocabulary should not be null");
    Preconditions.checkNotNull(stopwords, "stopwords should not be null");
    Preconditions.checkNotNull(includeTags, "includeTags should not be null");
    Preconditions.checkNotNull(keywords, "keywords should not be null");

    name_ = name;
    vocabulary_ = vocabulary;
    stopwords_ = ImmutableSet.copyOf(stopwords);
    includeTags_ = ImmutableSet.copyOf(includeTags);
    keywords_ = ImmutableMap.copyOf(keywords);
    whitelist_ = whitelist == null ? null : ImmutableSet.copyOf(whitelist);
  }

  /**
   * Train a new binary classifier.
   *
   * <ul>
   * <li>{@code args[0]} the vocabulary as a gzipped TSV file.</li>
   * <li>{@code args[1]} the gold labels as a gzipped JSONL file.</li>
   * <li>{@code args[2]} the keywords extracted by the {@link DocSetLabeler} as a gzipped TSV file.</li>
   * <li>{@code args[3]} the types of tokens to keep: WORD, PUNCTUATION, etc. (optional, default is {WORD, NUMBER, TERMINAL_MARK})</li>
   * <li>{@code args[4]} a list of classifiers to train (optional, default is DecisionTree)</li>
   * <li>{@code args[5]} a list of labels to train for (optional, default is all)</li>
   * </ul>
   */
  public static void main(String[] args) throws IOException {

    File fileVocabulary = new File(args[0]);
    File fileGoldlabels = new File(args[1]);
    File fileKeywords = new File(args[2]);
    Set<String> includeTags = args.length < 4 ? Sets.newHashSet("WORD", "NUMBER", "TERMINAL_MARK")
        : Sets.newHashSet(Splitter.on(',').trimResults().omitEmptyStrings().split(args[3]));
    Set<eBinaryClassifier> classifiers = args.length < 5 ? Sets.newHashSet(eBinaryClassifier.DT)
        : Splitter.on(',').trimResults().omitEmptyStrings().splitToStream(args[4]).map(eBinaryClassifier::valueOf)
            .collect(Collectors.toSet());
    Set<String> labels = args.length < 6 ? GoldLabel.load(fileGoldlabels).map(GoldLabel::label).toSet()
        : Sets.newHashSet(Splitter.on(',').trimResults().omitEmptyStrings().splitToList(args[5]));

    Preconditions.checkArgument(fileVocabulary.exists(), "missing vocabulary");
    Preconditions.checkArgument(fileGoldlabels.exists(), "missing gold labels");
    Preconditions.checkArgument(fileKeywords.exists(), "missing keywords");

    Vocabulary vocabulary = new Vocabulary();
    vocabulary.load(fileVocabulary);

    Set<String> stopwords = Sets.newHashSet(vocabulary.stopwords(50));
    File fileObservations = new File(String.format("%s/observations.txt", fileKeywords.getParent()));

    try (Observations observations = new Observations(fileObservations)) {
      for (String label : labels) {

        File fileStack = new File(String.format("%s/stack-%s.xml.gz", fileKeywords.getParent(), label));

        if (fileStack.exists()) {
          continue;
        }

        Stopwatch stopwatch = Stopwatch.createStarted();

        // Build both a train and a test dataset from a set of gold labels
        Map.Entry<List<GoldLabel>, List<GoldLabel>> trainTest = GoldLabel.split(
            GoldLabel.load(fileGoldlabels, label).toList());
        List<GoldLabel> train = trainTest.getKey();
        List<GoldLabel> test = trainTest.getValue();

        if (train.stream().noneMatch(gl -> gl.isTruePositive() || gl.isFalseNegative())) {
          continue; // The train dataset must have both classes in it
        }
        if (test.stream().noneMatch(gl -> gl.isTruePositive() || gl.isFalseNegative())) {
          continue; // The test dataset must have both classes in it
        }

        Map<String, Double> keywords = new HashMap<>();
        DocSetLabeler.load(fileKeywords, label)
            .forEachRemaining(entry -> keywords.put(entry.getKey(), entry.getValue()));

        @Var List<FeatureVector> trainVectors = vectors(vocabulary, stopwords, includeTags, keywords.keySet(), null,
            View.of(train).concat(test).toList());
        Set<String> whitelist = whitelist(vocabulary, trainVectors);

        trainVectors = vectors(vocabulary, stopwords, includeTags, keywords.keySet(), whitelist, train);
        List<Integer> trainClasses = classes(train);

        List<FeatureVector> testVectors = vectors(vocabulary, stopwords, includeTags, keywords.keySet(), whitelist,
            test);
        List<Integer> testClasses = classes(test);

        @Var int idx = 1;
        List<Model> models = new ArrayList<>();
        String[][] metrics = new String[1 /* header */ + classifiers.size()][5];
        metrics[0][0] = "Classifier";
        metrics[0][1] = "Metric";
        metrics[0][2] = "Train";
        metrics[0][3] = "Test";
        metrics[0][4] = "Model";

        for (eBinaryClassifier classifier : classifiers) {
          try {

            // Train/test model
            Model model = new Model(label, vocabulary, stopwords, includeTags, keywords, whitelist);
            ConfusionMatrix confusionMatrixTrain = model.train(trainVectors, trainClasses, classifier);
            ConfusionMatrix confusionMatrixTest = model.test(testVectors, testClasses);

            model.init(View.of(trainVectors).concat(testVectors).toList(),
                View.of(trainClasses).concat(testClasses).toList().stream().mapToInt(x -> x).toArray());

            // Models without metrics should be discarded
            if (Double.isFinite(model.confusionMatrix().matthewsCorrelationCoefficient())) {
              model.tokenizer_ = null;
              model.tokenizerOnNormalizedText_ = null;
              model.featurizer_ = null;
              models.add(model);
            }

            // Format metrics
            DecimalFormat df = new DecimalFormat("#.#####");
            metrics[idx][0] = classifier.toString();
            metrics[idx][1] = "MCC";
            metrics[idx][2] = df.format(confusionMatrixTrain.matthewsCorrelationCoefficient());
            metrics[idx][3] = df.format(confusionMatrixTest.matthewsCorrelationCoefficient());
            metrics[idx++][4] = df.format(model.confusionMatrix().matthewsCorrelationCoefficient());

          } catch (Exception e) {

            logger_.error(LogFormatter.create().message(e).formatError());

            // Format metrics
            metrics[idx][0] = classifier.toString();
            metrics[idx][1] = "MCC";
            metrics[idx][2] = "";
            metrics[idx][3] = "";
            metrics[idx++][4] = "";
          }
        }

        // Display metrics
        observations.add(String.format(
            "============================================================\n== %s\n============================================================\n%s",
            label, AsciiTable.format(metrics, true)));

        // Try to improve the output by stacking the models
        Stack stack = new Stack(models);
        stack.compactify();

        stopwatch.stop();
        observations.add("Stack is " + stack);
        observations.add(stack.confusionMatrix().toString());
        observations.add(String.format("Stack built in %d seconds.", stopwatch.elapsed(TimeUnit.SECONDS)));

        models.clear();
        keywords.clear();
        trainVectors.clear();
        trainClasses.clear();
        testVectors.clear();
        testClasses.clear();
        whitelist.clear();
        train.clear();
        test.clear();

        XStream.save(fileStack, stack);

        // Extract focus points
        // GoldLabel.load(fileGoldlabels, label).forEachRemaining(gl -> {
        //   if (stack.predict(gl.data()) == OK) {
        //     Result<String> focus = stack.focus(gl.data());
        //     observations.add(
        //         String.format("===[ FOCUS / ACTUAL IS %s / PREDICTED IS OK ]===", mapToClassLabel_.apply(gl)));
        //     observations.add(focus.get("<empty>"));
        //   }
        // });
      }
    }
  }

  private static Set<String> whitelist(Vocabulary vocabulary, List<FeatureVector> vectors) {

    Preconditions.checkNotNull(vocabulary, "vocabulary should not be null");
    Preconditions.checkNotNull(vectors, "vectors should not be null");

    Set<Integer> zeroed = findZeroedEntries(vectors);
    Set<Map.Entry<Integer, Integer>> correlated = Sets.newHashSet(); // findCorrelatedEntries(vectors, eCorrelation.KENDALL, 0.85, 50);

    // A correlated with B and B correlated with C does not imply A correlated with C
    Set<Integer> dropped = Sets.union(zeroed, Sets.difference(View.of(correlated).map(Map.Entry::getValue).toSet(),
        View.of(correlated).map(Map.Entry::getKey).toSet()));

    return View.of(Sets.difference(View.range(1, vocabulary.size()).toSet(), dropped))
        .map(idx -> vocabulary.term(idx + 1)).toSet();
  }

  private static List<FeatureVector> vectors(Vocabulary vocabulary, Set<String> stopwords, Set<String> includeTags,
      Set<String> keywords, Set<String> whitelist, List<GoldLabel> goldLabels) {

    Preconditions.checkNotNull(goldLabels, "goldLabels should not be null");

    return View.of(goldLabels).map(GoldLabel::data)
        .map(tokenize(vocabulary, stopwords, includeTags, keywords).andThen(featurize(vocabulary, whitelist))).toList();
  }

  private static List<Integer> classes(List<GoldLabel> goldLabels) {

    Preconditions.checkNotNull(goldLabels, "goldLabels should not be null");

    return View.of(goldLabels).map(mapToClassId_).toList();
  }

  private static Function<String, View<List<Span>>> tokenize(Vocabulary vocabulary, Set<String> stopwords,
      Set<String> includeTags, Set<String> keywords) {

    Preconditions.checkState(vocabulary != null, "vocabulary should not be null");
    Preconditions.checkState(stopwords != null, "stopwords should not be null");
    Preconditions.checkState(includeTags != null, "includeTags should not be null");
    Preconditions.checkState(keywords != null, "keywords should not be null");

    return txt -> Vocabulary.tokenizer(includeTags, 9).apply(Strings.nullToEmpty(txt))
        .filter(tkn -> vocabulary.index(tkn.text()) != 0 /* UNK */ && !stopwords.contains(tkn.text()))
        .overlappingWindowWithStrictLength(3)
        .filter(tkns -> tkns.stream().anyMatch(tkn -> keywords.contains(tkn.text())));
  }

  @Beta
  private static Function<String, View<List<Span>>> tokenizeOnNormalizedText(Vocabulary vocabulary,
      Set<String> stopwords, Set<String> includeTags, Set<String> keywords) {

    Preconditions.checkState(vocabulary != null, "vocabulary should not be null");
    Preconditions.checkState(stopwords != null, "stopwords should not be null");
    Preconditions.checkState(includeTags != null, "includeTags should not be null");
    Preconditions.checkState(keywords != null, "keywords should not be null");

    return txt -> Vocabulary.tokenizerOnNormalizedText(includeTags, 9).apply(Strings.nullToEmpty(txt))
        .filter(tkn -> vocabulary.index(tkn.text()) != 0 /* UNK */ && !stopwords.contains(tkn.text()))
        .overlappingWindowWithStrictLength(3)
        .filter(tkns -> tkns.stream().anyMatch(tkn -> keywords.contains(tkn.text())));
  }

  private static Function<View<List<Span>>, FeatureVector> featurize(Vocabulary vocabulary, Set<String> whitelist) {

    Preconditions.checkState(vocabulary != null, "vocabulary should not be null");

    TfIdfVectorizer vectorizer = new TfIdfVectorizer(vocabulary, whitelist);
    return tkns -> vectorizer.apply(tkns.flatten(View::of).map(Span::text).toList());
  }

  private static AbstractBinaryClassifier classifier(eBinaryClassifier classifier) {
    switch (classifier) {
      case DNB:
        return new DiscreteNaiveBayesClassifier();
      case FLD:
        return new FisherLinearDiscriminantClassifier();
      case KNN:
        return new KNearestNeighborClassifier(new StandardScaler());
      case MLP:
        return new MultiLayerPerceptronClassifier(new StandardScaler());
      case SVM:
        return new SvmClassifier(new StandardScaler());
      case RF:
        return new RandomForestClassifier();
      case DT:
        return new DecisionTreeClassifier();
      case GBT:
        return new GradientBoostedTreesClassifier();
      case AB:
        return new AdaBoostClassifier();
      default:
        return new LogisticRegressionClassifier(new StandardScaler());
    }
  }

  @Override
  public String toString() {
    return classifier_.type();
  }

  @Override
  public int predict(String txt) {
    return predict(tokenizer().andThen(featurizer()).apply(Strings.nullToEmpty(txt)));
  }

  @Beta
  @Override
  public int predictOnNormalizedText(String txt) {
    return predict(tokenizerOnNormalizedText().andThen(featurizer()).apply(Strings.nullToEmpty(txt)));
  }

  @Override
  public int predict(FeatureVector vector) {

    Preconditions.checkNotNull(vector, "vector should not be null");
    Preconditions.checkState(classifier_ != null, "classifier_ should not be null");

    return classifier_.predict(vector);
  }

  @Override
  public Result<String> focus(String txt) {

    Set<Map.Entry<Span, Integer>> spans = mergeSpans(tokenizer().apply(Strings.nullToEmpty(txt))
        .map(tkns -> new Span(tkns.get(0).rawText(), tkns.get(0).begin(), tkns.get(tkns.size() - 1).end())).toSet());

    if (spans.isEmpty()) {
      return Result.empty();
    }

    List<Map.Entry<Span, Integer>> spansSorted = View.of(spans).toSortedList(
        Comparator.comparingInt((Map.Entry<Span, Integer> e) -> e.getValue()).reversed()
            .thenComparing(Comparator.comparingInt((Map.Entry<Span, Integer> e) -> e.getKey().length()).reversed()));

    Map.Entry<Span, Integer> best = spansSorted.get(0);
    return Result.of(best.getKey().text());
  }

  public String name() {
    return name_;
  }

  public ConfusionMatrix train(List<FeatureVector> vectors, List<Integer> classes,
      Model.eBinaryClassifier typeClassifier) {

    Preconditions.checkNotNull(vectors, "vectors should not be null");
    Preconditions.checkNotNull(classes, "classes should not be null");
    Preconditions.checkNotNull(typeClassifier, "typeClassifier should not be null");

    classifier_ = classifier(typeClassifier);
    classifier_.train(new FeatureMatrix(vectors), classes.stream().mapToInt(x -> x).toArray());

    return test(vectors, classes);
  }

  public ConfusionMatrix test(List<FeatureVector> vectors, List<Integer> classes) {

    Preconditions.checkNotNull(vectors, "vectors should not be null");
    Preconditions.checkNotNull(classes, "classes should not be null");

    ConfusionMatrix confusionMatrix = new ConfusionMatrix();
    View.of(vectors).map(this::predict).index()
        .forEachRemaining(e -> confusionMatrix.add(classes.get(e.getKey() - 1), e.getValue()));

    return confusionMatrix;
  }

  private Function<String, View<List<Span>>> tokenizer() {
    if (tokenizer_ == null) {

      Preconditions.checkState(vocabulary_ != null, "vocabulary should not be null");
      Preconditions.checkState(stopwords_ != null, "stopwords should not be null");
      Preconditions.checkState(includeTags_ != null, "includeTags should not be null");
      Preconditions.checkState(keywords_ != null, "keywords should not be null");

      tokenizer_ = tokenize(vocabulary_, stopwords_, includeTags_, keywords_.keySet());
    }
    return tokenizer_;
  }

  @Beta
  private Function<String, View<List<Span>>> tokenizerOnNormalizedText() {
    if (tokenizerOnNormalizedText_ == null) {

      Preconditions.checkState(vocabulary_ != null, "vocabulary should not be null");
      Preconditions.checkState(stopwords_ != null, "stopwords should not be null");
      Preconditions.checkState(includeTags_ != null, "includeTags should not be null");
      Preconditions.checkState(keywords_ != null, "keywords should not be null");

      tokenizerOnNormalizedText_ = tokenizeOnNormalizedText(vocabulary_, stopwords_, includeTags_, keywords_.keySet());
    }
    return tokenizerOnNormalizedText_;
  }

  private Function<View<List<Span>>, FeatureVector> featurizer() {
    if (featurizer_ == null) {

      Preconditions.checkState(vocabulary_ != null, "vocabulary should not be null");
      Preconditions.checkState(whitelist_ != null, "whitelist_ should not be null");

      featurizer_ = featurize(vocabulary_, whitelist_);
    }
    return featurizer_;
  }

  private Set<Map.Entry<Span, Integer>> mergeSpans(Set<Span> spanz) {

    Preconditions.checkNotNull(spanz, "spanz should not be null");

    Set<Map.Entry<Span, Integer>> spans = new HashSet<>();

    for (Span span : spanz) {
      spans.add(mergeSpans(Sets.difference(spanz, Sets.newHashSet(span)), span, 1));
    }
    return spans;
  }

  private Map.Entry<Span, Integer> mergeSpans(Set<Span> spans, Span span, int depth) {

    Preconditions.checkNotNull(spans, "spans should not be null");
    Preconditions.checkArgument(depth >= 0, "depth must be >= 0");

    for (Span s : spans) {
      if (s.overlapsAll(span)) {
        return mergeSpans(Sets.difference(spans, Sets.newHashSet(span)), s, depth + 1);
      }
      if (span.overlapsAll(s)) {
        return mergeSpans(Sets.difference(spans, Sets.newHashSet(s)), span, depth + 1);
      }
      if (s.overlapsLeftOf(span)) {
        return mergeSpans(Sets.difference(spans, Sets.newHashSet(span, s)),
            new Span(span.rawText(), s.begin(), span.end()), depth + 1);
      }
      if (span.overlapsLeftOf(s)) {
        return mergeSpans(Sets.difference(spans, Sets.newHashSet(span, s)),
            new Span(span.rawText(), span.begin(), s.end()), depth + 1);
      }
      if (s.end() < span.begin() && (span.begin() - s.end()) <= 25) {
        return mergeSpans(Sets.difference(spans, Sets.newHashSet(s)), new Span(span.rawText(), s.begin(), span.end()),
            depth + 1);
      }
      if (span.end() < s.begin() && (s.begin() - span.end()) <= 25) {
        return mergeSpans(Sets.difference(spans, Sets.newHashSet(s)), new Span(span.rawText(), span.begin(), s.end()),
            depth + 1);
      }
    }
    return new SimpleImmutableEntry<>(span, depth);
  }

  public enum eBinaryClassifier {
    DNB, FLD, KNN, MLP, SVM, RF, DT, GBT, AB, LOGIT
  }
}