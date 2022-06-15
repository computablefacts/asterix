package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.Span;
import com.computablefacts.asterix.SpanSequence;
import com.computablefacts.asterix.View;
import com.computablefacts.asterix.codecs.StringCodec;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multiset.Entry;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
@CheckReturnValue
final public class Vocabulary {

  private final String tokenUnk_ = "<UNK>";
  private final int idxUnk_ = 0;
  private final Multiset<String> tf_; // term frequency
  private final Multiset<String> df_; // document frequency
  private final BiMap<String, Integer> idx_; // one-hot encoding
  private int nbTermsSeen_ = 0;
  private int nbDocsSeen_ = 0;
  private boolean isFrozen_ = false;

  public Vocabulary() {
    tf_ = HashMultiset.create();
    df_ = HashMultiset.create();
    idx_ = HashBiMap.create();
  }

  /**
   * Build a vocabulary from a set of tokenized documents.
   *
   * @param docs a set of tokenized documents.
   * @return a {@link Vocabulary}.
   */
  public static Vocabulary of(View<List<String>> docs) {

    Preconditions.checkNotNull(docs, "docs should not be null");

    Set<String> tokensSeen = new HashSet<>();
    Vocabulary vocabulary = new Vocabulary();
    docs.map(HashMultiset::create).forEachRemaining(terms -> {

      vocabulary.nbDocsSeen_ += 1;
      AtomicBoolean unkAlreadySeen = new AtomicBoolean(false);

      terms.entrySet().forEach(term -> {

        String token = term.getElement();
        int count = term.getCount();

        vocabulary.nbTermsSeen_ += count;

        if (tokensSeen.contains(token)) {
          vocabulary.tf_.add(token, count);
          vocabulary.df_.add(token);
        } else {

          tokensSeen.add(token);
          vocabulary.tf_.add(vocabulary.tokenUnk_);

          if (!unkAlreadySeen.getAndSet(true)) {
            vocabulary.df_.add(vocabulary.tokenUnk_);
          }
          if (count > 1) {
            vocabulary.tf_.add(token, count - 1);
            vocabulary.df_.add(token);
          }
        }
      });
    });
    tokensSeen.clear();
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

    Set<String> tokensSeen = new HashSet<>();
    Vocabulary vocabulary = new Vocabulary();
    docs.map(HashMultiset::create).forEachRemaining(terms -> {

      vocabulary.nbDocsSeen_ += 1;
      AtomicBoolean unkAlreadySeen = new AtomicBoolean(false);

      terms.entrySet().forEach(term -> {

        String token = term.getElement();
        int count = term.getCount();

        vocabulary.nbTermsSeen_ += count;

        if (tokensSeen.contains(token)) {
          vocabulary.tf_.add(token, count);
          vocabulary.df_.add(token);
        } else {

          tokensSeen.add(token);
          vocabulary.tf_.add(vocabulary.tokenUnk_);

          if (!unkAlreadySeen.getAndSet(true)) {
            vocabulary.df_.add(vocabulary.tokenUnk_);
          }
          if (count > 1) {
            vocabulary.tf_.add(token, count - 1);
            vocabulary.df_.add(token);
          }
        }
      });
    });
    tokensSeen.clear();
    vocabulary.freeze(minTermFreq, minDocFreq, maxVocabSize);

    return vocabulary;
  }

  /**
   * Remove all terms from the current vocabulary.
   */
  public void clear() {
    tf_.clear();
    df_.clear();
    idx_.clear();
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

    return idx_.getOrDefault(term, idxUnk_);
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
    return ntf(term(index));
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
    return ndf(term(index));
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
   * Subsampling attempts to minimize the impact of high-frequency words on the training of a word
   * embedding model.
   *
   * @param spans a span sequence.
   * @return sub-sampled spans.
   */
  public View<SpanSequence> subSample(View<SpanSequence> spans) {

    Preconditions.checkNotNull(spans, "spans should not be null");
    Preconditions.checkState(isFrozen_, "vocabulary must be frozen");

    Multiset<String> counts = HashMultiset.create();
    List<SpanSequence> newSpans = spans.peek(
        sentence -> View.of(sentence).map(Span::text).map(token -> term(index(token)))
            .forEachRemaining(token -> counts.add(token))).toList();
    int nbTokens = counts.size();
    Random random = new Random();

    return View.of(newSpans).map(sequence -> {

      SpanSequence newSequence = new SpanSequence();

      View.of(sequence).filter(token -> random.nextFloat() < Math.sqrt(
              1e-4 / (double) counts.count(token.text()) * (double) nbTokens))
          .forEachRemaining(newSequence::add);

      return newSequence;
    }).filter(sequence -> sequence.size() > 0);
  }

  /**
   * Find the most probable term following a given ngram.
   * <p>
   * In order to properly work:
   * <ul>
   * <li>The vocabulary must be for ngrams of length {@code ngram.nb_tokens + 1}</li>
   * <li>The ngram's terms must be separated with a single character</li>
   * </ul>
   *
   * @param ngram the prefix.
   * @return the most probable term following the given prefix.
   */
  public Optional<String> mostProbableNextTerm(String ngram) {

    Preconditions.checkNotNull(ngram, "ngram should not be null");
    Preconditions.checkState(isFrozen_, "vocabulary must be frozen");

    Set<String> ngrams = tf_.elementSet().stream().filter(n -> n.startsWith(ngram))
        .collect(Collectors.toSet());

    if (ngrams.isEmpty()) {
      return Optional.empty();
    }
    if (ngrams.size() == 1) {
      String token = Iterables.get(ngrams, 0).substring(ngram.length() + 1 /* separator */);
      return Optional.of(token);
    }

    int rnd = new Random().nextInt(ngrams.stream().mapToInt(tf_::count).sum());
    @Var int sum = 0;

    for (String n : ngrams) {

      sum += tf_.count(n);

      if (rnd < sum) {
        String token = n.substring(ngram.length() + 1 /* separator */);
        return Optional.of(token);
      }
    }
    return Optional.empty();
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

    View.of(idx_.keySet()).map(term -> term + "\t" + tf(term) + "\t" + df(term))
        .prepend(String.format("# %d %d\nterm\ttf\tdf", nbTermsSeen_, nbDocsSeen_))
        .toFile(Function.identity(), file, false, true);
  }

  /**
   * Load a vocabulary previously saved using {@link #save(File)}.
   *
   * @param file the file to load.
   */
  public void load(File file) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file does not exist : %s", file);

    idx_.clear(); // it allows us to merge multiple files together

    String stats = View.of(file, true).take(1).toList().get(0);

    int index1 = stats.indexOf(' ');
    int index2 = stats.lastIndexOf(' ');

    nbTermsSeen_ = Integer.parseInt(stats.substring(index1 + 1, index2), 10);
    nbDocsSeen_ = Integer.parseInt(stats.substring(index2 + 1), 10);

    View.of(file, true).drop(1 /* number of terms/docs seen */ + 1/* header */)
        .forEachRemaining(row -> {
          int idx1 = row.indexOf('\t');
          int idx2 = row.lastIndexOf('\t');
          String term = row.substring(0, idx1);
          int tf = Integer.parseInt(row.substring(idx1 + 1, idx2), 10);
          int df = Integer.parseInt(row.substring(idx2 + 1), 10);
          tf_.add(term, tf);
          df_.add(term, df);
        });

    freeze(-1, -1, -1);
  }

  public Vocabulary patterns() {

    Preconditions.checkState(isFrozen_, "vocabulary must be frozen");

    Vocabulary vocabulary = new Vocabulary();
    vocabulary.nbTermsSeen_ = nbTermsSeen_;
    vocabulary.nbDocsSeen_ = nbDocsSeen_;

    idx_.keySet().forEach(term -> {
      if (tokenUnk_.equals(term)) {
        vocabulary.tf_.add(term, tf(term));
        vocabulary.df_.add(term, df(term));
      } else {
        String pattern = newPattern(term);
        vocabulary.tf_.add(pattern, tf(term));
        vocabulary.df_.add(pattern, df(term));
      }
    });

    vocabulary.freeze(-1, -1, -1);

    Preconditions.checkState(tf_.size() == vocabulary.tf_.size());
    Preconditions.checkState(df_.size() == vocabulary.df_.size());
    Preconditions.checkState(idx_.size() <= vocabulary.idx_.size());

    return vocabulary;
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
      df_.entrySet().removeIf(freq -> !tf_.contains(freq.getElement()));
      tf_.entrySet().removeIf(freq -> !df_.contains(freq.getElement()));
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

  private String newPattern(String token) {

    Preconditions.checkNotNull(token, "token should not be null");

    String lowercase = token.toLowerCase();
    String uppercase = token.toUpperCase();
    String normalizedLowercase = StringCodec.removeDiacriticalMarks(lowercase);
    String normalizedUppercase = StringCodec.removeDiacriticalMarks(uppercase);

    if (token.length() != lowercase.length() || token.length() != uppercase.length()
        || token.length() != normalizedLowercase.length()
        || token.length() != normalizedUppercase.length()) {

      // For example the lowercase character 'ß' is mapped to 'SS' in uppercase...
      return ".*";
    }

    StringBuilder pattern = new StringBuilder(token.length());

    for (int k = 0; k < token.length(); k++) {

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
}