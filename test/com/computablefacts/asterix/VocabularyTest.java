package com.computablefacts.asterix;

import com.computablefacts.asterix.ml.TextNormalizer;
import com.computablefacts.asterix.ml.TextTokenizer;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Assert;
import org.junit.Test;

public class VocabularyTest {

  @Test
  public void testEqualsAndHashCode() {
    EqualsVerifier.forClass(Vocabulary.class).withIgnoredFields("tokenUnk_", "idxUnk_", "termsSeen_")
        .suppress(Warning.NONFINAL_FIELDS).verify();
  }

  @Test
  public void testVocabulary() {

    Vocabulary vocabulary = partialVocabulary();

    Assert.assertEquals(10, vocabulary.size());
    Assert.assertEquals(86, vocabulary.nbTermsSeen());
    Assert.assertEquals(3, vocabulary.nbDocsSeen());

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

    Vocabulary sample = partialVocabulary();
    Vocabulary full = fullVocabulary();

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

    Vocabulary vocabulary = partialVocabulary();

    Assert.assertEquals(10, vocabulary.size());
    Assert.assertEquals(86, vocabulary.nbTermsSeen());
    Assert.assertEquals(3, vocabulary.nbDocsSeen());

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

    Vocabulary vocabulary = partialVocabulary();

    Assert.assertEquals(10, vocabulary.size());
    Assert.assertEquals(86, vocabulary.nbTermsSeen());
    Assert.assertEquals(3, vocabulary.nbDocsSeen());

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

    Vocabulary vocabulary = partialVocabulary();

    Assert.assertEquals(10, vocabulary.size());
    Assert.assertEquals(86, vocabulary.nbTermsSeen());
    Assert.assertEquals(3, vocabulary.nbDocsSeen());

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

    Vocabulary vocabulary = partialVocabulary();

    Assert.assertEquals(10, vocabulary.size());
    Assert.assertEquals(86, vocabulary.nbTermsSeen());
    Assert.assertEquals(3, vocabulary.nbDocsSeen());

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

    Vocabulary vocabulary = partialVocabulary();

    Assert.assertEquals(10, vocabulary.size());
    Assert.assertEquals(86, vocabulary.nbTermsSeen());
    Assert.assertEquals(3, vocabulary.nbDocsSeen());

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

    Vocabulary vocabulary = partialVocabulary();

    Assert.assertEquals(10, vocabulary.size());
    Assert.assertEquals(86, vocabulary.nbTermsSeen());
    Assert.assertEquals(3, vocabulary.nbDocsSeen());

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

    String path = Files.createTempDirectory("test-").toFile().getPath();
    File file = new File(path + File.separator + "vocab.tsv.gz");

    Vocabulary vocabulary = partialVocabulary();
    vocabulary.save(file);

    Assert.assertEquals(10, vocabulary.size());
    Assert.assertEquals(86, vocabulary.nbTermsSeen());
    Assert.assertEquals(3, vocabulary.nbDocsSeen());

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

    Vocabulary vocabulary = fullVocabulary();

    Assert.assertEquals(15, vocabulary.terms().size());
    Assert.assertEquals(15, Sets.intersection(
        Sets.newHashSet("<UNK>", "-", "address", "in", "most", "popular", "with", "yahoo", ".", "’", ",", "as", "to",
            "and", "the"), vocabulary.terms()).size());
  }

  @Test
  public void testStopwords() {

    Vocabulary vocabulary = fullVocabulary();
    List<String> stopwords = vocabulary.stopwords(3);

    Assert.assertEquals(Lists.newArrayList(",", ".", "and"), stopwords);
  }

  @Test
  public void testCommandLineExtractUnigrams() throws Exception {

    File documents = DocumentTest.documents();
    File vocabularyCompressed = new File(String.format("%s/vocabulary.tsv.gz", documents.getParent()));
    File vocabularyDecompressed = new File(String.format("%s/vocabulary.tsv", documents.getParent()));
    String[] args = new String[]{documents.getAbsolutePath(), "1", "0.01", "0.99", "1000", "WORD,NUMBER"};
    Vocabulary.main(args);

    Assert.assertTrue(vocabularyCompressed.exists());
    Assert.assertFalse(vocabularyDecompressed.exists());
    Assert.assertTrue(IO.gunzip(vocabularyCompressed, vocabularyDecompressed));
    Assert.assertTrue(vocabularyCompressed.exists());
    Assert.assertTrue(vocabularyDecompressed.exists());

    Vocabulary vocab = new Vocabulary();
    vocab.load(vocabularyCompressed);

    Assert.assertEquals(1000, vocab.size());
    Assert.assertEquals(2591028, vocab.nbTermsSeen());
    Assert.assertEquals(403, vocab.nbDocsSeen());

    List<String> lines = IO.readLines(vocabularyDecompressed);

    for (int i = 3; i < lines.size(); i++) {
      String line = lines.get(i);
      Assert.assertTrue(line.matches("^" + (i - 1 /* comment */ - 1 /* header */) + "\t[^ ]+\t\\d+\t\\d+$"));
    }
  }

  @Test
  public void testCommandLineExtractBigrams() throws Exception {

    File documents = DocumentTest.documents();
    File vocabularyCompressed = new File(String.format("%s/vocabulary.tsv.gz", documents.getParent()));
    File vocabularyDecompressed = new File(String.format("%s/vocabulary.tsv", documents.getParent()));
    String[] args = new String[]{documents.getAbsolutePath(), "2", "0.01", "0.99", "1000", "WORD,NUMBER"};
    Vocabulary.main(args);

    Assert.assertTrue(vocabularyCompressed.exists());
    Assert.assertFalse(vocabularyDecompressed.exists());
    Assert.assertTrue(IO.gunzip(vocabularyCompressed, vocabularyDecompressed));
    Assert.assertTrue(vocabularyCompressed.exists());
    Assert.assertTrue(vocabularyDecompressed.exists());

    Vocabulary vocab = new Vocabulary();
    vocab.load(vocabularyCompressed);

    Assert.assertEquals(1000, vocab.size());
    Assert.assertEquals(2590625, vocab.nbTermsSeen());
    Assert.assertEquals(403, vocab.nbDocsSeen());

    List<String> lines = IO.readLines(vocabularyDecompressed);

    for (int i = 3; i < lines.size(); i++) {
      String line = lines.get(i);
      Assert.assertTrue(line.matches("^" + (i - 1 /* comment */ - 1 /* header */) + "\t[^ ]+_[^ ]+\t\\d+\t\\d+$"));
    }
  }

  @Test
  public void testCommandLineExtractTrigrams() throws Exception {

    File documents = DocumentTest.documents();
    File vocabularyCompressed = new File(String.format("%s/vocabulary.tsv.gz", documents.getParent()));
    File vocabularyDecompressed = new File(String.format("%s/vocabulary.tsv", documents.getParent()));
    String[] args = new String[]{documents.getAbsolutePath(), "3", "0.01", "0.99", "1000", "WORD,NUMBER"};
    Vocabulary.main(args);

    Assert.assertTrue(vocabularyCompressed.exists());
    Assert.assertFalse(vocabularyDecompressed.exists());
    Assert.assertTrue(IO.gunzip(vocabularyCompressed, vocabularyDecompressed));
    Assert.assertTrue(vocabularyCompressed.exists());
    Assert.assertTrue(vocabularyDecompressed.exists());

    Vocabulary vocab = new Vocabulary();
    vocab.load(vocabularyCompressed);

    Assert.assertEquals(1000, vocab.size());
    Assert.assertEquals(2590222, vocab.nbTermsSeen());
    Assert.assertEquals(403, vocab.nbDocsSeen());

    List<String> lines = IO.readLines(vocabularyDecompressed);

    for (int i = 3; i < lines.size(); i++) {
      String line = lines.get(i);
      Assert.assertTrue(
          line.matches("^" + (i - 1 /* comment */ - 1 /* header */) + "\t[^ ]+_[^ ]+_[^ ]+\t\\d+\t\\d+$"));
    }
  }

  private Vocabulary partialVocabulary() {

    View<List<String>> docs = View.of(sentences()).map(new TextNormalizer(true)).map(new TextTokenizer())
        .map(spans -> View.of(spans).map(Span::text).toList());

    Vocabulary vocabulary = new Vocabulary();
    vocabulary.add(docs);
    vocabulary.freeze(0.01, 1.0, 10);
    return vocabulary;
  }

  private Vocabulary fullVocabulary() {

    View<List<String>> docs = View.of(sentences()).map(new TextNormalizer(true)).map(new TextTokenizer())
        .map(spans -> View.of(spans).map(Span::text).toList());

    Vocabulary vocabulary = new Vocabulary();
    vocabulary.add(docs);
    vocabulary.freeze(0.0, 1.0, -1);
    return vocabulary;
  }

  private List<String> sentences() {
    return Lists.newArrayList("Welcome to Yahoo!, the world’s most visited home page.",
        "Quickly find what you’re searching for, get in touch with friends and stay in-the-know with the latest news and information.",
        "CloudSponge provides an interface to easily enable your users to import contacts from a variety of the most popular webmail services including Yahoo, Gmail and Hotmail/MSN as well as popular desktop address books such as Mac Address Book and Outlook.");
  }
}
