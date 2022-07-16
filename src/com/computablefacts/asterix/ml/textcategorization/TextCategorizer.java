package com.computablefacts.asterix.ml.textcategorization;

import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.KO;
import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.OK;

import com.computablefacts.asterix.Generated;
import com.computablefacts.asterix.View;
import com.computablefacts.asterix.ml.ConfusionMatrix;
import com.computablefacts.asterix.ml.GoldLabel;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.errorprone.annotations.CheckReturnValue;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * {@link TextCategorizer} is able to categorize texts by computing the similarity of the
 * {@link Fingerprint} of a text with a collection of the fingerprints of the categories.
 */
@CheckReturnValue
final public class TextCategorizer {

  private final List<Fingerprint> fingerprints_ = new ArrayList<>();

  public TextCategorizer() {
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
    List<String> labels = Splitter.on(',').trimResults()
        .splitToList(args.length < 2 ? "" : args[1]);

    Preconditions.checkArgument(file.exists(), "missing gold labels: %s", file);

    System.out.printf("Gold labels dataset is %s\n", file);

    for (String label : labels) {

      System.out.println(
          "================================================================================");
      System.out.printf("== Label is %s\n", label);
      System.out.println(
          "================================================================================");
      System.out.println("Creating fingerprints...");

      Stopwatch stopwatch = Stopwatch.createStarted();
      Map<String, Fingerprint> categories = new HashMap<>();

      GoldLabel.load(file, label).displayProgress(5000)
          .filter(gl -> gl.isTruePositive() || gl.isFalseNegative()).forEachRemaining(gl -> {

            String lbl = gl.label();

            if (!categories.containsKey(lbl)) {

              Fingerprint fingerprint = new Fingerprint();
              fingerprint.category(lbl);

              categories.put(lbl, fingerprint);
            }
            categories.get(lbl).add(gl.data());
          });

      stopwatch.stop();

      System.out.printf("Fingerprints created in %d seconds for %d categories.\n",
          stopwatch.elapsed(TimeUnit.SECONDS), categories.size());
      System.out.println("Initializing text categorizer...");

      TextCategorizer categorizer = new TextCategorizer();
      categories.values().forEach(categorizer::add);

      System.out.println("Text categorizer initialized.");

      ConfusionMatrix confusionMatrix = new ConfusionMatrix();

      View<GoldLabel> sample = View.of(
          GoldLabel.load(file, null).filter(gl -> !label.equals(gl.label())).sample(5000));

      GoldLabel.load(file, label).concat(sample).displayProgress(5000).forEachRemaining(gl -> {

        String actualLabel = gl.label();
        int actual = gl.isTruePositive() || gl.isFalseNegative() ? OK : KO;

        String predictedLabel = categorizer.categorize(gl.data());
        int prediction = (actual == OK && predictedLabel.equals(actualLabel)) || (actual == KO
            && !predictedLabel.equals(actualLabel)) ? OK : KO;

        confusionMatrix.add(actual, prediction);
      });

      System.out.println(confusionMatrix);
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
    int nbCandidates = distances.entrySet().stream().filter(e -> e.getValue() <= newThreshold)
        .mapToInt(e -> 1).sum();

    return nbCandidates > maxCandidates ? "<UNK>" : fp.category();
  }
}
