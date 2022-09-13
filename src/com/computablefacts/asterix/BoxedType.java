package com.computablefacts.asterix;

import com.computablefacts.asterix.codecs.JsonCodec;
import com.computablefacts.asterix.codecs.StringCodec;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CheckReturnValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@CheckReturnValue
final public class BoxedType<T> {

  private static final BoxedType<?> NULL = new BoxedType<>(null);
  private static final BoxedType<?> TRUE = new BoxedType<>(true);
  private static final BoxedType<?> FALSE = new BoxedType<>(false);
  private final T value_; // T in {Boolean, BigInteger, BigDecimal, String, Date, Collection, Map}

  private BoxedType(T value) {
    value_ = value;
  }

  public static BoxedType<?> empty() {
    return NULL;
  }

  @Deprecated
  public static BoxedType<?> create(int[] values) {
    return BoxedType.of(values);
  }

  @Deprecated
  public static BoxedType<?> create(long[] values) {
    return BoxedType.of(values);
  }

  @Deprecated
  public static BoxedType<?> create(double[] values) {
    return BoxedType.of(values);
  }

  @Deprecated
  public static BoxedType<?> create(float[] values) {
    return BoxedType.of(values);
  }

  @Deprecated
  public static BoxedType<?> create(boolean[] values) {
    return BoxedType.of(values);
  }

  @Deprecated
  public static BoxedType<?> create(Object[] values) {
    return of(values);
  }

  @Deprecated
  public static BoxedType<?> create(Object value) {
    return of(value);
  }

  @Deprecated
  public static BoxedType<?> create(Object value, boolean interpretStringInScientificNotation) {
    return of(value, interpretStringInScientificNotation);
  }

  public static BoxedType<?> of(int[] values) {
    return BoxedType.of(Arrays.stream(values).boxed().collect(Collectors.toList()));
  }

  public static BoxedType<?> of(long[] values) {
    return BoxedType.of(Arrays.stream(values).boxed().collect(Collectors.toList()));
  }

  public static BoxedType<?> of(double[] values) {
    return BoxedType.of(Arrays.stream(values).boxed().collect(Collectors.toList()));
  }

  public static BoxedType<?> of(float[] values) {
    List<Float> floats = new ArrayList<>(values.length);
    for (float f : values) {
      floats.add(f);
    }
    return BoxedType.of(floats);
  }

  public static BoxedType<?> of(boolean[] values) {
    List<Boolean> booleans = new ArrayList<>(values.length);
    for (boolean b : values) {
      booleans.add(b);
    }
    return BoxedType.of(booleans);
  }

  public static BoxedType<?> of(Object[] values) {
    return BoxedType.of(Lists.newArrayList(values));
  }

  public static BoxedType<?> of(Object value) {
    return of(value, true);
  }

  public static BoxedType<?> of(Object value, boolean interpretStringInScientificNotation) {
    return value == null ? NULL : value instanceof BoxedType ? (BoxedType<?>) value
        : value instanceof Boolean ? (Boolean) value ? TRUE : FALSE
            : new BoxedType<>(StringCodec.defaultCoercer(value, interpretStringInScientificNotation));
  }

  @Override
  public int hashCode() {
    if (isBoolean()) {
      return asBool().hashCode();
    }
    if (isNumber()) {
      return asBigDecimal().stripTrailingZeros().hashCode();
    }
    if (isString()) {
      return asString().hashCode();
    }
    return Objects.hash(value());
  }

  @Override
  public boolean equals(Object o) {

    if (o == this) {
      return true;
    }
    if (!(o instanceof BoxedType)) {
      return false;
    }

    BoxedType<?> bt = (BoxedType<?>) o;

    if (isBoolean() && bt.isBoolean()) {
      return asBool().equals(bt.asBool());
    }
    if (isNumber() && bt.isNumber()) {
      return asBigDecimal().compareTo(bt.asBigDecimal()) == 0;
    }
    if (isString() && bt.isString()) {
      return asString().equals(bt.asString());
    }
    return Objects.equals(value(), bt.value());
  }

  @Override
  public String toString() {
    return Strings.nullToEmpty(asString());
  }

  public T value() {
    return value_;
  }

  public boolean isEmpty() {
    return value_ == null;
  }

  public boolean isBoolean() {
    return value_ instanceof Boolean;
  }

  public boolean isNumber() {
    return value_ instanceof Number;
  }

  public boolean isBigInteger() {
    return value_ instanceof BigInteger;
  }

  public boolean isBigDecimal() {
    return value_ instanceof BigDecimal;
  }

  public boolean isString() {
    return value_ instanceof String;
  }

  public boolean isCollection() {
    return value_ instanceof Collection;
  }

  public boolean isMap() {
    return value_ instanceof Map;
  }

  public boolean isDate() {
    return value_ instanceof Date;
  }

  public Boolean asBool() {
    return isBoolean() ? (Boolean) value_ : null;
  }

  public Integer asInt() {
    return isNumber() ? ((Number) value_).intValue() : null;
  }

  public Float asFloat() {
    return isNumber() ? ((Number) value_).floatValue() : null;
  }

  public Double asDouble() {
    return isNumber() ? ((Number) value_).doubleValue() : null;
  }

  public Long asLong() {
    return isNumber() ? ((Number) value_).longValue() : null;
  }

  public BigInteger asBigInteger() {
    return isBigInteger() ? (BigInteger) value_
        : isBigDecimal() && isInteger((BigDecimal) value_) ? ((BigDecimal) value_).toBigInteger() : null;
  }

  public BigDecimal asBigDecimal() {
    return isBigDecimal() ? (BigDecimal) value_ : isBigInteger() ? new BigDecimal((BigInteger) value_) : null;
  }

  public String asString() {
    String str = isEmpty() ? null : isString() ? (String) value_ : isBoolean() ? Boolean.toString(asBool())
        : isBigInteger() ? asBigInteger().toString(10) : isBigDecimal() ? asBigDecimal().stripTrailingZeros().toString()
            : isDate() ? DateTimeFormatter.ISO_INSTANT.format(asDate().toInstant())
                : isCollection() ? JsonCodec.asString(asCollection())
                    : isMap() ? JsonCodec.asString(asMap()) : value_.toString();
    if (str == null) {
      return null;
    }
    if (str.startsWith("\"") && str.endsWith("\"")) {
      return str.length() == 1 ? "" /* deal with '"' */ : str.substring(1, str.length() - 1);
    }
    return str;
  }

  public Collection<?> asCollection() {
    return isCollection() ? (Collection<?>) value_ : Lists.newArrayList(value_);
  }

  public Map<?, ?> asMap() {
    if (isMap()) {
      return (Map<?, ?>) value_;
    }
    Map<String, T> map = new HashMap<>();
    map.put("root", value_);
    return map;
  }

  public Date asDate() {
    return isDate() ? (Date) value_ : null;
  }

  /**
   * Compare two boxed types. This method returns an empty {@link Optional} if the types of the underlying values are
   * not comparable to each others.
   *
   * @param bt {@link BoxedType}
   * @return returns an empty {@link Optional} if the underlying values cannot be compared. The result of
   * {@link Comparable#compareTo} otherwise.
   */
  public Optional<Integer> compareTo(@NotNull BoxedType<?> bt) {
    if (isEmpty() && bt.isEmpty()) {
      return Optional.of(0);
    }
    if (isEmpty()) {
      return Optional.of(-1);
    }
    if (bt.isEmpty()) {
      return Optional.of(1);
    }
    if (isBoolean() && bt.isBoolean()) {
      return Optional.of(asBool().compareTo(bt.asBool()));
    }
    if (isNumber() && bt.isNumber()) {
      return Optional.of(asBigDecimal().compareTo(bt.asBigDecimal()));
    }
    if (isString() && bt.isString()) {
      return Optional.of(asString().compareTo(bt.asString()));
    }
    if (isComparableTo(bt)) {
      Comparable obj1 = asComparable();
      Comparable obj2 = bt.asComparable();
      return Optional.of(obj1.compareTo(obj2));
    }
    return Optional.empty();
  }

  private boolean isInteger(BigDecimal bd) {
    return bd.stripTrailingZeros().scale() <= 0;
  }

  private Comparable<T> asComparable() {
    return isComparable() ? (Comparable<T>) value_ : null;
  }

  private boolean isComparable() {
    return value_ instanceof Comparable;
  }

  private boolean isComparableTo(BoxedType<?> bt) {
    if (isEmpty() || bt.isEmpty()) {
      return false;
    }
    if (!isComparable() || !bt.isComparable()) {
      return false;
    }
    return value().getClass().equals(bt.value().getClass());
  }
}
