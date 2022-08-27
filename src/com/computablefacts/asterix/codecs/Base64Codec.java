package com.computablefacts.asterix.codecs;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import com.google.re2j.Pattern;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@CheckReturnValue
final public class Base64Codec {

  private static final Pattern PATTERN_BASE64 = Pattern.compile(
      "^(?i)((?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=))(?-i)$",
      Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL);

  private Base64Codec() {
  }

  public static Base64.Encoder newEncoder() {
    return Base64.getEncoder();
  }

  public static Base64.Decoder newDecoder() {
    return Base64.getDecoder();
  }

  /**
   * Check if a string is probably encoded in Base64.
   *
   * @param str string to test.
   * @return true if the string is probably a Base64-encoded string, false otherwise.
   */
  public static boolean isProbablyBase64(@Var String str) {
    if (Strings.isNullOrEmpty(str)) {
      return false;
    }
    str = str.trim();
    return str.length() % 4 == 0 && PATTERN_BASE64.matcher(str).matches();
  }

  /**
   * Decode a string from Base64.
   *
   * @param encoder a {@link Base64.Encoder}.
   * @param value   q Base64-encoded string.
   * @return a string.
   */
  public static String encodeB64(Base64.Encoder encoder, String value) {
    return Preconditions.checkNotNull(encoder, "encoder should not be null")
        .encodeToString(Strings.nullToEmpty(value).getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Encode a string to Base64.
   *
   * @param decoder a {@link Base64.Decoder}.
   * @param value   a string to encode.
   * @return a Base64-encoded string.
   */
  public static String decodeB64(Base64.Decoder decoder, String value) {
    return new String(Preconditions.checkNotNull(decoder, "decoder should not be null")
        .decode(Preconditions.checkNotNull(value, "value should not be null")), StandardCharsets.UTF_8);
  }
}
