package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.SpanSequence;
import org.junit.Assert;
import org.junit.Test;

public class ConvertTokensToNGramsTest {

  @Test
  public void testNGramize() {

    SpanSequence spans = new ConvertTokensToNGrams(3).apply(new TokenizeText().apply(text()));

    Assert.assertEquals(86, spans.size());

    for (int i = 0; i < spans.size(); i++) {
      System.out.println(
          "span=\"" + spans.span(i).text() + "\", index=" + i + ", tags=" + spans.span(i).tags()
              + ", features=" + spans.span(i).features());
    }

    Assert.assertEquals("Welcome to Yahoo", spans.span(0).text());
    Assert.assertEquals("to Yahoo!", spans.span(1).text());
    Assert.assertEquals("Yahoo!,", spans.span(2).text());
    Assert.assertEquals("!, the", spans.span(3).text());
    Assert.assertEquals(", the world", spans.span(4).text());
    Assert.assertEquals("the world’", spans.span(5).text());
    Assert.assertEquals("world’s", spans.span(6).text());
    Assert.assertEquals("’s most", spans.span(7).text());
    Assert.assertEquals("s most visited", spans.span(8).text());
    Assert.assertEquals("most visited home", spans.span(9).text());
    Assert.assertEquals("visited home page", spans.span(10).text());
    Assert.assertEquals("home page.", spans.span(11).text());

    Assert.assertTrue(spans.span(0).tags().isEmpty());
    Assert.assertTrue(spans.span(1).tags().isEmpty());
    Assert.assertTrue(spans.span(2).tags().isEmpty());
    Assert.assertTrue(spans.span(3).tags().isEmpty());
    Assert.assertTrue(spans.span(4).tags().isEmpty());
    Assert.assertTrue(spans.span(5).tags().isEmpty());
    Assert.assertTrue(spans.span(6).tags().isEmpty());
    Assert.assertTrue(spans.span(7).tags().isEmpty());
    Assert.assertTrue(spans.span(8).tags().isEmpty());
    Assert.assertTrue(spans.span(9).tags().isEmpty());
    Assert.assertTrue(spans.span(10).tags().isEmpty());
    Assert.assertTrue(spans.span(11).tags().isEmpty());
  }

  private String text() {
    return "Welcome to Yahoo!, the world’s most visited home page. Quickly find what you’re searching for, get in touch with friends and stay in-the-know with the latest news and information. CloudSponge provides an interface to easily enable your users to import contacts from a variety of the most popular webmail services including Yahoo, Gmail and Hotmail/MSN as well as popular desktop address books such as Mac Address Book and Outlook.";
  }
}
