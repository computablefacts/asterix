package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.SpanSequence;
import org.junit.Assert;
import org.junit.Test;

public class SimpleTokenizerTest {

  @Test
  public void testTokenizer() {

    SpanSequence spans = new SimpleTokenizer().apply(text());

    Assert.assertEquals(86, spans.size());

    for (int i = 0; i < spans.size(); i++) {
      System.out.println(
          "span=\"" + spans.span(i).text() + "\", index=" + i + ", tags=" + spans.span(i).tags()
              + ", features=" + spans.span(i).features());
    }

    Assert.assertEquals("Welcome", spans.span(0).text());
    Assert.assertEquals("to", spans.span(1).text());
    Assert.assertEquals("Yahoo", spans.span(2).text());
    Assert.assertEquals("!", spans.span(3).text());
    Assert.assertEquals(",", spans.span(4).text());
    Assert.assertEquals("the", spans.span(5).text());
    Assert.assertEquals("world", spans.span(6).text());
    Assert.assertEquals("’", spans.span(7).text());
    Assert.assertEquals("s", spans.span(8).text());
    Assert.assertEquals("most", spans.span(9).text());
    Assert.assertEquals("visited", spans.span(10).text());
    Assert.assertEquals("home", spans.span(11).text());
    Assert.assertEquals("page", spans.span(12).text());
    Assert.assertEquals(".", spans.span(13).text());

    Assert.assertTrue(SimpleTokenizer.isWord(spans.span(0)));
    Assert.assertTrue(SimpleTokenizer.isWord(spans.span(1)));
    Assert.assertTrue(SimpleTokenizer.isWord(spans.span(2)));
    Assert.assertTrue(SimpleTokenizer.isPunctuation(spans.span(3)));
    Assert.assertTrue(SimpleTokenizer.isPunctuation(spans.span(4)));
    Assert.assertTrue(SimpleTokenizer.isWord(spans.span(5)));
    Assert.assertTrue(SimpleTokenizer.isWord(spans.span(6)));
    Assert.assertTrue(SimpleTokenizer.isApostrophe(spans.span(7)));
    Assert.assertTrue(SimpleTokenizer.isWord(spans.span(8)));
    Assert.assertTrue(SimpleTokenizer.isWord(spans.span(9)));
    Assert.assertTrue(SimpleTokenizer.isWord(spans.span(10)));
    Assert.assertTrue(SimpleTokenizer.isWord(spans.span(11)));
    Assert.assertTrue(SimpleTokenizer.isWord(spans.span(12)));
    Assert.assertTrue(SimpleTokenizer.isPunctuation(spans.span(13)));

    Assert.assertEquals("in", spans.span(30).text());
    Assert.assertEquals("-", spans.span(31).text());
    Assert.assertEquals("the", spans.span(32).text());
    Assert.assertEquals("-", spans.span(33).text());
    Assert.assertEquals("know", spans.span(34).text());

    Assert.assertTrue(SimpleTokenizer.isWord(spans.span(30)));
    Assert.assertTrue(SimpleTokenizer.isListMark(spans.span(31)));
    Assert.assertTrue(SimpleTokenizer.isWord(spans.span(32)));
    Assert.assertTrue(SimpleTokenizer.isListMark(spans.span(33)));
    Assert.assertTrue(SimpleTokenizer.isWord(spans.span(34)));
  }

  private String text() {
    return "Welcome to Yahoo!, the world’s most visited home page. Quickly find what you’re searching for, get in touch with friends and stay in-the-know with the latest news and information. CloudSponge provides an interface to easily enable your users to import contacts from a variety of the most popular webmail services including Yahoo, Gmail and Hotmail/MSN as well as popular desktop address books such as Mac Address Book and Outlook.";
  }
}
