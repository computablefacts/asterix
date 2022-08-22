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
    View<List<String>> tokens = View.of(sentences()).map(normalizer).map(new TextTokenizer())
        .map(spans -> View.of(spans).map(Span::text).toList());

    Vocabulary vocabulary = Vocabulary.of(tokens, 0.01, 0.99, 100);
    TfIdfVectorizer vectorizer = new TfIdfVectorizer(vocabulary, true);

    List<FeatureVector> vectors = View.of(sentences()).map(normalizer).map(new TextTokenizer())
        .map(vectorizer).toList();

    Assert.assertEquals(vocabulary.size() - 1, vectors.get(0).length());
    Assert.assertEquals(
        "[0.0, 0.0, 0.0, 0.4175666238781924, 0.0, 0.0, 0.4175666238781924, 0.4175666238781924, 0.3175701804283441, 0.0, 0.4175666238781924, 0.3175701804283441, 0.0, 0.3175701804283441]",
        vectors.get(0).toString());

    Assert.assertEquals(vocabulary.size() - 1, vectors.get(1).length());
    Assert.assertEquals(
        "[0.4614620795549112, 0.0, 0.4614620795549112, 0.0, 0.0, 0.4614620795549112, 0.0, 0.2307310397774556, 0.17547690294787652, 0.0, 0.0, 0.17547690294787652, 0.35095380589575303, 0.35095380589575303]",
        vectors.get(1).toString());

    Assert.assertEquals(vocabulary.size() - 1, vectors.get(2).length());
    Assert.assertEquals(
        "[0.0, 0.38455284165612813, 0.0, 0.19227642082806407, 0.38455284165612813, 0.0, 0.19227642082806407, 0.0, 0.14623117405163255, 0.5768292624841922, 0.38455284165612813, 0.14623117405163255, 0.2924623481032651, 0.14623117405163255]",
        vectors.get(2).toString());
  }

  @Test
  public void testVectorizeOnSubsetOfVocabulary() {

    TextNormalizer normalizer = new TextNormalizer(true);
    View<List<String>> tokens = View.of(sentences()).map(normalizer).map(new TextTokenizer())
        .map(spans -> View.of(spans).map(Span::text).toList());

    Vocabulary vocabulary = Vocabulary.of(tokens, 0.01, 0.99, 100);
    TfIdfVectorizer vectorizer = new TfIdfVectorizer(vocabulary, true);
    vectorizer.subsetOfVocabularyConsidered(Lists.newArrayList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));

    List<FeatureVector> vectors = View.of(sentences()).map(normalizer).map(new TextTokenizer())
        .map(vectorizer).toList();

    Assert.assertEquals(10, vectors.get(0).length());
    Assert.assertEquals(
        "[0.0, 0.0, 0.0, 0.5286346066596935, 0.0, 0.0, 0.5286346066596935, 0.5286346066596935, 0.4020402441612698, 0.0]",
        vectors.get(0).toString());

    Assert.assertEquals(10, vectors.get(1).length());
    Assert.assertEquals(
        "[0.5427573398831147, 0.0, 0.5427573398831147, 0.0, 0.0, 0.5427573398831147, 0.0, 0.27137866994155735, 0.2063904733987656, 0.0]",
        vectors.get(1).toString());

    Assert.assertEquals(10, vectors.get(2).length());
    Assert.assertEquals(
        "[0.0, 0.45200308932281713, 0.0, 0.22600154466140857, 0.45200308932281713, 0.0, 0.22600154466140857, 0.0, 0.17188000000724263, 0.6780046339842257]",
        vectors.get(2).toString());
  }

  private List<String> sentences() {
    return Lists.newArrayList("Welcome to Yahoo!, the world’s most visited home page.",
        "Quickly find what you’re searching for, get in touch with friends and stay in-the-know with the latest news and information.",
        "CloudSponge provides an interface to easily enable your users to import contacts from a variety of the most popular webmail services including Yahoo, Gmail and Hotmail/MSN as well as popular desktop address books such as Mac Address Book and Outlook.");
  }
}
