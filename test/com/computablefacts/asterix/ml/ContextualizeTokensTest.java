package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.SpanSequence;
import com.computablefacts.asterix.View;
import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class ContextualizeTokensTest {

  @Test
  public void testContextualizeTokens() {

    List<SpanSequence> sentences = View.of(sentences())
        .map(new NormalizeText(true))
        .map(new TokenizeText())
        .filter(spans -> spans.size() > 2)
        .map(new ContextualizeTokens(5))
        .toList();

    Assert.assertEquals(3, sentences.size());

    Assert.assertEquals(14, sentences.get(0).size());
    Assert.assertEquals(28, sentences.get(1).size());
    Assert.assertEquals(44, sentences.get(2).size());

    Assert.assertTrue(sentences.get(0).stream()
        .allMatch(span -> span.hasFeature("CTX_BEFORE") && span.hasFeature("CTX_AFTER")));
    Assert.assertTrue(sentences.get(1).stream()
        .allMatch(span -> span.hasFeature("CTX_BEFORE") && span.hasFeature("CTX_AFTER")));
    Assert.assertTrue(sentences.get(2).stream()
        .allMatch(span -> span.hasFeature("CTX_BEFORE") && span.hasFeature("CTX_AFTER")));
  }

  private List<String> sentences() {
    return Lists.newArrayList("Welcome to Yahoo!, the world’s most visited home page.",
        "Quickly find what you’re searching for, get in touch with friends and stay in-the-know with the latest news and information.",
        "CloudSponge provides an interface to easily enable your users to import contacts from a variety of the most popular webmail services including Yahoo, Gmail and Hotmail/MSN as well as popular desktop address books such as Mac Address Book and Outlook.");
  }
}
