package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.View;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implements a modified version of the DocSetLabeler algorithm from "Topic Similarity Networks: Visual Analytics for
 * Large Document Sets" by Arun S. Maiya and Robert M. Rolfe.
 */
@CheckReturnValue
public abstract class AbstractDocSetLabeler {

  protected AbstractDocSetLabeler() {
  }

  @Beta
  public static List<Map.Entry<String, Double>> findInterestingNGrams(Vocabulary unigrams, Vocabulary bigrams,
      Vocabulary trigrams, Vocabulary quadgrams, Vocabulary quintgrams, Vocabulary sextgrams, List<String> ok,
      List<String> ko) {

    Preconditions.checkNotNull(ok, "ok should not be null");
    Preconditions.checkNotNull(ko, "ko should not be null");

    Set<String> includeTags = Sets.newHashSet("WORD", "NUMBER", "TERMINAL_MARK"); // TODO : expose as parameter
    Function<String, List<String>> getUnigrams = unigrams == null ? null : Vocabulary.tokenizer(includeTags, 1);
    Function<String, List<String>> getBigrams = bigrams == null ? null : Vocabulary.tokenizer(includeTags, 2);
    Function<String, List<String>> getTrigrams = trigrams == null ? null : Vocabulary.tokenizer(includeTags, 3);
    Function<String, List<String>> getQuadgrams = quadgrams == null ? null : Vocabulary.tokenizer(includeTags, 4);
    Function<String, List<String>> getQuintgrams = quintgrams == null ? null : Vocabulary.tokenizer(includeTags, 5);
    Function<String, List<String>> getSextgrams = sextgrams == null ? null : Vocabulary.tokenizer(includeTags, 6);

    // Deduplicate datasets
    Set<String> newOk = Sets.newHashSet(ok);
    Set<String> newKo = Sets.difference(Sets.newHashSet(ko), Sets.newHashSet(ok));

    // Build vocabulary for the ok and ko datasets
    Vocabulary unigramsOk = unigrams == null ? null
        : Vocabulary.of(View.of(newOk).map(getUnigrams).displayProgress(10_000), 0.01, 0.99, 100_000);
    Vocabulary bigramsOk = bigrams == null ? null
        : Vocabulary.of(View.of(newOk).map(getBigrams).displayProgress(10_000), 0.01, 0.99, 100_000);
    Vocabulary trigramsOk = trigrams == null ? null
        : Vocabulary.of(View.of(newOk).map(getTrigrams).displayProgress(10_000), 0.01, 0.99, 100_000);
    Vocabulary quadgramsOk = quadgrams == null ? null
        : Vocabulary.of(View.of(newOk).map(getQuadgrams).displayProgress(10_000), 0.01, 0.99, 100_000);
    Vocabulary quintgramsOk = quintgrams == null ? null
        : Vocabulary.of(View.of(newOk).map(getQuintgrams).displayProgress(10_000), 0.01, 0.99, 100_000);
    Vocabulary sextgramsOk = sextgrams == null ? null
        : Vocabulary.of(View.of(newOk).map(getSextgrams).displayProgress(10_000), 0.01, 0.99, 100_000);

    Vocabulary unigramsKo = unigrams == null ? null
        : Vocabulary.of(View.of(newKo).map(getUnigrams).displayProgress(10_000), 0.01, 0.99, 100_000);
    Vocabulary bigramsKo = bigrams == null ? null
        : Vocabulary.of(View.of(newKo).map(getBigrams).displayProgress(10_000), 0.01, 0.99, 100_000);
    Vocabulary trigramsKo = trigrams == null ? null
        : Vocabulary.of(View.of(newKo).map(getTrigrams).displayProgress(10_000), 0.01, 0.99, 100_000);
    Vocabulary quadgramsKo = quadgrams == null ? null
        : Vocabulary.of(View.of(newKo).map(getQuadgrams).displayProgress(10_000), 0.01, 0.99, 100_000);
    Vocabulary quintgramsKo = quintgrams == null ? null
        : Vocabulary.of(View.of(newKo).map(getQuintgrams).displayProgress(10_000), 0.01, 0.99, 100_000);
    Vocabulary sextgramsKo = sextgrams == null ? null
        : Vocabulary.of(View.of(newKo).map(getSextgrams).displayProgress(10_000), 0.01, 0.99, 100_000);

    AbstractDocSetLabeler docSetLabeler = new AbstractDocSetLabeler() {

      @Override
      protected List<Map.Entry<String, Double>> filterOutCandidates(List<Map.Entry<String, Double>> candidates) {

        Set<Integer> ignored = new HashSet<>();

        for (int i = 0; i < candidates.size(); i++) {
          for (int j = i + 1; j < candidates.size(); j++) {

            String c1 = candidates.get(i).getKey().substring(2);
            String c2 = candidates.get(j).getKey().substring(2);

            if (c1.contains(c2) || c2.contains(c1)) {
              ignored.add(j);
            }
          }
        }
        for (int idx = candidates.size() - 1; idx >= 0; idx--) {
          if (ignored.contains(idx)) {
            candidates.remove(idx);
          }
        }
        return candidates.stream().map(c -> new SimpleImmutableEntry<>(c.getKey().substring(2), c.getValue()))
            .collect(Collectors.toList());
      }

      @Override
      protected Multiset<String> candidates(String text) {

        Multiset<String> multiset = HashMultiset.create();
        if (unigrams != null) {
          HashMultiset.create(getUnigrams.apply(text)).entrySet()
              .forEach(e -> multiset.add("1/" + e.getElement(), e.getCount()));
        }
        if (bigrams != null) {
          HashMultiset.create(getBigrams.apply(text)).entrySet()
              .forEach(e -> multiset.add("2/" + e.getElement(), e.getCount()));
        }
        if (trigrams != null) {
          HashMultiset.create(getTrigrams.apply(text)).entrySet()
              .forEach(e -> multiset.add("3/" + e.getElement(), e.getCount()));
        }
        if (quadgrams != null) {
          HashMultiset.create(getQuadgrams.apply(text)).entrySet()
              .forEach(e -> multiset.add("4/" + e.getElement(), e.getCount()));
        }
        if (quintgrams != null) {
          HashMultiset.create(getQuintgrams.apply(text)).entrySet()
              .forEach(e -> multiset.add("5/" + e.getElement(), e.getCount()));
        }
        if (sextgrams != null) {
          HashMultiset.create(getSextgrams.apply(text)).entrySet()
              .forEach(e -> multiset.add("6/" + e.getElement(), e.getCount()));
        }
        return multiset;
      }

      @Override
      protected double computeX(String text, String candidate, int count) {

        double tfIdfOk;
        double tfIdfKo;

        if (unigrams != null && candidate.startsWith("1/")) {
          tfIdfOk = unigramsOk.tfIdf(candidate.substring(2), count);
          tfIdfKo = unigramsKo.tfIdf(candidate.substring(2), count);
        } else if (bigrams != null && candidate.startsWith("2/")) {
          tfIdfOk = bigramsOk.tfIdf(candidate.substring(2), count);
          tfIdfKo = bigramsKo.tfIdf(candidate.substring(2), count);
        } else if (trigrams != null && candidate.startsWith("3/")) {
          tfIdfOk = trigramsOk.tfIdf(candidate.substring(2), count);
          tfIdfKo = trigramsKo.tfIdf(candidate.substring(2), count);
        } else if (quadgrams != null && candidate.startsWith("4/")) {
          tfIdfOk = quadgramsOk.tfIdf(candidate.substring(2), count);
          tfIdfKo = quadgramsKo.tfIdf(candidate.substring(2), count);
        } else if (quintgrams != null && candidate.startsWith("5/")) {
          tfIdfOk = quintgramsOk.tfIdf(candidate.substring(2), count);
          tfIdfKo = quintgramsKo.tfIdf(candidate.substring(2), count);
        } else if (sextgrams != null && candidate.startsWith("6/")) {
          tfIdfOk = sextgramsOk.tfIdf(candidate.substring(2), count);
          tfIdfKo = sextgramsKo.tfIdf(candidate.substring(2), count);
        } else {
          return 0.0;
        }

        // The more common the word is in the ok dataset, the better
        return tfIdfOk <= tfIdfKo ? 1.0 : 0.0;
      }

      @Override
      protected double computeY(String text, String candidate, int count) {

        // The less common the word is in the whole vocabulary, the better
        if (unigrams != null && candidate.startsWith("1/")) {
          return 1.0 - unigrams.ntf(candidate.substring(2));
        }
        if (bigrams != null && candidate.startsWith("2/")) {
          return 1.0 - bigrams.ntf(candidate.substring(2));
        }
        if (trigrams != null && candidate.startsWith("3/")) {
          return 1.0 - trigrams.ntf(candidate.substring(2));
        }
        if (quadgrams != null && candidate.startsWith("4/")) {
          return 1.0 - quadgrams.ntf(candidate.substring(2));
        }
        if (quintgrams != null && candidate.startsWith("5/")) {
          return 1.0 - quintgrams.ntf(candidate.substring(2));
        }
        if (sextgrams != null && candidate.startsWith("6/")) {
          return 1.0 - sextgrams.ntf(candidate.substring(2));
        }
        return 0.0;
      }
    };
    return docSetLabeler.labels(Lists.newArrayList(newOk), Lists.newArrayList(newKo), 100, 10);
  }

  /**
   * See https://en.wikipedia.org/wiki/Entropy_(information_theory)#Definition for details.
   *
   * @param nbDocs    the total number of documents.
   * @param nbPosDocs the number of documents that contain this label.
   * @param nbNegDocs the number of documents that contain this label.
   * @return the entropy.
   */
  static double entropy(double nbDocs, double nbPosDocs, double nbNegDocs) {

    Preconditions.checkArgument(nbDocs >= 0.0, "nbDocs must be >= 0");
    Preconditions.checkArgument(nbPosDocs >= 0.0, "nbPosDocs must be >= 0");
    Preconditions.checkArgument(nbNegDocs >= 0.0, "nbNegDocs must be >= 0");
    Preconditions.checkArgument(nbDocs == nbPosDocs + nbNegDocs, "nbDocs must be = nbPosDocs + nbNegDocs");

    if (nbDocs == 0.0) {
      return 0.0;
    }

    double positive = nbPosDocs / nbDocs;
    double negative = nbNegDocs / nbDocs;

    return (positive == 0.0 ? 0.0 : -positive * log2(positive)) + (negative == 0.0 ? 0.0 : -negative * log2(negative));
  }

  static double log2(double a) {
    return Math.log(a) / Math.log(2);
  }

  /**
   * See https://en.wikipedia.org/wiki/Information_gain_ratio#Information_gain_calculation for details.
   * <p>
   * A good high-level overview can be found here
   * https://towardsdatascience.com/entropy-how-decision-trees-make-decisions-2946b9c18c8
   *
   * @param nbPosDocs          the number of documents in the positive dataset.
   * @param nbMatchesInPosDocs the number of documents in the positive dataset that contain a given label.
   * @param nbNegDocs          the number of documents in the negative dataset.
   * @param nbMatchesInNegDocs the number of documents in the negative dataset that contain a given label.
   * @return the information gain.
   */
  static double informationGain(double nbPosDocs, double nbMatchesInPosDocs, double nbNegDocs,
      double nbMatchesInNegDocs) {

    Preconditions.checkArgument(nbMatchesInPosDocs <= nbPosDocs, "nbMatchesInPosDocs must be <= nbPosDocs");
    Preconditions.checkArgument(nbMatchesInNegDocs <= nbNegDocs, "nbMatchesInNegDocs must be <= nbNegDocs");

    double nbDocs = nbPosDocs + nbNegDocs;
    double nbMatchesInDocs = nbMatchesInPosDocs + nbMatchesInNegDocs;

    Preconditions.checkState(nbMatchesInDocs <= nbDocs);

    double h = entropy(nbDocs, nbMatchesInDocs, nbDocs - nbMatchesInDocs);
    double hPos = entropy(nbPosDocs, nbMatchesInPosDocs, nbPosDocs - nbMatchesInPosDocs);
    double hNeg = entropy(nbNegDocs, nbMatchesInNegDocs, nbNegDocs - nbMatchesInNegDocs);

    return h - ((nbPosDocs / nbDocs) * hPos + (nbNegDocs / nbDocs) * hNeg);
  }

  /**
   * See https://en.wikipedia.org/wiki/Information_gain_ratio#Intrinsic_value_calculation for details.
   *
   * @param nbPosDocs          the number of documents in the positive dataset.
   * @param nbMatchesInPosDocs the number of documents in the positive dataset that contain a given label.
   * @param nbNegDocs          the number of documents in the negative dataset.
   * @param nbMatchesInNegDocs the number of documents in the negative dataset that contain a given label.
   * @return the intrinsic value.
   */
  static double intrinsicValue(double nbPosDocs, double nbMatchesInPosDocs, double nbNegDocs,
      double nbMatchesInNegDocs) {

    Preconditions.checkArgument(nbMatchesInPosDocs <= nbPosDocs, "nbMatchesInPosDocs must be <= nbPosDocs");
    Preconditions.checkArgument(nbMatchesInNegDocs <= nbNegDocs, "nbMatchesInNegDocs must be <= nbNegDocs");

    double nbDocs = nbPosDocs + nbNegDocs;
    double nbMatchesInDocs = nbMatchesInPosDocs + nbMatchesInNegDocs;

    Preconditions.checkState(nbMatchesInDocs <= nbDocs);

    return -(nbMatchesInDocs / nbDocs) * log2(nbMatchesInDocs / nbDocs);
  }

  /**
   * See https://en.wikipedia.org/wiki/Information_gain_ratio#Information_gain_ratio_calculation for details.
   *
   * @param candidate  the candidate label.
   * @param contenders the contenders that could be picked as candidate.
   * @param counts     the precomputed counts for each contender.
   * @param nbPosDocs  the number of documents in the positive dataset.
   * @param nbNegDocs  the number of documents in the negative dataset.
   * @return the information gain ratio.
   */
  static double informationGainRatio(String candidate, Set<String> contenders,
      Map<String, Map.Entry<Double, Double>> counts, double nbPosDocs, double nbNegDocs) {

    double informationGain = informationGain(nbPosDocs, counts.get(candidate).getKey(), nbNegDocs,
        counts.get(candidate).getValue());
    @Var double intrinsicValue = 0.0d;

    for (String contender : contenders) {
      intrinsicValue += intrinsicValue(nbPosDocs, counts.get(contender).getKey(), nbNegDocs,
          counts.get(contender).getValue());
    }
    return informationGain / intrinsicValue;
  }

  static Map<String, Map.Entry<Double, Double>> counts(Map<String, Set<String>> pos, Map<String, Set<String>> neg) {

    Set<String> contenders = Sets.union(pos.values().stream().flatMap(Set::stream).collect(Collectors.toSet()),
        neg.values().stream().flatMap(Set::stream).collect(Collectors.toSet()));

    Map<String, Map.Entry<Double, Double>> counts = new HashMap<>();

    for (String contender : contenders) {

      double nbMatchesInPosDocs = pos.values().stream()
          .mapToDouble(list -> list.stream().anyMatch(term -> term.equals(contender)) ? 1.0 : 0.0).sum();
      double nbMatchesInNegDocs = neg.values().stream()
          .mapToDouble(list -> list.stream().anyMatch(term -> term.equals(contender)) ? 1.0 : 0.0).sum();

      counts.put(contender, new SimpleImmutableEntry<>(nbMatchesInPosDocs, nbMatchesInNegDocs));
    }
    return counts;
  }

  /**
   * Extract labels from texts.
   *
   * @param ok                     a subset of the corpus having caller-defined characteristics that should be matched.
   * @param ko                     a subset of the corpus having caller-defined characteristics that should not be
   *                               matched.
   * @param nbCandidatesToConsider the number of candidate terms to consider in each document.
   * @param nbLabelsToReturn       the number of labels to return.
   * @return labels and scores.
   */
  public List<Map.Entry<String, Double>> labels(List<String> ok, List<String> ko, int nbCandidatesToConsider,
      int nbLabelsToReturn) {

    Preconditions.checkNotNull(ok, "ok should not be null");
    Preconditions.checkNotNull(ko, "ko should not be null");
    Preconditions.checkArgument(nbCandidatesToConsider > 0, "nbCandidatesToConsider should be > 0");
    Preconditions.checkArgument(nbLabelsToReturn > 0, "nbLabelsToReturn should be > 0");
    Preconditions.checkArgument(nbLabelsToReturn <= nbCandidatesToConsider,
        "nbLabelsToReturn should be <= nbCandidatesToConsider");

    if (ok.isEmpty() || ko.isEmpty()) {
      return new ArrayList<>();
    }

    Comparator<Map.Entry<String, Double>> byScoreDesc = Comparator.comparingDouble(
            (Map.Entry<String, Double> pair) -> pair.getValue()).thenComparingInt(pair -> pair.getKey().length())
        .thenComparing(Entry::getKey).reversed();

    Map<String, Set<String>> pos = new HashMap<>();
    Map<String, Set<String>> neg = new HashMap<>();

    Set<String> corpus = Sets.union(Sets.newHashSet(ok), Sets.newHashSet(ko));
    @Var int nbTextsProcessed = 0;
    int nbTexts = corpus.size();

    for (String text : corpus) {

      nbTextsProcessed++;

      List<Map.Entry<String, Double>> weights = new ArrayList<>();
      Multiset<String> candidates = candidates(text);

      for (Multiset.Entry<String> candidate : candidates.entrySet()) {

        double x = computeX(text, candidate.getElement(), candidate.getCount());
        double y = computeY(text, candidate.getElement(), candidate.getCount());
        double weight = (2.0 * x * y) / (x + y);

        Preconditions.checkState(0.0 <= x && x <= 1.0, "x should be such as 0.0 <= x <= 1.0 : %s", x);
        Preconditions.checkState(0.0 <= y && y <= 1.0, "y should be such as 0.0 <= y <= 1.0 : %s", y);

        weights.add(new AbstractMap.SimpleEntry<>(candidate.getElement(), weight));
      }

      Set<String> selection = weights.isEmpty() ? Sets.newHashSet()
          : weights.stream().sorted(byScoreDesc).limit(nbCandidatesToConsider).map(Map.Entry::getKey)
              .collect(Collectors.toSet());

      if (ok.contains(text)) {
        pos.put(text, selection);
      } else {
        neg.put(text, selection);
      }
    }

    Preconditions.checkState(nbTexts == nbTextsProcessed);

    // For each candidate, precompute the number of matches in the pos/neg datasets
    Map<String, Map.Entry<Double, Double>> counts = counts(pos, neg);

    // For each candidate keyword compute the information gain
    Set<String> contenders = Sets.union(pos.values().stream().flatMap(Set::stream).collect(Collectors.toSet()),
        neg.values().stream().flatMap(Set::stream).collect(Collectors.toSet()));

    List<Map.Entry<String, Double>> candidates = pos.values().parallelStream().flatMap(Collection::stream).map(
            candidate -> new AbstractMap.SimpleEntry<>(candidate,
                informationGainRatio(candidate, contenders, counts, pos.size(), neg.size()))).sorted(byScoreDesc).distinct()
        .collect(Collectors.toList());

    // Keep the keywords with the highest information gain
    return filterOutCandidates(candidates).stream().limit(nbLabelsToReturn).collect(Collectors.toList());
  }

  protected List<Map.Entry<String, Double>> filterOutCandidates(List<Map.Entry<String, Double>> candidates) {
    return candidates;
  }

  protected abstract Multiset<String> candidates(String text);

  protected abstract double computeX(String text, String candidate, int count);

  protected abstract double computeY(String text, String candidate, int count);
}
