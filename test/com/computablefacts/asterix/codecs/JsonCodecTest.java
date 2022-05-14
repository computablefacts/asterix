package com.computablefacts.asterix.codecs;

import java.sql.Date;
import java.text.ParseException;
import java.time.Instant;
import java.util.*;

import org.junit.Assert;
import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class JsonCodecTest {

  @Test
  public void testDeserializeNullObject() {
    Assert.assertNotNull(JsonCodec.asObject(null));
  }

  @Test
  public void testDeserializeNullCollection() {
    Assert.assertNotNull(JsonCodec.asCollection(null));
  }

  @Test
  public void testDeserializeNullArray() {
    Assert.assertNotNull(JsonCodec.asArray(null));
  }

  @Test
  public void testDeserializeEmptyObject() {
    Assert.assertNotNull(JsonCodec.asObject("{}"));
  }

  @Test
  public void testDeserializeEmptyCollection() {
    Assert.assertNotNull(JsonCodec.asCollection("[]"));
  }

  @Test
  public void testDeserializeEmptyArray() {
    Assert.assertNotNull(JsonCodec.asArray("[]"));
  }

  @Test
  public void testSerializeNullObject() {
    Assert.assertEquals("{}", JsonCodec.asString((Map) null));
  }

  @Test
  public void testSerializeNullCollection() {
    Assert.assertEquals("[]", JsonCodec.asString((List) null));
  }

  @Test
  public void testSerializeNullArray() {
    Assert.assertEquals("[]", JsonCodec.asString((Map[]) null));
  }

  @Test
  public void testSerializeEmptyObject() {
    Assert.assertEquals("{}", JsonCodec.asString(new HashMap<>()));
  }

  @Test
  public void testSerializeEmptyCollection() {
    Assert.assertEquals("[]", JsonCodec.asString(new ArrayList<>()));
  }

  @Test
  public void testSerializeEmptyArray() {
    Assert.assertEquals("[]", JsonCodec.asString());
  }

  @Test
  public void testMapFromNullObject() {
    Assert.assertEquals(Collections.emptyMap(), JsonCodec.asMap((Map) null));
  }

  @Test
  public void testCollectionOfMapsFromNullCollection() {
    Assert.assertEquals(Collections.emptyList(), JsonCodec.asCollectionOfMaps((List) null));
  }

  @Test
  public void testCollectionOfMapsFromNullArray() {
    Assert.assertEquals(Collections.emptyList(), JsonCodec.asCollectionOfMaps((Map[]) null));
  }

  @Test
  public void testMapFromEmptyObject() {
    Assert.assertEquals(Collections.emptyMap(), JsonCodec.asMap(new HashMap<>()));
  }

  @Test
  public void testCollectionOfMapsFromEmptyCollection() {
    Assert.assertEquals(Collections.emptyList(), JsonCodec.asCollectionOfMaps(new ArrayList<>()));
  }

  @Test
  public void testCollectionOfMapsFromEmptyArray() {
    Assert.assertEquals(Collections.emptyList(), JsonCodec.asCollectionOfMaps());
  }

  @Test
  public void testSimpleCollection() {

    List<SimplePojo<?>> pojos = Lists.newArrayList(new SimplePojo<>("key1", "value1"),
        new SimplePojo<>("key2", 2), new SimplePojo<>("key3", false));
    String json = JsonCodec.asString(pojos);

    Assert.assertFalse(Strings.isNullOrEmpty(json));

    Collection<Map<String, Object>> collection1 = JsonCodec.asCollection(json);
    Collection<Map<String, Object>> collection2 = JsonCodec.asCollection(
        "[{\"key\":\"key1\",\"value\":\"value1\"},{\"key\":\"key2\",\"value\":2},{\"key\":\"key3\",\"value\":false}]");

    Assert.assertEquals(collection1, collection2);
  }

  @Test
  public void testSerializeDeserializeDate() throws ParseException {

    Map<String, Object> map1 =
        ImmutableMap.of("date", Date.from(Instant.parse("2004-04-01T00:00:00Z")));

    Map<String, Object> map2 = ImmutableMap.of("date", "2004-04-01T00:00:00Z");

    String json1 = JsonCodec.asString(map1);
    String json2 = JsonCodec.asString(map2);

    Assert.assertEquals("{\"date\":\"2004-04-01T00:00:00Z\"}", json1);
    Assert.assertEquals("{\"date\":\"2004-04-01T00:00:00Z\"}", json2);

    Assert.assertNotEquals(map1, JsonCodec.asObject(json1));
    Assert.assertNotEquals(map1, JsonCodec.asObject(json2));

    Assert.assertEquals(map2, JsonCodec.asObject(json1));
    Assert.assertEquals(map2, JsonCodec.asObject(json2));
  }

  @Test
  public void testFlatten() {

    String json =
        "{\"id\":\"0002\",\"type\":\"donut\",\"name\":\"Raised\",\"ppu\":0.55,\"batters\":{\"batter\":[{\"id\":\"1001\",\"type\":\"Regular\"}]},\"topping\":[{\"id\":\"5001\",\"type\":\"None\"},{\"id\":\"5002\",\"type\":\"Glazed\"},{\"id\":\"5005\",\"type\":\"Sugar\"},{\"id\":\"5003\",\"type\":\"Chocolate\"},{\"id\":\"5004\",\"type\":\"Maple\"}]}";
    Map<String, Object> map = JsonCodec.flatten(json, '¤');

    Assert.assertTrue(map.containsKey("id"));
    Assert.assertTrue(map.containsKey("type"));
    Assert.assertTrue(map.containsKey("name"));
    Assert.assertTrue(map.containsKey("ppu"));
    Assert.assertTrue(map.containsKey("batters¤batter[0]¤id"));
    Assert.assertTrue(map.containsKey("batters¤batter[0]¤type"));
    Assert.assertTrue(map.containsKey("topping[0]¤id"));
    Assert.assertTrue(map.containsKey("topping[0]¤type"));
    Assert.assertTrue(map.containsKey("topping[1]¤id"));
    Assert.assertTrue(map.containsKey("topping[1]¤type"));
    Assert.assertTrue(map.containsKey("topping[2]¤id"));
    Assert.assertTrue(map.containsKey("topping[2]¤type"));
    Assert.assertTrue(map.containsKey("topping[3]¤id"));
    Assert.assertTrue(map.containsKey("topping[3]¤type"));
    Assert.assertTrue(map.containsKey("topping[4]¤id"));
    Assert.assertTrue(map.containsKey("topping[4]¤type"));

    Assert.assertEquals("0002", map.get("id"));
    Assert.assertEquals("donut", map.get("type"));
    Assert.assertEquals("Raised", map.get("name"));
    Assert.assertEquals(0.55, map.get("ppu"));
    Assert.assertEquals("1001", map.get("batters¤batter[0]¤id"));
    Assert.assertEquals("Regular", map.get("batters¤batter[0]¤type"));
    Assert.assertEquals("5001", map.get("topping[0]¤id"));
    Assert.assertEquals("None", map.get("topping[0]¤type"));
    Assert.assertEquals("5002", map.get("topping[1]¤id"));
    Assert.assertEquals("Glazed", map.get("topping[1]¤type"));
    Assert.assertEquals("5005", map.get("topping[2]¤id"));
    Assert.assertEquals("Sugar", map.get("topping[2]¤type"));
    Assert.assertEquals("5003", map.get("topping[3]¤id"));
    Assert.assertEquals("Chocolate", map.get("topping[3]¤type"));
    Assert.assertEquals("5004", map.get("topping[4]¤id"));
    Assert.assertEquals("Maple", map.get("topping[4]¤type"));
  }

  @Test
  public void testFlattenKeepArrays() {

    String json =
        "{\"id\":\"0002\",\"type\":\"donut\",\"name\":\"Raised\",\"ppu\":0.55,\"batters\":{\"batter\":[{\"id\":\"1001\",\"type\":\"Regular\"}]},\"topping\":[{\"id\":\"5001\",\"type\":\"None\"},{\"id\":\"5002\",\"type\":\"Glazed\"},{\"id\":\"5005\",\"type\":\"Sugar\"},{\"id\":\"5003\",\"type\":\"Chocolate\"},{\"id\":\"5004\",\"type\":\"Maple\"}]}";
    Map<String, Object> map = JsonCodec.flattenKeepArrays(json, '¤');

    Assert.assertTrue(map.containsKey("id"));
    Assert.assertTrue(map.containsKey("type"));
    Assert.assertTrue(map.containsKey("name"));
    Assert.assertTrue(map.containsKey("ppu"));
    Assert.assertTrue(map.containsKey("batters¤batter"));
    Assert.assertTrue(map.containsKey("topping"));

    Assert.assertEquals("0002", map.get("id"));
    Assert.assertEquals("donut", map.get("type"));
    Assert.assertEquals("Raised", map.get("name"));
    Assert.assertEquals(0.55, map.get("ppu"));
    Assert.assertEquals(1, ((Collection<?>) map.get("batters¤batter")).size());
    Assert.assertEquals(5, ((Collection<?>) map.get("topping")).size());
  }

  @Test
  public void testFlattenKeepPrimitiveArrays() {

    String json =
        "{\"id\":\"0002\",\"type\":\"donut\",\"name\":\"Raised\",\"ppu\":0.55,\"batters\":{\"batter\":[\"Regular\",\"Slim\"]},\"topping\":[{\"id\":\"5001\",\"type\":\"None\"},{\"id\":\"5002\",\"type\":\"Glazed\"},{\"id\":\"5005\",\"type\":\"Sugar\"},{\"id\":\"5003\",\"type\":\"Chocolate\"},{\"id\":\"5004\",\"type\":\"Maple\"}]}";
    Map<String, Object> map = JsonCodec.flattenKeepPrimitiveArrays(json, '¤');

    Assert.assertTrue(map.containsKey("id"));
    Assert.assertTrue(map.containsKey("type"));
    Assert.assertTrue(map.containsKey("name"));
    Assert.assertTrue(map.containsKey("ppu"));
    Assert.assertTrue(map.containsKey("batters¤batter"));
    Assert.assertTrue(map.containsKey("topping[0]¤type"));
    Assert.assertTrue(map.containsKey("topping[1]¤id"));
    Assert.assertTrue(map.containsKey("topping[1]¤type"));
    Assert.assertTrue(map.containsKey("topping[2]¤id"));
    Assert.assertTrue(map.containsKey("topping[2]¤type"));
    Assert.assertTrue(map.containsKey("topping[3]¤id"));
    Assert.assertTrue(map.containsKey("topping[3]¤type"));
    Assert.assertTrue(map.containsKey("topping[4]¤id"));
    Assert.assertTrue(map.containsKey("topping[4]¤type"));

    Assert.assertEquals("0002", map.get("id"));
    Assert.assertEquals("donut", map.get("type"));
    Assert.assertEquals("Raised", map.get("name"));
    Assert.assertEquals(0.55, map.get("ppu"));
    Assert.assertEquals(Lists.newArrayList("Regular", "Slim"), map.get("batters¤batter"));
    Assert.assertEquals("5001", map.get("topping[0]¤id"));
    Assert.assertEquals("None", map.get("topping[0]¤type"));
    Assert.assertEquals("5002", map.get("topping[1]¤id"));
    Assert.assertEquals("Glazed", map.get("topping[1]¤type"));
    Assert.assertEquals("5005", map.get("topping[2]¤id"));
    Assert.assertEquals("Sugar", map.get("topping[2]¤type"));
    Assert.assertEquals("5003", map.get("topping[3]¤id"));
    Assert.assertEquals("Chocolate", map.get("topping[3]¤type"));
    Assert.assertEquals("5004", map.get("topping[4]¤id"));
    Assert.assertEquals("Maple", map.get("topping[4]¤type"));
  }

  @Test
  public void testUnflattenAsString() {

    Map<String, Object> map = new HashMap<>();
    map.put("id", "0002");
    map.put("type", "donut");
    map.put("name", "Raised");
    map.put("ppu", 0.55);
    map.put("batters¤batter[0]¤id", "1001");
    map.put("batters¤batter[0]¤type", "Regular");
    map.put("topping[0]¤id", "5001");
    map.put("topping[0]¤type", "None");
    map.put("topping[1]¤id", "5002");
    map.put("topping[1]¤type", "Glazed");
    map.put("topping[2]¤id", "5005");
    map.put("topping[2]¤type", "Sugar");
    map.put("topping[3]¤id", "5003");
    map.put("topping[3]¤type", "Chocolate");
    map.put("topping[4]¤id", "5004");
    map.put("topping[4]¤type", "Maple");

    String json =
        "{\"id\":\"0002\",\"type\":\"donut\",\"name\":\"Raised\",\"ppu\":0.55,\"batters\":{\"batter\":[{\"id\":\"1001\",\"type\":\"Regular\"}]},\"topping\":[{\"id\":\"5001\",\"type\":\"None\"},{\"id\":\"5002\",\"type\":\"Glazed\"},{\"id\":\"5005\",\"type\":\"Sugar\"},{\"id\":\"5003\",\"type\":\"Chocolate\"},{\"id\":\"5004\",\"type\":\"Maple\"}]}";

    Assert.assertEquals(JsonCodec.asObject(json),
        JsonCodec.asObject(JsonCodec.unflattenAsString(map, '¤')));
  }

  @Test
  public void testUnflattenAsMap() {

    Map<String, Object> map = new HashMap<>();
    map.put("id", "0002");
    map.put("type", "donut");
    map.put("name", "Raised");
    map.put("ppu", 0.55);
    map.put("batters¤batter[0]¤id", "1001");
    map.put("batters¤batter[0]¤type", "Regular");
    map.put("topping[0]¤id", "5001");
    map.put("topping[0]¤type", "None");
    map.put("topping[1]¤id", "5002");
    map.put("topping[1]¤type", "Glazed");
    map.put("topping[2]¤id", "5005");
    map.put("topping[2]¤type", "Sugar");
    map.put("topping[3]¤id", "5003");
    map.put("topping[3]¤type", "Chocolate");
    map.put("topping[4]¤id", "5004");
    map.put("topping[4]¤type", "Maple");

    String json =
        "{\"id\":\"0002\",\"type\":\"donut\",\"name\":\"Raised\",\"ppu\":0.55,\"batters\":{\"batter\":[{\"id\":\"1001\",\"type\":\"Regular\"}]},\"topping\":[{\"id\":\"5001\",\"type\":\"None\"},{\"id\":\"5002\",\"type\":\"Glazed\"},{\"id\":\"5005\",\"type\":\"Sugar\"},{\"id\":\"5003\",\"type\":\"Chocolate\"},{\"id\":\"5004\",\"type\":\"Maple\"}]}";

    Assert.assertEquals(JsonCodec.asObject(json), JsonCodec.unflattenAsMap(map, '¤'));
  }

  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private static class SimplePojo<T> {

    @JsonProperty("key")
    public final String key_;

    @JsonProperty("value")
    public final T value_;

    public SimplePojo(String key, T value) {
      key_ = key;
      value_ = value;
    }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof SimplePojo)) {
        return false;
      }
      SimplePojo pojo = (SimplePojo) o;
      return Objects.equals(key_, pojo.key_) && Objects.equals(value_, pojo.value_);
    }

    @Override
    public int hashCode() {
      return Objects.hash(key_, value_);
    }
  }
}
