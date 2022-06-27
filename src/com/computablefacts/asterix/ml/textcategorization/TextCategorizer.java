package com.computablefacts.asterix.ml.textcategorization;

import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.KO;
import static com.computablefacts.asterix.ml.classification.AbstractBinaryClassifier.OK;

import com.computablefacts.asterix.Generated;
import com.computablefacts.asterix.ml.ConfusionMatrix;
import com.computablefacts.asterix.ml.GoldLabel;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
   * <li>{@code args[0]} the corpus of documents as a gzipped JSONL file.</li>
   * <li>{@code args[1]} the category to load.</li>
   * </ul>
   */
  @Beta
  @Generated
  public static void main(String[] args) {

    File file = new File(args[0]);
    String label = args.length == 2 ? args[1] : null;
    Map<String, Fingerprint> categories = new HashMap<>();

    GoldLabel.load(file, label).filter(gl -> gl.isTruePositive() || gl.isFalseNegative())
        .forEachRemaining(gl -> {

          String lbl = gl.label();

          if (!categories.containsKey(lbl)) {
            Fingerprint fingerprint = new Fingerprint();
            fingerprint.category(lbl);
            categories.put(lbl, fingerprint);
          }
          categories.get(lbl).add(gl.data());
        });

    TextCategorizer categorizer = new TextCategorizer();
    categories.values().forEach(categorizer::add);

    ConfusionMatrix confusionMatrix = new ConfusionMatrix();

    GoldLabel.load(file, label).forEachRemaining(gl -> {

      String actual = gl.label();
      int act = gl.isTruePositive() || gl.isFalseNegative() ? OK : KO;

      String prediction = categorizer.categorize(gl.data());
      int pred =
          (act == OK && prediction.equals(actual)) || (act == KO && !prediction.equals(actual)) ? OK
              : KO;

      confusionMatrix.add(act, pred);
    });

    System.out.println(confusionMatrix);
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
