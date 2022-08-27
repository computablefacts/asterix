package com.computablefacts.asterix;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.util.Iterator;

/**
 * Java implementation of Jonathan Wood's "Text Parsing Helper Class".
 *
 * @see <a href="http://www.blackbeltcoder.com/Articles/strings/a-text-parsing-helper-class">Text
 * Parsing Helper Class</a>
 */
@CheckReturnValue
final public class StringIterator implements Iterator<Character> {

  public static final char CR = '\n';
  public static final char LF = '\r';
  public static final char SPACE = ' ';

  private String text_ = null;
  private int position_ = 0;

  public StringIterator(String text) {
    reset(text);
  }

  /**
   * Check if a character is a whitespace. This method takes into account Unicode space characters.
   *
   * @param c character as a unicode code point.
   * @return true if c is a space character.
   */
  public static boolean isWhitespace(int c) {
    return Character.isWhitespace(c) || Character.isSpaceChar(c);
  }

  /**
   * Check if a character is a punctuation in the standard ASCII.
   *
   * @param c character.
   * @return true iif c is a punctuation character.
   */
  public static boolean isPunctuation(char c) {
    return isInRange(c, '!', '/') || isInRange(c, ':', '@') || isInRange(c, '[', '`') || isInRange(
        c, '{', '~');
  }

  /**
   * Check if a character is a punctuation in Unicode.
   *
   * @param c character.
   * @return true iif c is a punctuation character.
   */
  @Generated
  public static boolean isGeneralPunctuation(char c) {
    return isInRange(c, '\u2000', '\u206F');
  }

  /**
   * Check if a character is a CJK symbol.
   *
   * @param c character.
   * @return true iif c is a CJK symbol.
   */
  @Generated
  public static boolean isCjkSymbol(char c) {
    return isInRange(c, '\u3001', '\u3003') || isInRange(c, '\u3008', '\u301F');
  }

  /**
   * Check if a character is a currency symbol.
   *
   * @param c character.
   * @return true iif c is a currency symbol.
   */
  public static boolean isCurrency(char c) {
    return (c == '$') || isInRange(c, '\u00A2', '\u00A5') || isInRange(c, '\u20A0', '\u20CF');
  }

  /**
   * Check if a character is an arrow symbol.
   *
   * @param c character.
   * @return true iif c is an arrow symbol.
   */
  public static boolean isArrow(char c) {
    return isInRange(c, '\u2190', '\u21FF') || isInRange(c, '\u27F0', '\u27FF') || isInRange(c,
        '\u2900', '\u297F');
  }

  /**
   * Check if a character is an hyphen.
   *
   * @param c character.
   * @return true iif c is an hyphen.
   */
  public static boolean isHyphen(char c) {
    return c == '-' || isInRange(c, '\u2010', '\u2014');
  }

  /**
   * Check if a character is an apostrophe.
   *
   * @param c character.
   * @return true iif c is an apostrophe.
   */
  @Generated
  public static boolean isApostrophe(char c) {
    return c == '\'' || c == '\u2019';
  }

  /**
   * Check if a character is a list mark.
   *
   * @param c character.
   * @return true iif c is a list mark.
   */
  public static boolean isListMark(char c) {
    return c == '-' || c == '\uF0F0' || c == '\u2022' || c == '\u2023' || c == '\u203B'
        || c == '\u2043';
  }

  /**
   * Check if a character is a final mark.
   *
   * @param c character.
   * @return true iif c is a final mark.
   */
  public static boolean isTerminalMark(char c) {
    return c == '.' || c == '?' || c == '!' || c == '\u203C' || isInRange(c, '\u2047', '\u2049');
  }

  /**
   * Check if a character is a separator.
   *
   * @param c character.
   * @return true iif c is a separator.
   */
  public static boolean isSeparatorMark(char c) {
    return c == ',' || c == ';' || c == ':' || c == '|' || c == '/' || c == '\\';
  }

  /**
   * Check if a character is a quotation mark.
   *
   * @param c character.
   * @return true iif c is a quotation mark.
   */
  public static boolean isQuotationMark(char c) {
    return isSingleQuotationMark(c) || isDoubleQuotationMark(c);
  }

  /**
   * Check if a character is a single quotation mark.
   *
   * @param c character.
   * @return true iif c is a single quotation mark.
   */
  public static boolean isSingleQuotationMark(char c) {
    return c == '\'' || c == '`' || isInRange(c, '\u2018', '\u201B');
  }

  /**
   * Check if a character is a double quotation mark.
   *
   * @param c character.
   * @return true iif c is a double quotation mark.
   */
  public static boolean isDoubleQuotationMark(char c) {
    return c == '"' || c == '«' || c == '»' || isInRange(c, '\u201C', '\u201F');
  }

  /**
   * Check if a character is a bracket.
   *
   * @param c character.
   * @return true iif c is a bracket.
   */
  public static boolean isBracket(char c) {
    return isLeftBracket(c) || isRightBracket(c);
  }

  /**
   * Check if a character is a left bracket.
   *
   * @param c character.
   * @return true iif c is a left bracket.
   */
  public static boolean isLeftBracket(char c) {
    return c == '(' || c == '{' || c == '[' || c == '<';
  }

  /**
   * Check if a character is a right bracket.
   *
   * @param c character.
   * @return true iif c is a right bracket.
   */
  public static boolean isRightBracket(char c) {
    return c == ')' || c == '}' || c == ']' || c == '>';
  }

  /**
   * Check if a character is contained in an interval.
   *
   * @param c character.
   * @param start lower bound (inclusive).
   * @param end upper bound (inclusive).
   * @return true iif c is contained in [start, end].
   */
  private static boolean isInRange(char c, int start, int end) {
    return start <= c && c <= end;
  }

  /**
   * Sets the current document and resets the current position to the start of it.
   */
  public void reset(String text) {

    Preconditions.checkNotNull(text);

    text_ = text;
    position_ = 0;
  }

  @Override
  public boolean hasNext() {
    return !isEndOfText();
  }

  @Override
  public Character next() {
    char c = peek();
    moveAhead();
    return c;
  }

  /**
   * Indicates if the current position is at the end of the current document.
   *
   * @return true iif we reached the end of the document, false otherwise.
   */
  public boolean isEndOfText() {
    return position_ >= text_.length();
  }

  /**
   * Returns the character at the specified number of characters beyond the current position, or a
   * null character if the specified position is at the end of the document.
   *
   * @param ahead The number of characters beyond the current position.
   * @return The character at the current position.
   */
  public char peek(int ahead) {

    Preconditions.checkArgument(ahead >= 0);

    int pos = position_ + ahead;
    if (pos < text_.length()) {
      return text_.charAt(pos);
    }
    return 0;
  }

  /**
   * Returns the character beyond the current position, or a null character if the specified
   * position is at the end of the document.
   *
   * @return The character at the current position.
   */
  public char peek() {
    return peek(0);
  }

  /**
   * Moves the current position ahead of one character.
   */
  public void moveAhead() {
    moveAhead(1);
  }

  /**
   * Moves the current position ahead the specified number of characters.
   *
   * @param ahead The number of characters to move ahead.
   */
  public void moveAhead(int ahead) {

    Preconditions.checkArgument(ahead >= 0);

    position_ = Math.min(position_ + ahead, text_.length());
  }

  public String string() {
    return text_;
  }

  public int position() {
    return position_;
  }

  public int remaining() {
    return text_.length() - position_;
  }

  /**
   * Extracts a substring from the specified range of the current text.
   */
  public String extract(int start) {
    return extract(start, text_.length());
  }

  /**
   * Extracts a substring from the specified range of the current text.
   */
  public String extract(int start, int end) {

    Preconditions.checkArgument(start >= 0 && start <= text_.length());
    Preconditions.checkArgument(end >= 0 && end <= text_.length() && end >= start);

    return text_.substring(start, end);
  }

  /**
   * Moves to the next occurrence of the specified string.
   *
   * @param s String to find.
   */
  public void moveTo(String s) {

    Preconditions.checkNotNull(s);

    position_ = text_.indexOf(s, position_);
    if (position_ < 0) {
      position_ = text_.length();
    }
  }

  /**
   * Moves to the next occurrence of the specified character.
   *
   * @param c Character to find.
   */
  public void moveTo(char c) {
    position_ = text_.indexOf(c, position_);
    if (position_ < 0) {
      position_ = text_.length();
    }
  }

  /**
   * Moves to the next occurrence of any one of the specified.
   *
   * @param chars Array of characters to find.
   */
  public void moveTo(char[] chars) {

    Preconditions.checkNotNull(chars);

    while (!isInArray(peek(), chars) && !isEndOfText()) {
      moveAhead();
    }
  }

  /**
   * Moves to the next occurrence of any character that is not one of the specified characters.
   *
   * @param chars Array of characters to move past.
   */
  public void movePast(char[] chars) {

    Preconditions.checkNotNull(chars);

    while (isInArray(peek(), chars) && !isEndOfText()) {
      moveAhead();
    }
  }

  /**
   * Moves the current position to the first character that is part of a newline.
   */
  public void moveToEndOfLine() {

    @Var char c = peek();

    while (c != LF && c != CR && !isEndOfText()) {
      moveAhead();
      c = peek();
    }
  }

  /**
   * Moves the current position to the next character that is a whitespace.
   */
  public void moveToWhitespace() {
    while (!isWhitespace(peek()) && !isEndOfText()) {
      moveAhead();
    }
  }

  /**
   * Moves the current position to the next character that is not whitespace.
   */
  public void movePastWhitespace() {
    while (isWhitespace(peek()) && !isEndOfText()) {
      moveAhead();
    }
  }

  /**
   * Determines if the specified character exists in the specified character array.
   *
   * @param c Character to find.
   * @param chars Character array to search.
   */
  private boolean isInArray(char c, char[] chars) {

    Preconditions.checkNotNull(chars);

    for (int i = 0; i < chars.length; i++) {
      if (chars[i] == c) {
        return true;
      }
    }
    return false;
  }
}
