package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.View;
import com.google.common.collect.Lists;
import com.google.re2j.Pattern;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class RegexVectorizerTest {

  @Test
  public void testVectorizeWithoutWeights() {

    Pattern pattern = Pattern.compile("(yahoo)|(gmail)|(hotmail)",
        Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    RegexVectorizer vectorizer = new RegexVectorizer(pattern);
    List<FeatureVector> vectors = View.of(sentences()).map(vectorizer).toList();

    Assert.assertEquals(3, vectors.get(0).length());
    Assert.assertEquals("[1.0, 0.0, 0.0]", vectors.get(0).toString());

    Assert.assertEquals(3, vectors.get(1).length());
    Assert.assertEquals("[0.0, 0.0, 0.0]", vectors.get(1).toString());

    Assert.assertEquals(3, vectors.get(2).length());
    Assert.assertEquals("[1.0, 1.0, 1.0]", vectors.get(2).toString());
  }

  @Test
  public void testVectorizeWithWeights() {

    Pattern pattern = Pattern.compile("(yahoo)|(gmail)|(hotmail)",
        Pattern.DOTALL | Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);
    RegexVectorizer vectorizer = new RegexVectorizer(pattern, Lists.newArrayList(1.0, 1.0, 1.0));
    List<FeatureVector> vectors = View.of(sentences()).map(vectorizer).toList();

    Assert.assertEquals(3, vectors.get(0).length());
    Assert.assertEquals("[1.0, 0.0, 0.0]", vectors.get(0).toString());

    Assert.assertEquals(3, vectors.get(1).length());
    Assert.assertEquals("[0.0, 0.0, 0.0]", vectors.get(1).toString());

    Assert.assertEquals(3, vectors.get(2).length());
    Assert.assertEquals("[1.0, 1.0, 1.0]", vectors.get(2).toString());
  }

  private List<String> sentences() {
    return Lists.newArrayList("Welcome to Yahoo!, the world’s most visited home page.",
        "Quickly find what you’re searching for, get in touch with friends and stay in-the-know with the latest news and information.",
        "CloudSponge provides an interface to easily enable your users to import contacts from a variety of the most popular webmail services including Yahoo, Gmail and Hotmail/MSN as well as popular desktop address books such as Mac Address Book and Outlook.");
  }
}
