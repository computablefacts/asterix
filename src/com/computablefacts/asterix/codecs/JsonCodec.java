package com.computablefacts.asterix.codecs;

import com.computablefacts.logfmt.LogFormatter;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.github.wnameless.json.base.JacksonJsonCore;
import com.github.wnameless.json.flattener.FlattenMode;
import com.github.wnameless.json.flattener.JsonFlattener;
import com.github.wnameless.json.unflattener.JsonUnflattener;
import com.google.errorprone.annotations.CheckReturnValue;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CheckReturnValue
final public class JsonCodec {

  private static final Logger logger_ = LoggerFactory.getLogger(JsonCodec.class);
  private static final ObjectMapper mapper_ = new ObjectMapper();
  private static final JacksonJsonCore jsonCore_ = new JacksonJsonCore(mapper_);

  static {
    SimpleModule dateSerializerModule = new SimpleModule();
    dateSerializerModule.addSerializer(Date.class, new CustomDateSerializer());
    mapper_.registerModule(dateSerializerModule);
  }

  private JsonCodec() {
  }

  /**
   * Flatten a single JSON object using {@code separator} as the attribute separator.
   *
   * @param json      the JSON object.
   * @param separator the separator to use.
   * @return a flattened JSON object.
   */
  public static Map<String, Object> flatten(String json, char separator) {
    return new JsonFlattener(json).withSeparator(separator).flattenAsMap();
  }

  /**
   * Flatten a single JSON object using {@code separator} as the attribute separator. Do not flatten arrays.
   *
   * @param json      the JSON object.
   * @param separator the separator to use.
   * @return a flattened JSON object.
   */
  public static Map<String, Object> flattenKeepArrays(String json, char separator) {
    return new JsonFlattener(json).withSeparator(separator).withFlattenMode(FlattenMode.KEEP_ARRAYS).flattenAsMap();
  }

  /**
   * Flatten a single JSON object using {@code separator} as the attribute separator. Do not flatten arrays of
   * primitives.
   *
   * @param json      the JSON object.
   * @param separator the separator to use.
   * @return a flattened JSON object.
   */
  public static Map<String, Object> flattenKeepPrimitiveArrays(String json, char separator) {
    return new JsonFlattener(json).withSeparator(separator).withFlattenMode(FlattenMode.KEEP_PRIMITIVE_ARRAYS)
        .flattenAsMap();
  }

  /**
   * Unflatten a single JSON object using {@code separator} as the attribute separator.
   *
   * @param json      the JSON object.
   * @param separator the separator to use.
   * @return an unflattened JSON object.
   */
  public static String unflattenAsString(Map<String, Object> json, char separator) {
    return new JsonUnflattener(jsonCore_, json).withSeparator(separator).unflatten();
  }

  /**
   * Unflatten a single JSON object using {@code separator} as the attribute separator.
   *
   * @param json      the JSON object.
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
  public static <T> @NotNull Map<String, Object> dtoToObject(T obj) {
    try {
      return obj == null ? Collections.emptyMap() : mapper_.convertValue(obj, new TypeReference<Map<String, Object>>() {
      });
    } catch (IllegalArgumentException e) {
      logger_.error(LogFormatter.create().message(e).formatError());
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
  public static <T> @NotNull Collection<Map<String, Object>> dtosToObjects(Collection<T> obj) {
    if (obj == null) {
      return Collections.emptyList();
    }
    return obj.stream().filter(Objects::nonNull).map(JsonCodec::dtoToObject).collect(Collectors.toList());
  }

  /**
   * Convert an array of objects to a {@link Collection} of JSON objects.
   *
   * @param obj an array of objects.
   * @param <T> the object type.
   * @return a {@link Collection} of JSON objects.
   */
  @SafeVarargs
  public static <T> @NotNull Collection<Map<String, Object>> dtosToObjects(T... obj) {
    if (obj == null) {
      return Collections.emptyList();
    }
    return Arrays.stream(obj).filter(Objects::nonNull).map(JsonCodec::dtoToObject).collect(Collectors.toList());
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
      logger_.error(LogFormatter.create().message(e).formatError());
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
      logger_.error(LogFormatter.create().message(e).formatError());
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
      logger_.error(LogFormatter.create().message(e).formatError());
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
      logger_.error(LogFormatter.create().message(e).formatError());
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
          : mapper_.readValue(json, TypeFactory.defaultInstance().constructCollectionType(List.class, Map.class));
    } catch (IOException e) {
      logger_.error(LogFormatter.create().message(e).formatError());
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
      logger_.error(LogFormatter.create().message(e).formatError());
    }
    return new Map[0];
  }

  /**
   * Convert a string to a {@link Collection} of objects.
   *
   * @param json string.
   * @return a {@link Collection} of objects.
   */
  public static @NotNull Collection<Object> asCollectionOfUnknownType(String json) {
    try {
      return json == null ? Collections.emptyList() : mapper_.readValue(json,
          TypeFactory.defaultInstance().constructCollectionType(List.class, TypeFactory.unknownType()));
    } catch (IOException e) {
      logger_.error(LogFormatter.create().message(e).formatError());
    }
    return Collections.emptyList();
  }

  /**
   * Convert a string to an array of objects.
   *
   * @param json string.
   * @return an array of objects.
   */
  public static @NotNull Object[] asArrayOfUnknownType(String json) {
    try {
      return json == null ? new Map[0]
          : mapper_.readValue(json, TypeFactory.defaultInstance().constructArrayType(TypeFactory.unknownType()));
    } catch (IOException e) {
      logger_.error(LogFormatter.create().message(e).formatError());
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
    public void serialize(Date value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
      jgen.writeString(DateTimeFormatter.ISO_INSTANT.format(value.toInstant()));
    }
  }
}
