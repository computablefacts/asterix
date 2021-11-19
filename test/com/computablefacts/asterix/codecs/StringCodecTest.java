package com.computablefacts.asterix.codecs;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.computablefacts.asterix.Span;
import com.computablefacts.asterix.SpanSequence;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

public class StringCodecTest {

  @Test
  public void testReverseNull() {
    assertEquals("", StringCodec.reverse(null));
  }

  @Test
  public void testReverseEmpty() {
    assertEquals("", StringCodec.reverse(""));
  }

  @Test
  public void testReverseString() {
    assertEquals("tset", StringCodec.reverse("test"));
  }

  @Test
  public void testIsLowerCase() {

    assertTrue(StringCodec.isLowerCase("lowercase"));
    assertFalse(StringCodec.isLowerCase("CamelCase"));
    assertFalse(StringCodec.isLowerCase("UPPERCASE"));

    assertTrue(StringCodec.isLowerCase(" \n\r lowercase"));
    assertTrue(StringCodec.isLowerCase("lowercase \n\r "));

    assertFalse(StringCodec.isLowerCase(" \n\r CamelCase"));
    assertFalse(StringCodec.isLowerCase("CamelCase \n\r "));

    assertFalse(StringCodec.isLowerCase(" \n\r UPPERCASE"));
    assertFalse(StringCodec.isLowerCase("UPPERCASE \n\r "));
  }

  @Test
  public void testIsUpperCase() {

    assertTrue(StringCodec.isUpperCase("UPPERCASE"));
    assertFalse(StringCodec.isUpperCase("CamelCase"));
    assertFalse(StringCodec.isUpperCase("lowercase"));

    assertTrue(StringCodec.isUpperCase(" \n\r UPPERCASE"));
    assertTrue(StringCodec.isUpperCase("UPPERCASE \n\r "));

    assertFalse(StringCodec.isUpperCase(" \n\r CamelCase"));
    assertFalse(StringCodec.isUpperCase("CamelCase \n\r "));

    assertFalse(StringCodec.isUpperCase(" \n\r lowercase"));
    assertFalse(StringCodec.isUpperCase("lowercase \n\r "));
  }

  @Test
  public void testIsCapitalized() {

    assertTrue(StringCodec.isCapitalized("Capitalized"));
    assertFalse(StringCodec.isCapitalized("UPPERCASE"));
    assertFalse(StringCodec.isCapitalized("lowercase"));
    assertFalse(StringCodec.isCapitalized("CamelCase"));

    assertTrue(StringCodec.isCapitalized(" \n\r Capitalized"));
    assertTrue(StringCodec.isCapitalized("Capitalized \n\r "));

    assertFalse(StringCodec.isCapitalized(" \n\r UPPERCASE"));
    assertFalse(StringCodec.isCapitalized("UPPERCASE \n\r "));

    assertFalse(StringCodec.isCapitalized(" \n\r lowercase"));
    assertFalse(StringCodec.isCapitalized("lowercase \n\r "));

    assertFalse(StringCodec.isCapitalized(" \n\r CamelCase"));
    assertFalse(StringCodec.isCapitalized("CamelCase \n\r "));
  }

  @Test
  public void testIsBlank() {

    assertTrue(StringCodec.isBlank(" \n\r\f\t "));
    assertFalse(StringCodec.isBlank(" nrft "));
  }

  @Test
  public void testTrimLeftEmptyString() {
    assertEquals("", StringCodec.trimLeft(""));
  }

  @Test
  public void testTrimLeft() {

    assertEquals("Capitalized", StringCodec.trimLeft(" \n Capitalized"));
    assertEquals("Capitalized", StringCodec.trimLeft(" \r Capitalized"));

    assertEquals("Capitalized \n ", StringCodec.trimLeft("Capitalized \n "));
    assertEquals("Capitalized \r ", StringCodec.trimLeft("Capitalized \r "));
  }

  @Test
  public void testTrimRightEmptyString() {
    assertEquals("", StringCodec.trimRight(""));
  }

  @Test
  public void testTrimRight() {

    assertEquals(" \n Capitalized", StringCodec.trimRight(" \n Capitalized"));
    assertEquals(" \r Capitalized", StringCodec.trimRight(" \r Capitalized"));

    assertEquals("Capitalized", StringCodec.trimRight("Capitalized \n "));
    assertEquals("Capitalized", StringCodec.trimRight("Capitalized \r "));
  }

  @Test
  public void testJoin() {

    List<String> sentence = Lists.newArrayList("Hello", "world", "!");

    assertEquals("Hello world !", StringCodec.join(sentence, ' '));
  }

  @Test
  public void testRemoveDiacriticalMarks() {

    String letters = "ÀÁÂÃÄÅàáâãäåÒÓÔÕÕÖØòóôõöøÈÉÊËèéêëðÇçÐÌÍÎÏìíîïÙÚÛÜùúûüÑñŠšŸÿýŽž";

    assertEquals("AAAAAAaaaaaaOOOOOOØoooooøEEEEeeeeðCcÐIIIIiiiiUUUUuuuuNnSsYyyZz",
        StringCodec.removeDiacriticalMarks(letters));
  }

  @Test
  public void testNormalizeNonBreakingSpace() {

    String sentence = "Hello\u00a0world\u00a0!";

    assertEquals("Hello world !", StringCodec.normalize(sentence));
  }

  @Test
  public void testNormalizeApostrophe() {

    String sentence = "l\u2019araignée";

    assertEquals("l'araignée", StringCodec.normalize(sentence));
  }

  @Test
  public void testNormalizeSingleQuote() {

    String sentence = "`Hello world !`";

    assertEquals("'Hello world !'", StringCodec.normalize(sentence));
  }

  @Test
  public void testNormalizeDoubleQuotes() {

    String sentence = "«Hello world !»";

    assertEquals("\"Hello world !\"", StringCodec.normalize(sentence));
  }

  /**
   * Mostly extracted from
   * https://github.com/apache/commons-lang/blob/master/src/test/java/org/apache/commons/lang3/math/NumberUtilsTest.java
   */
  @Test
  public void testInvalidNumbers() {
    assertFalse(StringCodec.isNumber(null));
    assertFalse(StringCodec.isNumber(""));
    assertFalse(StringCodec.isNumber(" "));
    assertFalse(StringCodec.isNumber("\r\n\t"));
    assertFalse(StringCodec.isNumber("--2.3"));
    assertFalse(StringCodec.isNumber(".12.3"));
    assertFalse(StringCodec.isNumber("-123E"));
    assertFalse(StringCodec.isNumber("-123E+-212"));
    assertFalse(StringCodec.isNumber("-123E2.12"));
    assertFalse(StringCodec.isNumber("0xGF"));
    assertFalse(StringCodec.isNumber("0xFAE-1"));
    assertFalse(StringCodec.isNumber("."));
    assertFalse(StringCodec.isNumber("-0ABC123"));
    assertFalse(StringCodec.isNumber("123.4E-D"));
    assertFalse(StringCodec.isNumber("123.4ED"));
    assertFalse(StringCodec.isNumber("+000E.12345"));
    assertFalse(StringCodec.isNumber("-000E.12345"));
    assertFalse(StringCodec.isNumber("1234E5l"));
    assertFalse(StringCodec.isNumber("11a"));
    assertFalse(StringCodec.isNumber("1a"));
    assertFalse(StringCodec.isNumber("a"));
    assertFalse(StringCodec.isNumber("11g"));
    assertFalse(StringCodec.isNumber("11z"));
    assertFalse(StringCodec.isNumber("11def"));
    assertFalse(StringCodec.isNumber("11d11"));
    assertFalse(StringCodec.isNumber("11 11"));
    assertFalse(StringCodec.isNumber(" 1111"));
    assertFalse(StringCodec.isNumber("1111 "));
    assertFalse(StringCodec.isNumber("1.1L"));

    // Added
    assertFalse(StringCodec.isNumber("+00.12345"));
    assertFalse(StringCodec.isNumber("+0002.12345"));
    assertFalse(StringCodec.isNumber("0x"));
    assertFalse(StringCodec.isNumber("EE"));
    assertFalse(StringCodec.isNumber("."));
    assertFalse(StringCodec.isNumber("1E-"));
    assertFalse(StringCodec.isNumber("123.4E."));
    assertFalse(StringCodec.isNumber("123.4E15E10"));
  }

  /**
   * Mostly extracted from
   * https://github.com/apache/commons-lang/blob/master/src/test/java/org/apache/commons/lang3/math/NumberUtilsTest.java
   */
  @Test
  public void testValidNumbers() {
    assertTrue(StringCodec.isNumber("12345"));
    assertTrue(StringCodec.isNumber("1234.5"));
    assertTrue(StringCodec.isNumber(".12345"));
    assertTrue(StringCodec.isNumber("1234E5"));
    assertTrue(StringCodec.isNumber("1234E+5"));
    assertTrue(StringCodec.isNumber("1234E-5"));
    assertTrue(StringCodec.isNumber("123.4E5"));
    assertTrue(StringCodec.isNumber("-1234"));
    assertTrue(StringCodec.isNumber("-1234.5"));
    assertTrue(StringCodec.isNumber("-.12345"));
    assertTrue(StringCodec.isNumber("-0001.12345"));
    assertTrue(StringCodec.isNumber("-000.12345"));
    assertTrue(StringCodec.isNumber("-1234E5"));
    assertTrue(StringCodec.isNumber("0"));
    assertTrue(StringCodec.isNumber("-0"));
    assertTrue(StringCodec.isNumber("01234"));
    assertTrue(StringCodec.isNumber("-01234"));
    assertTrue(StringCodec.isNumber("-0xABC123"));
    assertTrue(StringCodec.isNumber("-0x0"));
    assertTrue(StringCodec.isNumber("123.4E21D"));
    assertTrue(StringCodec.isNumber("-221.23F"));
    assertTrue(StringCodec.isNumber("22338L"));
    assertTrue(StringCodec.isNumber("2."));
  }


  @Test
  public void testLexicodeEdgeCases() {
    assertEquals("", StringCodec.defaultLexicoder(null));
    assertEquals("", StringCodec.defaultLexicoder(""));
  }

  @Test
  public void testLexicodeString() {
    assertEquals("abc", StringCodec.defaultLexicoder("abc"));
    assertEquals("   ", StringCodec.defaultLexicoder("   "));
    assertEquals("abc 123", StringCodec.defaultLexicoder("abc 123"));
    assertEquals("123 abc", StringCodec.defaultLexicoder("123 abc"));
  }

  @Test
  public void testLexicodeInteger() {
    assertEquals("?0*", StringCodec.defaultLexicoder(0));
    assertEquals("??220*", StringCodec.defaultLexicoder(20));
    assertEquals("**779?", StringCodec.defaultLexicoder(-20));
    assertEquals("?0*", StringCodec.defaultLexicoder(1 / 3));
    assertEquals("?3*", StringCodec.defaultLexicoder(6 / 2));
    assertEquals("??3522*", StringCodec.defaultLexicoder(0x20A));
  }

  @Test
  public void testLexicodeLong() {
    assertEquals("??9123456789*", StringCodec.defaultLexicoder(123456789L));
    assertEquals("**0876543210?", StringCodec.defaultLexicoder(-123456789L));
  }

  @Test
  public void testLexicodeDouble() {
    assertEquals("?1.5*", StringCodec.defaultLexicoder(1.5d));
    assertEquals("?0.0*", StringCodec.defaultLexicoder(0.0d));
    assertEquals("*8:4?", StringCodec.defaultLexicoder(-1.5d));
    assertEquals("?1.5555555555555556*",
        StringCodec.defaultLexicoder(1.555555555555555555555555555555555555d));
    assertEquals("?0.3333333333333333*", StringCodec.defaultLexicoder(1.0d / 3d));
    assertEquals("?3.0*", StringCodec.defaultLexicoder(6d / 2.0d));
    assertEquals("inf", StringCodec.defaultLexicoder(1.5d / 0.0d));
    assertEquals("inf", StringCodec.defaultLexicoder(-1.5d / 0.0d));
    assertEquals("NaN", StringCodec.defaultLexicoder(0.0d / 0.0d));
  }

  @Test
  public void testLexicodeFloat() {
    assertEquals("?1.5*", StringCodec.defaultLexicoder(1.5f));
    assertEquals("?0.0*", StringCodec.defaultLexicoder(0.0f));
    assertEquals("*8:4?", StringCodec.defaultLexicoder(-1.5f));
    assertEquals("?1.5555555820465088*",
        StringCodec.defaultLexicoder(1.555555555555555555555555555555555555f));
    assertEquals("?3.0*", StringCodec.defaultLexicoder(6f / 2.0f));
    assertEquals("inf", StringCodec.defaultLexicoder(1.5f / 0.0f));
    assertEquals("inf", StringCodec.defaultLexicoder(-1.5f / 0.0f));
    assertEquals("NaN", StringCodec.defaultLexicoder(0.0f / 0.0f));
  }

  @Test
  public void testLexicodeDate() {
    Date date = new Date();
    assertEquals(DateTimeFormatter.ISO_INSTANT.format(date.toInstant()),
        StringCodec.defaultLexicoder(date));
  }

  @Test
  public void testLexicodeList() {
    List<Integer> listInt = Ints.asList(1, 2, 3, 4, 5);
    List<Integer> listEmpty = Ints.asList();
    assertEquals("", StringCodec.defaultLexicoder(listInt));
    assertEquals("", StringCodec.defaultLexicoder(listEmpty));
  }

  @Test
  public void testLexicodeMap() {
    Map<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 3);
    Map<String, Integer> mapEmpty = ImmutableMap.of();
    assertEquals("", StringCodec.defaultLexicoder(map));
    assertEquals("", StringCodec.defaultLexicoder(mapEmpty));
  }

  @Test
  public void testLexicodeArray() {
    int[] array = {1, 2, 3};
    int[] arrayEmpty = {};
    assertEquals("", StringCodec.defaultLexicoder(array));
    assertEquals("", StringCodec.defaultLexicoder(arrayEmpty));
  }

  @Test
  public void testLexicodeCharacter() {
    // TODO : tous les caractères sont mappés sur la chaîne de caractères vide. Est-ce correct?
    assertEquals("", StringCodec.defaultLexicoder('a'));
    assertEquals("", StringCodec.defaultLexicoder('\0'));
  }

  @Test
  public void testLexicodeBigInteger() {
    assertEquals("??210*", StringCodec.defaultLexicoder(BigInteger.valueOf(10)));
    assertEquals("??214*", StringCodec.defaultLexicoder(BigInteger.valueOf(14)));
  }

  @Test
  public void testLexicodeBigDecimal() {
    assertEquals("??210*", StringCodec.defaultLexicoder(BigDecimal.valueOf(10)));
    assertEquals("??214.7*", StringCodec.defaultLexicoder(BigDecimal.valueOf(14.7)));
  }

  @Test
  public void testLexicodeBoolean() {
    assertEquals("true", StringCodec.defaultLexicoder(true));
    assertEquals("false", StringCodec.defaultLexicoder(false));
  }

  @Test
  public void testTokenizeNullString() {
    assertEquals(new SpanSequence(), StringCodec.defaultTokenizer(null));
  }

  @Test
  public void testTokenizeEmptyString() {
    assertEquals(new SpanSequence(), StringCodec.defaultTokenizer(""));
  }

  @Test
  public void testTokenizeGibberish() {

    SpanSequence spansComputed = StringCodec.defaultTokenizer("~\"`!@#$%^&*()-+=[]{}\\|;:,.<>?/_");

    String textNormalized = "~\"'!@#$%^&*()-+=[]{}\\|;:,.<>?/_";
    SpanSequence spansExpected = new SpanSequence();
    spansExpected.add(new Span(textNormalized, 6, 7)); // $

    assertEquals(spansExpected, spansComputed);
  }

  @Test
  public void testTokenizeSentence() {

    SpanSequence spansComputed =
        StringCodec.defaultTokenizer("Nous sommes le 29 avril 2017 (29/04/2017) et il est 12:43.");

    String textNormalized = "nous sommes le 29 avril 2017 (29/04/2017) et il est 12:43.";
    SpanSequence spansExpected = new SpanSequence();
    spansExpected.add(new Span(textNormalized, 0, 4)); // nous
    spansExpected.add(new Span(textNormalized, 5, 11)); // sommes
    spansExpected.add(new Span(textNormalized, 12, 14)); // le
    spansExpected.add(new Span(textNormalized, 15, 17)); // 29
    spansExpected.add(new Span(textNormalized, 18, 23)); // avril
    spansExpected.add(new Span(textNormalized, 24, 28)); // 2017
    spansExpected.add(new Span(textNormalized, 30, 32)); // 29
    spansExpected.add(new Span(textNormalized, 33, 35)); // 04
    spansExpected.add(new Span(textNormalized, 36, 40)); // 2017
    spansExpected.add(new Span(textNormalized, 42, 44)); // et
    spansExpected.add(new Span(textNormalized, 45, 47)); // il
    spansExpected.add(new Span(textNormalized, 48, 51)); // est
    spansExpected.add(new Span(textNormalized, 52, 54)); // 12
    spansExpected.add(new Span(textNormalized, 55, 57)); // 43

    assertEquals(spansExpected, spansComputed);
  }

  @Test
  public void testTokenizeEmail() {

    SpanSequence spansComputed = StringCodec.defaultTokenizer("csavelief@mncc.fr.");

    String textNormalized = "csavelief@mncc.fr.";
    SpanSequence spansExpected = new SpanSequence();
    spansExpected.add(new Span(textNormalized, 0, 9)); // csavelief
    spansExpected.add(new Span(textNormalized, 10, 14)); // mncc
    spansExpected.add(new Span(textNormalized, 15, 17)); // fr

    assertEquals(spansExpected, spansComputed);
  }

  @Test
  public void testSplitOnNewline() {

    SpanSequence sequence = StringCodec.defaultTokenizer("Tom\n\nCruise");

    assertEquals(2, sequence.size());
    assertEquals(new Span("tom\n\ncruise", 0, 3), sequence.span(0));
    assertEquals(new Span("tom\n\ncruise", 5, 11), sequence.span(1));
  }

  @Test
  public void testSplitOnTab() {

    SpanSequence sequence = StringCodec.defaultTokenizer("Tom\t\tCruise");

    assertEquals(2, sequence.size());
    assertEquals(new Span("tom\t\tcruise", 0, 3), sequence.span(0));
    assertEquals(new Span("tom\t\tcruise", 5, 11), sequence.span(1));
  }

  @Test
  public void testSplitOnCarriageReturn() {

    SpanSequence sequence = StringCodec.defaultTokenizer("Tom\r\rCruise");

    assertEquals(2, sequence.size());
    assertEquals(new Span("tom\r\rcruise", 0, 3), sequence.span(0));
    assertEquals(new Span("tom\r\rcruise", 5, 11), sequence.span(1));
  }

  @Test
  public void testSplitOnWhitespace() {

    SpanSequence sequence = StringCodec.defaultTokenizer("Tom  Cruise");

    assertEquals(2, sequence.size());
    assertEquals(new Span("tom  cruise", 0, 3), sequence.span(0));
    assertEquals(new Span("tom  cruise", 5, 11), sequence.span(1));
  }

  @Test
  public void testSplitOnNoBreakSpace() {

    SpanSequence sequence = StringCodec.defaultTokenizer("Tom\u00a0\u00a0Cruise");

    assertEquals(2, sequence.size());
    assertEquals(new Span("tom  cruise", 0, 3), sequence.span(0));
    assertEquals(new Span("tom  cruise", 5, 11), sequence.span(1));
  }

  @Test
  public void testCoerceNull() {
    Assert.assertNull(StringCodec.defaultCoercer(null, true));
    Assert.assertNull(StringCodec.defaultCoercer(null, false));
  }

  @Test
  public void testCoerceQuotedString() {
    Assert.assertEquals("\"string\"", StringCodec.defaultCoercer("\"string\""));
  }

  @Test
  public void testCoerceBoolean() {

    Assert.assertEquals(true, StringCodec.defaultCoercer("true"));
    Assert.assertEquals(true, StringCodec.defaultCoercer("TRUE"));
    Assert.assertEquals(true, StringCodec.defaultCoercer(true));

    Assert.assertEquals(false, StringCodec.defaultCoercer("false"));
    Assert.assertEquals(false, StringCodec.defaultCoercer("FALSE"));
    Assert.assertEquals(false, StringCodec.defaultCoercer(false));
  }

  @Test
  public void testCoerceInteger() {

    // int
    Assert.assertEquals(BigInteger.ONE, StringCodec.defaultCoercer(1));
    Assert.assertNotEquals(BigDecimal.ONE, StringCodec.defaultCoercer(1));

    // Integer
    Assert.assertEquals(BigInteger.ONE, StringCodec.defaultCoercer("1"));
    Assert.assertEquals(BigInteger.ONE, StringCodec.defaultCoercer(BigInteger.valueOf(1)));

    Assert.assertEquals(BigInteger.valueOf(1), StringCodec.defaultCoercer("1"));
    Assert.assertEquals(BigInteger.valueOf(1), StringCodec.defaultCoercer(BigInteger.valueOf(1)));

    // double
    Assert.assertNotEquals(BigDecimal.ONE, StringCodec.defaultCoercer("1.0"));
    Assert.assertNotEquals(BigDecimal.ONE, StringCodec.defaultCoercer(BigDecimal.valueOf(1.0)));

    Assert.assertEquals(BigDecimal.valueOf(1.0), StringCodec.defaultCoercer("1.0"));
    Assert.assertEquals(BigDecimal.valueOf(1.0),
        StringCodec.defaultCoercer(BigDecimal.valueOf(1.0)));
  }

  @Test
  public void testCoerceLong() {

    BigInteger bigInteger = BigInteger.valueOf(Long.MAX_VALUE);
    BigDecimal bigDecimal = BigDecimal.valueOf(Long.MAX_VALUE);

    Assert.assertEquals(bigInteger, StringCodec.defaultCoercer("9223372036854775807"));
    Assert.assertEquals(bigInteger, StringCodec.defaultCoercer(Long.MAX_VALUE));
    Assert.assertEquals(bigDecimal, StringCodec.defaultCoercer(bigDecimal));
    Assert.assertEquals(bigInteger, StringCodec.defaultCoercer(bigInteger));
  }

  @Test
  public void testCoerceDouble() {

    BigDecimal bigDecimal = BigDecimal.valueOf(Double.MAX_VALUE);

    Assert.assertEquals(bigDecimal, StringCodec.defaultCoercer("1.7976931348623157e+308"));
    Assert.assertEquals(bigDecimal, StringCodec.defaultCoercer(Double.MAX_VALUE));
    Assert.assertEquals(bigDecimal, StringCodec.defaultCoercer(bigDecimal));
  }

  @Test
  public void testCoerceFloat() {

    BigDecimal bigDecimal = BigDecimal.valueOf(Float.MAX_VALUE);

    // Should be 3.4028235e+38 but is 3.4028234663852886E+38 due to rounding shenanigans
    Assert.assertEquals(bigDecimal, StringCodec.defaultCoercer("3.4028234663852886E+38"));
    Assert.assertEquals(bigDecimal, StringCodec.defaultCoercer(Float.MAX_VALUE));
    Assert.assertEquals(bigDecimal, StringCodec.defaultCoercer(bigDecimal));
  }

  @Test
  public void testCoerceDateString() {

    Assert.assertEquals(Date.from(Instant.parse("2021-04-21T19:02:00Z")),
        StringCodec.defaultCoercer("2021-04-21T19:02:00Z"));

    Assert.assertEquals(Date.from(Instant.parse("2021-04-21T19:02:00.000Z")),
        StringCodec.defaultCoercer("2021-04-21T19:02:00.000Z"));
  }

  @Test
  public void testCoerceNumberStringEndingWithDot() {
    Assert.assertEquals("1.", StringCodec.defaultCoercer("1."));
  }

  @Test
  public void testCoerceNumberStringStartingWithDot() {
    Assert.assertEquals(".1", StringCodec.defaultCoercer(".1"));
  }

  @Test
  public void testCoerceStringOnlyMadeOfZeroes() {
    Assert.assertEquals(BigInteger.ZERO, StringCodec.defaultCoercer("0"));
    Assert.assertEquals("00", StringCodec.defaultCoercer("00"));
  }

  @Test
  public void testCoerceNumberStringPrefixedWithZeroes() {
    Assert.assertEquals(BigInteger.valueOf(7), StringCodec.defaultCoercer("7"));
    Assert.assertEquals("007", StringCodec.defaultCoercer("007"));
  }

  @Test
  public void testCoerceNumberStringAsScientificNotation() {

    BigDecimal bigDecimal = BigDecimal.valueOf(7.9E+287);

    Assert.assertEquals(bigDecimal, StringCodec.defaultCoercer("79E286", true));
    Assert.assertNotEquals("79E286", StringCodec.defaultCoercer("79E286", true));

    Assert.assertNotEquals(bigDecimal, StringCodec.defaultCoercer("79E286", false));
    Assert.assertEquals("79E286", StringCodec.defaultCoercer("79E286", false));
  }
}
