package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.Span;
import com.computablefacts.asterix.View;
import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class NGramsToTfIdfVectorTest {

  @Test
  public void testVectorize() {

    View<List<String>> tokens = View.of(sentences()).map(new NormalizeText(true))
        .map(new TokenizeText()).map(spans -> View.of(spans).map(Span::text).toList());

    Vocabulary vocabulary = Vocabulary.of(tokens, 1, 1, 100);

    View.of(sentences()).map(new NormalizeText(true)).map(new TokenizeText())
        .map(new NGramsToTfIdfVector(vocabulary)).forEachRemaining(vector -> {
          Assert.assertEquals(vocabulary.size() - 1, vector.length);
        });
  }

  private List<String> sentences() {
    return Lists.newArrayList("Welcome to Yahoo!, the world’s most visited home page.",
        "Quickly find what you’re searching for, get in touch with friends and stay in-the-know with the latest news and information.",
        "CloudSponge provides an interface to easily enable your users to import contacts from a variety of the most popular webmail services including Yahoo, Gmail and Hotmail/MSN as well as popular desktop address books such as Mac Address Book and Outlook.");
  }
}
