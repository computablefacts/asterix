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

    List<FeatureVector> vectors = View.of(sentences()).map(normalizer).map(new TextTokenizer())
        .map(vectorizer).toList();

    Assert.assertEquals(vocabulary.size() - 1, vectors.get(0).length());
    Assert.assertEquals(
        "[0.0, 0.0, 0.0, 0.37796447300922725, 0.0, 0.0, 0.37796447300922725, 0.37796447300922725, 0.37796447300922725, 0.0, 0.37796447300922725, 0.37796447300922725, 0.0, 0.37796447300922725]",
        vectors.get(0).toString());

    Assert.assertEquals(vocabulary.size() - 1, vectors.get(1).length());
    Assert.assertEquals(
        "[0.41702882811414954, 0.0, 0.41702882811414954, 0.0, 0.0, 0.41702882811414954, 0.0, 0.20851441405707477, 0.20851441405707477, 0.0, 0.0, 0.20851441405707477, 0.41702882811414954, 0.41702882811414954]",
        vectors.get(1).toString());

    Assert.assertEquals(vocabulary.size() - 1, vectors.get(2).length());
    Assert.assertEquals(
        "[0.0, 0.3651483716701107, 0.0, 0.18257418583505536, 0.3651483716701107, 0.0, 0.18257418583505536, 0.0, 0.18257418583505536, 0.5477225575051661, 0.3651483716701107, 0.18257418583505536, 0.3651483716701107, 0.18257418583505536]",
        vectors.get(2).toString());
  }

  @Test
  public void testVectorizeOnSubsetOfVocabulary() {

    TextNormalizer normalizer = new TextNormalizer(true);
    View<List<String>> tokens = View.of(sentences()).map(normalizer).map(new TextTokenizer())
        .map(spans -> View.of(spans).map(Span::text).toList());

    Vocabulary vocabulary = Vocabulary.of(tokens, 0.01, 0.99, 100);
    CountVectorizer vectorizer = new CountVectorizer(vocabulary, true);
    vectorizer.subsetOfVocabularyConsidered(Lists.newArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

    List<FeatureVector> vectors = View.of(sentences()).map(normalizer).map(new TextTokenizer())
        .map(vectorizer).toList();

    Assert.assertEquals(10, vectors.get(0).length());
    Assert.assertEquals("[0.0, 0.0, 0.0, 0.5, 0.0, 0.0, 0.5, 0.5, 0.5, 0.0]",
        vectors.get(0).toString());

    Assert.assertEquals(10, vectors.get(1).length());
    Assert.assertEquals(
        "[0.5345224838248487, 0.0, 0.5345224838248487, 0.0, 0.0, 0.5345224838248487, 0.0, 0.26726124191242434, 0.26726124191242434, 0.0]",
        vectors.get(1).toString());

    Assert.assertEquals(10, vectors.get(2).length());
    Assert.assertEquals(
        "[0.0, 0.4472135954999579, 0.0, 0.22360679774997896, 0.4472135954999579, 0.0, 0.22360679774997896, 0.0, 0.22360679774997896, 0.6708203932499368]",
        vectors.get(2).toString());
  }

  private List<String> sentences() {
    return Lists.newArrayList("Welcome to Yahoo!, the world’s most visited home page.",
        "Quickly find what you’re searching for, get in touch with friends and stay in-the-know with the latest news and information.",
        "CloudSponge provides an interface to easily enable your users to import contacts from a variety of the most popular webmail services including Yahoo, Gmail and Hotmail/MSN as well as popular desktop address books such as Mac Address Book and Outlook.");
  }
}
