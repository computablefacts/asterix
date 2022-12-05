package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.Span;
import com.computablefacts.asterix.TextNormalizer;
import com.computablefacts.asterix.TextTokenizer;
import com.computablefacts.asterix.View;
import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class CountVectorizerTest {

  @Test
  public void testVectorize() {

    TextNormalizer normalizer = new TextNormalizer(true);
    TextTokenizer tokenizer = new TextTokenizer();

    View<List<String>> tokens = View.of(sentences()).map(normalizer.andThen(tokenizer))
        .map(spans -> View.of(spans).map(Span::text).toList());

    Vocabulary vocabulary = Vocabulary.of(tokens, 0.01, 0.99, 100);
    CountVectorizer vectorizer = new CountVectorizer(vocabulary);

    List<FeatureVector> vectors = View.of(sentences()).map(
            normalizer.andThen(tokenizer).andThen(spans -> View.of(spans).map(Span::text).toList()).andThen(vectorizer))
        .toList();

    Assert.assertEquals(vocabulary.size() - 1 /* UNK */, vectors.get(0).length());
    Assert.assertEquals(
        "[0.0, 0.0, 0.0, 0.07142857142857142, 0.0, 0.0, 0.07142857142857142, 0.07142857142857142, 0.07142857142857142, 0.07142857142857142, 0.0, 0.07142857142857142, 0.0, 0.07142857142857142]",
        vectors.get(0).toString());

    Assert.assertEquals(vocabulary.size() - 1 /* UNK */, vectors.get(1).length());
    Assert.assertEquals(
        "[0.07142857142857142, 0.0, 0.07142857142857142, 0.0, 0.0, 0.07142857142857142, 0.0, 0.03571428571428571, 0.03571428571428571, 0.03571428571428571, 0.0, 0.0, 0.07142857142857142, 0.07142857142857142]",
        vectors.get(1).toString());

    Assert.assertEquals(vocabulary.size() - 1 /* UNK */, vectors.get(2).length());
    Assert.assertEquals(
        "[0.0, 0.045454545454545456, 0.0, 0.022727272727272728, 0.045454545454545456, 0.0, 0.022727272727272728, 0.0, 0.022727272727272728, 0.022727272727272728, 0.06818181818181818, 0.045454545454545456, 0.045454545454545456, 0.022727272727272728]",
        vectors.get(2).toString());
  }

  private List<String> sentences() {
    return Lists.newArrayList("Welcome to Yahoo!, the world’s most visited home page.",
        "Quickly find what you’re searching for, get in touch with friends and stay in-the-know with the latest news and information.",
        "CloudSponge provides an interface to easily enable your users to import contacts from a variety of the most popular webmail services including Yahoo, Gmail and Hotmail/MSN as well as popular desktop address books such as Mac Address Book and Outlook.");
  }
}
