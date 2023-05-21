package com.computablefacts.asterix.nlp;

import com.computablefacts.asterix.Result;
import com.computablefacts.asterix.View;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Implements a modified version of the DocSetLabeler algorithm from "Topic Similarity Networks: Visual Analytics for
 * Large Document Sets" by Arun S. Maiya and Robert M. Rolfe.
 */
@CheckReturnValue
public abstract class AbstractDocSetLabeler {

  protected AbstractDocSetLabeler() {
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
   * @param nbPosDocs         the number of documents in the positive dataset.
   * @param nbMatchesInPosDoc the number of documents in the positive dataset that contain a given label.
   * @param nbNegDocs         the number of documents in the negative dataset.
   * @param nbMatchesInNegDoc the number of documents in the negative dataset that contain a given label.
   * @param intrinsicValue    the intrinsic value.
   * @return the information gain ratio.
   */
  static double informationGainRatio(double nbPosDocs, double nbMatchesInPosDoc, double nbNegDocs,
      double nbMatchesInNegDoc, double intrinsicValue) {
    double informationGain = informationGain(nbPosDocs, nbMatchesInPosDoc, nbNegDocs, nbMatchesInNegDoc);
    return informationGain / intrinsicValue;
  }

  /**
   * For each contender, compute the number of occurrences of the keyword in the positive and negative datasets.
   *
   * @param contenders the contenders.
   * @param pos
   * @param neg
   * @return
   */
  static Map<String, Map.Entry<Double, Double>> counts(Set<String> contenders, Map<String, Set<String>> pos,
      Map<String, Set<String>> neg) {

    List<Set<String>> posSets = pos.values().stream().map(Sets::newHashSet).collect(Collectors.toList());
    List<Set<String>> negSets = neg.values().stream().map(Sets::newHashSet).collect(Collectors.toList());

    Map<String, Map.Entry<Double, Double>> counts = new ConcurrentHashMap<>();
    contenders.forEach(contender -> {

      double nbMatchesInPosDocs = posSets.stream().mapToDouble(set -> set.contains(contender) ? 1.0 : 0.0).sum();
      double nbMatchesInNegDocs = negSets.stream().mapToDouble(set -> set.contains(contender) ? 1.0 : 0.0).sum();

      counts.put(contender, new SimpleImmutableEntry<>(nbMatchesInPosDocs, nbMatchesInNegDocs));
    });
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

    Map<String, Set<String>> pos = new ConcurrentHashMap<>();
    Map<String, Set<String>> neg = new ConcurrentHashMap<>();

    Set<String> corpus = Sets.union(Sets.newHashSet(ok), Sets.newHashSet(ko));
    AtomicInteger nbTextsProcessed = new AtomicInteger(0);
    int nbTexts = corpus.size();

    View.of(corpus).forEachRemainingInParallel(text -> {

      nbTextsProcessed.incrementAndGet();

      Set<String> selection = Result.of(candidates(text))
          .map(candidates -> View.of(candidates.entrySet()).map(candidate -> {

            double x = computeX(text, candidate.getElement(), candidate.getCount());
            double y = computeY(text, candidate.getElement(), candidate.getCount());
            double weight = (2.0 * x * y) / (x + y);

            Preconditions.checkState(0.0 <= x && x <= 1.0, "x should be such as 0.0 <= x <= 1.0 : %s", x);
            Preconditions.checkState(0.0 <= y && y <= 1.0, "y should be such as 0.0 <= y <= 1.0 : %s", y);

            return (Map.Entry<String, Double>) new AbstractMap.SimpleEntry<>(candidate.getElement(), weight);
          }).sort(byScoreDesc).take(nbCandidatesToConsider).map(Map.Entry::getKey).toSet()).get(Sets::newHashSet);

      if (ok.contains(text)) {
        pos.put(text, selection);
      } else {
        neg.put(text, selection);
      }
    });

    Preconditions.checkState(nbTexts == nbTextsProcessed.get());

    Set<String> contenders = Sets.union(pos.values().stream().flatMap(Set::stream).collect(Collectors.toSet()),
        neg.values().stream().flatMap(Set::stream).collect(Collectors.toSet()));

    // For each candidate keyword, compute the number of matches in the pos/neg datasets
    Map<String, Map.Entry<Double, Double>> counts = counts(contenders, pos, neg);

    // For each candidate keyword, compute the intrinsic value
    @Var double intrinsicValue = 0.0d;

    for (String contender : contenders) {
      intrinsicValue += intrinsicValue(pos.size(), counts.get(contender).getKey(), neg.size(),
          counts.get(contender).getValue());
    }

    double newIntrinsicValue = intrinsicValue;

    // For each candidate keyword, compute the information gain ratio
    List<Map.Entry<String, Double>> candidates = pos.values().stream().flatMap(Collection::stream).map(
            candidate -> new AbstractMap.SimpleEntry<>(candidate,
                informationGainRatio(pos.size(), counts.get(candidate).getKey(), neg.size(),
                    counts.get(candidate).getValue(), newIntrinsicValue))).sorted(byScoreDesc).distinct()
        .collect(Collectors.toList());

    // Keep the keywords with the highest information gain
    return filterOutCandidates(candidates).take(nbLabelsToReturn).toList();
  }

  protected View<Map.Entry<String, Double>> filterOutCandidates(List<Map.Entry<String, Double>> candidates) {
    return View.of(candidates);
  }

  protected abstract Multiset<String> candidates(String text);

  protected abstract double computeX(String text, String candidate, int count);

  protected abstract double computeY(String text, String candidate, int count);
}
