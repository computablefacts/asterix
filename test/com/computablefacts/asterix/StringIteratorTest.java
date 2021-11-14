package com.computablefacts.asterix;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

public class StringIteratorTest {

  /**
   * Mostly extracted from
   * https://github.com/apache/commons-lang/blob/master/src/test/java/org/apache/commons/lang3/math/NumberUtilsTest.java
   */
  @Test
  public void testInvalidNumbers() {
    assertFalse(StringIterator.isNumber(null));
    assertFalse(StringIterator.isNumber(""));
    assertFalse(StringIterator.isNumber(" "));
    assertFalse(StringIterator.isNumber("\r\n\t"));
    assertFalse(StringIterator.isNumber("--2.3"));
    assertFalse(StringIterator.isNumber(".12.3"));
    assertFalse(StringIterator.isNumber("-123E"));
    assertFalse(StringIterator.isNumber("-123E+-212"));
    assertFalse(StringIterator.isNumber("-123E2.12"));
    assertFalse(StringIterator.isNumber("0xGF"));
    assertFalse(StringIterator.isNumber("0xFAE-1"));
    assertFalse(StringIterator.isNumber("."));
    assertFalse(StringIterator.isNumber("-0ABC123"));
    assertFalse(StringIterator.isNumber("123.4E-D"));
    assertFalse(StringIterator.isNumber("123.4ED"));
    assertFalse(StringIterator.isNumber("+000E.12345"));
    assertFalse(StringIterator.isNumber("-000E.12345"));
    assertFalse(StringIterator.isNumber("1234E5l"));
    assertFalse(StringIterator.isNumber("11a"));
    assertFalse(StringIterator.isNumber("1a"));
    assertFalse(StringIterator.isNumber("a"));
    assertFalse(StringIterator.isNumber("11g"));
    assertFalse(StringIterator.isNumber("11z"));
    assertFalse(StringIterator.isNumber("11def"));
    assertFalse(StringIterator.isNumber("11d11"));
    assertFalse(StringIterator.isNumber("11 11"));
    assertFalse(StringIterator.isNumber(" 1111"));
    assertFalse(StringIterator.isNumber("1111 "));
    assertFalse(StringIterator.isNumber("1.1L"));

    // Added
    assertFalse(StringIterator.isNumber("+00.12345"));
    assertFalse(StringIterator.isNumber("+0002.12345"));
    assertFalse(StringIterator.isNumber("0x"));
    assertFalse(StringIterator.isNumber("EE"));
    assertFalse(StringIterator.isNumber("."));
    assertFalse(StringIterator.isNumber("1E-"));
    assertFalse(StringIterator.isNumber("123.4E."));
    assertFalse(StringIterator.isNumber("123.4E15E10"));
  }

  /**
   * Mostly extracted from
   * https://github.com/apache/commons-lang/blob/master/src/test/java/org/apache/commons/lang3/math/NumberUtilsTest.java
   */
  @Test
  public void testValidNumbers() {
    assertTrue(StringIterator.isNumber("12345"));
    assertTrue(StringIterator.isNumber("1234.5"));
    assertTrue(StringIterator.isNumber(".12345"));
    assertTrue(StringIterator.isNumber("1234E5"));
    assertTrue(StringIterator.isNumber("1234E+5"));
    assertTrue(StringIterator.isNumber("1234E-5"));
    assertTrue(StringIterator.isNumber("123.4E5"));
    assertTrue(StringIterator.isNumber("-1234"));
    assertTrue(StringIterator.isNumber("-1234.5"));
    assertTrue(StringIterator.isNumber("-.12345"));
    assertTrue(StringIterator.isNumber("-0001.12345"));
    assertTrue(StringIterator.isNumber("-000.12345"));
    assertTrue(StringIterator.isNumber("-1234E5"));
    assertTrue(StringIterator.isNumber("0"));
    assertTrue(StringIterator.isNumber("-0"));
    assertTrue(StringIterator.isNumber("01234"));
    assertTrue(StringIterator.isNumber("-01234"));
    assertTrue(StringIterator.isNumber("-0xABC123"));
    assertTrue(StringIterator.isNumber("-0x0"));
    assertTrue(StringIterator.isNumber("123.4E21D"));
    assertTrue(StringIterator.isNumber("-221.23F"));
    assertTrue(StringIterator.isNumber("22338L"));
    assertTrue(StringIterator.isNumber("2."));
  }

  @Test
  public void testReverseNull() {
    assertEquals("", StringIterator.reverse(null));
  }

  @Test
  public void testReverseEmpty() {
    assertEquals("", StringIterator.reverse(""));
  }

  @Test
  public void testReverseString() {
    assertEquals("tset", StringIterator.reverse("test"));
  }

  @Test
  public void testNullStringConstructor() {

    try {
      StringIterator iterator = new StringIterator(null);
      assertTrue(false);
    } catch (NullPointerException e) {
      assertTrue(true);
    }
  }

  @Test
  public void testEmptyStringConstructor() {

    String string = "";
    StringIterator iterator = new StringIterator(string);

    assertEquals(string, iterator.string());
    assertEquals(0, iterator.position());
    assertEquals(0, iterator.remaining());
    assertEquals(0, iterator.peek());
    assertEquals((char) 0, (char) iterator.next());
    assertEquals(string, iterator.extract(0));
    assertEquals(string, iterator.extract(0, string.length()));
    assertFalse(iterator.hasNext());
    assertTrue(iterator.isEndOfText());
  }

  @Test
  public void testStringConstructor() {

    String string = "this is a <a href=\"https://www.google.com\">hyperlink</a>!";
    StringIterator iterator = new StringIterator(string);

    assertEquals(string, iterator.string());
    assertEquals(0, iterator.position());
    assertEquals(string.length(), iterator.remaining());
    assertTrue(iterator.hasNext());
    assertFalse(iterator.isEndOfText());
  }

  @Test
  public void testPeek() {

    String string = "this is a <a href=\"https://www.google.com\">hyperlink</a>!";
    StringIterator iterator = new StringIterator(string);

    assertEquals('t', iterator.peek());
    assertEquals('t', iterator.peek(0));
    assertEquals('!', iterator.peek(string.length() - 1));
    assertEquals(0, iterator.peek(string.length()));
  }

  @Test
  public void testExtract() {

    String string = "this is a <a href=\"https://www.google.com\">hyperlink</a>!";
    StringIterator iterator = new StringIterator(string);

    assertEquals(string, iterator.extract(0));
    assertEquals(string, iterator.extract(0, string.length()));
    assertEquals("this", iterator.extract(0, 4));
    assertEquals(" ", iterator.extract(4, 5));
    assertEquals("\"", iterator.extract(18, 19));
    assertEquals("https://www.google.com", iterator.extract(19, 41));
  }

  @Test
  public void testMoveAhead() {

    String string = "this is a <a href=\"https://www.google.com\">hyperlink</a>!";
    StringIterator iterator = new StringIterator(string);

    // Next
    assertEquals('t', iterator.peek());
    assertEquals('h', iterator.peek(1));
    assertEquals(57, iterator.remaining());
    assertFalse(iterator.isEndOfText());

    // Next
    iterator.moveAhead();
    assertEquals('h', iterator.peek());
    assertEquals('i', iterator.peek(1));
    assertEquals(56, iterator.remaining());
    assertFalse(iterator.isEndOfText());

    // Move to last char
    iterator.moveAhead(string.length() - 2);
    assertEquals('!', iterator.peek());
    assertEquals(0, iterator.peek(1));
    assertEquals(1, iterator.remaining());
    assertFalse(iterator.isEndOfText());

    // Move past the last char
    iterator.moveAhead();
    assertEquals(0, iterator.peek());
    assertEquals(0, iterator.peek(1));
    assertEquals(0, iterator.remaining());
    assertTrue(iterator.isEndOfText());
  }

  @Test
  public void testMoveToString() {

    String string = "this is a <a href=\"https://www.google.com\">hyperlink</a>!";
    StringIterator iterator = new StringIterator(string);

    // Forward search
    iterator.moveTo("https");
    assertEquals('h', iterator.peek());
    assertEquals(19, iterator.position());
    assertEquals(38, iterator.remaining());

    // Stay still
    iterator.moveTo("");
    assertEquals('h', iterator.peek());
    assertEquals(19, iterator.position());
    assertEquals(38, iterator.remaining());

    // Backward search
    iterator.moveTo("this");
    assertEquals(0, iterator.peek());
    assertEquals(57, iterator.position());
    assertEquals(0, iterator.remaining());
  }

  @Test
  public void testMoveToChar() {

    String string = "this is a <a href=\"https://www.google.com\">hyperlink</a>!";
    StringIterator iterator = new StringIterator(string);

    iterator.moveTo('<');
    assertEquals('<', iterator.peek());
    assertEquals(10, iterator.position());
    assertEquals(47, iterator.remaining());
  }

  @Test
  public void testMoveToOneChar() {

    char[] chars = new char[] {'<', '>'};
    String string = "this is a <a href=\"https://www.google.com\">hyperlink</a>!";
    StringIterator iterator = new StringIterator(string);

    iterator.moveTo(chars);
    assertEquals('<', iterator.peek());
    assertEquals(10, iterator.position());

    iterator.moveAhead();
    int begin = iterator.position();

    iterator.moveTo(chars);
    int end = iterator.position();
    assertEquals('>', iterator.peek());
    assertEquals(42, end);

    assertEquals("a href=\"https://www.google.com\"", iterator.extract(begin, end));
  }

  @Test
  public void testMoveToEndOfLine() {

    String string = "this is a <a href=\"https://www.google.com\">\nhyperlink\n</a>!";
    StringIterator iterator = new StringIterator(string);

    iterator.moveToEndOfLine();
    iterator.movePastWhitespace();
    assertEquals('h', iterator.peek());
    assertEquals(44, iterator.position());
    assertEquals(15, iterator.remaining());
  }

  @Test
  public void testMoveTo() {

    char[] chars = new char[] {'<', '>'};
    String string = "this is a <a href=\"https://www.google.com\">hyperlink</a>!";
    StringIterator iterator = new StringIterator(string);

    iterator.moveTo(chars);
    assertEquals('<', iterator.peek());
    assertEquals(10, iterator.position());

    int begin = iterator.position();
    iterator.movePast(chars);

    iterator.moveTo(chars);
    iterator.movePast(chars);
    int end = iterator.position();
    assertEquals('h', iterator.peek());
    assertEquals(43, end);

    assertEquals("<a href=\"https://www.google.com\">", iterator.extract(begin, end));
  }

  @Test
  public void testMovePast() {

    char[] chars = new char[] {'<', '>'};
    String string = "this is a <a href=\"https://www.google.com\">hyperlink</a>!";
    StringIterator iterator = new StringIterator(string);

    iterator.moveTo(chars);
    assertEquals('<', iterator.peek());
    assertEquals(10, iterator.position());

    iterator.movePast(chars);
    int begin = iterator.position();

    iterator.moveTo(chars);
    int end = iterator.position();
    assertEquals('>', iterator.peek());
    assertEquals(42, end);

    assertEquals("a href=\"https://www.google.com\"", iterator.extract(begin, end));
  }

  @Test
  public void testMoveToWhitespace() {

    String string = "this is a <a href=\"https://www.google.com\">\nhyperlink\n</a>!";
    StringIterator iterator = new StringIterator(string);

    // Move to ' '
    iterator.moveTo('a');
    iterator.moveToWhitespace();
    assertEquals(' ', iterator.peek());
    assertEquals(9, iterator.position());
    assertEquals(50, iterator.remaining());

    // Move to '\n'
    iterator.moveTo('>');
    iterator.moveToWhitespace();
    assertEquals('\n', iterator.peek());
    assertEquals(43, iterator.position());
    assertEquals(16, iterator.remaining());
  }

  @Test
  public void testMovePastWhitespace() {

    String string = "this is a <a href=\"https://www.google.com\">\nhyperlink\n</a>!";
    StringIterator iterator = new StringIterator(string);

    // Move past ' '
    iterator.moveTo('a');
    iterator.moveAhead();
    iterator.movePastWhitespace();
    assertEquals('<', iterator.peek());
    assertEquals(10, iterator.position());
    assertEquals(49, iterator.remaining());

    // Move past '\n'
    iterator.moveTo('>');
    iterator.moveAhead();
    iterator.movePastWhitespace();
    assertEquals('h', iterator.peek());
    assertEquals(44, iterator.position());
    assertEquals(15, iterator.remaining());
  }

  @Test
  public void testIsLowerCase() {

    assertTrue(StringIterator.isLowerCase("lowercase"));
    assertFalse(StringIterator.isLowerCase("CamelCase"));
    assertFalse(StringIterator.isLowerCase("UPPERCASE"));

    assertTrue(StringIterator.isLowerCase(" \n\r lowercase"));
    assertTrue(StringIterator.isLowerCase("lowercase \n\r "));

    assertFalse(StringIterator.isLowerCase(" \n\r CamelCase"));
    assertFalse(StringIterator.isLowerCase("CamelCase \n\r "));

    assertFalse(StringIterator.isLowerCase(" \n\r UPPERCASE"));
    assertFalse(StringIterator.isLowerCase("UPPERCASE \n\r "));
  }

  @Test
  public void testIsUpperCase() {

    assertTrue(StringIterator.isUpperCase("UPPERCASE"));
    assertFalse(StringIterator.isUpperCase("CamelCase"));
    assertFalse(StringIterator.isUpperCase("lowercase"));

    assertTrue(StringIterator.isUpperCase(" \n\r UPPERCASE"));
    assertTrue(StringIterator.isUpperCase("UPPERCASE \n\r "));

    assertFalse(StringIterator.isUpperCase(" \n\r CamelCase"));
    assertFalse(StringIterator.isUpperCase("CamelCase \n\r "));

    assertFalse(StringIterator.isUpperCase(" \n\r lowercase"));
    assertFalse(StringIterator.isUpperCase("lowercase \n\r "));
  }

  @Test
  public void testIsCapitalized() {

    assertTrue(StringIterator.isCapitalized("Capitalized"));
    assertFalse(StringIterator.isCapitalized("UPPERCASE"));
    assertFalse(StringIterator.isCapitalized("lowercase"));
    assertFalse(StringIterator.isCapitalized("CamelCase"));

    assertTrue(StringIterator.isCapitalized(" \n\r Capitalized"));
    assertTrue(StringIterator.isCapitalized("Capitalized \n\r "));

    assertFalse(StringIterator.isCapitalized(" \n\r UPPERCASE"));
    assertFalse(StringIterator.isCapitalized("UPPERCASE \n\r "));

    assertFalse(StringIterator.isCapitalized(" \n\r lowercase"));
    assertFalse(StringIterator.isCapitalized("lowercase \n\r "));

    assertFalse(StringIterator.isCapitalized(" \n\r CamelCase"));
    assertFalse(StringIterator.isCapitalized("CamelCase \n\r "));
  }

  @Test
  public void testIsBlank() {

    assertTrue(StringIterator.isBlank(" \n\r\f\t "));
    assertFalse(StringIterator.isBlank(" nrft "));
  }

  @Test
  public void testTrimLeftEmptyString() {
    assertEquals("", StringIterator.trimLeft(""));
  }

  @Test
  public void testTrimLeft() {

    assertEquals("Capitalized", StringIterator.trimLeft(" \n Capitalized"));
    assertEquals("Capitalized", StringIterator.trimLeft(" \r Capitalized"));

    assertEquals("Capitalized \n ", StringIterator.trimLeft("Capitalized \n "));
    assertEquals("Capitalized \r ", StringIterator.trimLeft("Capitalized \r "));
  }

  @Test
  public void testTrimRightEmptyString() {
    assertEquals("", StringIterator.trimRight(""));
  }

  @Test
  public void testTrimRight() {

    assertEquals(" \n Capitalized", StringIterator.trimRight(" \n Capitalized"));
    assertEquals(" \r Capitalized", StringIterator.trimRight(" \r Capitalized"));

    assertEquals("Capitalized", StringIterator.trimRight("Capitalized \n "));
    assertEquals("Capitalized", StringIterator.trimRight("Capitalized \r "));
  }

  @Test
  public void testJoin() {

    List<String> sentence = Lists.newArrayList("Hello", "world", "!");

    assertEquals("Hello world !", StringIterator.join(sentence, ' '));
  }

  @Test
  public void testRemoveDiacriticalMarks() {

    String letters = "ÀÁÂÃÄÅàáâãäåÒÓÔÕÕÖØòóôõöøÈÉÊËèéêëðÇçÐÌÍÎÏìíîïÙÚÛÜùúûüÑñŠšŸÿýŽž";

    assertEquals("AAAAAAaaaaaaOOOOOOØoooooøEEEEeeeeðCcÐIIIIiiiiUUUUuuuuNnSsYyyZz",
        StringIterator.removeDiacriticalMarks(letters));
  }

  @Test
  public void testNormalizeNonBreakingSpace() {

    String sentence = "Hello\u00a0world\u00a0!";

    assertEquals("Hello world !", StringIterator.normalize(sentence));
  }

  @Test
  public void testNormalizeApostrophe() {

    String sentence = "l\u2019araignée";

    assertEquals("l'araignée", StringIterator.normalize(sentence));
  }

  @Test
  public void testNormalizeSingleQuote() {

    String sentence = "`Hello world !`";

    assertEquals("'Hello world !'", StringIterator.normalize(sentence));
  }

  @Test
  public void testNormalizeDoubleQuotes() {

    String sentence = "«Hello world !»";

    assertEquals("\"Hello world !\"", StringIterator.normalize(sentence));
  }

  @Test
  public void testIsPunctuation() {
    assertTrue(StringIterator.isPunctuation('.'));
    assertTrue(StringIterator.isPunctuation('?'));
    assertTrue(StringIterator.isPunctuation('!'));
    assertTrue(StringIterator.isPunctuation(','));
    assertTrue(StringIterator.isPunctuation(';'));
  }

  @Test
  public void testIsCurrency() {
    assertTrue(StringIterator.isCurrency('$'));
    assertTrue(StringIterator.isCurrency('£'));
    assertTrue(StringIterator.isCurrency('€'));
    assertTrue(StringIterator.isCurrency('¥'));
  }

  @Test
  public void testIsArrow() {
    assertTrue(StringIterator.isArrow('←'));
    assertTrue(StringIterator.isArrow('→'));
    assertTrue(StringIterator.isArrow('↑'));
    assertTrue(StringIterator.isArrow('↓'));
    assertTrue(StringIterator.isArrow('⇦'));
    assertTrue(StringIterator.isArrow('⇨'));
    assertTrue(StringIterator.isArrow('⇧'));
    assertTrue(StringIterator.isArrow('⇩'));
    assertTrue(StringIterator.isArrow('⇐'));
    assertTrue(StringIterator.isArrow('⇒'));
    assertTrue(StringIterator.isArrow('⇑'));
    assertTrue(StringIterator.isArrow('⇓'));
  }

  @Test
  public void testIsHyphen() {
    assertTrue(StringIterator.isHyphen('-'));
    assertTrue(StringIterator.isHyphen('-'));
    assertTrue(StringIterator.isHyphen('‑'));
  }

  @Test
  public void testIsListMark() {
    assertTrue(StringIterator.isListMark('-'));
    assertTrue(StringIterator.isListMark('⁃'));
    assertTrue(StringIterator.isListMark('•'));
  }

  @Test
  public void testIsTerminalMark() {
    assertTrue(StringIterator.isTerminalMark('.'));
    assertTrue(StringIterator.isTerminalMark('?'));
    assertTrue(StringIterator.isTerminalMark('!'));
  }

  @Test
  public void testIsSeparatorMark() {
    assertTrue(StringIterator.isSeparatorMark(','));
    assertTrue(StringIterator.isSeparatorMark(';'));
    assertTrue(StringIterator.isSeparatorMark(':'));
    assertTrue(StringIterator.isSeparatorMark('|'));
    assertTrue(StringIterator.isSeparatorMark('/'));
    assertTrue(StringIterator.isSeparatorMark('\\'));
  }

  @Test
  public void testIsQuotationMark() {
    assertTrue(StringIterator.isQuotationMark('\''));
    assertTrue(StringIterator.isQuotationMark('`'));
    assertTrue(StringIterator.isQuotationMark('"'));
    assertTrue(StringIterator.isQuotationMark('«'));
    assertTrue(StringIterator.isQuotationMark('»'));
  }

  @Test
  public void testIsSingleQuotationMark() {
    assertTrue(StringIterator.isSingleQuotationMark('\''));
    assertTrue(StringIterator.isSingleQuotationMark('`'));
  }

  @Test
  public void testIsDoubleQuotationMark() {
    assertTrue(StringIterator.isDoubleQuotationMark('"'));
    assertTrue(StringIterator.isDoubleQuotationMark('«'));
    assertTrue(StringIterator.isDoubleQuotationMark('»'));
  }

  @Test
  public void testIsBracket() {
    assertTrue(StringIterator.isBracket('('));
    assertTrue(StringIterator.isBracket(')'));
    assertTrue(StringIterator.isBracket('{'));
    assertTrue(StringIterator.isBracket('}'));
    assertTrue(StringIterator.isBracket('['));
    assertTrue(StringIterator.isBracket(']'));
    assertTrue(StringIterator.isBracket('<'));
    assertTrue(StringIterator.isBracket('>'));
  }

  @Test
  public void testIsLeftBracket() {
    assertTrue(StringIterator.isLeftBracket('('));
    assertTrue(StringIterator.isLeftBracket('{'));
    assertTrue(StringIterator.isLeftBracket('['));
    assertTrue(StringIterator.isLeftBracket('<'));
  }

  @Test
  public void testIsRightBracket() {
    assertTrue(StringIterator.isRightBracket(')'));
    assertTrue(StringIterator.isRightBracket('}'));
    assertTrue(StringIterator.isRightBracket(']'));
    assertTrue(StringIterator.isRightBracket('>'));
  }
}
