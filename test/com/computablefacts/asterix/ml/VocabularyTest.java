package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.Span;
import com.computablefacts.asterix.SpanSequence;
import com.computablefacts.asterix.View;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.Var;
import java.io.File;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class VocabularyTest {

  @Test
  public void testVocabulary() {

    Vocabulary vocabulary = vocabulary();

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
  public void testTermFrequency() {

    Vocabulary vocabulary = vocabulary();

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

    Vocabulary vocabulary = vocabulary();

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

    Vocabulary vocabulary = vocabulary();

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

    Vocabulary vocabulary = vocabulary();

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

    Vocabulary vocabulary = vocabulary();

    Assert.assertEquals(10, vocabulary.size());

    Assert.assertEquals(0.7123179275482191, vocabulary.idf(0), 0.000001);
    Assert.assertEquals(0.7123179275482191, vocabulary.idf("<UNK>"), 0.000001);

    Assert.assertEquals(1.4054651081081644, vocabulary.idf(1), 0.000001);
    Assert.assertEquals(1.4054651081081644, vocabulary.idf("-"), 0.000001);

    Assert.assertEquals(1.4054651081081644, vocabulary.idf(2), 0.000001);
    Assert.assertEquals(1.4054651081081644, vocabulary.idf("address"), 0.000001);

    Assert.assertEquals(1.4054651081081644, vocabulary.idf(3), 0.000001);
    Assert.assertEquals(1.4054651081081644, vocabulary.idf("in"), 0.000001);
  }

  @Test
  public void testTfIdf() {

    Vocabulary vocabulary = vocabulary();

    Assert.assertEquals(10, vocabulary.size());

    Assert.assertEquals(0.7123179275482191, vocabulary.tfIdf(0, 1), 0.000001);
    Assert.assertEquals(0.7123179275482191, vocabulary.tfIdf("<UNK>", 1), 0.000001);

    Assert.assertEquals(1.4054651081081644, vocabulary.tfIdf(1, 1), 0.000001);
    Assert.assertEquals(1.4054651081081644, vocabulary.tfIdf("-", 1), 0.000001);

    Assert.assertEquals(1.4054651081081644, vocabulary.tfIdf(2, 1), 0.000001);
    Assert.assertEquals(1.4054651081081644, vocabulary.tfIdf("address", 1), 0.000001);

    Assert.assertEquals(1.4054651081081644, vocabulary.tfIdf(3, 1), 0.000001);
    Assert.assertEquals(1.4054651081081644, vocabulary.tfIdf("in", 1), 0.000001);
  }

  @Test
  public void testSubSamplingDoesNotReturnEmptyLists() {

    List<SpanSequence> spans = View.of(sentences()).map(new NormalizeText(true))
        .map(new TokenizeText()).toList();

    View<List<String>> tokens = View.of(spans).map(span -> View.of(span).map(Span::text).toList());

    Vocabulary vocabulary = Vocabulary.of(tokens);
    List<SpanSequence> samples = vocabulary.subSample(View.of(spans)).toList();

    Assert.assertFalse(samples.isEmpty());
    Assert.assertTrue(samples.stream().noneMatch(sample -> sample.size() == 0));
  }

  @Test
  public void testMostProbableNextToken() {

    List<SpanSequence> spans = View.of(sentences()).concat(View.of(sentences()))
        .map(new NormalizeText(true)).map(new TokenizeText()).toList();

    View<List<String>> tokens = View.of(spans).map(
        span -> View.of(span).map(Span::text).overlappingWindowWithStrictLength(2)
            .map(s3 -> Joiner.on('_').join(s3)).toList());

    Vocabulary vocabulary = Vocabulary.of(View.of(tokens));

    @Var String token = vocabulary.mostProbableNextTerm("mac").orElse("<UNK>");

    Assert.assertEquals("address", token);

    token = vocabulary.mostProbableNextTerm("the").orElse("<UNK>");

    Assert.assertTrue(Sets.newHashSet("world", "latest", "-", "most").contains(token));
  }

  @Test
  public void testSaveThenLoadVocabulary() throws Exception {

    String path = java.nio.file.Files.createTempDirectory("test-").toFile().getPath();
    File file = new File(path + File.separator + "vocab.tsv.gz");

    Vocabulary vocabulary = vocabulary();
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

  private Vocabulary vocabulary() {
    View<List<String>> tokens = View.of(sentences()).map(new NormalizeText(true))
        .map(new TokenizeText()).map(spans -> View.of(spans).map(Span::text).toList());
    return Vocabulary.of(tokens, 1, 1, 10);
  }

  private List<String> sentences() {
    return Lists.newArrayList("Welcome to Yahoo!, the world’s most visited home page.",
        "Quickly find what you’re searching for, get in touch with friends and stay in-the-know with the latest news and information.",
        "CloudSponge provides an interface to easily enable your users to import contacts from a variety of the most popular webmail services including Yahoo, Gmail and Hotmail/MSN as well as popular desktop address books such as Mac Address Book and Outlook.");
  }
}
