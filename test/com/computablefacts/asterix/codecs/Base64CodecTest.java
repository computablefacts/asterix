package com.computablefacts.asterix.codecs;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Test;

public class Base64CodecTest {

  @Test(expected = NullPointerException.class)
  public void testDeserializeNullEncoder() {
    String b64 = Base64Codec.decodeB64(null, "");
  }

  @Test(expected = NullPointerException.class)
  public void testDeserializeNullString() {
    String b64 = Base64Codec.decodeB64(Base64Codec.newDecoder(), null);
  }

  @Test
  public void testDeserializeEmptyString() {
    Assert.assertEquals("", Base64Codec.decodeB64(Base64Codec.newDecoder(), ""));
  }

  @Test(expected = NullPointerException.class)
  public void testSerializeNullEncoder() {
    String b64 = Base64Codec.encodeB64(null, "");
  }

  @Test
  public void testSerializeNullString() {
    Assert.assertEquals("", Base64Codec.encodeB64(Base64Codec.newEncoder(), null));
  }

  @Test
  public void testSerializeEmptyString() {
    Assert.assertEquals("", Base64Codec.encodeB64(Base64Codec.newEncoder(), ""));
  }

  @Test
  public void testEncodeDecode() {

    String b64 = Base64Codec.encodeB64(Base64Codec.newEncoder(), "test");
    String string = Base64Codec.decodeB64(Base64Codec.newDecoder(), "dGVzdA==");

    Assert.assertTrue(Base64Codec.isProbablyBase64(b64));
    Assert.assertEquals("dGVzdA==", b64);
    Assert.assertEquals("test", string);
  }

  @Test
  public void testIsProbablyBase64NullString() {
    assertFalse(Base64Codec.isProbablyBase64(null));
  }

  @Test
  public void testIsProbablyBase64EmptyString() {
    assertFalse(Base64Codec.isProbablyBase64(""));
  }

  @Test
  public void testIsProbablyBase64() {

    String text =
        "uO3lZkI9fkWmzqM3QQuBCB6XhargnehMptMRKoZQxmNDSlMYi8fBv1M7ATIpdFvQaa/MyzTbYhmeLgrCxqMIlmLDLgHG3fkVe/0Vr7eulqemWjZEJABbpLoIHjtduuzioHzyJANZQZXL9MSvADGZk3RDX6cuE8rvV5x+il1GR5PGFNq4NdFRCYm4PxBcM1XKl2b0CkvIPAY/jJoYM2hWDv9OPP5LKhzFKyNdWT6dVU+wqDInfEHqX7y2DAp+i2bhu0ZJItJmZa6tSe/XUZ/pGt/x5vy6ffXm850a3Gg6o0CwuY0tzcz+6nY0rrswbju5l2YgWb7b4Guu87gz+GLWzw==";

    assertTrue(Base64Codec.isProbablyBase64(text));
  }

  @Test
  public void testIsNotBase64() {

    String text = "====";

    assertFalse(Base64Codec.isProbablyBase64(text));
  }
}
