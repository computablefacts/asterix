package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.Span;
import com.computablefacts.asterix.View;
import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class CountVectorizerTest {

  @Test
  public void testVectorize() {

    TextNormalizer normalizer = new TextNormalizer(true);
    View<List<String>> tokens = View.of(sentences()).map(normalizer).map(new TextTokenizer())
        .map(spans -> View.of(spans).map(Span::text).toList());

    Vocabulary vocabulary = Vocabulary.of(tokens, 0.01, 0.99, 100);
    CountVectorizer vectorizer = new CountVectorizer(vocabulary, true);

    View.of(sentences()).map(normalizer).map(new TextTokenizer()).map(vectorizer)
        .forEachRemaining(vector -> Assert.assertEquals(vocabulary.size() - 1, vector.length()));
  }

  @Test
  public void testVectorizeOnSubsetOfVocabulary() {

    TextNormalizer normalizer = new TextNormalizer(true);
    View<List<String>> tokens = View.of(sentences()).map(normalizer).map(new TextTokenizer())
        .map(spans -> View.of(spans).map(Span::text).toList());

    Vocabulary vocabulary = Vocabulary.of(tokens, 0.01, 0.99, 100);
    CountVectorizer vectorizer = new CountVectorizer(vocabulary, true);
    vectorizer.subsetOfVocabularyConsidered(Lists.newArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

    View.of(sentences()).map(normalizer).map(new TextTokenizer()).map(vectorizer)
        .forEachRemaining(vector -> Assert.assertEquals(10, vector.length()));
  }

  private List<String> sentences() {
    return Lists.newArrayList("Welcome to Yahoo!, the world’s most visited home page.",
        "Quickly find what you’re searching for, get in touch with friends and stay in-the-know with the latest news and information.",
        "CloudSponge provides an interface to easily enable your users to import contacts from a variety of the most popular webmail services including Yahoo, Gmail and Hotmail/MSN as well as popular desktop address books such as Mac Address Book and Outlook.");
  }
}
