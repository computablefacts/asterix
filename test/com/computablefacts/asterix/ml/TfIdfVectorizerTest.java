package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.Span;
import com.computablefacts.asterix.View;
import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class TfIdfVectorizerTest {

  @Test
  public void testVectorize() {

    TextNormalizer normalizer = new TextNormalizer(true);
    TextTokenizer tokenizer = new TextTokenizer();

    View<List<String>> tokens = View.of(sentences()).map(normalizer.andThen(tokenizer))
        .map(spans -> View.of(spans).map(Span::text).toList());

    Vocabulary vocabulary = Vocabulary.of(tokens, 0.01, 0.99, 100);
    TfIdfVectorizer vectorizer = new TfIdfVectorizer(vocabulary);

    List<FeatureVector> vectors = View.of(sentences()).map(normalizer.andThen(tokenizer).andThen(vectorizer)).toList();

    Assert.assertEquals(vocabulary.size() - 1 /* UNK */, vectors.get(0).length());
    Assert.assertEquals(
        "[0.0, 0.0, 0.0, 1.6931471805599454, 0.0, 0.0, 1.6931471805599454, 1.6931471805599454, 1.2876820724517808, 1.2876820724517808, 0.0, 1.6931471805599454, 0.0, 1.2876820724517808]",
        vectors.get(0).toString());

    Assert.assertEquals(vocabulary.size() - 1 /* UNK */, vectors.get(1).length());
    Assert.assertEquals(
        "[3.386294361119891, 0.0, 3.386294361119891, 0.0, 0.0, 3.386294361119891, 0.0, 1.6931471805599454, 1.2876820724517808, 1.2876820724517808, 0.0, 0.0, 2.5753641449035616, 2.5753641449035616]",
        vectors.get(1).toString());

    Assert.assertEquals(vocabulary.size() - 1 /* UNK */, vectors.get(2).length());
    Assert.assertEquals(
        "[0.0, 3.386294361119891, 0.0, 1.6931471805599454, 3.386294361119891, 0.0, 1.6931471805599454, 0.0, 1.2876820724517808, 1.2876820724517808, 5.079441541679836, 3.386294361119891, 2.5753641449035616, 1.2876820724517808]",
        vectors.get(2).toString());
  }

  private List<String> sentences() {
    return Lists.newArrayList("Welcome to Yahoo!, the world’s most visited home page.",
        "Quickly find what you’re searching for, get in touch with friends and stay in-the-know with the latest news and information.",
        "CloudSponge provides an interface to easily enable your users to import contacts from a variety of the most popular webmail services including Yahoo, Gmail and Hotmail/MSN as well as popular desktop address books such as Mac Address Book and Outlook.");
  }
}
