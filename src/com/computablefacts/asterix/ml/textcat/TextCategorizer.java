package com.computablefacts.asterix.ml.textcat;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.ArrayList;
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
