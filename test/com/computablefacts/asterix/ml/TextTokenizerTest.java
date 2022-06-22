package com.computablefacts.asterix.ml;

import com.computablefacts.asterix.DocumentTest;
import com.computablefacts.asterix.SpanSequence;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import org.junit.Assert;
import org.junit.Test;

public class TextTokenizerTest {

  @Test
  public void testTokenizeText() {

    SpanSequence spans = new TextTokenizer().apply(text());

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

    Assert.assertTrue(TextTokenizer.isWord(spans.span(0)));
    Assert.assertTrue(TextTokenizer.isWord(spans.span(1)));
    Assert.assertTrue(TextTokenizer.isWord(spans.span(2)));
    Assert.assertTrue(TextTokenizer.isPunctuation(spans.span(3)));
    Assert.assertTrue(TextTokenizer.isPunctuation(spans.span(4)));
    Assert.assertTrue(TextTokenizer.isWord(spans.span(5)));
    Assert.assertTrue(TextTokenizer.isWord(spans.span(6)));
    Assert.assertTrue(TextTokenizer.isApostrophe(spans.span(7)));
    Assert.assertTrue(TextTokenizer.isWord(spans.span(8)));
    Assert.assertTrue(TextTokenizer.isWord(spans.span(9)));
    Assert.assertTrue(TextTokenizer.isWord(spans.span(10)));
    Assert.assertTrue(TextTokenizer.isWord(spans.span(11)));
    Assert.assertTrue(TextTokenizer.isWord(spans.span(12)));
    Assert.assertTrue(TextTokenizer.isPunctuation(spans.span(13)));

    Assert.assertEquals("in", spans.span(30).text());
    Assert.assertEquals("-", spans.span(31).text());
    Assert.assertEquals("the", spans.span(32).text());
    Assert.assertEquals("-", spans.span(33).text());
    Assert.assertEquals("know", spans.span(34).text());

    Assert.assertTrue(TextTokenizer.isWord(spans.span(30)));
    Assert.assertTrue(TextTokenizer.isListMark(spans.span(31)));
    Assert.assertTrue(TextTokenizer.isWord(spans.span(32)));
    Assert.assertTrue(TextTokenizer.isListMark(spans.span(33)));
    Assert.assertTrue(TextTokenizer.isWord(spans.span(34)));
  }

  @Test
  public void testTokenizePapers() {

    TextTokenizer tokenizer = new TextTokenizer();
    List<Map.Entry<String, SpanSequence>> papers = DocumentTest.papers().map(
        document -> (Map.Entry<String, SpanSequence>) new AbstractMap.SimpleImmutableEntry<>(
            document.docId(), tokenizer.apply((String) document.text()))).toList();

    Optional<String> hasApostrophe = papers.stream()
        .filter(e -> e.getValue().stream().anyMatch(TextTokenizer::isApostrophe)).map(Entry::getKey)
        .findFirst();

    Assert.assertEquals("5677", hasApostrophe.get());

    Optional<String> hasArrow = papers.stream()
        .filter(e -> e.getValue().stream().anyMatch(TextTokenizer::isArrow)).map(Entry::getKey)
        .findFirst();

    Assert.assertEquals("5677", hasArrow.get());

    Optional<String> hasBracket = papers.stream()
        .filter(e -> e.getValue().stream().anyMatch(TextTokenizer::isBracket)).map(Entry::getKey)
        .findFirst();

    Assert.assertEquals("5677", hasBracket.get());

    Optional<String> hasCjkSymbol = papers.stream()
        .filter(e -> e.getValue().stream().anyMatch(TextTokenizer::isCjkSymbol)).map(Entry::getKey)
        .findFirst();

    Assert.assertEquals("5641", hasCjkSymbol.get());

    Optional<String> hasCurrency = papers.stream()
        .filter(e -> e.getValue().stream().anyMatch(TextTokenizer::isCurrency)).map(Entry::getKey)
        .findFirst();

    Assert.assertEquals("6002", hasCurrency.get());

    Optional<String> hasDoubleQuotationMark = papers.stream()
        .filter(e -> e.getValue().stream().anyMatch(TextTokenizer::isDoubleQuotationMark))
        .map(Entry::getKey).findFirst();

    Assert.assertEquals("5677", hasDoubleQuotationMark.get());

    Optional<String> hasGeneralPunctuation = papers.stream()
        .filter(e -> e.getValue().stream().anyMatch(TextTokenizer::isGeneralPunctuation))
        .map(Entry::getKey).findFirst();

    Assert.assertEquals("5677", hasGeneralPunctuation.get());

    Optional<String> hasNumber = papers.stream()
        .filter(e -> e.getValue().stream().anyMatch(TextTokenizer::isNumber)).map(Entry::getKey)
        .findFirst();

    Assert.assertEquals("5677", hasNumber.get());

    Optional<String> hasListMark = papers.stream()
        .filter(e -> e.getValue().stream().anyMatch(TextTokenizer::isListMark)).map(Entry::getKey)
        .findFirst();

    Assert.assertEquals("5677", hasListMark.get());

    Optional<String> hasPunctuation = papers.stream()
        .filter(e -> e.getValue().stream().anyMatch(TextTokenizer::isPunctuation))
        .map(Entry::getKey).findFirst();

    Assert.assertEquals("5677", hasPunctuation.get());

    Optional<String> hasQuotationMark = papers.stream()
        .filter(e -> e.getValue().stream().anyMatch(TextTokenizer::isQuotationMark))
        .map(Entry::getKey).findFirst();

    Assert.assertEquals("5677", hasQuotationMark.get());

    Optional<String> hasSeparatorMark = papers.stream()
        .filter(e -> e.getValue().stream().anyMatch(TextTokenizer::isSeparatorMark))
        .map(Entry::getKey).findFirst();

    Assert.assertEquals("5677", hasSeparatorMark.get());

    Optional<String> hasSingleQuotationMark = papers.stream()
        .filter(e -> e.getValue().stream().anyMatch(TextTokenizer::isSingleQuotationMark))
        .map(Entry::getKey).findFirst();

    Assert.assertEquals("5677", hasSingleQuotationMark.get());

    Optional<String> hasTerminalMark = papers.stream()
        .filter(e -> e.getValue().stream().anyMatch(TextTokenizer::isTerminalMark))
        .map(Entry::getKey).findFirst();

    Assert.assertEquals("5677", hasTerminalMark.get());
  }

  private String text() {
    return "Welcome to Yahoo!, the world’s most visited home page. Quickly find what you’re searching for, get in touch with friends and stay in-the-know with the latest news and information. CloudSponge provides an interface to easily enable your users to import contacts from a variety of the most popular webmail services including Yahoo, Gmail and Hotmail/MSN as well as popular desktop address books such as Mac Address Book and Outlook.";
  }
}
