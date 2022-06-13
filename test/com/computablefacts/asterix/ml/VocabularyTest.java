package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.Span;
import com.computablefacts.asterix.SpanSequence;
import com.computablefacts.asterix.View;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.Var;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class VocabularyTest {

  @Test
  public void testVocabulary() {

    View<String> tokens = View.of(text()).map(new NormalizeText(true)).map(new TokenizeText())
        .flatten(View::of).map(Span::text);
    Vocabulary vocabulary = Vocabulary.of(tokens, 2, 10);

    Assert.assertEquals(10, vocabulary.size());

    Assert.assertEquals("<UNK>", vocabulary.token(0));
    Assert.assertEquals(0, vocabulary.index("<UNK>"));

    Assert.assertEquals("-", vocabulary.token(1));
    Assert.assertEquals(1, vocabulary.index("-"));

    Assert.assertEquals("address", vocabulary.token(2));
    Assert.assertEquals(2, vocabulary.index("address"));

    Assert.assertEquals("in", vocabulary.token(3));
    Assert.assertEquals(3, vocabulary.index("in"));

    Assert.assertEquals("most", vocabulary.token(4));
    Assert.assertEquals(4, vocabulary.index("most"));

    Assert.assertEquals("popular", vocabulary.token(5));
    Assert.assertEquals(5, vocabulary.index("popular"));

    Assert.assertEquals("with", vocabulary.token(6));
    Assert.assertEquals(6, vocabulary.index("with"));

    Assert.assertEquals("yahoo", vocabulary.token(7));
    Assert.assertEquals(7, vocabulary.index("yahoo"));

    Assert.assertEquals("’", vocabulary.token(8));
    Assert.assertEquals(8, vocabulary.index("’"));

    Assert.assertEquals(",", vocabulary.token(9));
    Assert.assertEquals(9, vocabulary.index(","));
  }

  @Test
  public void testFrequency() {

    View<String> tokens = View.of(text()).map(new NormalizeText(true)).map(new TokenizeText())
        .flatten(View::of).map(Span::text);
    Vocabulary vocabulary = Vocabulary.of(tokens, 2, 10);

    Assert.assertEquals(10, vocabulary.size());

    Assert.assertEquals(0, vocabulary.frequency(0));
    Assert.assertEquals(0, vocabulary.frequency("<UNK>"));

    Assert.assertEquals(2, vocabulary.frequency(1));
    Assert.assertEquals(2, vocabulary.frequency("-"));

    Assert.assertEquals(2, vocabulary.frequency(2));
    Assert.assertEquals(2, vocabulary.frequency("address"));

    Assert.assertEquals(2, vocabulary.frequency(3));
    Assert.assertEquals(2, vocabulary.frequency("in"));
  }

  @Test
  public void testNormalizedFrequency() {

    View<String> tokens = View.of(text()).map(new NormalizeText(true)).map(new TokenizeText())
        .flatten(View::of).map(Span::text);
    Vocabulary vocabulary = Vocabulary.of(tokens, 2, 10);

    Assert.assertEquals(10, vocabulary.size());

    Assert.assertEquals(0.0, vocabulary.normalizedFrequency(0), 0.000001);
    Assert.assertEquals(0.0, vocabulary.normalizedFrequency("<UNK>"), 0.000001);

    Assert.assertEquals(0.023255813953488372, vocabulary.normalizedFrequency(1), 0.000001);
    Assert.assertEquals(0.023255813953488372, vocabulary.normalizedFrequency("-"), 0.000001);

    Assert.assertEquals(0.023255813953488372, vocabulary.normalizedFrequency(2), 0.000001);
    Assert.assertEquals(0.023255813953488372, vocabulary.normalizedFrequency("address"), 0.000001);

    Assert.assertEquals(0.023255813953488372, vocabulary.normalizedFrequency(3), 0.000001);
    Assert.assertEquals(0.023255813953488372, vocabulary.normalizedFrequency("in"), 0.000001);
  }

  @Test
  public void testSubSamplingDoesNotReturnEmptyLists() {

    List<SpanSequence> spans = View.of(sentences()).map(new NormalizeText(true))
        .map(new TokenizeText()).toList();
    Vocabulary vocabulary = Vocabulary.of(View.of(spans).flatten(View::of).map(Span::text), 2, 10);
    List<SpanSequence> samples = vocabulary.subSample(View.of(spans)).toList();

    Assert.assertFalse(samples.isEmpty());
    Assert.assertTrue(samples.stream().noneMatch(sample -> sample.size() == 0));
  }

  @Test
  public void testMostProbableNextToken() {

    List<SpanSequence> spans = View.of(sentences()).map(new NormalizeText(true))
        .map(new TokenizeText()).toList();
    Vocabulary vocabulary = Vocabulary.of(View.of(spans).flatten(
            s1 -> View.of(s1).map(Span::text).overlappingWindow(2).map(s3 -> Joiner.on('\0').join(s3))),
        2, 10);

    @Var String token = vocabulary.mostProbableNextToken("mac").orElse("<UNK>");

    Assert.assertEquals("address", token);

    token = vocabulary.mostProbableNextToken("the").orElse("<UNK>");

    Assert.assertTrue(Sets.newHashSet("world", "latest", "-", "most").contains(token));
  }

  private String text() {
    return Joiner.on(' ').join(sentences());
  }

  private List<String> sentences() {
    return Lists.newArrayList("Welcome to Yahoo!, the world’s most visited home page.",
        "Quickly find what you’re searching for, get in touch with friends and stay in-the-know with the latest news and information.",
        "CloudSponge provides an interface to easily enable your users to import contacts from a variety of the most popular webmail services including Yahoo, Gmail and Hotmail/MSN as well as popular desktop address books such as Mac Address Book and Outlook.");
  }
}
