package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.View;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.List;
import java.util.Random;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
@CheckReturnValue
final public class Vocabulary {

  private final String tokenUnk_ = "<UNK>";
  private final int idxUnk_ = 0;
  private final Multiset<String> freq_;
  private final BiMap<String, Integer> idx_;
  private boolean isFrozen_ = false;

  public Vocabulary() {
    freq_ = HashMultiset.create();
    idx_ = HashBiMap.create();
  }

  public static Vocabulary of(View<String> tokens, int minTokenFreq, int maxVocabSize) {

    Preconditions.checkNotNull(tokens, "tokens should not be null");
    Preconditions.checkArgument(minTokenFreq > 0, "minTokenFreq must be > 0");
    Preconditions.checkArgument(maxVocabSize > 0, "maxVocabSize must be > 0");

    Vocabulary vocabulary = new Vocabulary();
    tokens.forEachRemaining(vocabulary::add);
    vocabulary.freeze(minTokenFreq, maxVocabSize);

    return vocabulary;
  }

  public int size() {

    Preconditions.checkState(isFrozen_, "vocabulary must be frozen");

    return idx_.size();
  }

  public int index(String token) {

    Preconditions.checkNotNull(token, "token should not be null");
    Preconditions.checkState(isFrozen_, "vocabulary must be frozen");

    return idx_.getOrDefault(token, idxUnk_);
  }

  public String token(int index) {

    Preconditions.checkArgument(index >= 0, "index must be >= 0");
    Preconditions.checkState(isFrozen_, "vocabulary must be frozen");

    return idx_.inverse().getOrDefault(index, tokenUnk_);
  }

  public int frequency(String token) {

    Preconditions.checkNotNull(token, "token should not be null");
    Preconditions.checkState(isFrozen_, "vocabulary must be frozen");

    return freq_.count(token);
  }

  public int frequency(int index) {
    return frequency(token(index));
  }

  private void add(String token) {

    Preconditions.checkState(!isFrozen_, "vocabulary is frozen");

    freq_.add(token);
  }

  /**
   * Subsampling attempts to minimize the impact of high-frequency words on the training of a word
   * embedding model.
   *
   * @param sentences a list of sentences.
   * @return subsampled sentences.
   */
  public View<List<String>> subSample(View<List<String>> sentences) {

    Preconditions.checkNotNull(sentences, "sentences should not be null");
    Preconditions.checkState(isFrozen_, "vocabulary must be frozen");

    Multiset<String> counts = HashMultiset.create();
    List<List<String>> newSentences = sentences.map(
            sentence -> View.of(sentence).map(token -> token(index(token))))
        .map(sentence -> sentence.peek(counts::add).toList()).toList();
    int nbTokens = counts.size();
    Random random = new Random();

    return View.of(newSentences).map(sentence -> View.of(sentence).filter(
            token -> random.nextFloat() < Math.sqrt(
                1e-4 / (double) counts.count(token) * (double) nbTokens)).toList())
        .filter(sentence -> !sentence.isEmpty());
  }

  private void freeze(int minTokenFreq, int maxVocabSize) {

    Preconditions.checkArgument(minTokenFreq > 0, "minTokenFreq must be > 0");
    Preconditions.checkState(!isFrozen_, "vocabulary is already frozen");
    Preconditions.checkArgument(maxVocabSize > 0, "maxVocabSize must be > 0");

    isFrozen_ = true;
    idx_.put(tokenUnk_, idxUnk_);
    freq_.entrySet().stream().filter(freq -> freq.getCount() >= minTokenFreq).sorted(
            (e1, e2) -> ComparisonChain.start().compare(e1.getCount(), e2.getCount())
                .compare(e1.getElement(), e2.getElement()).result()).limit(maxVocabSize - 1 /* UNK */)
        .forEach(token -> {
          if (!idx_.containsKey(token.getElement())) {
            idx_.put(token.getElement(), idx_.size());
          }
        });
  }
}