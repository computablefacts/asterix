package com.computablefacts.asterix.ml.textcategorization;

import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.KO;
import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.OK;

import com.computablefacts.asterix.Generated;
import com.computablefacts.asterix.View;
import com.computablefacts.asterix.ml.ConfusionMatrix;
import com.computablefacts.asterix.ml.GoldLabel;
import com.computablefacts.asterix.ml.Model;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.errorprone.annotations.CheckReturnValue;
import java.io.File;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * {@link TextCategorizer} is able to categorize texts by computing the similarity of the {@link Fingerprint} of a text
 * with a collection of the fingerprints of the categories.
 */
@CheckReturnValue
final public class TextCategorizer {

  private final List<Fingerprint> fingerprints_ = new ArrayList<>();

  public TextCategorizer() {
  }

  public static TextCategorizer trainTextCategorizer(List<String> texts, List<Integer> categories) {

    Preconditions.checkNotNull(texts, "texts should not be null");
    Preconditions.checkNotNull(categories, "categories should not be null");
    Preconditions.checkArgument(texts.size() == categories.size(),
        "mismatch between the number of texts and the number of categories");

    Fingerprint ok = new Fingerprint();
    ok.category("OK");

    Fingerprint ko = new Fingerprint();
    ko.category("KO");

    TextCategorizer categorizer = new TextCategorizer();
    categorizer.add(ok);
    categorizer.add(ko);

    View.of(texts).zip(categories).displayProgress(texts.size())
        .peek(entry -> entry.getValue() == OK, entry -> ok.add(entry.getKey()), entry -> ko.add(entry.getKey()))
        .forEachRemaining(entry -> Preconditions.checkState(entry.getValue() == KO || entry.getValue() == OK,
            "invalid prediction: should be either 1 (in class) or 0 (not in class)"));
    return categorizer;
  }

  /**
   * Build a {@link TextCategorizer} from a set of gold labels.
   * <ul>
   * <li>{@code args[0]} the gold labels as a gzipped JSONL file.</li>
   * <li>{@code args[1]} the list of labels to consider.</li>
   * </ul>
   */
  @Beta
  @Generated
  public static void main(String[] args) {

    File file = new File(args[0]);
    List<String> labels = Splitter.on(',').trimResults().splitToList(args.length < 2 ? "" : args[1]);

    Preconditions.checkArgument(file.exists(), "missing gold labels: %s", file);

    System.out.printf("Gold labels dataset is %s\n", file);

    for (String label : labels) {

      System.out.println("================================================================================");
      System.out.printf("== Label is %s\n", label);
      System.out.println("================================================================================");
      System.out.println("Assembling dataset...");
      Stopwatch stopwatch = Stopwatch.createStarted();

      Map.Entry<List<String>, List<Integer>> dataset = GoldLabel.load(file, label).displayProgress(5000)
          .unzip(gl -> new SimpleImmutableEntry<>(gl.data(), gl.isTruePositive() || gl.isFalseNegative() ? OK : KO));

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
      System.out.println("Training text categorizer...");
      stopwatch.reset().start();

      TextCategorizer categorizer = trainTextCategorizer(dataset.getKey(), dataset.getValue());

      stopwatch.stop();
      System.out.printf("Text categorizer trained in %d seconds.\n", stopwatch.elapsed(TimeUnit.SECONDS));
      System.out.println("Testing text categorizer...");
      stopwatch.reset().start();

      ConfusionMatrix confusionMatrix = new ConfusionMatrix();

      View.of(dataset.getKey()).zip(dataset.getValue()).forEachRemaining(entry -> {

        String category = categorizer.categorize(entry.getKey());
        int actual = entry.getValue();
        int prediction = "OK".equals(category) ? OK : KO;

        confusionMatrix.add(actual, prediction);
      });

      stopwatch.stop();
      System.out.println(confusionMatrix);
      System.out.printf("Text categorizer tested in %d seconds.\n", stopwatch.elapsed(TimeUnit.SECONDS));
      System.out.println("Saving text categorizer...");

      Model.save(new File(String.format("%stext-categorizer-%s.xml.gz", file.getParent() + File.separator, label)),
          categorizer);

      System.out.println("Text categorizer saved.");
    }
  }

  public void add(Fingerprint fp) {

    Preconditions.checkNotNull(fp, "fp should not be null");

    fingerprints_.add(fp);
  }

  public String categorize(String text) {
    return categorize(text, 1.03, 5);
  }

  public String categorize(String text, double threshold, int maxCandidates) {

    Preconditions.checkNotNull(text, "text should not be null");
    Preconditions.checkArgument(threshold >= 1.0, "threshold should be >= 1.0");
    Preconditions.checkArgument(maxCandidates >= 1, "maxCandidates should be >= 1");

    if (text.length() < 10) {
      return "<UNK>";
    }

    Fingerprint fp = new Fingerprint();
    fp.add(text);

    Map<String, Integer> distances = fp.categorize(fingerprints_);
    int minDistance = distances.values().stream().mapToInt(i -> i).min().orElse(0);
    double newThreshold = minDistance * threshold;
    int nbCandidates = distances.entrySet().stream().filter(e -> e.getValue() <= newThreshold).mapToInt(e -> 1).sum();

    return nbCandidates > maxCandidates ? "<UNK>" : fp.category();
  }
}
