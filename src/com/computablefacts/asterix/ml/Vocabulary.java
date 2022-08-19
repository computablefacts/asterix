package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.Document;
import com.computablefacts.asterix.Generated;
import com.computablefacts.asterix.Span;
import com.computablefacts.asterix.SpanSequence;
import com.computablefacts.asterix.View;
import com.computablefacts.asterix.codecs.JsonCodec;
import com.computablefacts.asterix.codecs.StringCodec;
import com.google.common.annotations.Beta;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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
  private final Multiset<String> tf_ = HashMultiset.create(); // normalized term -> term frequency
  private final Multiset<String> df_ = HashMultiset.create(); // normalized term -> document frequency
  private final BiMap<String, Integer> idx_ = HashBiMap.create(); // one-hot encoding
  private final Map<String, Set<String>> forms_ = new HashMap<>(); // normalized term -> raw terms
  private int nbTermsSeen_ = 0;
  private int nbDocsSeen_ = 0;
  private boolean isFrozen_ = false;

  public Vocabulary() {
  }

  public Vocabulary(File file) {
    load(Preconditions.checkNotNull(file, "file should not be null"));
  }
  
  /**
   * Build a vocabulary from a corpus of documents. To be versatile, this method does not attempt to
   * remove stop words, diacritical marks or even lowercase tokens.
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
  @Generated
  public static void main(String[] args) {

    File file = new File(args[0]);
    double minDocFreq = Double.parseDouble(args[1]);
    double maxDocFreq = Double.parseDouble(args[2]);
    int maxVocabSize = Integer.parseInt(args[3], 10);
    Set<String> includeTags = Sets.newHashSet(
        Splitter.on(',').trimResults().omitEmptyStrings().split(args[4]));
    int ngramLength = Integer.parseInt(args[5], 10);

    Preconditions.checkArgument(0.0 <= minDocFreq && minDocFreq <= 1.0,
        "minDocFreq must be such as 0.0 <= minDocFreq <= 1.0");
    Preconditions.checkArgument(minDocFreq <= maxDocFreq && maxDocFreq <= 1.0,
        "minDocFreq must be such as minDocFreq <= maxDocFreq <= 1.0");
    Preconditions.checkArgument(maxVocabSize > 0, "maxVocabSize must be = -1 or > 0");
    Preconditions.checkArgument(!includeTags.isEmpty(), "includeTags should not be empty");
    Preconditions.checkArgument(ngramLength > 0, "ngramLength must be > 0");

    Vocabulary vocabulary;
    File vocab = new File(
        String.format("%svocabulary-%d.tsv.gz", file.getParent() + File.separator, ngramLength));

    System.out.printf("Dataset is %s\n", file);
    System.out.printf("NGrams length is %d\n", ngramLength);
    System.out.printf("Min. document freq. is %.01f%% of all documents\n", minDocFreq * 100);
    System.out.printf("Max. document freq. is %.01f%% of all documents\n", maxDocFreq * 100);
    System.out.printf("Max. vocab size is %d\n", maxVocabSize);
    System.out.printf("Included tags are %s\n", includeTags);
    System.out.println("Building vocabulary...");

    Stopwatch stopwatch = Stopwatch.createStarted();
    View<SpanSequence> documents = Document.of(file, true).displayProgress(5000)
        .map(doc -> (String) doc.text()).map(new TextTokenizer());
    View<List<String>> ngrams;

    if (ngramLength == 1) {
      ngrams = documents.map(
          seq -> View.of(seq).filter(span -> !Sets.intersection(includeTags, span.tags()).isEmpty())
              .map(Span::text).toList());
    } else {
      ngrams = documents.map(
          seq -> View.of(seq).filter(span -> !Sets.intersection(includeTags, span.tags()).isEmpty())
              .map(Span::text).overlappingWindowWithStrictLength(ngramLength)
              .map(tks -> Joiner.on('_').join(tks)).toList());
    }

    vocabulary = Vocabulary.of(ngrams, minDocFreq, maxDocFreq, maxVocabSize);
    vocabulary.save(vocab);
    stopwatch.stop();

    System.out.printf("Vocabulary built in %d seconds\n", stopwatch.elapsed(TimeUnit.SECONDS));
    System.out.printf("Vocabulary size is %d\n", vocabulary.size());
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
   * @param docs a set of tokenized documents.
   * @param minDocFreq the document-frequency threshold (as a number of documents) under which a
   * term must be excluded.
   * @param maxDocFreq the document-frequency threshold (as a number of documents) above which a
   * term must be excluded.
   * @param maxVocabSize the maximum number of terms to include in the vocabulary.
   * @return a {@link Vocabulary}.
   */
  public static Vocabulary of(View<List<String>> docs, int minDocFreq, int maxDocFreq,
      int maxVocabSize) {

    Preconditions.checkNotNull(docs, "docs should not be null");
    Preconditions.checkArgument(0 <= minDocFreq, "minDocFreq must be >= 0");
    Preconditions.checkArgument(minDocFreq <= maxDocFreq, "minDocFreq must be >= minDocFreq ");
    Preconditions.checkArgument(maxVocabSize == -1 || maxVocabSize > 0,
        "maxVocabSize must be = -1 or > 0");

    Vocabulary vocabulary = new Vocabulary();
    vocabulary.fill(docs);
    vocabulary.freeze(minDocFreq, maxDocFreq, maxVocabSize);

    return vocabulary;
  }

  /**
   * Build a vocabulary from a set of tokenized documents.
   *
   * @param docs a set of tokenized documents.
   * @param minDocFreq the document-frequency threshold (as a percentage of the total number of
   * documents) under which a term must be excluded.
   * @param maxDocFreq the document-frequency threshold (as a percentage of the total number of
   * documents) above which a term must be excluded.
   * @param maxVocabSize the maximum number of terms to include in the vocabulary.
   * @return a {@link Vocabulary}.
   */
  public static Vocabulary of(View<List<String>> docs, double minDocFreq, double maxDocFreq,
      int maxVocabSize) {

    Preconditions.checkNotNull(docs, "docs should not be null");
    Preconditions.checkArgument(0.0 <= minDocFreq && minDocFreq <= 1.0,
        "minDocFreq must be such as 0.0 <= minDocFreq <= 1.0");
    Preconditions.checkArgument(minDocFreq <= maxDocFreq && maxDocFreq <= 1.0,
        "minDocFreq must be such as minDocFreq <= maxDocFreq <= 1.0");
    Preconditions.checkArgument(maxVocabSize == -1 || maxVocabSize > 0,
        "maxVocabSize must be = -1 or > 0");

    Vocabulary vocabulary = new Vocabulary();
    vocabulary.fill(docs);
    vocabulary.freeze((int) (minDocFreq * vocabulary.nbDocsSeen_),
        (int) (maxDocFreq * vocabulary.nbDocsSeen_), maxVocabSize);

    return vocabulary;
  }

  public static String normalize(String term) {

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
        && Objects.equals(forms_, v.forms_) && nbTermsSeen_ == v.nbTermsSeen_
        && nbDocsSeen_ == v.nbDocsSeen_ && isFrozen_ == v.isFrozen_;
  }

  @Override
  public int hashCode() {
    return Objects.hash(tf_, df_, idx_, forms_, nbTermsSeen_, nbDocsSeen_, isFrozen_);
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
    return 1 + Math.log((double) (1 + nbDocsSeen_) / (double) (1 + df(term)));
  }

  /**
   * Returns the inverse document frequency which measures how common a word is among all
   * documents.
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
            + "\t" + JsonCodec.asString(forms_.getOrDefault(term, new HashSet<>()))).prepend(
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
          Collection<?> forms = JsonCodec.asCollectionOfUnknownType(columns.get(4));

          tf_.add(term, tf);
          df_.add(term, df);
          idx_.put(term, idx);
          forms_.put(term, Sets.newHashSet((Collection<String>) forms));
        });
  }

  /**
   * Remove terms from the vocabulary and reindex it.
   *
   * @param idxsToRemove the terms to remove.
   */
  public void reduce(Set<Integer> idxsToRemove) {

    Preconditions.checkNotNull(idxsToRemove, "idxsToRemove should not be null");
    Preconditions.checkState(isFrozen_, "vocabulary must be frozen");

    Vocabulary vocabulary = new Vocabulary();
    vocabulary.nbTermsSeen_ = nbTermsSeen_;
    vocabulary.nbDocsSeen_ = nbDocsSeen_;

    @Var int newIdx = 0;
    List<Integer> idxs = idx_.values().stream().sorted().collect(Collectors.toList());

    for (int idx : idxs) {
      if (!idxsToRemove.contains(idx)) {

        String term = term(idx);

        vocabulary.idx_.put(term, newIdx++);
        vocabulary.tf_.add(term, tf_.count(term));
        vocabulary.df_.add(term, df_.count(term));
        vocabulary.forms_.put(term, forms_.get(term));
      }
    }

    clear();

    isFrozen_ = true;
    nbTermsSeen_ = vocabulary.nbTermsSeen_;
    nbDocsSeen_ = vocabulary.nbDocsSeen_;
    idx_.putAll(vocabulary.idx_);
    tf_.addAll(vocabulary.tf_);
    df_.addAll(vocabulary.df_);
    forms_.putAll(vocabulary.forms_);
  }

  private void fill(View<List<String>> docs) {

    Preconditions.checkNotNull(docs, "docs should not be null");

    Set<String> normalizedTermsSeen = new HashSet<>();

    docs.map(HashMultiset::create).forEachRemaining(terms -> {

      nbDocsSeen_ += 1;
      AtomicBoolean unkAlreadySeen = new AtomicBoolean(false);

      terms.entrySet().forEach(term -> {

        String normalizedTerm = normalize(term.getElement());
        int termCount = term.getCount();

        if (!forms_.containsKey(normalizedTerm)) {
          forms_.put(normalizedTerm, new HashSet<>());
        }

        forms_.get(normalizedTerm).add(term.getElement());
        nbTermsSeen_ += termCount;

        if (normalizedTermsSeen.contains(normalizedTerm)) {
          tf_.add(normalizedTerm, termCount);
          df_.add(normalizedTerm);
        } else {

          normalizedTermsSeen.add(normalizedTerm);
          tf_.add(tokenUnk_);

          if (!unkAlreadySeen.getAndSet(true)) {
            df_.add(tokenUnk_);
          }
          if (termCount > 1) {
            tf_.add(normalizedTerm, termCount - 1);
            df_.add(normalizedTerm);
          }
        }
      });
    });
  }

  private void freeze(int minDocFreq, int maxDocFreq, int maxVocabSize) {

    Preconditions.checkState(!isFrozen_, "vocabulary is already frozen");

    isFrozen_ = true;
    idx_.put(tokenUnk_, idxUnk_);

    df_.entrySet()
        .removeIf(freq -> !tokenUnk_.equals(freq.getElement()) && freq.getCount() < minDocFreq);
    df_.entrySet()
        .removeIf(freq -> !tokenUnk_.equals(freq.getElement()) && freq.getCount() > maxDocFreq);
    forms_.entrySet().removeIf(
        freq -> !tokenUnk_.equals(freq.getKey()) && (!tf_.contains(freq.getKey()) || !df_.contains(
            freq.getKey())));
    df_.entrySet().removeIf(
        freq -> !tokenUnk_.equals(freq.getElement()) && !forms_.containsKey(freq.getElement()));
    tf_.entrySet().removeIf(
        freq -> !tokenUnk_.equals(freq.getElement()) && !forms_.containsKey(freq.getElement()));

    Preconditions.checkState(!forms_.containsKey(tokenUnk_));
    Preconditions.checkState(tf_.contains(tokenUnk_));
    Preconditions.checkState(df_.contains(tokenUnk_));
    Preconditions.checkState(tf_.elementSet().size() == forms_.keySet().size() + 1);
    Preconditions.checkState(df_.elementSet().size() == forms_.keySet().size() + 1);

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