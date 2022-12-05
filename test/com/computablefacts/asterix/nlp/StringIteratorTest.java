package com.computablefacts.asterix.nlp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StringIteratorTest {

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

    char[] chars = new char[]{'<', '>'};
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

    char[] chars = new char[]{'<', '>'};
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

    char[] chars = new char[]{'<', '>'};
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
  public void testIsWhitespace() {
    assertTrue(StringIterator.isWhitespace(' '));
    assertTrue(StringIterator.isWhitespace('\n'));
    assertTrue(StringIterator.isWhitespace('\t'));
    assertTrue(StringIterator.isWhitespace('\u00a0'));
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
