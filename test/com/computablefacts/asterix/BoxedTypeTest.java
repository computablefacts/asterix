package com.computablefacts.asterix;

import com.computablefacts.asterix.codecs.JsonCodec;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Assert;
import org.junit.Test;

public class BoxedTypeTest {

  @Test
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(BoxedType.class).verify();
  }

  @Test
  public void testEqualsWithNull() {

    BoxedType<?> bt = BoxedType.create(1);

    Assert.assertFalse(bt.equals(null));
    Assert.assertNull(BoxedType.empty().asString());
    Assert.assertEquals("", BoxedType.empty().toString());
  }

  @Test
  public void testEqualsWithWrongObjectType() {

    BoxedType<?> bt = BoxedType.create(1);

    Assert.assertFalse(bt.equals("string"));
  }

  @Test
  public void testEqualsWithQuotedString() {

    BoxedType<?> bt1 = BoxedType.create("\"string\"");
    BoxedType<?> bt2 = BoxedType.create("string");

    Assert.assertTrue(bt1.equals(bt2));
    Assert.assertTrue(bt2.equals(bt1));

    Assert.assertEquals("string", bt1.asString());
    Assert.assertEquals("string", bt2.asString());

    Assert.assertEquals("string", bt1.toString());
    Assert.assertEquals("string", bt2.toString());
  }

  @Test
  public void testBoolean() {

    // TRUE
    BoxedType<?> btTrue = BoxedType.create(true);

    Assert.assertEquals(BoxedType.create("true"), btTrue);
    Assert.assertEquals(BoxedType.create("TRUE"), btTrue);
    Assert.assertEquals(BoxedType.create(true), btTrue);

    Assert.assertEquals("true", btTrue.asString());
    Assert.assertEquals("true", btTrue.toString());

    Assert.assertEquals(true, btTrue.asBool());
    Assert.assertEquals(null, btTrue.asDate());
    Assert.assertEquals(null, btTrue.asInt());
    Assert.assertEquals(null, btTrue.asLong());
    Assert.assertEquals(null, btTrue.asDouble());
    Assert.assertEquals(null, btTrue.asFloat());
    Assert.assertEquals(null, btTrue.asBigDecimal());
    Assert.assertEquals(null, btTrue.asBigInteger());

    // FALSE
    BoxedType<?> btFalse = BoxedType.create(false);

    Assert.assertEquals(BoxedType.create("false"), btFalse);
    Assert.assertEquals(BoxedType.create("FALSE"), btFalse);
    Assert.assertEquals(BoxedType.create(false), btFalse);

    Assert.assertEquals("false", btFalse.asString());
    Assert.assertEquals("false", btFalse.toString());

    Assert.assertEquals(false, btFalse.asBool());
    Assert.assertEquals(null, btFalse.asDate());
    Assert.assertEquals(null, btFalse.asInt());
    Assert.assertEquals(null, btFalse.asLong());
    Assert.assertEquals(null, btFalse.asDouble());
    Assert.assertEquals(null, btFalse.asFloat());
    Assert.assertEquals(null, btFalse.asBigDecimal());
    Assert.assertEquals(null, btFalse.asBigInteger());
  }

  @Test
  public void testInteger() {

    BoxedType<?> bt = BoxedType.create(1);

    Assert.assertEquals(BoxedType.create("1"), bt);
    Assert.assertEquals(BoxedType.create(1.0), bt);
    Assert.assertEquals(BoxedType.create(BigDecimal.valueOf(1.0)), bt);
    Assert.assertEquals(BoxedType.create(BigInteger.valueOf(1)), bt);

    Assert.assertEquals("1", bt.asString());
    Assert.assertEquals("1", bt.toString());

    Assert.assertEquals(null, bt.asBool());
    Assert.assertEquals(null, bt.asDate());
    Assert.assertEquals(Integer.valueOf(1), bt.asInt());
    Assert.assertEquals(Long.valueOf(1), bt.asLong());
    Assert.assertEquals(Double.valueOf(1.0d), bt.asDouble());
    Assert.assertEquals(Float.valueOf(1.0f), bt.asFloat());
    Assert.assertEquals(BigDecimal.ONE, bt.asBigDecimal());
    Assert.assertEquals(BigInteger.ONE, bt.asBigInteger());
  }

  @Test
  public void testLong() {

    BoxedType<?> bt = BoxedType.create(Long.MAX_VALUE);

    Assert.assertEquals(BoxedType.create("9223372036854775807"), bt);
    Assert.assertEquals(BoxedType.create(Long.MAX_VALUE), bt);
    Assert.assertEquals(BoxedType.create(BigDecimal.valueOf(Long.MAX_VALUE)), bt);
    Assert.assertEquals(BoxedType.create(BigInteger.valueOf(Long.MAX_VALUE)), bt);

    Assert.assertEquals("9223372036854775807", bt.asString());
    Assert.assertEquals("9223372036854775807", bt.toString());

    Assert.assertEquals(null, bt.asBool());
    Assert.assertEquals(null, bt.asDate());
    Assert.assertEquals(Integer.valueOf(-1), bt.asInt());
    Assert.assertEquals(Long.valueOf(Long.MAX_VALUE), bt.asLong());
    Assert.assertEquals(Double.valueOf(Long.MAX_VALUE), bt.asDouble());
    Assert.assertEquals(Float.valueOf(Long.MAX_VALUE), bt.asFloat());
    Assert.assertEquals(BigDecimal.valueOf(Long.MAX_VALUE), bt.asBigDecimal());
    Assert.assertEquals(BigInteger.valueOf(Long.MAX_VALUE), bt.asBigInteger());
  }

  @Test
  public void testDouble() {

    BoxedType<?> bt = BoxedType.create(Double.MAX_VALUE);

    Assert.assertEquals(BoxedType.create("1.7976931348623157e+308"), bt);
    Assert.assertEquals(BoxedType.create(Double.MAX_VALUE), bt);
    Assert.assertEquals(BoxedType.create(BigDecimal.valueOf(Double.MAX_VALUE)), bt);

    Assert.assertEquals("1.7976931348623157E+308", bt.asString());
    Assert.assertEquals("1.7976931348623157E+308", bt.toString());

    Assert.assertEquals(null, bt.asBool());
    Assert.assertEquals(null, bt.asDate());
    Assert.assertEquals(Integer.valueOf(0), bt.asInt());
    Assert.assertEquals(Long.valueOf(0), bt.asLong());
    Assert.assertEquals(Double.valueOf(Double.MAX_VALUE), bt.asDouble());
    Assert.assertEquals(Float.valueOf(Float.POSITIVE_INFINITY), bt.asFloat());
    Assert.assertEquals(BigDecimal.valueOf(Double.MAX_VALUE), bt.asBigDecimal());
    Assert.assertEquals(BigDecimal.valueOf(Double.MAX_VALUE).toBigInteger(), bt.asBigInteger());
  }

  @Test
  public void testFloat() {

    BoxedType<?> bt = BoxedType.create(Float.MAX_VALUE);

    // Should be 3.4028235e+38 but is 3.4028234663852886E+38 due to rounding shenanigans
    Assert.assertEquals(BoxedType.create("3.4028234663852886E+38"), bt);
    Assert.assertEquals(BoxedType.create(Float.MAX_VALUE), bt);
    Assert.assertEquals(BoxedType.create(BigDecimal.valueOf(Float.MAX_VALUE)), bt);

    Assert.assertEquals("3.4028234663852886E+38", bt.asString());
    Assert.assertEquals("3.4028234663852886E+38", bt.toString());

    Assert.assertEquals(null, bt.asBool());
    Assert.assertEquals(null, bt.asDate());
    Assert.assertEquals(Integer.valueOf(-1585446912), bt.asInt());
    Assert.assertEquals(Long.valueOf(3828375778387558400L), bt.asLong());
    Assert.assertEquals(Double.valueOf(Float.MAX_VALUE), bt.asDouble());
    Assert.assertEquals(Float.valueOf(3.4028235E38f), bt.asFloat());
    Assert.assertEquals(BigDecimal.valueOf(Float.MAX_VALUE), bt.asBigDecimal());
    Assert.assertEquals(BigDecimal.valueOf(Float.MAX_VALUE).toBigInteger(), bt.asBigInteger());
  }

  @Test
  public void testDate() {

    Date date1 = new Date();
    Date date2 = new Date(date1.getTime() + 100);

    BoxedType<?> bt1 = BoxedType.create(date1);
    BoxedType<?> bt2 = BoxedType.create(date2);

    Assert.assertNotEquals(bt2, bt1);
    Assert.assertNotEquals(bt1, bt2);

    Assert.assertEquals(null, bt1.asBool());
    Assert.assertEquals(date1, bt1.asDate());
    Assert.assertEquals(null, bt1.asInt());
    Assert.assertEquals(null, bt1.asLong());
    Assert.assertEquals(null, bt1.asDouble());
    Assert.assertEquals(null, bt1.asFloat());
    Assert.assertEquals(null, bt1.asBigDecimal());
    Assert.assertEquals(null, bt1.asBigInteger());
  }

  @Test
  public void testNumberStringEndingWithDot() {

    BoxedType<?> bt = BoxedType.create("1.");

    Assert.assertEquals(BoxedType.create("1."), bt);
    Assert.assertNotEquals(BoxedType.create(1), bt);

    Assert.assertEquals("1.", bt.asString());
    Assert.assertEquals("1.", bt.toString());

    Assert.assertEquals(null, bt.asBool());
    Assert.assertEquals(null, bt.asDate());
    Assert.assertEquals(null, bt.asInt());
    Assert.assertEquals(null, bt.asLong());
    Assert.assertEquals(null, bt.asDouble());
    Assert.assertEquals(null, bt.asFloat());
    Assert.assertEquals(null, bt.asBigDecimal());
    Assert.assertEquals(null, bt.asBigInteger());
  }

  @Test
  public void testNumberStringStartingWithDot() {

    BoxedType<?> bt = BoxedType.create(".1");

    Assert.assertEquals(BoxedType.create(".1"), bt);
    Assert.assertNotEquals(BoxedType.create(0.1), bt);

    Assert.assertEquals(".1", bt.asString());
    Assert.assertEquals(".1", bt.toString());

    Assert.assertEquals(null, bt.asBool());
    Assert.assertEquals(null, bt.asDate());
    Assert.assertEquals(null, bt.asInt());
    Assert.assertEquals(null, bt.asLong());
    Assert.assertEquals(null, bt.asDouble());
    Assert.assertEquals(null, bt.asFloat());
    Assert.assertEquals(null, bt.asBigDecimal());
    Assert.assertEquals(null, bt.asBigInteger());
  }

  @Test
  public void testNumberStringAsScientificNotation() {

    BoxedType<?> bt1 = BoxedType.create("79E286");

    Assert.assertEquals(BoxedType.create("79E286"), bt1);
    Assert.assertEquals(BoxedType.create(7.9E+287), bt1);

    Assert.assertEquals("7.9E+287", bt1.asString());
    Assert.assertEquals("7.9E+287", bt1.toString());

    Assert.assertEquals(null, bt1.asBool());
    Assert.assertEquals(null, bt1.asDate());
    Assert.assertEquals(Integer.valueOf(0), bt1.asInt());
    Assert.assertEquals(Long.valueOf(0), bt1.asLong());
    Assert.assertEquals(Double.valueOf(7.9E287), bt1.asDouble());
    Assert.assertEquals(Float.valueOf(Float.POSITIVE_INFINITY), bt1.asFloat());
    Assert.assertEquals(BigDecimal.valueOf(7.9E+287), bt1.asBigDecimal());
    Assert.assertEquals(BigDecimal.valueOf(7.9E+287).toBigInteger(), bt1.asBigInteger());

    BoxedType<?> bt2 = BoxedType.create("79E286", false);

    Assert.assertEquals(BoxedType.create("79E286", false), bt2);
    Assert.assertNotEquals(BoxedType.create(7.9E+287), bt2);

    Assert.assertEquals("79E286", bt2.asString());
    Assert.assertEquals("79E286", bt2.toString());

    Assert.assertEquals(null, bt2.asBool());
    Assert.assertEquals(null, bt2.asDate());
    Assert.assertEquals(null, bt2.asInt());
    Assert.assertEquals(null, bt2.asLong());
    Assert.assertEquals(null, bt2.asDouble());
    Assert.assertEquals(null, bt2.asFloat());
    Assert.assertEquals(null, bt2.asBigDecimal());
    Assert.assertEquals(null, bt2.asBigInteger());
  }

  @Test
  public void testNullCompareTo() {

    BoxedType<?> bt1 = BoxedType.empty();
    BoxedType<?> bt2 = BoxedType.empty();

    Assert.assertTrue(bt1.compareTo(bt2).get() == 0);
  }

  @Test
  public void testBooleanCompareTo() {

    BoxedType<?> btTrue = BoxedType.create(true);

    Assert.assertTrue(BoxedType.empty().compareTo(btTrue).get() == -1);
    Assert.assertTrue(btTrue.compareTo(BoxedType.empty()).get() == 1);

    Assert.assertTrue(BoxedType.create("true").compareTo(btTrue).get() == 0);
    Assert.assertTrue(BoxedType.create("TRUE").compareTo(btTrue).get() == 0);
    Assert.assertTrue(BoxedType.create(true).compareTo(btTrue).get() == 0);

    BoxedType<?> btFalse = BoxedType.create(false);

    Assert.assertTrue(BoxedType.empty().compareTo(btFalse).get() == -1);
    Assert.assertTrue(btFalse.compareTo(BoxedType.empty()).get() == 1);

    Assert.assertTrue(BoxedType.create("false").compareTo(btFalse).get() == 0);
    Assert.assertTrue(BoxedType.create("FALSE").compareTo(btFalse).get() == 0);
    Assert.assertTrue(BoxedType.create(false).compareTo(btFalse).get() == 0);
  }

  @Test
  public void testIntegerCompareTo() {

    BoxedType<?> bt = BoxedType.create(1);

    Assert.assertTrue(BoxedType.empty().compareTo(bt).get() == -1);
    Assert.assertTrue(bt.compareTo(BoxedType.empty()).get() == 1);

    Assert.assertTrue(BoxedType.create("1").compareTo(bt).get() == 0);
    Assert.assertTrue(BoxedType.create(1.0).compareTo(bt).get() == 0);
    Assert.assertTrue(BoxedType.create(BigDecimal.valueOf(1.0)).compareTo(bt).get() == 0);
    Assert.assertTrue(BoxedType.create(BigInteger.valueOf(1)).compareTo(bt).get() == 0);
  }

  @Test
  public void testDecimalCompareTo() {

    BoxedType<?> bt = BoxedType.create(1.1);

    Assert.assertTrue(BoxedType.empty().compareTo(bt).get() == -1);
    Assert.assertTrue(bt.compareTo(BoxedType.empty()).get() == 1);

    Assert.assertTrue(BoxedType.create("1.1").compareTo(bt).get() == 0);
    Assert.assertTrue(BoxedType.create(1.1).compareTo(bt).get() == 0);
    Assert.assertTrue(BoxedType.create(BigDecimal.valueOf(1.1)).compareTo(bt).get() == 0);
  }

  @Test
  public void testNumberStringEndingWithDotCompareTo() {

    BoxedType<?> bt = BoxedType.create("1.");

    Assert.assertTrue(BoxedType.empty().compareTo(bt).get() == -1);
    Assert.assertTrue(bt.compareTo(BoxedType.empty()).get() == 1);

    Assert.assertTrue(BoxedType.create("1.").compareTo(bt).get() == 0);
  }

  @Test
  public void testNumberStringStartingWithDotCompareTo() {

    BoxedType<?> bt = BoxedType.create(".1");

    Assert.assertTrue(BoxedType.empty().compareTo(bt).get() == -1);
    Assert.assertTrue(bt.compareTo(BoxedType.empty()).get() == 1);

    Assert.assertTrue(BoxedType.create(".1").compareTo(bt).get() == 0);
  }

  @Test
  public void testNumberStringAsScientificNotationCompareTo() {

    BoxedType<?> bt1 = BoxedType.create("79E286");

    Assert.assertTrue(BoxedType.empty().compareTo(bt1).get() == -1);
    Assert.assertTrue(bt1.compareTo(BoxedType.empty()).get() == 1);

    Assert.assertTrue(BoxedType.create("79E286").compareTo(bt1).get() == 0);
    Assert.assertTrue(BoxedType.create(7.9E+287).compareTo(bt1).get() == 0);

    BoxedType<?> bt2 = BoxedType.create("79E286", false);

    Assert.assertTrue(BoxedType.empty().compareTo(bt2).get() == -1);
    Assert.assertTrue(bt2.compareTo(BoxedType.empty()).get() == 1);

    Assert.assertTrue(BoxedType.create("79E286", false).compareTo(bt2).get() == 0);
    Assert.assertFalse(BoxedType.create(7.9E+287).compareTo(bt2).isPresent());
  }

  @Test
  public void testBoxList() {

    BoxedType<?> bt1 = BoxedType.create(Lists.newArrayList(1, 2, 3, 4, 5, 6));
    BoxedType<?> bt2 = BoxedType.create(Lists.newArrayList(1, 2, 3, 4, 5, 6));
    BoxedType<?> bt3 = BoxedType.create(Lists.newArrayList(1, 2, 3));

    Assert.assertTrue(bt1.isCollection());

    Assert.assertEquals(bt1.hashCode(), bt2.hashCode());
    Assert.assertNotEquals(bt1.hashCode(), bt3.hashCode());

    Assert.assertTrue(bt1.equals(bt2));
    Assert.assertFalse(bt1.equals(bt3));

    Assert.assertEquals("[1,2,3,4,5,6]", bt1.toString());
    Assert.assertEquals("[1,2,3]", bt3.toString());
  }

  @Test
  public void testBoxSet() {

    BoxedType<?> bt1 = BoxedType.create(Sets.newHashSet(1, 2, 3, 4, 5, 6));
    BoxedType<?> bt2 = BoxedType.create(Sets.newHashSet(1, 2, 3, 4, 5, 6));
    BoxedType<?> bt3 = BoxedType.create(Sets.newHashSet(1, 2, 3));

    Assert.assertTrue(bt1.isCollection());

    Assert.assertEquals(bt1.hashCode(), bt2.hashCode());
    Assert.assertNotEquals(bt1.hashCode(), bt3.hashCode());

    Assert.assertTrue(bt1.equals(bt2));
    Assert.assertFalse(bt1.equals(bt3));
  }

  @Test
  public void testBoxStringArray() {

    BoxedType<?> bt1 = BoxedType.create(new String[]{"1", "2", "3", "4", "5", "6"});
    BoxedType<?> bt2 = BoxedType.create(new String[]{"1", "2", "3", "4", "5", "6"});
    BoxedType<?> bt3 = BoxedType.create(new String[]{"1", "2", "3"});

    Assert.assertTrue(bt1.isCollection());

    Assert.assertEquals(bt1.hashCode(), bt2.hashCode());
    Assert.assertNotEquals(bt1.hashCode(), bt3.hashCode());

    Assert.assertTrue(bt1.equals(bt2));
    Assert.assertFalse(bt1.equals(bt3));

    Assert.assertEquals("[\"1\",\"2\",\"3\",\"4\",\"5\",\"6\"]", bt1.toString());
    Assert.assertEquals("[\"1\",\"2\",\"3\"]", bt3.toString());
  }

  @Test
  public void testBoxIntArray() {

    BoxedType<?> bt1 = BoxedType.create(new int[]{1, 2, 3, 4, 5, 6});
    BoxedType<?> bt2 = BoxedType.create(new int[]{1, 2, 3, 4, 5, 6});
    BoxedType<?> bt3 = BoxedType.create(new int[]{1, 2, 3});

    Assert.assertTrue(bt1.isCollection());

    Assert.assertEquals(bt1.hashCode(), bt2.hashCode());
    Assert.assertNotEquals(bt1.hashCode(), bt3.hashCode());

    Assert.assertTrue(bt1.equals(bt2));
    Assert.assertFalse(bt1.equals(bt3));

    Assert.assertEquals("[1,2,3,4,5,6]", bt1.toString());
    Assert.assertEquals("[1,2,3]", bt3.toString());
  }

  @Test
  public void testBoxLongArray() {

    BoxedType<?> bt1 = BoxedType.create(new long[]{1, 2, 3, 4, 5, 6});
    BoxedType<?> bt2 = BoxedType.create(new long[]{1, 2, 3, 4, 5, 6});
    BoxedType<?> bt3 = BoxedType.create(new long[]{1, 2, 3});

    Assert.assertTrue(bt1.isCollection());

    Assert.assertEquals(bt1.hashCode(), bt2.hashCode());
    Assert.assertNotEquals(bt1.hashCode(), bt3.hashCode());

    Assert.assertTrue(bt1.equals(bt2));
    Assert.assertFalse(bt1.equals(bt3));

    Assert.assertEquals("[1,2,3,4,5,6]", bt1.toString());
    Assert.assertEquals("[1,2,3]", bt3.toString());
  }

  @Test
  public void testBoxFloatArray() {

    BoxedType<?> bt1 = BoxedType.create(new float[]{1, 2, 3, 4, 5, 6});
    BoxedType<?> bt2 = BoxedType.create(new float[]{1, 2, 3, 4, 5, 6});
    BoxedType<?> bt3 = BoxedType.create(new float[]{1, 2, 3});

    Assert.assertTrue(bt1.isCollection());

    Assert.assertEquals(bt1.hashCode(), bt2.hashCode());
    Assert.assertNotEquals(bt1.hashCode(), bt3.hashCode());

    Assert.assertTrue(bt1.equals(bt2));
    Assert.assertFalse(bt1.equals(bt3));

    Assert.assertEquals("[1.0,2.0,3.0,4.0,5.0,6.0]", bt1.toString());
    Assert.assertEquals("[1.0,2.0,3.0]", bt3.toString());
  }

  @Test
  public void testBoxDoubleArray() {

    BoxedType<?> bt1 = BoxedType.create(new double[]{1, 2, 3, 4, 5, 6});
    BoxedType<?> bt2 = BoxedType.create(new double[]{1, 2, 3, 4, 5, 6});
    BoxedType<?> bt3 = BoxedType.create(new double[]{1, 2, 3});

    Assert.assertTrue(bt1.isCollection());

    Assert.assertEquals(bt1.hashCode(), bt2.hashCode());
    Assert.assertNotEquals(bt1.hashCode(), bt3.hashCode());

    Assert.assertTrue(bt1.equals(bt2));
    Assert.assertFalse(bt1.equals(bt3));

    Assert.assertEquals("[1.0,2.0,3.0,4.0,5.0,6.0]", bt1.toString());
    Assert.assertEquals("[1.0,2.0,3.0]", bt3.toString());
  }

  @Test
  public void testBoxBooleanArray() {

    BoxedType<?> bt1 = BoxedType.create(new boolean[]{true, false, true, false, true, false});
    BoxedType<?> bt2 = BoxedType.create(new boolean[]{true, false, true, false, true, false});
    BoxedType<?> bt3 = BoxedType.create(new boolean[]{true, false, true});

    Assert.assertTrue(bt1.isCollection());

    Assert.assertEquals(bt1.hashCode(), bt2.hashCode());
    Assert.assertNotEquals(bt1.hashCode(), bt3.hashCode());

    Assert.assertTrue(bt1.equals(bt2));
    Assert.assertFalse(bt1.equals(bt3));

    Assert.assertEquals("[true,false,true,false,true,false]", bt1.toString());
    Assert.assertEquals("[true,false,true]", bt3.toString());
  }

  @Test
  public void testBoxMap() {

    BoxedType<?> bt1 = BoxedType.create(JsonCodec.asObject("{\"id\":1}"));
    BoxedType<?> bt2 = BoxedType.create(JsonCodec.asObject("{\"id\":1,\"id\":2}"));
    BoxedType<?> bt3 = BoxedType.create(JsonCodec.asObject("{\"id\":1,\"id\":2,\"id\":3}"));

    Assert.assertTrue(bt1.isMap());

    Assert.assertNotEquals(bt1.hashCode(), bt2.hashCode());
    Assert.assertNotEquals(bt1.hashCode(), bt3.hashCode());

    Assert.assertFalse(bt1.equals(bt2));
    Assert.assertFalse(bt1.equals(bt3));

    Assert.assertEquals("{\"id\":1}", bt1.toString());
    Assert.assertEquals("{\"id\":2}", bt2.toString());
    Assert.assertEquals("{\"id\":3}", bt3.toString());
  }

  @Test
  public void testBoxStringOnlyMadeOfZeroes() {

    BoxedType<?> bt1 = BoxedType.create(0);
    BoxedType<?> bt2 = BoxedType.create("0");
    BoxedType<?> bt3 = BoxedType.create("00");

    Assert.assertEquals(bt1.hashCode(), bt2.hashCode());
    Assert.assertNotEquals(bt1.hashCode(), bt3.hashCode());

    Assert.assertTrue(bt1.equals(bt2));
    Assert.assertFalse(bt1.equals(bt3));

    Assert.assertEquals("0", bt1.toString());
    Assert.assertEquals("0", bt2.toString());
    Assert.assertEquals("00", bt3.toString());
  }

  @Test
  public void testBoxNumbersPrefixedWithZeroes() {

    BoxedType<?> bt1 = BoxedType.create(7);
    BoxedType<?> bt2 = BoxedType.create("7");
    BoxedType<?> bt3 = BoxedType.create("007");

    Assert.assertEquals(bt1.hashCode(), bt2.hashCode());
    Assert.assertNotEquals(bt1.hashCode(), bt3.hashCode());

    Assert.assertTrue(bt1.equals(bt2));
    Assert.assertFalse(bt1.equals(bt3));

    Assert.assertEquals("7", bt1.toString());
    Assert.assertEquals("7", bt2.toString());
    Assert.assertEquals("007", bt3.toString());
  }

  @Test
  public void testBoxLong() {
    Assert.assertEquals("0016540034028L", BoxedType.create("0016540034028L").value());
    Assert.assertEquals("16540034028L", BoxedType.create("16540034028L").value());
  }

  @Test
  public void testBoxDoubleOrFloat() {

    // double
    Assert.assertEquals("0016540034028D", BoxedType.create("0016540034028D").value());
    Assert.assertEquals("16540034028D", BoxedType.create("16540034028D").value());

    // float
    Assert.assertEquals("0016540034028F", BoxedType.create("0016540034028F").value());
    Assert.assertEquals("16540034028F", BoxedType.create("16540034028F").value());
  }

  @Test
  public void testAsCollection() {
    Assert.assertEquals(Lists.newArrayList("string"), BoxedType.create("string").asCollection());
    Assert.assertEquals(Lists.newArrayList(BigInteger.valueOf(1)), BoxedType.create(1).asCollection());
    Assert.assertEquals(Lists.newArrayList(BigInteger.valueOf(1L)), BoxedType.create(1L).asCollection());
    Assert.assertEquals(Lists.newArrayList(BigDecimal.valueOf(1.0f)), BoxedType.create(1.0f).asCollection());
    Assert.assertEquals(Lists.newArrayList(BigDecimal.valueOf(1.0d)), BoxedType.create(1.0d).asCollection());
  }

  @Test
  public void testAsMap() {

    Map<Object, Object> map = new HashMap<>();
    map.put("root", "string");

    Assert.assertEquals(map, BoxedType.create("string").asMap());

    map.put("root", BigInteger.valueOf(1));

    Assert.assertEquals(map, BoxedType.create(1).asMap());

    map.put("root", BigInteger.valueOf(1L));

    Assert.assertEquals(map, BoxedType.create(1L).asMap());

    map.put("root", BigDecimal.valueOf(1.0f));

    Assert.assertEquals(map, BoxedType.create(1.0f).asMap());

    map.put("root", BigDecimal.valueOf(1.0d));

    Assert.assertEquals(map, BoxedType.create(1.0d).asMap());
  }
}