package com.computablefacts.asterix.codecs;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.Normalizer;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import com.computablefacts.asterix.Span;
import com.computablefacts.asterix.SpanSequence;
import com.computablefacts.asterix.StringIterator;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import com.google.re2j.Matcher;
import com.google.re2j.Pattern;

@CheckReturnValue
final public class StringCodec {

  private static final SpanSequence SPAN_SEQUENCE_EMPTY = new SpanSequence();
  private static final Pattern SPLIT_ON_PUNCT =
      Pattern.compile("[^\r\n\t\\p{P}\\p{Zs}\\|\\^<>+=~]+",
          Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

  private StringCodec() {}

  /**
   * Reverse a string.
   *
   * @param string the string to reverse.
   * @return the reversed string.
   */
  public static String reverse(String string) {
    return new StringBuilder(Strings.nullToEmpty(string)).reverse().toString();
  }

  /**
   * Normalize quotation marks and apostrophes.
   *
   * @param text document.
   * @return A normalized text.
   */
  public static String normalize(String text) {

    Preconditions.checkArgument(!Strings.isNullOrEmpty(text));

    StringBuilder builder = new StringBuilder(text.length());
    StringIterator iterator = new StringIterator(text);

    while (iterator.hasNext()) {
      char c = iterator.next();
      if (StringIterator.isApostrophe(c)) {
        builder.append('\'');
      } else if (StringIterator.isSingleQuotationMark(c)) {
        builder.append('\'');
      } else if (StringIterator.isDoubleQuotationMark(c)) {
        builder.append('"');
      } else if (c == '\u00a0' /* non-breaking space */) {
        builder.append(StringIterator.SPACE);
      } else {
        if (c != '\r' || iterator.peek() != '\n') { // convert Windows EOL to Unix EOL
          builder.append(c);
        }
      }
    }
    return builder.toString();
  }

  /**
   * Remove whitespace prefix from string.
   *
   * @param s string.
   * @return string without whitespaces at the beginning.
   */
  public static String trimLeft(String s) {

    Preconditions.checkNotNull(s);

    for (int i = 0; i < s.length(); i++) {
      int c = s.codePointAt(i);
      if (!StringIterator.isWhitespace(c)) {
        return i == 0 ? s : s.substring(i);
      }
    }
    return "";
  }

  /**
   * Remove whitespace suffix from string.
   *
   * @param s string.
   * @return string without whitespaces at the end.
   */
  public static String trimRight(String s) {

    Preconditions.checkNotNull(s);

    for (int i = s.length() - 1; i >= 0; i--) {
      int c = s.codePointAt(i);
      if (!StringIterator.isWhitespace(c)) {
        return i == s.length() - 1 ? s : s.substring(0, i + 1);
      }
    }
    return "";
  }

  /**
   * Check if a string is blank.
   *
   * @param s string.
   * @return true iif s is only made of whitespace characters.
   */
  public static boolean isBlank(String s) {

    Preconditions.checkNotNull(s);

    for (int i = 0; i < s.length(); i++) {
      int c = s.codePointAt(i);
      if (!StringIterator.isWhitespace(c)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Check if a string is capitalized.
   *
   * @param s string.
   * @return true iif s starts with an upper case character and all other characters are lower case.
   */
  public static boolean isCapitalized(String s) {

    Preconditions.checkNotNull(s);

    @Var
    boolean isFirst = true;

    for (int i = 0; i < s.length(); i++) {
      int c = s.codePointAt(i);
      if (isFirst) {
        if (c != Character.toUpperCase(c)) {
          return false;
        }
        isFirst = StringIterator.isWhitespace(c);
      } else {
        if (c != Character.toLowerCase(c)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Check if a string is upper case.
   *
   * @param s string.
   * @return true iif s is only made of upper case characters.
   */
  public static boolean isUpperCase(String s) {

    Preconditions.checkNotNull(s);

    for (int i = 0; i < s.length(); i++) {
      int c = s.codePointAt(i);
      if (c != Character.toUpperCase(c)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Check if a string is lower case.
   *
   * @param s string.
   * @return true iif s is only made of lower case characters.
   */
  public static boolean isLowerCase(String s) {

    Preconditions.checkNotNull(s);

    for (int i = 0; i < s.length(); i++) {
      int c = s.codePointAt(i);
      if (c != Character.toLowerCase(c)) {
        return false;
      }
    }
    return true;
  }

  /**
   * A string normalizer which performs the following steps:
   * <ol>
   * <li>Unicode canonical decomposition ({@link Normalizer.Form#NFD})</li>
   * <li>Removal of diacritical marks</li>
   * <li>Unicode canonical composition ({@link Normalizer.Form#NFC})</li>
   * </ol>
   */
  public static String removeDiacriticalMarks(String s) {

    Preconditions.checkNotNull(s);

    String decomposed = Normalizer.normalize(s, Normalizer.Form.NFD);
    java.util.regex.Pattern diacriticals =
        java.util.regex.Pattern.compile("\\p{InCombiningDiacriticalMarks}");
    java.util.regex.Matcher matcher = diacriticals.matcher(decomposed);
    String noDiacriticals = matcher.replaceAll("");
    return Normalizer.normalize(noDiacriticals, Normalizer.Form.NFC);
  }

  /**
   * Join a list of strings. Similar to Guava's
   *
   * <pre>
   * Joiner.on(separator).join(strings)
   * </pre>
   *
   * @return a string.
   */
  public static String join(List<String> strings, char separator) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < strings.size(); i++) {
      if (i > 0) {
        builder.append(separator);
      }
      builder.append(strings.get(i));
    }
    return builder.toString();
  }

  /**
   * <p>
   * Checks whether the String a valid Java number.
   * </p>
   *
   * <p>
   * Valid numbers include hexadecimal marked with the <code>0x</code> qualifier, scientific
   * notation and numbers marked with a type qualifier (e.g. 123L).
   * </p>
   *
   * <p>
   * <code>Null</code> and empty String will return <code>false</code>.
   * </p>
   *
   * @param text the <code>String</code> to check
   * @return <code>true</code> if the string is a correctly formatted number
   *
   * @see <a href=
   *      "https://commons.apache.org/proper/commons-math">https://commons.apache.org/proper/commons-math</a>
   */
  public static boolean isNumber(String text) {

    if (Strings.isNullOrEmpty(text)) {
      return false;
    }

    char[] chars = text.toCharArray();
    @Var
    int sz = chars.length;
    @Var
    boolean hasExp = false;
    @Var
    boolean hasDecPoint = false;
    @Var
    boolean allowSigns = false;
    @Var
    boolean foundDigit = false;

    // deal with any possible sign up front
    int start = (chars[0] == '-') ? 1 : 0;
    if (sz > start + 1 && chars[start] == '0' && chars[start + 1] == 'x') {
      @Var
      int i = start + 2;
      if (i == sz) {
        return false; // text == "0x"
      }

      // checking hex (it can't be anything else)
      for (; i < chars.length; i++) {
        if ((chars[i] < '0' || chars[i] > '9') && (chars[i] < 'a' || chars[i] > 'f')
            && (chars[i] < 'A' || chars[i] > 'F')) {
          return false;
        }
      }
      return true;
    }

    sz--; // don't want to loop to the last char, check it afterwords

    // for type qualifiers
    @Var
    int i = start;

    // loop to the next to last char or to the last char if we need another digit to
    // make a valid number (e.g. chars[0..5] = "1234E")
    while (i < sz || (i < sz + 1 && allowSigns && !foundDigit)) {
      if (chars[i] >= '0' && chars[i] <= '9') {
        foundDigit = true;
        allowSigns = false;

      } else if (chars[i] == '.') {
        if (hasDecPoint || hasExp) {

          // two decimal points or dec in exponent
          return false;
        }
        hasDecPoint = true;
      } else if (chars[i] == 'e' || chars[i] == 'E') {

        // we've already taken care of hex.
        if (hasExp) {

          // two E's
          return false;
        }
        if (!foundDigit) {
          return false;
        }
        hasExp = true;
        allowSigns = true;
      } else if (chars[i] == '+' || chars[i] == '-') {
        if (!allowSigns) {
          return false;
        }
        allowSigns = false;
        foundDigit = false; // we need a digit after the E
      } else {
        return false;
      }
      i++;
    }
    if (i < chars.length) {
      if (chars[i] >= '0' && chars[i] <= '9') {

        // no type qualifier, OK
        return true;
      }
      if (chars[i] == 'e' || chars[i] == 'E') {

        // can't have an E at the last byte
        return false;
      }
      if (chars[i] == '.') {
        if (hasDecPoint || hasExp) {

          // two decimal points or dec in exponent
          return false;
        }

        // single trailing decimal point after non-exponent is ok
        return foundDigit;
      }
      if (!allowSigns
          && (chars[i] == 'd' || chars[i] == 'D' || chars[i] == 'f' || chars[i] == 'F')) {
        return foundDigit;
      }
      if (chars[i] == 'l' || chars[i] == 'L') {

        // not allowing L with an exponent or decimal point
        return foundDigit && !hasExp && !hasDecPoint;
      }

      // last character is illegal
      return false;
    }

    // allowSigns is true iff the val ends in 'E'
    // found digit it to make sure weird stuff like '.' and '1E-' doesn't pass
    return !allowSigns && foundDigit;
  }

  /**
   * A function that encodes a primitive to a lexicographically sortable string.
   *
   * @param object the primitive to lexicode.
   * @return a lexicographically sortable string.
   */
  public static String defaultLexicoder(Object object) {
    if (object == null) {
      return "";
    }
    if (object instanceof String) {
      return (String) object;
    }
    if (object instanceof Boolean) {
      return Boolean.toString((Boolean) object);
    }
    if (object instanceof BigInteger) {
      return BigDecimalCodec.encode(new BigDecimal((BigInteger) object));
    }
    if (object instanceof BigDecimal) {
      return BigDecimalCodec.encode((BigDecimal) object);
    }
    if (object instanceof Integer) {
      return BigDecimalCodec.encode(BigDecimal.valueOf((Integer) object));
    }
    if (object instanceof Long) {
      return BigDecimalCodec.encode(BigDecimal.valueOf((Long) object));
    }
    if (object instanceof Double) {
      double d = (Double) object;
      if (Double.isNaN(d)) {
        return "NaN";
      }
      if (Double.isInfinite(d)) {
        return "inf";
      }
      return BigDecimalCodec.encode(BigDecimal.valueOf(d));
    }
    if (object instanceof Float) {
      float f = (Float) object;
      if (Float.isNaN(f)) {
        return "NaN";
      }
      if (Float.isInfinite(f)) {
        return "inf";
      }
      return BigDecimalCodec.encode(BigDecimal.valueOf(f));
    }
    if (object instanceof Date) {
      return DateTimeFormatter.ISO_INSTANT.format(((Date) object).toInstant());
    }
    return "";
  }

  /**
   * A naive tokenizer that removes diacritics and split on punctuation marks.
   *
   * @param text the text to tokenize.
   * @return the extracted tokens as a {@link SpanSequence}.
   */
  public static SpanSequence defaultTokenizer(String text) {

    if (text == null || text.isEmpty()) {
      return SPAN_SEQUENCE_EMPTY;
    }

    SpanSequence spanSequence = new SpanSequence();
    String newText = removeDiacriticalMarks(normalize(text)).toLowerCase();
    Matcher matcher = SPLIT_ON_PUNCT.matcher(newText);

    while (matcher.find()) {

      int begin = matcher.start();
      int end = matcher.end();

      spanSequence.add(new Span(newText, begin, end));
    }
    return spanSequence;
  }

  /**
   * Coerce a given value.
   *
   * @param value the value to box/coerce.
   * @return a coerced value.
   */
  public static Object defaultCoercer(Object value) {
    return defaultCoercer(value, true);
  }

  /**
   * Coerce a given value.
   *
   * @param value the value to box/coerce.
   * @param interpretStringInScientificNotation true iif "79E2863560" should be interpreted as
   *        7.9E+2863561 and false otherwise.
   * @return a coerced value.
   */
  public static Object defaultCoercer(Object value, boolean interpretStringInScientificNotation) {
    if (value == null || value instanceof Boolean || value instanceof BigInteger
        || value instanceof BigDecimal) {
      return value;
    }
    if (value instanceof Integer) {
      return BigInteger.valueOf((Integer) value);
    }
    if (value instanceof Long) {
      return BigInteger.valueOf((Long) value);
    }
    if (value instanceof Float) {
      return BigDecimal.valueOf((Float) value);
    }
    if (value instanceof Double) {
      return BigDecimal.valueOf((Double) value);
    }
    if (value instanceof String) {

      // Attempt type coercion
      String text = (String) value;

      // Check if text is a boolean
      if ("true".equalsIgnoreCase(text)) {
        return true;
      }
      if ("false".equalsIgnoreCase(text)) {
        return false;
      }

      // Check if text is a date in ISO Instant format
      if (text.length() >= 20 && text.length() <= 24
          && (text.charAt(10) == 'T' || text.charAt(10) == 't')
          && (text.charAt(text.length() - 1) == 'Z' || text.charAt(text.length() - 1) == 'z')) {
        try {
          return Date.from(Instant.parse(text));
        } catch (Exception e) {
          // FALL THROUGH
        }
      }

      if (interpretStringInScientificNotation || (!text.contains("E") && !text.contains("e"))) {
        try {

          BigInteger bigInteger = new BigInteger(text);

          // Here, text is an integer (otherwise a NumberFormatException has been thrown)
          StringIterator iterator = new StringIterator(text);
          iterator.movePast(new char[] {'0'});

          // The condition below ensures "0" is interpreted as a number but "00" as a string
          if (iterator.position() > 1 || (iterator.position() > 0 && iterator.remaining() > 0)) {

            // text matching [0]+[0-9]+ should be interpreted as string
            return text;
          }
          return bigInteger;
        } catch (NumberFormatException ex) {
          // FALL THROUGH
        }

        try {

          BigDecimal bigDecimal = new BigDecimal(text);
          String textTrimmed = text.trim();

          // text matching \d+[.] should be interpreted as string
          // text matching [.]\d+ should be interpreted as string
          if (!textTrimmed.endsWith(".") && !textTrimmed.startsWith(".")) {
            return bigDecimal;
          }
        } catch (NumberFormatException ex) {
          // FALL THROUGH
        }
      }
      return text;
    }
    return value;
  }
}
