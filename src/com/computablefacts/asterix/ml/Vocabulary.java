package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.BloomFilter;
import com.computablefacts.asterix.Document;
import com.computablefacts.asterix.Span;
import com.computablefacts.asterix.TextNormalizer;
import com.computablefacts.asterix.TextTokenizer;
import com.computablefacts.asterix.View;
import com.computablefacts.asterix.console.Observations;
import com.google.common.annotations.Beta;
import com.google.common.base.CharMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import com.google.common.collect.BiMap;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AtomicDouble;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.io.File;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Comparator;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.concurrent.NotThreadSafe;

@Deprecated
@NotThreadSafe
@CheckReturnValue
final public class Vocabulary {

  private final static String tokenUnk_ = "<UNK>";
  private final static int idxUnk_ = 0;
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
   * An implementation of the RAKE algorithm.
   *
   * @param texts     the source texts.
   * @param keepToken a function that returns true iif a token must be kept and false otherwise (optional).
   * @return the tokens and ngrams weights.
   */
  @Beta
  public static Map.Entry<Map<String, Double>, Map<String, Double>> rake(View<List<String>> texts,
      Predicate<String> keepToken) {

    Preconditions.checkNotNull(texts, "texts should not be null");

    Map<String, Multiset<String>> matrix = rakeSplit(texts, keepToken);
    AtomicDouble minTokenWeight = new AtomicDouble(Double.MAX_VALUE);
    AtomicDouble maxTokenWeight = new AtomicDouble(Double.MIN_VALUE);
    Map<String, Double> weightedTokens = new HashMap<>();

    matrix.keySet().forEach(tkn -> {

      double deg = matrix.get(tkn).size() + matrix.get(tkn).elementSet().stream()
          .mapToDouble(ngram -> CharMatcher.is('_').countIn(ngram)).sum();
      double freq = matrix.get(tkn).size();
      double score = deg / freq;

      if (score < minTokenWeight.doubleValue()) {
        minTokenWeight.set(score);
      }
      if (score > maxTokenWeight.doubleValue()) {
        maxTokenWeight.set(score);
      }
      weightedTokens.put(tkn, score);
    });

    AtomicDouble minNGramWeight = new AtomicDouble(Double.MAX_VALUE);
    AtomicDouble maxNGramWeight = new AtomicDouble(Double.MIN_VALUE);
    Map<String, Double> weightedNGrams = new HashMap<>();

    View.of(matrix.values()).flatten(ngrams -> View.of(ngrams.elementSet())).forEachRemaining(ngram -> {

      double score = Splitter.on('_').splitToStream(ngram).mapToDouble(weightedTokens::get).sum();

      if (score < minNGramWeight.doubleValue()) {
        minNGramWeight.set(score);
      }
      if (score > maxNGramWeight.doubleValue()) {
        maxNGramWeight.set(score);
      }
      weightedNGrams.put(ngram, score);
    });
/*
    weightedTokens.entrySet().forEach(e -> e.setValue(
        (e.getValue() - minTokenWeight.doubleValue()) / (maxTokenWeight.doubleValue() - minTokenWeight.doubleValue())));

    weightedNGrams.entrySet().forEach(e -> e.setValue(
        (e.getValue() - minNGramWeight.doubleValue()) / (maxNGramWeight.doubleValue() - minNGramWeight.doubleValue())));
*/
    return new SimpleImmutableEntry<>(weightedTokens, weightedNGrams);
  }

  @Beta
  public static Map<String, Multiset<String>> rakeSplit(View<List<String>> texts, Predicate<String> keepToken) {

    Preconditions.checkNotNull(texts, "texts should not be null");

    Map<String, Multiset<String>> matrix = new HashMap<>();

    texts.forEachRemaining(tokens -> {

      List<String> subTokens = keepToken == null ? tokens : new ArrayList<>();

      for (int k = keepToken == null ? tokens.size() : 0; k < tokens.size(); k++) {
        if (keepToken.test(tokens.get(k))) {
          subTokens.add(tokens.get(k));
        } else {
          String ngram = Joiner.on('_').join(subTokens);
          for (String tkn : subTokens) {
            if (!matrix.containsKey(tkn)) {
              matrix.put(tkn, HashMultiset.create());
            }
            matrix.get(tkn).add(ngram);
          }
          subTokens.clear();
        }
      }
      if (!subTokens.isEmpty()) {
        String ngram = Joiner.on('_').join(subTokens);
        for (String tkn : subTokens) {
          if (!matrix.containsKey(tkn)) {
            matrix.put(tkn, HashMultiset.create());
          }
          matrix.get(tkn).add(ngram);
        }
      }
    });
    return matrix;
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
   * <li>{@code args[6]} the prefix length after which a token must be chopped (optional, default is 6).</li>
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
    int chopAt = args.length < 7 ? 6 : Integer.parseInt(args[6], 10);

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
      observations.add(String.format("Chopping tokens after %d characters.", chopAt));
      observations.add(String.format("Min. document freq. is %.01f%% of all documents.", minDocFreq * 100));
      observations.add(String.format("Max. document freq. is %.01f%% of all documents.", maxDocFreq * 100));
      observations.add(String.format("Max. vocab size is %d.", maxVocabSize));
      observations.add(String.format("Included tags are %s.", includeTags));
      observations.add("Building vocabulary...");
      Stopwatch stopwatch = Stopwatch.createStarted();

      Predicate<Span> keepSpan = span -> !Sets.intersection(span.tags(), includeTags).isEmpty();
      Function<String, List<String>> tokenizer = tokenizer(keepSpan, ngramsLength, chopAt);
      View<List<String>> tokens = Document.of(file, true).displayProgress(10_000)
          .flatten(doc -> View.of(Splitter.on('\f').splitToStream((String) doc.text())))
          .filter(page -> !Strings.isNullOrEmpty(page)).map(tokenizer);
      Vocabulary vocabulary = of(tokens, minDocFreq, maxDocFreq, maxVocabSize);
      vocabulary.save(
          new File(String.format("%svocabulary-%dgrams.tsv.gz", file.getParent() + File.separator, ngramsLength)));

      stopwatch.stop();
      observations.add(String.format("Vocabulary built in %d seconds.", stopwatch.elapsed(TimeUnit.SECONDS)));
      observations.add(String.format("Vocabulary size is %d.", vocabulary.size()));

      if (ngramsLength == 1) {

        observations.add("Extracting keywords...");
        stopwatch.reset().start();

        Set<String> stopwords = Sets.newHashSet(vocabulary.stopwords(50));
        Predicate<String> isStopword = tkn -> tkn.length() == 1 || stopwords.contains(tkn)
            || vocabulary.index(tkn) == idxUnk_;
        Map.Entry<Map<String, Double>, Map<String, Double>> rake = rake(Document.of(file, true).displayProgress(10_000)
            .flatten(doc -> View.of(Splitter.on('\f').splitToStream((String) doc.text())))
            .filter(page -> !Strings.isNullOrEmpty(page)).map(tokenizer), isStopword);

        List<Map.Entry<String, Double>> keywords = View.of(rake.getValue().entrySet()).toSortedList(
            Comparator.comparingDouble((Map.Entry<String, Double> e) -> e.getValue()).reversed()
                .thenComparing(Map.Entry::getKey));

        View.of(keywords).toFile(keyword -> keyword.getKey() + "\t" + keyword.getValue(),
            new File(String.format("%sweighted-keywords.tsv.gz", file.getParent() + File.separator)), false, true);

        stopwatch.stop();
        observations.add(String.format("Keywords extracted in %d seconds.", stopwatch.elapsed(TimeUnit.SECONDS)));
        observations.add(String.format("The number of extracted keywords is %d.", keywords.size()));
      }
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
        "maxDocFreq must be such as minDocFreq <= maxDocFreq <= 1.0");
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
   * @param keepSpan the types of tokens to keep: WORD, PUNCTUATION, etc. (optional)
   * @param length   the ngrams length: 1 = unigrams, 2 = bigrams, 3 = trigrams, etc.
   * @param chopAt   only keep the first `chopAt` characters of each token. (optional)
   * @return the tokenized text.
   */
  @Deprecated
  static Function<String, List<String>> tokenizer(Predicate<Span> keepSpan, int length, int chopAt) {
    return tokenizer(keepSpan, null, null, length, chopAt);
  }

  /**
   * A pipeline that maps a single text to a list of tokens.
   *
   * @param keepSpan  the types of tokens to keep: WORD, PUNCTUATION, etc. (optional)
   * @param keepToken the tokens to keep: stopwords, etc. (optional)
   * @param keepNGram the ngrams to keep (optional)
   * @param length    the ngrams length: 1 = unigrams, 2 = bigrams, 3 = trigrams, etc.
   * @param chopAt    only keep the first `chopAt` characters of each token. (optional)
   * @return the tokenized text.
   */
  static Function<String, List<String>> tokenizer(Predicate<Span> keepSpan, Predicate<String> keepToken,
      Predicate<List<String>> keepNGram, int length, int chopAt) {

    Preconditions.checkArgument(length >= 1, "length must be >= 1");

    TextNormalizer normalizer = new TextNormalizer(true);
    TextTokenizer tokenizer = new TextTokenizer();
    Function<String, String> chopToken = tkn -> chopAt <= 0 ? tkn : tkn.substring(0, Math.min(chopAt, tkn.length()));
    Predicate<Span> newKeepTag = span -> keepSpan == null || keepSpan.test(span);
    Predicate<String> newKeepToken = tkn -> keepToken == null || keepToken.test(tkn);
    Predicate<List<String>> newKeepNGram = tkns -> keepNGram == null || keepNGram.test(tkns);

    if (length == 1) {
      return text -> normalizer.andThen(tokenizer).andThen(seq -> {
        List<String> ngram = View.of(seq).filter(newKeepTag).map(Span::text).map(chopToken).filter(newKeepToken)
            .toList();
        return newKeepNGram.test(ngram) ? ngram : Lists.<String>newArrayList();
      }).apply(text);
    }
    return text -> normalizer.andThen(tokenizer).andThen(
        seq -> View.of(seq).filter(newKeepTag).map(Span::text).map(chopToken).filter(newKeepToken)
            .overlappingWindowWithStrictLength(length).filter(newKeepNGram).map(tks -> Joiner.on('_').join(tks))
            .toList()).apply(text);
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
   * Returns the inverse document frequency which measures how common a word is among all documents. The fewer documents
   * the term appears in, the higher the idf value.
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
   * Compiles a list of possible stopwords from the current vocabulary.
   *
   * @param max the maximum number of words to return.
   * @return a list of stopwords.
   */
  public List<String> stopwords(int max) {
    return terms().stream().sorted(Comparator.comparing(this::idf)).limit(max).collect(Collectors.toList());
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