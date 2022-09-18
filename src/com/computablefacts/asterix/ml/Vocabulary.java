package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.BloomFilter;
import com.computablefacts.asterix.Document;
import com.computablefacts.asterix.Span;
import com.computablefacts.asterix.View;
import com.computablefacts.asterix.console.Observations;
import com.google.common.annotations.Beta;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.collect.BiMap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
@CheckReturnValue
final public class Vocabulary {

  private final String tokenUnk_ = "<UNK>";
  private final int idxUnk_ = 0;
  private final Map<String, Integer> tf_ = new HashMap<>(); // normalized term -> term frequency
  private final Map<String, Integer> df_ = new HashMap<>(); // normalized term -> document frequency
  private final BiMap<String, Integer> idx_ = HashBiMap.create(); // one-hot encoding
  private int nbTermsSeen_ = 0;
  private int nbDocsSeen_ = 0;
  private boolean isFrozen_ = false;

  public Vocabulary() {
  }

  public Vocabulary(File file) {
    load(Preconditions.checkNotNull(file, "file should not be null"));
  }

  /**
   * Build a vocabulary from a corpus of documents. To be versatile, this method does not attempt to remove stop words,
   * diacritical marks or even lowercase tokens.
   * <ul>
   * <li>{@code args[0]} the corpus of documents as a gzipped JSONL file.</li>
   * <li>{@code args[1]} the threshold (document frequency) under which a token must be excluded from the vocabulary.</li>
   * <li>{@code args[2]} the threshold (document frequency) above which a token must be excluded from the vocabulary.</li>
   * <li>{@code args[3]} the maximum size of the {@link Vocabulary}.</li>
   * <li>{@code args[4]} the types of tokens to keep: WORD, PUNCTUATION, etc.</li>
   * <li>{@code args[5]} the ngrams length: 1 = unigrams, 2 = bigrams, 3 = trigrams, etc.</li>
   * </ul>
   */
  @Beta
  public static void main(String[] args) {

    File file = new File(args[0]);
    double minDocFreq = Double.parseDouble(args[1]);
    double maxDocFreq = Double.parseDouble(args[2]);
    int maxVocabSize = Integer.parseInt(args[3], 10);
    Set<String> includeTags = Sets.newHashSet(Splitter.on(',').trimResults().omitEmptyStrings().split(args[4]));
    int ngramsLength = Integer.parseInt(args[5], 10);

    Preconditions.checkArgument(0.0 <= minDocFreq && minDocFreq <= 1.0,
        "minDocFreq must be such as 0.0 <= minDocFreq <= 1.0");
    Preconditions.checkArgument(minDocFreq <= maxDocFreq && maxDocFreq <= 1.0,
        "maxDocFreq must be such as minDocFreq <= maxDocFreq <= 1.0");
    Preconditions.checkArgument(maxVocabSize > 0, "maxVocabSize must be = -1 or > 0");
    Preconditions.checkArgument(!includeTags.isEmpty(), "includeTags should not be empty");
    Preconditions.checkArgument(ngramsLength > 0, "ngramLength must be > 0");

    try (Observations observations = new Observations(
        new File(String.format("%sobservations.txt", file.getParent() + File.separator)))) {

      observations.add(String.format("Dataset is %s.", file));
      observations.add(String.format("NGrams length is %d.", ngramsLength));
      observations.add(String.format("Min. document freq. is %.01f%% of all documents.", minDocFreq * 100));
      observations.add(String.format("Max. document freq. is %.01f%% of all documents.", maxDocFreq * 100));
      observations.add(String.format("Max. vocab size is %d.", maxVocabSize));
      observations.add(String.format("Included tags are %s.", includeTags));
      observations.add("Building vocabulary...");

      Stopwatch stopwatch = Stopwatch.createStarted();
      View<List<String>> tokens = Document.of(file, true)
          .map(doc -> tokenizer(includeTags, ngramsLength).apply((String) doc.text())).displayProgress(10_000);
      Vocabulary vocabulary = Vocabulary.of(tokens, minDocFreq, maxDocFreq, maxVocabSize);
      vocabulary.save(
          new File(String.format("%svocabulary-%dgrams.tsv.gz", file.getParent() + File.separator, ngramsLength)));
      stopwatch.stop();

      observations.add(String.format("Vocabulary built in %d seconds.", stopwatch.elapsed(TimeUnit.SECONDS)));
      observations.add(String.format("Vocabulary size is %d.", vocabulary.size()));
    }
  }

  /**
   * Build a vocabulary from a set of tokenized documents.
   *
   * @param docs a set of tokenized documents.
   * @return a {@link Vocabulary}.
   */
  public static Vocabulary of(View<List<String>> docs) {
    return of(docs, 0.0, 1.0, -1);
  }

  /**
   * Build a vocabulary from a set of tokenized documents.
   *
   * @param docs         a set of tokenized documents.
   * @param minDocFreq   the document-frequency threshold (as a number of documents) under which a term must be
   *                     excluded.
   * @param maxDocFreq   the document-frequency threshold (as a number of documents) above which a term must be
   *                     excluded.
   * @param maxVocabSize the maximum number of terms to include in the vocabulary.
   * @return a {@link Vocabulary}.
   */
  public static Vocabulary of(View<List<String>> docs, int minDocFreq, int maxDocFreq, int maxVocabSize) {

    Preconditions.checkNotNull(docs, "docs should not be null");
    Preconditions.checkArgument(0 <= minDocFreq, "minDocFreq must be >= 0");
    Preconditions.checkArgument(minDocFreq <= maxDocFreq, "minDocFreq must be >= minDocFreq ");
    Preconditions.checkArgument(maxVocabSize == -1 || maxVocabSize > 0, "maxVocabSize must be = -1 or > 0");

    Vocabulary vocabulary = new Vocabulary();
    vocabulary.fill(docs);
    vocabulary.freeze(minDocFreq, maxDocFreq, maxVocabSize);

    return vocabulary;
  }

  /**
   * Build a vocabulary from a set of tokenized documents.
   *
   * @param docs         a set of tokenized documents.
   * @param minDocFreq   the document-frequency threshold (as a percentage of the total number of documents) under which
   *                     a term must be excluded.
   * @param maxDocFreq   the document-frequency threshold (as a percentage of the total number of documents) above which
   *                     a term must be excluded.
   * @param maxVocabSize the maximum number of terms to include in the vocabulary.
   * @return a {@link Vocabulary}.
   */
  public static Vocabulary of(View<List<String>> docs, double minDocFreq, double maxDocFreq, int maxVocabSize) {

    Preconditions.checkArgument(0.0 <= minDocFreq && minDocFreq <= 1.0,
        "minDocFreq must be such as 0.0 <= minDocFreq <= 1.0");
    Preconditions.checkArgument(minDocFreq <= maxDocFreq && maxDocFreq <= 1.0,
        "minDocFreq must be such as minDocFreq <= maxDocFreq <= 1.0");
    Preconditions.checkArgument(maxVocabSize == -1 || maxVocabSize > 0, "maxVocabSize must be = -1 or > 0");

    Vocabulary vocabulary = new Vocabulary();
    vocabulary.fill(docs);
    vocabulary.freeze((int) (minDocFreq * vocabulary.nbDocsSeen_), (int) (maxDocFreq * vocabulary.nbDocsSeen_),
        maxVocabSize);

    return vocabulary;
  }

  /**
   * A pipeline that maps a single text to a list of tokens.
   *
   * @param tagsToKeep the types of tokens to keep: WORD, PUNCTUATION, etc.
   * @param length     the ngrams length: 1 = unigrams, 2 = bigrams, 3 = trigrams, etc.
   * @return the tokenized text.
   */
  static Function<String, List<String>> tokenizer(Set<String> tagsToKeep, int length) {

    Preconditions.checkArgument(length >= 1, "length must be >= 1");

    TextNormalizer normalizer = new TextNormalizer(true);
    TextTokenizer tokenizer = new TextTokenizer();
    Predicate<Span> filter = span -> tagsToKeep == null || !Sets.intersection(tagsToKeep, span.tags()).isEmpty();

    if (length == 1) {
      return text -> normalizer.andThen(tokenizer).andThen(seq -> View.of(seq).filter(filter).map(Span::text).toList())
          .apply(text);
    }
    return text -> normalizer.andThen(tokenizer).andThen(
        seq -> View.of(seq).filter(filter).map(Span::text).overlappingWindowWithStrictLength(length)
            .map(tks -> Joiner.on('_').join(tks)).toList()).apply(text);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Vocabulary)) {
      return false;
    }
    Vocabulary v = (Vocabulary) obj;
    return Objects.equals(tf_, v.tf_) && Objects.equals(df_, v.df_) && Objects.equals(idx_, v.idx_)
        && nbTermsSeen_ == v.nbTermsSeen_ && nbDocsSeen_ == v.nbDocsSeen_ && isFrozen_ == v.isFrozen_;
  }

  @Override
  public int hashCode() {
    return Objects.hash(tf_, df_, idx_, nbTermsSeen_, nbDocsSeen_, isFrozen_);
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
   * Returns the number of a documents used to build the vocabulary.
   *
   * @return the number of docuemnts seen.
   */
  public int nbDocsSeen() {
    return nbDocsSeen_;
  }

  /**
   * Returns the number of a terms used to build the vocabulary.
   *
   * @return the number of term seen.
   */
  public int nbTermsSeen() {
    return nbTermsSeen_;
  }

  /**
   * Returns all the terms in the current vocabulary.
   *
   * @return the terms.
   */
  public Set<String> terms() {
    return ImmutableSet.copyOf(idx_.keySet());
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

    return tf_.get(term(index(term)));
  }

  /**
   * Returns the term frequency of a given term.
   *
   * @param index the term index in the current vocabulary.
   * @return the term frequency. This value is in [0, #terms].
   */
  public int tf(int index) {

    Preconditions.checkState(isFrozen_, "vocabulary must be frozen");

    return tf_.get(term(index));
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

    return df_.get(term(index(term)));
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

    return df_.get(term(index));
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
   * Returns the inverse document frequency which measures how common a word is among all documents.
   *
   * @param term the term.
   * @return the inverse document frequency.
   */
  public double idf(String term) {
    return 1 + Math.log((double) (1 + nbDocsSeen_) / (double) (1 + df(term)));
  }

  /**
   * Returns the inverse document frequency which measures how common a word is among all documents.
   *
   * @param index the term index in the current vocabulary.
   * @return the inverse document frequency.
   */
  public double idf(int index) {
    return 1 + Math.log((double) (1 + nbDocsSeen_) / (double) (1 + df(index)));
  }

  /**
   * Computes the TF-IDF score of a given term.
   *
   * @param term  the term for which TF-IDF must be computed.
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

    View.of(idx_.keySet())
        .map(term -> idx_.get(term) + "\t" + term + "\t" + tf_.getOrDefault(term, 0) + "\t" + df_.getOrDefault(term, 0))
        .prepend(String.format("# %d %d\nidx\tnormalized_term\ttf\tdf", nbTermsSeen_, nbDocsSeen_))
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
    Preconditions.checkState(!isFrozen_, "vocabulary should not be frozen");

    isFrozen_ = true;
    String stats = View.of(file, true).take(1).toList().get(0);

    int index1 = stats.indexOf(' ');
    int index2 = stats.lastIndexOf(' ');

    nbTermsSeen_ = Integer.parseInt(stats.substring(index1 + 1, index2), 10);
    nbDocsSeen_ = Integer.parseInt(stats.substring(index2 + 1), 10);

    View.of(file, true).drop(1 /* number of terms/docs seen */ + 1/* header */).forEachRemaining(row -> {

      List<String> columns = Splitter.on('\t').trimResults().splitToList(row);
      int idx = Integer.parseInt(columns.get(0), 10);
      String term = columns.get(1);
      int tf = Integer.parseInt(columns.get(2), 10);
      int df = Integer.parseInt(columns.get(3), 10);

      tf_.put(term, tf_.getOrDefault(term, 0) + tf);
      df_.put(term, df_.getOrDefault(term, 0) + df);
      idx_.put(term, idx);
    });
  }

  private void fill(View<List<String>> docs) {

    Preconditions.checkNotNull(docs, "docs should not be null");

    TermsSeen termsSeen = new TermsSeen(20_000);
    docs.map(HashMultiset::create).forEachRemaining(terms -> {

      nbDocsSeen_ += 1;
      AtomicBoolean unkAlreadySeen = new AtomicBoolean(false);

      terms.entrySet().forEach(term -> {

        String termValue = term.getElement();
        int termCount = term.getCount();
        nbTermsSeen_ += termCount;

        if (termsSeen.contains(termValue)) {
          tf_.put(termValue, tf_.getOrDefault(termValue, 0) + termCount);
          df_.put(termValue, df_.getOrDefault(termValue, 0) + 1);
        } else {

          termsSeen.add(termValue);
          tf_.put(tokenUnk_, tf_.getOrDefault(tokenUnk_, 0) + 1);

          if (!unkAlreadySeen.getAndSet(true)) {
            df_.put(tokenUnk_, df_.getOrDefault(tokenUnk_, 0) + 1);
          }
          if (termCount > 1) {
            tf_.put(termValue, tf_.getOrDefault(termValue, 0) + termCount - 1);
            df_.put(termValue, df_.getOrDefault(termValue, 0) + 1);
          }
        }
      });
    });
  }

  private void freeze(int minDocFreq, int maxDocFreq, int maxVocabSize) {

    Preconditions.checkState(!isFrozen_, "vocabulary is already frozen");

    isFrozen_ = true;
    idx_.put(tokenUnk_, idxUnk_);

    df_.entrySet().removeIf(
        freq -> !tokenUnk_.equals(freq.getKey()) && (freq.getValue() < minDocFreq || freq.getValue() > maxDocFreq));
    tf_.entrySet().removeIf(freq -> !tokenUnk_.equals(freq.getKey()) && !df_.containsKey(freq.getKey()));

    Preconditions.checkState(tf_.containsKey(tokenUnk_));
    Preconditions.checkState(df_.containsKey(tokenUnk_));
    Preconditions.checkState(df_.entrySet().size() == tf_.entrySet().size());

    @Var Stream<Map.Entry<String, Integer>> stream = tf_.entrySet().stream().filter(e -> !tokenUnk_.equals(e.getKey()))
        .sorted(
            (e1, e2) -> ComparisonChain.start().compare(e1.getValue(), e2.getValue()).compare(e1.getKey(), e2.getKey())
                .result());

    if (maxVocabSize > 0) {
      stream = stream.limit(maxVocabSize - 1 /* UNK */);
    }

    stream.forEach(token -> {
      if (!idx_.containsKey(token.getKey())) {
        idx_.put(token.getKey(), idx_.size());
      }
    });
  }

  private static final class TermsSeen {

    private final int threshold_;
    private Set<String> set_ = null;
    private BloomFilter<String> bloomFilter_ = null;

    public TermsSeen(int threshold) {

      Preconditions.checkArgument(threshold > 0, "threshold must be > 0");

      threshold_ = threshold;
    }

    boolean contains(String term) {

      Preconditions.checkNotNull(term, "term must not be null");

      return (set_ != null || bloomFilter_ != null) && (bloomFilter_ != null ? bloomFilter_.contains(term)
          : set_.contains(term));
    }

    void add(String term) {

      Preconditions.checkNotNull(term, "term must not be null");
      Preconditions.checkState((set_ == null && bloomFilter_ == null) || set_ != null || bloomFilter_ != null);

      if (bloomFilter_ != null) {
        bloomFilter_.add(term);
      } else {
        if (set_ == null) {
          set_ = new HashSet<>();
        }
        set_.add(term);
        if (set_.size() > threshold_) {
          bloomFilter_ = new BloomFilter<>(0.01, 1_000_000_000);
          set_.forEach(bloomFilter_::add);
          set_ = null;
        }
      }
    }
  }
}