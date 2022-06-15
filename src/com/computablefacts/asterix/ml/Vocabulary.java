package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.View;
import com.computablefacts.asterix.codecs.StringCodec;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import com.google.re2j.Pattern;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
@CheckReturnValue
final public class Vocabulary {

  private final String tokenUnk_ = "<UNK>";
  private final int idxUnk_ = 0;
  private final Multiset<String> tf_ = HashMultiset.create(); // normalized term -> term frequency
  private final Multiset<String> df_ = HashMultiset.create(); // normalized term -> document frequency
  private final BiMap<String, Integer> idx_ = HashBiMap.create(); // one-hot encoding
  private final Map<String, Set<String>> forms_ = new HashMap<>(); // normalized term -> raw terms
  private int nbTermsSeen_ = 0;
  private int nbDocsSeen_ = 0;
  private boolean isFrozen_ = false;

  public Vocabulary() {
  }

  /**
   * Build a vocabulary from a set of tokenized documents.
   *
   * @param docs a set of tokenized documents.
   * @return a {@link Vocabulary}.
   */
  public static Vocabulary of(View<List<String>> docs) {

    Preconditions.checkNotNull(docs, "docs should not be null");

    Set<String> normalizedTermsSeen = new HashSet<>();
    Vocabulary vocabulary = new Vocabulary();
    docs.map(HashMultiset::create).forEachRemaining(terms -> {

      vocabulary.nbDocsSeen_ += 1;
      AtomicBoolean unkAlreadySeen = new AtomicBoolean(false);

      terms.entrySet().forEach(term -> {

        String normalizedTerm = normalize(term.getElement());
        int termCount = term.getCount();

        if (!vocabulary.forms_.containsKey(normalizedTerm)) {
          vocabulary.forms_.put(normalizedTerm, new HashSet<>());
        }

        vocabulary.forms_.get(normalizedTerm).add(term.getElement());
        vocabulary.nbTermsSeen_ += termCount;

        if (normalizedTermsSeen.contains(normalizedTerm)) {
          vocabulary.tf_.add(normalizedTerm, termCount);
          vocabulary.df_.add(normalizedTerm);
        } else {

          normalizedTermsSeen.add(normalizedTerm);
          vocabulary.tf_.add(vocabulary.tokenUnk_);

          if (!unkAlreadySeen.getAndSet(true)) {
            vocabulary.df_.add(vocabulary.tokenUnk_);
          }
          if (termCount > 1) {
            vocabulary.tf_.add(normalizedTerm, termCount - 1);
            vocabulary.df_.add(normalizedTerm);
          }
        }
      });
    });
    normalizedTermsSeen.clear();
    vocabulary.freeze(-1, -1, -1);

    return vocabulary;
  }

  /**
   * Build a vocabulary from a set of tokenized documents.
   *
   * @param docs a set of tokenized documents.
   * @param minTermFreq the term-frequency threshold under which a term must be excluded.
   * @param minDocFreq the document-frequency threshold under which a term must be excluded.
   * @param maxVocabSize the maximum number of terms to include in the vocabulary.
   * @return a {@link Vocabulary}.
   */
  public static Vocabulary of(View<List<String>> docs, int minTermFreq, int minDocFreq,
      int maxVocabSize) {

    Preconditions.checkNotNull(docs, "docs should not be null");
    Preconditions.checkArgument(minTermFreq > 0, "minTermFreq must be > 0");
    Preconditions.checkArgument(maxVocabSize > 0, "maxVocabSize must be > 0");

    Set<String> normalizedTermsSeen = new HashSet<>();
    Vocabulary vocabulary = new Vocabulary();
    docs.map(HashMultiset::create).forEachRemaining(terms -> {

      vocabulary.nbDocsSeen_ += 1;
      AtomicBoolean unkAlreadySeen = new AtomicBoolean(false);

      terms.entrySet().forEach(term -> {

        String normalizedTerm = normalize(term.getElement());
        int termCount = term.getCount();

        if (!vocabulary.forms_.containsKey(normalizedTerm)) {
          vocabulary.forms_.put(normalizedTerm, new HashSet<>());
        }

        vocabulary.forms_.get(normalizedTerm).add(term.getElement());
        vocabulary.nbTermsSeen_ += termCount;

        if (normalizedTermsSeen.contains(normalizedTerm)) {
          vocabulary.tf_.add(normalizedTerm, termCount);
          vocabulary.df_.add(normalizedTerm);
        } else {

          normalizedTermsSeen.add(normalizedTerm);
          vocabulary.tf_.add(vocabulary.tokenUnk_);

          if (!unkAlreadySeen.getAndSet(true)) {
            vocabulary.df_.add(vocabulary.tokenUnk_);
          }
          if (termCount > 1) {
            vocabulary.tf_.add(normalizedTerm, termCount - 1);
            vocabulary.df_.add(normalizedTerm);
          }
        }
      });
    });
    normalizedTermsSeen.clear();
    vocabulary.freeze(minTermFreq, minDocFreq, maxVocabSize);

    return vocabulary;
  }

  private static String normalize(String term) {

    Preconditions.checkNotNull(term, "term should not be null");

    String lowercase = term.toLowerCase();
    String uppercase = term.toUpperCase();
    String normalizedLowercase = StringCodec.removeDiacriticalMarks(lowercase);
    String normalizedUppercase = StringCodec.removeDiacriticalMarks(uppercase);

    if (term.length() != lowercase.length() || term.length() != uppercase.length()
        || term.length() != normalizedLowercase.length()
        || term.length() != normalizedUppercase.length()) {

      // For example, the lowercase character 'ß' is mapped to 'SS' in uppercase...
      return Pattern.quote(term);
    }
    if (!term.equals(Pattern.quote(term))) {

      // For example, the characters '[' or ']' must be escaped
      return Pattern.quote(term);
    }

    StringBuilder pattern = new StringBuilder(term.length());

    for (int k = 0; k < term.length(); k++) {

      char charLowerCase = lowercase.charAt(k);
      char charUpperCase = uppercase.charAt(k);
      char charNormalizedLowerCase = normalizedLowercase.charAt(k);
      char charNormalizedUpperCase = normalizedUppercase.charAt(k);

      pattern.append('[');
      pattern.append(charLowerCase);
      if (charLowerCase != charUpperCase) {
        pattern.append(charUpperCase);
      }
      if (charLowerCase != charNormalizedLowerCase && charUpperCase != charNormalizedLowerCase) {
        pattern.append(charNormalizedLowerCase);
      }
      if (charLowerCase != charNormalizedUpperCase && charUpperCase != charNormalizedUpperCase
          && charNormalizedLowerCase != charNormalizedUpperCase) {
        pattern.append(charNormalizedUpperCase);
      }
      pattern.append(']');
    }
    return pattern.toString();
  }

  /**
   * Remove all terms from the current vocabulary.
   */
  public void clear() {
    tf_.clear();
    df_.clear();
    idx_.clear();
    forms_.clear();
    isFrozen_ = false;
  }

  /**
   * Returns the size of the vocabulary i.e. the number of distinct terms.
   *
   * @return the vocabulary size.
   */
  public int size() {

    Preconditions.checkState(isFrozen_, "vocabulary must be frozen");

    return idx_.size();
  }

  /**
   * Map a term to a term index.
   *
   * @param term the term.
   * @return the index.
   */
  public int index(String term) {

    Preconditions.checkNotNull(term, "term should not be null");
    Preconditions.checkState(isFrozen_, "vocabulary must be frozen");

    return idx_.getOrDefault(normalize(term), idxUnk_);
  }

  /**
   * Map a term index to a term.
   *
   * @param index the index.
   * @return the term.
   */
  public String term(int index) {

    Preconditions.checkArgument(index >= 0, "index must be >= 0");
    Preconditions.checkState(isFrozen_, "vocabulary must be frozen");

    return idx_.inverse().getOrDefault(index, tokenUnk_);
  }

  /**
   * Returns the term frequency of a given term.
   *
   * @param term the term.
   * @return the term frequency. This value is in [0, #terms].
   */
  public int tf(String term) {

    Preconditions.checkNotNull(term, "term should not be null");
    Preconditions.checkState(isFrozen_, "vocabulary must be frozen");

    return tf_.count(term(index(term)));
  }

  /**
   * Returns the term frequency of a given term.
   *
   * @param index the term index in the current vocabulary.
   * @return the term frequency. This value is in [0, #terms].
   */
  public int tf(int index) {

    Preconditions.checkState(isFrozen_, "vocabulary must be frozen");

    return tf_.count(term(index));
  }

  /**
   * Returns the document frequency of a given term.
   *
   * @param term the term.
   * @return the document frequency. This value is in [0, #documents].
   */
  public int df(String term) {

    Preconditions.checkNotNull(term, "term should not be null");
    Preconditions.checkState(isFrozen_, "vocabulary must be frozen");

    return df_.count(term(index(term)));
  }

  /**
   * Returns the document frequency of a given term.
   *
   * @param index the term index in the current vocabulary.
   * @return the document frequency. This value is in [0, #documents].
   */
  public int df(int index) {

    Preconditions.checkArgument(index >= 0, "index must be >= 0");
    Preconditions.checkState(isFrozen_, "vocabulary must be frozen");

    return df_.count(term(index));
  }

  /**
   * Returns the normalized term frequency of a given term.
   *
   * @param term the term.
   * @return the normalized term frequency. This value is in [0, 1].
   */
  public double ntf(String term) {
    return (double) tf(term) / (double) nbTermsSeen_;
  }

  /**
   * Returns the normalized term frequency of a given term.
   *
   * @param index the term index in the current vocabulary.
   * @return the normalized term frequency. This value is in [0, 1].
   */
  public double ntf(int index) {
    return (double) tf(index) / (double) nbTermsSeen_;
  }

  /**
   * Returns the normalized document frequency of a given term.
   *
   * @param term the term.
   * @return the normalized document frequency. This value is in [0, 1].
   */
  public double ndf(String term) {
    return (double) df(term) / (double) nbDocsSeen_;
  }

  /**
   * Returns the normalized document frequency of a given term.
   *
   * @param index the term index in the current vocabulary.
   * @return the normalized document frequency. This value is in [0, 1].
   */
  public double ndf(int index) {
    return (double) df(index) / (double) nbDocsSeen_;
  }

  /**
   * Returns the inverse document frequency which measures how common a word is among all
   * documents.
   *
   * @param term the term.
   * @return the inverse document frequency.
   */
  public double idf(String term) {
    return 1 + Math.log((double) nbDocsSeen_ / (double) (1 + df(term)));
  }

  /**
   * Returns the inverse document frequency which measures how common a word is among all
   * documents.
   *
   * @param index the term index in the current vocabulary.
   * @return the inverse document frequency.
   */
  public double idf(int index) {
    return 1 + Math.log((double) nbDocsSeen_ / (double) (1 + df(index)));
  }

  /**
   * Computes the TF-IDF score of a given term.
   *
   * @param term the term for which TF-IDF must be computed.
   * @param count the term frequency in the source document.
   * @return TF-IDF.
   */
  public double tfIdf(String term, int count) {
    return count * idf(term);
  }

  /**
   * Computes the TF-IDF score of a given term.
   *
   * @param index the term index in the current vocabulary for which TF-IDF must be computed.
   * @param count the term frequency in the source document.
   * @return TF-IDF.
   */
  public double tfIdf(int index, int count) {
    return count * idf(index);
  }

  /**
   * Save the current vocabulary as a gzipped TSV file.
   *
   * @param file the file to create.
   */
  public void save(File file) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(!file.exists(), "file already exists : %s", file);
    Preconditions.checkState(isFrozen_, "vocabulary must be frozen");

    View.of(idx_.keySet()).map(
        term -> idx_.get(term) + "\t" + term + "\t" + tf_.count(term) + "\t" + df_.count(term)
            + "\t" + Joiner.on('\0').join(forms_.getOrDefault(term, new HashSet<>()))).prepend(
        String.format("# %d %d\nidx\tnormalized_term\ttf\tdf\traw_terms", nbTermsSeen_,
            nbDocsSeen_)).toFile(Function.identity(), file, false, true);
  }

  /**
   * Load a vocabulary previously saved using {@link #save(File)}.
   *
   * @param file the file to load.
   */
  public void load(File file) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file does not exist : %s", file);
    Preconditions.checkState(!isFrozen_, "vocabulary should not be frozen");

    isFrozen_ = true;
    String stats = View.of(file, true).take(1).toList().get(0);

    int index1 = stats.indexOf(' ');
    int index2 = stats.lastIndexOf(' ');

    nbTermsSeen_ = Integer.parseInt(stats.substring(index1 + 1, index2), 10);
    nbDocsSeen_ = Integer.parseInt(stats.substring(index2 + 1), 10);

    View.of(file, true).drop(1 /* number of terms/docs seen */ + 1/* header */)
        .forEachRemaining(row -> {

          List<String> columns = Splitter.on('\t').trimResults().splitToList(row);
          int idx = Integer.parseInt(columns.get(0), 10);
          String term = columns.get(1);
          int tf = Integer.parseInt(columns.get(2), 10);
          int df = Integer.parseInt(columns.get(3), 10);
          List<String> forms = Splitter.on('\0').trimResults().splitToList(columns.get(4));

          tf_.add(term, tf);
          df_.add(term, df);
          idx_.put(term, idx);
          forms_.put(term, Sets.newHashSet(forms));
        });
  }

  private void freeze(int minTermFreq, int minDocFreq, int maxVocabSize) {

    Preconditions.checkState(!isFrozen_, "vocabulary is already frozen");

    isFrozen_ = true;
    idx_.put(tokenUnk_, idxUnk_);

    if (minDocFreq > 0) {
      df_.entrySet().removeIf(freq -> freq.getCount() < minDocFreq);
    }
    if (minTermFreq > 0) {
      tf_.entrySet().removeIf(freq -> freq.getCount() < minTermFreq);
    }

    while (tf_.elementSet().size() != df_.elementSet().size()) {
      forms_.entrySet()
          .removeIf(freq -> !tf_.contains(freq.getKey()) || !df_.contains(freq.getKey()));
      df_.entrySet().removeIf(freq -> !forms_.containsKey(freq.getElement()));
      tf_.entrySet().removeIf(freq -> !forms_.containsKey(freq.getElement()));
    }

    @Var Stream<Entry<String>> stream = tf_.entrySet().stream()
        .filter(e -> !tokenUnk_.equals(e.getElement())).sorted(
            (e1, e2) -> ComparisonChain.start().compare(e1.getCount(), e2.getCount())
                .compare(e1.getElement(), e2.getElement()).result());

    if (maxVocabSize > 0) {
      stream = stream.limit(maxVocabSize - 1 /* UNK */);
    }

    stream.forEach(token -> {
      if (!idx_.containsKey(token.getElement())) {
        idx_.put(token.getElement(), idx_.size());
      }
    });
  }
}