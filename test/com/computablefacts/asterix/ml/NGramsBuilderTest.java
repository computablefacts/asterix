package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.SpanSequence;
import org.junit.Assert;
import org.junit.Test;

public class NGramsBuilderTest {

  @Test
  public void testNGramize() {

    SpanSequence spans = new NGramsBuilder(3).apply(new TextTokenizer().apply(text()));

    Assert.assertEquals(84, spans.size());

    for (int i = 0; i < spans.size(); i++) {
      System.out.println(
          "span=\"" + spans.span(i).text() + "\", index=" + i + ", tags=" + spans.span(i).tags() + ", features="
              + spans.span(i).features());
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

    Assert.assertEquals("Welcome_to_Yahoo", spans.span(0).getFeature("NGRAM"));
    Assert.assertEquals("to_Yahoo_!", spans.span(1).getFeature("NGRAM"));
    Assert.assertEquals("Yahoo_!_,", spans.span(2).getFeature("NGRAM"));
    Assert.assertEquals("!_,_the", spans.span(3).getFeature("NGRAM"));
    Assert.assertEquals(",_the_world", spans.span(4).getFeature("NGRAM"));
    Assert.assertEquals("the_world_’", spans.span(5).getFeature("NGRAM"));
    Assert.assertEquals("world_’_s", spans.span(6).getFeature("NGRAM"));
    Assert.assertEquals("’_s_most", spans.span(7).getFeature("NGRAM"));
    Assert.assertEquals("s_most_visited", spans.span(8).getFeature("NGRAM"));
    Assert.assertEquals("most_visited_home", spans.span(9).getFeature("NGRAM"));
    Assert.assertEquals("visited_home_page", spans.span(10).getFeature("NGRAM"));
    Assert.assertEquals("home_page_.", spans.span(11).getFeature("NGRAM"));

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
