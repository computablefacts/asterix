package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.Span;
import com.computablefacts.asterix.View;
import org.junit.Assert;
import org.junit.Test;

public class VocabularyTest {

  @Test
  public void testVocabulary() {

    TextToNormalizedText ttnt = new TextToNormalizedText(true);
    TextToTokens ttt = new TextToTokens();
    View<String> tokens = View.of(ttt.apply(ttnt.apply(text()))).map(Span::text);
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

    TextToNormalizedText ttnt = new TextToNormalizedText(true);
    TextToTokens ttt = new TextToTokens();
    View<String> tokens = View.of(ttt.apply(ttnt.apply(text()))).map(Span::text);
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

  private String text() {
    return "Welcome to Yahoo!, the world’s most visited home page. Quickly find what you’re searching for, get in touch with friends and stay in-the-know with the latest news and information. CloudSponge provides an interface to easily enable your users to import contacts from a variety of the most popular webmail services including Yahoo, Gmail and Hotmail/MSN as well as popular desktop address books such as Mac Address Book and Outlook.";
  }
}
