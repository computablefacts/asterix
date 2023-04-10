package com.computablefacts.asterix.nlp;

import com.computablefacts.asterix.Document;
import com.computablefacts.asterix.RandomString;
import com.computablefacts.asterix.View;
import com.computablefacts.junon.Fact;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.io.File;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@CheckReturnValue
final public class DocSetLabeler extends AbstractDocSetLabeler {

  private final Vocabulary vocabulary_;
  private final Function<String, List<String>> tokenizer_;
  private final Set<String> stopwords_;
  private final Map<String, Multiset<String>> cache_ = new ConcurrentHashMap<>();

  public DocSetLabeler(Vocabulary vocabulary, Function<String, List<String>> tokenizer) {

    Preconditions.checkNotNull(vocabulary, "vocabulary should not be null");
    Preconditions.checkNotNull(tokenizer, "tokenizer should not be null");

    tokenizer_ = tokenizer;
    vocabulary_ = vocabulary;
    stopwords_ = Sets.newHashSet(vocabulary.stopwords(50));
  }

  /**
   * Infer keywords from a set of positive matches and a set of negative matches.
   *
   * <ul>
   * <li>{@code args[0]} the corpus of documents as a gzipped JSONL file.</li>
   * <li>{@code args[1]} the corpus of facts as a gzipped JSONL file.</li>
   * <li>{@code args[2]} the ngrams length: 1 = unigrams, 2 = bigrams, 3 = trigrams, etc. (optional, default is 1)</li>
   * <li>{@code args[3]} the threshold (document frequency) under which a token must be excluded from the vocabulary (optional, default is 1%).</li>
   * <li>{@code args[4]} the threshold (document frequency) above which a token must be excluded from the vocabulary (optional, default is 99%).</li>
   * <li>{@code args[5]} the maximum size of the {@link Vocabulary} (optional, default is 100 000).</li>
   * <li>{@code args[6]} the types of tokens to keep: WORD, PUNCTUATION, etc. (optional, default is {WORD, NUMBER, TERMINAL_MARK})</li>
   * <li>{@code args[7]} the prefix length after which a token must be chopped (optional, default is 9).</li>
   * </ul>
   */
  public static void main(String[] args) {

    File documents = new File(args[0]);
    File facts = new File(args[1]);
    int ngramsLength = args.length < 3 ? 1 : Integer.parseInt(args[2], 10);
    double minDocFreq = args.length < 4 ? 0.01 : Double.parseDouble(args[3]);
    double maxDocFreq = args.length < 5 ? 0.99 : Double.parseDouble(args[4]);
    int maxVocabSize = args.length < 6 ? 100_000 : Integer.parseInt(args[5], 10);
    Set<String> includeTags = args.length < 7 ? Sets.newHashSet("WORD", "NUMBER", "TERMINAL_MARK")
        : Sets.newHashSet(Splitter.on(',').trimResults().omitEmptyStrings().split(args[6]));
    int chopAt = args.length < 8 ? 9 : Integer.parseInt(args[7], 10);

    Preconditions.checkArgument(documents.exists(), "missing documents");
    Preconditions.checkArgument(facts.exists(), "missing facts");
    Preconditions.checkArgument(ngramsLength > 0, "ngramLength must be > 0");
    Preconditions.checkArgument(0.0 <= minDocFreq && minDocFreq <= 1.0,
        "minDocFreq must be such as 0.0 <= minDocFreq <= 1.0");
    Preconditions.checkArgument(minDocFreq <= maxDocFreq && maxDocFreq <= 1.0,
        "maxDocFreq must be such as minDocFreq <= maxDocFreq <= 1.0");
    Preconditions.checkArgument(maxVocabSize > 0, "maxVocabSize must be = -1 or > 0");
    Preconditions.checkArgument(!includeTags.isEmpty(), "includeTags should not be empty");

    File fileVocabulary = new File(String.format("%s/vocabulary-%dgrams.tsv.gz", documents.getParent(), ngramsLength));
    Vocabulary vocabulary = new Vocabulary();

    if (!fileVocabulary.exists()) {
      String[] argz = new String[]{documents.getAbsolutePath(), Integer.toString(ngramsLength, 10),
          Double.toString(minDocFreq), Double.toString(maxDocFreq), Integer.toString(maxVocabSize, 10),
          Joiner.on(',').join(includeTags), Integer.toString(chopAt, 10)};
      Vocabulary.main(argz);
    }

    vocabulary.load(fileVocabulary);

    File fileGoldLabels = new File(String.format("%s/gold-labels.jsonl.gz", facts.getParent()));
    List<GoldLabel> goldLabels;

    if (fileGoldLabels.exists()) {
      goldLabels = GoldLabel.load(fileGoldLabels).toList();
    } else {

      RandomString randomString = new RandomString(5);
      goldLabels = Document.of(documents, facts).displayProgress(10_000).flatten(doc -> {

        // Load user-defined gold labels
        List<GoldLabel> gls = View.of(doc.facts()).map(GoldLabel::fromFact).toList();

        // Load synthetic gold labels
        Set<String> labels = View.of(doc.facts()).map(Fact::type).toSet();

        for (String label : labels) {

          Set<Integer> pagesIdx = View.of(doc.facts()).filter(fact -> fact.isValid() && label.equals(fact.type()))
              .map(fact -> fact.provenance().page()).toSet();

          if (!pagesIdx.isEmpty()) {

            List<String> pages = Splitter.on('\f').splitToList((String) doc.text());

            for (int i = 0; i < pages.size(); i++) {
              if (!pagesIdx.contains(i + 1) /* pages are 1 based */) {

                String id = randomString.nextString();
                String data = pages.get(i);
                boolean isTrueNegative = true;
                boolean isTruePositive = false;
                boolean isFalseNegative = false;
                boolean isFalsePositive = false;

                if (!Strings.isNullOrEmpty(data)) {
                  gls.add(
                      new GoldLabel(id, label, data, isTrueNegative, isTruePositive, isFalseNegative, isFalsePositive));
                }
              }
            }
          }
        }
        return View.of(gls);
      }).toList();

      GoldLabel.save(new File(String.format("%s/gold-labels.jsonl.gz", facts.getParent())), View.of(goldLabels));
    }

    int nbCandidatesToConsider = 50; // TODO : move as parameter?
    int nbLabelsToReturn = 25; // TODO : move as parameter?

    @Var View<Map.Entry<String, Map.Entry<String, Double>>> keywords = null;
    Set<String> labels = View.of(goldLabels).map(GoldLabel::label).toSet();

    for (String label : labels) {

      Set<String> ok = View.of(goldLabels).filter(gl -> label.equals(gl.label()))
          .filter(gl -> gl.isTruePositive() || gl.isFalseNegative()).map(GoldLabel::data).toSet();
      Set<String> ko = View.of(goldLabels).filter(gl -> label.equals(gl.label()))
          .filter(gl -> gl.isFalsePositive() || gl.isTrueNegative()).map(GoldLabel::data).toSet();

      Function<String, List<String>> tokenize = Vocabulary.tokenizer(includeTags, chopAt, ngramsLength);
      DocSetLabeler docSetLabeler = new DocSetLabeler(vocabulary, tokenize);
      List<Map.Entry<String, Double>> candidates = docSetLabeler.labels(Lists.newArrayList(ok), Lists.newArrayList(ko),
          nbCandidatesToConsider, nbLabelsToReturn);

      if (keywords == null) {
        keywords = View.of(candidates).map(k -> new SimpleImmutableEntry<>(label, k));
      } else {
        keywords = keywords.concat(View.of(candidates).map(k -> new SimpleImmutableEntry<>(label, k)));
      }
      System.out.println(label + " -> " + candidates);
    }

    save(new File(String.format("%s/keywords-%dgrams.tsv.gz", facts.getParent(), ngramsLength)), keywords);
  }

  /**
   * Save keywords to a gzipped TSV file.
   *
   * @param file     the output file.
   * @param keywords the keywords to save.
   */
  public static void save(File file, View<Map.Entry<String, Map.Entry<String, Double>>> keywords) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(!file.exists(), "file already exists : %s", file);
    Preconditions.checkNotNull(keywords, "keywords must be frozen");

    keywords.map(
            keyword -> keyword.getKey() + "\t" + keyword.getValue().getKey() + "\t" + keyword.getValue().getValue())
        .prepend("label\tkeyword\tweight").toFile(file, false, true);
  }

  /**
   * Load keywords from a gzipped TSV file.
   *
   * @param file the input file.
   * @return a set of keywords.
   */
  public static View<Map.Entry<String, Map.Entry<String, Double>>> load(File file) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file file does not exist : %s", file);

    return View.of(file, true).skip(1 /* header */).map(row -> {

      List<String> columns = Splitter.on('\t').trimResults().splitToList(row);
      String lbl = columns.get(0);
      String keyword = columns.get(1);
      Double weight = Double.valueOf(columns.get(2));

      return new SimpleImmutableEntry<>(lbl, new SimpleImmutableEntry<>(keyword, weight));
    });
  }

  /**
   * Load a given set of keywords from a gzipped TSV file.
   *
   * @param file  the input file.
   * @param label the specific label to load.
   * @return a set of keywords.
   */
  public static View<Map.Entry<String, Double>> load(File file, String label) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file file does not exist : %s", file);
    Preconditions.checkNotNull(label, "label should not be null");

    return View.of(file, true).skip(1 /* header */).map(row -> {

      List<String> columns = Splitter.on('\t').trimResults().splitToList(row);
      String lbl = columns.get(0);
      String keyword = columns.get(1);
      Double weight = Double.valueOf(columns.get(2));

      return new SimpleImmutableEntry<>(lbl, new SimpleImmutableEntry<>(keyword, weight));
    }).filter(keyword -> label.equals(keyword.getKey())).map(SimpleImmutableEntry::getValue);
  }

  @Override
  protected Multiset<String> candidates(String text) {
    if (!cache_.containsKey(text)) {
      cache_.put(text, HashMultiset.create(
          View.of(text).map(tokenizer_).flatten(View::of).filter(tkn -> !stopwords_.contains(tkn)).toList()));
    }
    return cache_.get(text);
  }

  @Override
  protected double computeX(String text, String candidate, int count) {
    return (double) count / (double) cache_.get(text).size();
  }

  @Override
  protected double computeY(String text, String candidate, int count) {
    return 1.0 / (1.0 + Math.exp(-1.0 * vocabulary_.tfIdf(candidate, count)));
  }
}
