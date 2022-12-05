package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.DocumentTest;
import com.computablefacts.asterix.IO;
import com.computablefacts.asterix.Span;
import com.computablefacts.asterix.TextNormalizer;
import com.computablefacts.asterix.TextTokenizer;
import com.computablefacts.asterix.View;
import com.computablefacts.asterix.codecs.JsonCodec;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.Var;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Assert;
import org.junit.Test;

@Deprecated
public class VocabularyTest {

  @Test
  public void testEqualsAndHashCode() {
    EqualsVerifier.forClass(Vocabulary.class).withIgnoredFields("tokenUnk_", "idxUnk_")
        .suppress(Warning.NONFINAL_FIELDS).verify();
  }

  /*
    @Test
    public void testNormalize() {
      Assert.assertEquals("[vV][éÉeE][hH][iI][cC][uU][lL][eE]", Vocabulary.normalize("véhicule"));
      Assert.assertEquals("[nN][äÄaA][hH][eE]", Vocabulary.normalize("Nähe"));
      Assert.assertEquals("\\[OK\\]", Vocabulary.normalize("[OK]"));
      Assert.assertEquals("Straße", Vocabulary.normalize("Straße"));
    }
  */

  @Test
  public void testVocabulary() {

    Vocabulary vocabulary = vocabularyFilteredByPercentages();

    Assert.assertEquals(10, vocabulary.size());

    Assert.assertEquals("<UNK>", vocabulary.term(0));
    Assert.assertEquals(0, vocabulary.index("<UNK>"));

    Assert.assertEquals("-", vocabulary.term(1));
    Assert.assertEquals(1, vocabulary.index("-"));

    Assert.assertEquals("address", vocabulary.term(2));
    Assert.assertEquals(2, vocabulary.index("address"));

    Assert.assertEquals("in", vocabulary.term(3));
    Assert.assertEquals(3, vocabulary.index("in"));

    Assert.assertEquals("most", vocabulary.term(4));
    Assert.assertEquals(4, vocabulary.index("most"));

    Assert.assertEquals("popular", vocabulary.term(5));
    Assert.assertEquals(5, vocabulary.index("popular"));

    Assert.assertEquals("with", vocabulary.term(6));
    Assert.assertEquals(6, vocabulary.index("with"));

    Assert.assertEquals("yahoo", vocabulary.term(7));
    Assert.assertEquals(7, vocabulary.index("yahoo"));

    Assert.assertEquals("’", vocabulary.term(8));
    Assert.assertEquals(8, vocabulary.index("’"));

    Assert.assertEquals(",", vocabulary.term(9));
    Assert.assertEquals(9, vocabulary.index(","));
  }

  @Test
  public void testPartialAsPercentageVsFullVocabulary() {

    Vocabulary sample = vocabularyFilteredByPercentages();
    Vocabulary full = vocabulary();

    for (int i = 0; i < sample.size(); i++) {
      Assert.assertEquals(sample.term(i), full.term(i));
      Assert.assertEquals(sample.tf(i), full.tf(i));
      Assert.assertEquals(sample.df(i), full.df(i));
      Assert.assertEquals(sample.ntf(i), full.ntf(i), 0.000001);
      Assert.assertEquals(sample.ndf(i), full.ndf(i), 0.000001);
    }
  }

  @Test
  public void testPartialAsNumberVsFullVocabulary() {

    Vocabulary sample = vocabularyFilteredByValues();
    Vocabulary full = vocabulary();

    for (int i = 0; i < sample.size(); i++) {
      Assert.assertEquals(sample.term(i), full.term(i));
      Assert.assertEquals(sample.tf(i), full.tf(i));
      Assert.assertEquals(sample.df(i), full.df(i));
      Assert.assertEquals(sample.ntf(i), full.ntf(i), 0.000001);
      Assert.assertEquals(sample.ndf(i), full.ndf(i), 0.000001);
    }
  }

  @Test
  public void testTermFrequency() {

    Vocabulary vocabulary = vocabularyFilteredByPercentages();

    Assert.assertEquals(10, vocabulary.size());

    Assert.assertEquals(64, vocabulary.tf(0));
    Assert.assertEquals(64, vocabulary.tf("<UNK>"));

    Assert.assertEquals(1, vocabulary.tf(1));
    Assert.assertEquals(1, vocabulary.tf("-"));

    Assert.assertEquals(1, vocabulary.tf(2));
    Assert.assertEquals(1, vocabulary.tf("address"));

    Assert.assertEquals(1, vocabulary.tf(3));
    Assert.assertEquals(1, vocabulary.tf("in"));
  }

  @Test
  public void testDocumentFrequency() {

    Vocabulary vocabulary = vocabularyFilteredByPercentages();

    Assert.assertEquals(10, vocabulary.size());

    Assert.assertEquals(3, vocabulary.df(0));
    Assert.assertEquals(3, vocabulary.df("<UNK>"));

    Assert.assertEquals(1, vocabulary.df(1));
    Assert.assertEquals(1, vocabulary.df("-"));

    Assert.assertEquals(1, vocabulary.df(2));
    Assert.assertEquals(1, vocabulary.df("address"));

    Assert.assertEquals(1, vocabulary.df(3));
    Assert.assertEquals(1, vocabulary.df("in"));
  }

  @Test
  public void testNormalizedTermFrequency() {

    Vocabulary vocabulary = vocabularyFilteredByPercentages();

    Assert.assertEquals(10, vocabulary.size());

    Assert.assertEquals(0.7441860465116279, vocabulary.ntf(0), 0.000001);
    Assert.assertEquals(0.7441860465116279, vocabulary.ntf("<UNK>"), 0.000001);

    Assert.assertEquals(0.011627906976744186, vocabulary.ntf(1), 0.000001);
    Assert.assertEquals(0.011627906976744186, vocabulary.ntf("-"), 0.000001);

    Assert.assertEquals(0.011627906976744186, vocabulary.ntf(2), 0.000001);
    Assert.assertEquals(0.011627906976744186, vocabulary.ntf("address"), 0.000001);

    Assert.assertEquals(0.011627906976744186, vocabulary.ntf(3), 0.000001);
    Assert.assertEquals(0.011627906976744186, vocabulary.ntf("in"), 0.000001);
  }

  @Test
  public void testNormalizedDocumentFrequency() {

    Vocabulary vocabulary = vocabularyFilteredByPercentages();

    Assert.assertEquals(10, vocabulary.size());

    Assert.assertEquals(1.0, vocabulary.ndf(0), 0.000001);
    Assert.assertEquals(1.0, vocabulary.ndf("<UNK>"), 0.000001);

    Assert.assertEquals(0.3333333333333333, vocabulary.ndf(1), 0.000001);
    Assert.assertEquals(0.3333333333333333, vocabulary.ndf("-"), 0.000001);

    Assert.assertEquals(0.3333333333333333, vocabulary.ndf(2), 0.000001);
    Assert.assertEquals(0.3333333333333333, vocabulary.ndf("address"), 0.000001);

    Assert.assertEquals(0.3333333333333333, vocabulary.ndf(3), 0.000001);
    Assert.assertEquals(0.3333333333333333, vocabulary.ndf("in"), 0.000001);
  }

  @Test
  public void testInverseDocumentFrequency() {

    Vocabulary vocabulary = vocabularyFilteredByPercentages();

    Assert.assertEquals(10, vocabulary.size());

    Assert.assertEquals(1.0, vocabulary.idf(0), 0.000001);
    Assert.assertEquals(1.0, vocabulary.idf("<UNK>"), 0.000001);

    Assert.assertEquals(1.6931471805599454, vocabulary.idf(1), 0.000001);
    Assert.assertEquals(1.6931471805599454, vocabulary.idf("-"), 0.000001);

    Assert.assertEquals(1.6931471805599454, vocabulary.idf(2), 0.000001);
    Assert.assertEquals(1.6931471805599454, vocabulary.idf("address"), 0.000001);

    Assert.assertEquals(1.6931471805599454, vocabulary.idf(3), 0.000001);
    Assert.assertEquals(1.6931471805599454, vocabulary.idf("in"), 0.000001);
  }

  @Test
  public void testTfIdf() {

    Vocabulary vocabulary = vocabularyFilteredByPercentages();

    Assert.assertEquals(10, vocabulary.size());

    Assert.assertEquals(1.0, vocabulary.tfIdf(0, 1), 0.000001);
    Assert.assertEquals(1.0, vocabulary.tfIdf("<UNK>", 1), 0.000001);

    Assert.assertEquals(1.6931471805599454, vocabulary.tfIdf(1, 1), 0.000001);
    Assert.assertEquals(1.6931471805599454, vocabulary.tfIdf("-", 1), 0.000001);

    Assert.assertEquals(1.6931471805599454, vocabulary.tfIdf(2, 1), 0.000001);
    Assert.assertEquals(1.6931471805599454, vocabulary.tfIdf("address", 1), 0.000001);

    Assert.assertEquals(1.6931471805599454, vocabulary.tfIdf(3, 1), 0.000001);
    Assert.assertEquals(1.6931471805599454, vocabulary.tfIdf("in", 1), 0.000001);
  }

  @Test
  public void testSaveThenLoadVocabulary() throws Exception {

    String path = java.nio.file.Files.createTempDirectory("test-").toFile().getPath();
    File file = new File(path + File.separator + "vocab.tsv.gz");

    Vocabulary vocabulary = vocabularyFilteredByPercentages();
    vocabulary.save(file);

    Assert.assertEquals(10, vocabulary.size());

    Assert.assertEquals("<UNK>", vocabulary.term(0));
    Assert.assertEquals(0, vocabulary.index("<UNK>"));

    Assert.assertEquals("-", vocabulary.term(1));
    Assert.assertEquals(1, vocabulary.index("-"));

    Assert.assertEquals("address", vocabulary.term(2));
    Assert.assertEquals(2, vocabulary.index("address"));

    Assert.assertEquals(0.7441860465116279, vocabulary.ntf(0), 0.000001);
    Assert.assertEquals(0.7441860465116279, vocabulary.ntf("<UNK>"), 0.000001);

    Assert.assertEquals(0.011627906976744186, vocabulary.ntf(1), 0.000001);
    Assert.assertEquals(0.011627906976744186, vocabulary.ntf("-"), 0.000001);

    Assert.assertEquals(0.011627906976744186, vocabulary.ntf(2), 0.000001);
    Assert.assertEquals(0.011627906976744186, vocabulary.ntf("address"), 0.000001);

    vocabulary.clear();
    vocabulary.load(file);

    Assert.assertEquals(10, vocabulary.size());

    Assert.assertEquals("<UNK>", vocabulary.term(0));
    Assert.assertEquals(0, vocabulary.index("<UNK>"));

    Assert.assertEquals("-", vocabulary.term(1));
    Assert.assertEquals(1, vocabulary.index("-"));

    Assert.assertEquals("address", vocabulary.term(2));
    Assert.assertEquals(2, vocabulary.index("address"));

    Assert.assertEquals(0.7441860465116279, vocabulary.ntf(0), 0.000001);
    Assert.assertEquals(0.7441860465116279, vocabulary.ntf("<UNK>"), 0.000001);

    Assert.assertEquals(0.011627906976744186, vocabulary.ntf(1), 0.000001);
    Assert.assertEquals(0.011627906976744186, vocabulary.ntf("-"), 0.000001);

    Assert.assertEquals(0.011627906976744186, vocabulary.ntf(2), 0.000001);
    Assert.assertEquals(0.011627906976744186, vocabulary.ntf("address"), 0.000001);
  }

  @Test
  public void testGetTerms() {

    Vocabulary vocabulary = vocabulary();

    Assert.assertEquals(15, vocabulary.terms().size());
    Assert.assertEquals(15, Sets.intersection(
        Sets.newHashSet("<UNK>", "-", "address", "in", "most", "popular", "with", "yahoo", ".", "’", ",", "as", "to",
            "and", "the"), vocabulary.terms()).size());
  }

  @Test
  public void testCallCommandLineForUnigrams() throws Exception {

    String path = Files.createTempDirectory("").toFile().getAbsolutePath();
    File file = new File(path + File.separator + "papers.jsonl.gz");
    DocumentTest.papers().toFile(doc -> JsonCodec.asString(doc.json()), file, false, true);

    int ngramLength = 1;

    File vocabCompressed = new File(
        String.format("%svocabulary-%dgrams.tsv.gz", file.getParent() + File.separator, ngramLength));
    File vocabDecompressed = new File(
        String.format("%svocabulary-%dgrams.tsv", file.getParent() + File.separator, ngramLength));
    File weightedKeywordsCompressed = new File(
        String.format("%sweighted-keywords.tsv.gz", file.getParent() + File.separator));
    File weightedKeywordsDecompressed = new File(
        String.format("%sweighted-keywords.tsv", file.getParent() + File.separator));

    if (vocabCompressed.exists()) {
      Assert.assertTrue(vocabCompressed.delete());
    }
    if (vocabDecompressed.exists()) {
      Assert.assertTrue(vocabDecompressed.delete());
    }
    if (weightedKeywordsCompressed.exists()) {
      Assert.assertTrue(weightedKeywordsCompressed.delete());
    }
    if (weightedKeywordsDecompressed.exists()) {
      Assert.assertTrue(weightedKeywordsDecompressed.delete());
    }

    String[] args = new String[]{file.getAbsolutePath(), "0.01", "0.99", "1000", "WORD,NUMBER,TERMINAL_MARK",
        Integer.toString(ngramLength, 10)};
    Vocabulary.main(args);

    // Check vocabulary
    Assert.assertTrue(vocabCompressed.exists());
    Assert.assertFalse(vocabDecompressed.exists());
    Assert.assertTrue(IO.gunzip(vocabCompressed, vocabDecompressed));
    Assert.assertTrue(vocabDecompressed.exists());

    @Var List<String> lines = IO.readLines(vocabDecompressed);

    Assert.assertEquals(1002, lines.size());
    Assert.assertTrue(lines.get(0).matches("^# \\d+ \\d+$"));
    Assert.assertEquals("idx\tnormalized_term\ttf\tdf", lines.get(1));
    Assert.assertTrue(lines.get(2).matches("^0\t<UNK>\t\\d+\t\\d+$"));

    for (int i = 3; i < lines.size(); i++) {

      String line = lines.get(i);

      Assert.assertTrue(line.matches((i - 1 /* comment */ - 1 /* header */) + "\t.*\t\\d+\t\\d+$"));
    }

    // Check keywords
    Assert.assertTrue(weightedKeywordsCompressed.exists());
    Assert.assertFalse(weightedKeywordsDecompressed.exists());
    Assert.assertTrue(IO.gunzip(weightedKeywordsCompressed, weightedKeywordsDecompressed));
    Assert.assertTrue(weightedKeywordsDecompressed.exists());

    lines = IO.readLines(weightedKeywordsDecompressed);

    Assert.assertEquals(94_196, lines.size());

    for (int i = 0; i < lines.size(); i++) {

      String line = lines.get(i);

      Assert.assertTrue(line.matches("^.*\t\\d+(\\.\\d+)?$"));
    }
  }

  @Test
  public void testCallCommandLineForBigrams() throws Exception {

    String path = Files.createTempDirectory("").toFile().getAbsolutePath();
    File file = new File(path + File.separator + "papers.jsonl.gz");
    DocumentTest.papers().toFile(doc -> JsonCodec.asString(doc.json()), file, false, true);

    int ngramLength = 2;

    File vocabCompressed = new File(
        String.format("%svocabulary-%dgrams.tsv.gz", file.getParent() + File.separator, ngramLength));
    File vocabDecompressed = new File(
        String.format("%svocabulary-%dgrams.tsv", file.getParent() + File.separator, ngramLength));

    if (vocabCompressed.exists()) {
      Assert.assertTrue(vocabCompressed.delete());
    }
    if (vocabDecompressed.exists()) {
      Assert.assertTrue(vocabDecompressed.delete());
    }

    String[] args = new String[]{file.getAbsolutePath(), "0.01", "0.99", "1000", "WORD,NUMBER,TERMINAL_MARK",
        Integer.toString(ngramLength, 10)};
    Vocabulary.main(args);

    Assert.assertTrue(vocabCompressed.exists());
    Assert.assertFalse(vocabDecompressed.exists());
    Assert.assertTrue(IO.gunzip(vocabCompressed, vocabDecompressed));
    Assert.assertTrue(vocabDecompressed.exists());

    List<String> lines = IO.readLines(vocabDecompressed);

    Assert.assertEquals(1002, lines.size());
    Assert.assertTrue(lines.get(0).matches("^# \\d+ \\d+$"));
    Assert.assertEquals("idx\tnormalized_term\ttf\tdf", lines.get(1));
    Assert.assertTrue(lines.get(2).matches("^0\t<UNK>\t\\d+\t\\d+$"));

    for (int i = 3; i < lines.size(); i++) {

      String line = lines.get(i);

      Assert.assertTrue(line.matches((i - 1 /* comment */ - 1 /* header */) + "\t.*\t\\d+\t\\d+$"));
    }
  }

  @Test
  public void testRake() {

    Set<String> includeTags = Sets.newHashSet("WORD", "NUMBER", "TERMINAL_MARK");
    Set<String> stopwords = Sets.newHashSet("out", "a", "new", "way", "for", "to", "and", "they", "like", ",", "it",
        "already", "on", "your", ".", "got", "the", "this", "is", "taken", "from");
    Predicate<Span> keepSpan = span -> !Sets.intersection(span.tags(), includeTags).isEmpty();
    Predicate<String> keepToken = tkn -> !stopwords.contains(tkn);
    Function<String, List<String>> tokenizer = Vocabulary.tokenizer(keepSpan, 1, -1);
    List<String> texts = Lists.newArrayList(
        "Google quietly rolled out a new way for Android users to listen to podcasts and subscribe to shows they like, and it already works on your phone. Podcast production company Pacific Content got the exclusive on it.This text is taken from Google news.");
    Map.Entry<Map<String, Double>, Map<String, Double>> rake = Vocabulary.rake(View.of(texts).map(tokenizer),
        keepToken);

    Map<String, Double> weightedNGrams = rake.getValue();

    Assert.assertEquals(25.0, weightedNGrams.get("podcast_production_company_pacific_content"), 0.000001);
    Assert.assertEquals(8.5, weightedNGrams.get("google_quietly_rolled"), 0.000001);
    Assert.assertEquals(4.5, weightedNGrams.get("google_news"), 0.000001);
    Assert.assertEquals(4.0, weightedNGrams.get("android_users"), 0.000001);
  }

  private Vocabulary vocabularyFilteredByValues() {
    View<List<String>> tokens = View.of(sentences()).map(new TextNormalizer(true)).map(new TextTokenizer())
        .map(spans -> View.of(spans).map(Span::text).toList());
    return Vocabulary.of(tokens, 0, 3, 10);
  }

  private Vocabulary vocabularyFilteredByPercentages() {
    View<List<String>> tokens = View.of(sentences()).map(new TextNormalizer(true)).map(new TextTokenizer())
        .map(spans -> View.of(spans).map(Span::text).toList());
    return Vocabulary.of(tokens, 0.01, 1.0, 10);
  }

  private Vocabulary vocabulary() {
    View<List<String>> tokens = View.of(sentences()).map(new TextNormalizer(true)).map(new TextTokenizer())
        .map(spans -> View.of(spans).map(Span::text).toList());
    return Vocabulary.of(tokens);
  }

  private List<String> sentences() {
    return Lists.newArrayList("Welcome to Yahoo!, the world’s most visited home page.",
        "Quickly find what you’re searching for, get in touch with friends and stay in-the-know with the latest news and information.",
        "CloudSponge provides an interface to easily enable your users to import contacts from a variety of the most popular webmail services including Yahoo, Gmail and Hotmail/MSN as well as popular desktop address books such as Mac Address Book and Outlook.");
  }
}
