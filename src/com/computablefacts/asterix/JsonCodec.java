package com.computablefacts.asterix;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.github.wnameless.json.base.JacksonJsonCore;
import com.github.wnameless.json.flattener.JsonFlattener;
import com.github.wnameless.json.unflattener.JsonUnflattener;
import com.google.errorprone.annotations.CheckReturnValue;

@CheckReturnValue
final public class JsonCodec {

  private static final ObjectMapper mapper_ = new ObjectMapper();
  private static final JacksonJsonCore jsonCore_ = new JacksonJsonCore(mapper_);

  static {
    SimpleModule dateSerializerModule = new SimpleModule();
    dateSerializerModule.addSerializer(Date.class, new CustomDateSerializer());
    mapper_.registerModule(dateSerializerModule);
  }

  private JsonCodec() {}

  /**
   * Flatten a single JSON object using {@code separator} as the attribute separator.
   *
   * @param json the JSON object.
   * @param separator the separator to use.
   * @return a flattened JSON object.
   */
  public static Map<String, Object> flatten(String json, char separator) {
    return new JsonFlattener(json).withSeparator(separator).flattenAsMap();
  }

  /**
   * Unflatten a single JSON object using {@code separator} as the attribute separator.
   *
   * @param json the JSON object.
   * @param separator the separator to use.
   * @return an unflattened JSON object.
   */
  public static String unflattenAsString(Map<String, Object> json, char separator) {
    return new JsonUnflattener(jsonCore_, json).withSeparator(separator).unflatten();
  }

  /**
   * Unflatten a single JSON object using {@code separator} as the attribute separator.
   *
   * @param json the JSON object.
   * @param separator the separator to use.
   * @return an unflattened JSON object.
   */
  public static Map<String, Object> unflattenAsMap(Map<String, Object> json, char separator) {
    return new JsonUnflattener(jsonCore_, json).withSeparator(separator).unflattenAsMap();
  }

  /**
   * Convert an object to a JSON object.
   *
   * @param obj an object.
   * @param <T> the object type.
   * @return a JSON object.
   */
  public static <T> @NotNull Map<String, Object> asMap(T obj) {
    try {
      return obj == null ? Collections.emptyMap()
          : mapper_.convertValue(obj, new TypeReference<Map<String, Object>>() {});
    } catch (IllegalArgumentException e) {
      // FALL THROUGH
    }
    return Collections.emptyMap();
  }

  /**
   * Convert a {@link Collection} of objects to a {@link Collection} of JSON objects.
   *
   * @param obj a {@link Collection} of objects.
   * @param <T> the object type.
   * @return a {@link Collection} of JSON objects.
   */
  public static <T> @NotNull Collection<Map<String, Object>> asCollectionOfMaps(Collection<T> obj) {
    if (obj == null) {
      return Collections.emptyList();
    }
    return obj.stream().filter(Objects::nonNull).map(JsonCodec::asMap).collect(Collectors.toList());
  }

  /**
   * Convert an array of objects to a {@link Collection} of JSON objects.
   *
   * @param obj an array of objects.
   * @param <T> the object type.
   * @return a {@link Collection} of JSON objects.
   */
  @SafeVarargs
  public static <T> @NotNull Collection<Map<String, Object>> asCollectionOfMaps(T... obj) {
    if (obj == null) {
      return Collections.emptyList();
    }
    return Arrays.stream(obj).filter(Objects::nonNull).map(JsonCodec::asMap)
        .collect(Collectors.toList());
  }

  /**
   * Convert an object to a JSON string.
   *
   * @param obj an object.
   * @param <T> the object type.
   * @return a JSON string.
   */
  public static <T> @NotNull String asString(T obj) {
    try {
      return obj == null ? "{}" : mapper_.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      // FALL THROUGH
    }
    return "{}";
  }

  /**
   * Convert a {@link Collection} of objects to a JSON string.
   *
   * @param obj a {@link Collection} of objects.
   * @param <T> the object type.
   * @return a JSON string.
   */
  public static <T> @NotNull String asString(Collection<T> obj) {
    try {
      return obj == null ? "[]" : mapper_.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      // FALL THROUGH
    }
    return "[]";
  }

  /**
   * Convert an array of objects to a JSON string.
   *
   * @param obj an array of objects.
   * @param <T> the object type.
   * @return a JSON string.
   */
  @SafeVarargs
  public static <T> @NotNull String asString(T... obj) {
    try {
      return obj == null ? "[]" : mapper_.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      // FALL THROUGH
    }
    return "[]";
  }

  /**
   * Convert a string to a JSON object.
   *
   * @param json string.
   * @return a JSON object.
   */
  public static @NotNull Map<String, Object> asObject(String json) {
    try {
      return json == null ? Collections.emptyMap()
          : mapper_.readValue(json, TypeFactory.defaultInstance().constructType(Map.class));
    } catch (IOException e) {
      // FALL THROUGH
    }
    return Collections.emptyMap();
  }

  /**
   * Convert a string to a {@link Collection} of JSON objects.
   *
   * @param json string.
   * @return a {@link Collection} of JSON objects.
   */
  public static @NotNull Collection<Map<String, Object>> asCollection(String json) {
    try {
      return json == null ? Collections.emptyList()
          : mapper_.readValue(json,
              TypeFactory.defaultInstance().constructCollectionType(List.class, Map.class));
    } catch (IOException e) {
      // FALL THROUGH
    }
    return Collections.emptyList();
  }

  /**
   * Convert a string to an array of JSON objects.
   *
   * @param json string.
   * @return an array of JSON objects.
   */
  public static @NotNull Map<String, Object>[] asArray(String json) {
    try {
      return json == null ? new Map[0]
          : mapper_.readValue(json, TypeFactory.defaultInstance().constructArrayType(Map.class));
    } catch (IOException e) {
      // FALL THROUGH
    }
    return new Map[0];
  }

  private static class CustomDateSerializer extends StdSerializer<Date> {

    public CustomDateSerializer() {
      this(null);
    }

    public CustomDateSerializer(Class<Date> t) {
      super(t);
    }

    @Override
    public void serialize(Date value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException {
      jgen.writeString(DateTimeFormatter.ISO_INSTANT.format(value.toInstant()));
    }
  }
}
