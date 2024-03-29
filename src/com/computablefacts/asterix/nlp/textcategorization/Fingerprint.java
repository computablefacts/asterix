package com.computablefacts.asterix.nlp.textcategorization;

import com.computablefacts.asterix.View;
import com.computablefacts.asterix.nlp.Span;
import com.computablefacts.asterix.nlp.SpanSequence;
import com.computablefacts.asterix.nlp.TextTokenizer;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.io.File;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.TreeSet;

/**
 * A {@link  Fingerprint} maps so called NGrams to their number of occurrences in the corresponding text.
 */
@CheckReturnValue
final public class Fingerprint {

  // The number of occurrences of each ngram
  private final Multiset<String> ngrams_ = HashMultiset.create();
  // NGrams sorted by the number of occurrences in the text which was used for creating the Fingerprint
  private final NavigableSet<Multiset.Entry<String>> entries_ = new TreeSet<>(new NGramEntryComparator());
  private String category_ = "<UNK>";

  public Fingerprint() {
  }

  public Fingerprint(Fingerprint fp) {

    Preconditions.checkNotNull(fp, "fp should not be null");

    ngrams_.addAll(fp.ngrams_);
    entries_.addAll(fp.entries_);
    category_ = fp.category_;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Fingerprint other = (Fingerprint) obj;
    return Objects.equals(category_, other.category_) && Objects.equals(ngrams_, other.ngrams_) && Objects.equals(
        entries_, other.entries_);
  }

  @Override
  public int hashCode() {
    return Objects.hash(category_, ngrams_, entries_);
  }

  @Override
  public String toString() {

    StringBuilder builder = new StringBuilder();

    for (Multiset.Entry<String> ngram : entries_) {
      builder.append(ngram.getElement()).append("\t").append(ngram.getCount()).append("\n");
    }
    return builder.toString();
  }

  public void save(File file) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(!file.exists(), "file already exists : %s", file);

    View.of(entries_).map(ngram -> ngram.getElement() + "\t" + ngram.getCount()).toFile(file, false, true);
  }

  public void load(File file) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file does not exist : %s", file);

    ngrams_.clear();
    entries_.clear();
    View.of(file, true).forEachRemaining(row -> {

      int index = row.lastIndexOf('\t');
      String ngram = row.substring(0, index);
      int count = Integer.parseInt(row.substring(index + 1), 10);

      ngrams_.add(ngram, count);
    });

    entries_.addAll(ngrams_.entrySet());
  }

  public String category() {
    return category_;
  }

  public void category(String category) {
    category_ = category;
  }

  /**
   * Creates a {@link Fingerprint} object from the given input text.
   *
   * <strong>BE WARNED THAT</strong> good results are obtained by passing to this method a full
   * text, together with numbers, punctuation and other text characters. So, if you have - say - HTML, just throw away
   * tags, but leave the rest if you want to obtain precise results: punctuation comes in very handy at determining the
   * language. At some extent, also upper/lower case letters could help.
   *
   * @param text the text upon which the fingerprint should be built.
   */
  public void add(String text) {

    Preconditions.checkNotNull(text, "text should not be null");

    int minLength = 1;
    int maxLength = 5;
    SpanSequence spans = new TextTokenizer().apply(text);

    for (int length = minLength; length <= maxLength; ++length) {
      for (Span span : spans) {

        String token = "_" + span.text() + "_";

        for (int i = 0; i < (token.length() - length + 1); i++) {
          String ngram = token.substring(i, i + length);
          ngrams_.add(ngram);
        }
      }
    }

    if (ngrams_.contains("_")) {
      int blanksScore = ngrams_.count("_");
      ngrams_.setCount("_", blanksScore / 2);
    }

    entries_.clear();
    entries_.addAll(ngrams_.entrySet());
  }

  /**
   * Computes the distance between the current fingerprint and a given fingerprint.
   *
   * @param fp a fingerprint.
   * @return the distance between the two fingerprints.
   */
  public int distanceTo(Fingerprint fp) {

    Preconditions.checkNotNull(fp, "fp should not be null");

    @Var int distance = 0;
    @Var int count = 0;

    for (Multiset.Entry<String> entry : entries_) {

      String ngram = entry.getElement();
      count++;

      if (count > 400) {
        break;
      }
      if (!fp.ngrams_.contains(ngram)) {
        distance += fp.ngrams_.size();
      } else {
        distance += Math.abs(position(ngram) - fp.position(ngram));
      }
    }
    return distance;
  }

  /**
   * Find out the most likely categories, if any, by comparing the distance from each of the categories.
   *
   * @param categories the list of possible categories.
   * @return the most likely categories.
   */
  public Map<String, Integer> categorize(Collection<Fingerprint> categories) {

    Preconditions.checkNotNull(categories, "categories should not be null");

    @Var int minDistance = Integer.MAX_VALUE;
    Map<String, Integer> distances = new HashMap<>(); // (category, distance to the current fingerprint)

    for (Fingerprint fp : categories) {

      int distance = distanceTo(fp);
      distances.put(fp.category(), distance);

      if (distance < minDistance) {
        minDistance = distance;
        category_ = fp.category();
      }
    }
    return distances;
  }

  private int position(String ngram) {

    Preconditions.checkNotNull(ngram, "ngram should not be null");

    @Var int pos = 1;
    @Var int value = entries_.first().getCount();

    for (Multiset.Entry<String> entry : entries_) {
      if (value != entry.getCount()) {
        value = entry.getCount();
        pos++;
      }
      if (entry.getElement().equals(ngram)) {
        return pos;
      }
    }
    return -1;
  }

  private final static class NGramEntryComparator implements Comparator<Multiset.Entry<String>> {

    public NGramEntryComparator() {
    }

    public int compare(Multiset.Entry<String> e1, Multiset.Entry<String> e2) {
      if (e2.getCount() - e1.getCount() == 0) {
        return (e1.getElement()).length() - (e2.getElement()).length() == 0 ? (e1.getElement()).compareTo(
            e2.getElement()) : (e1.getElement()).length() - (e2.getElement()).length();
      }
      return e2.getCount() - e1.getCount();
    }
  }
}