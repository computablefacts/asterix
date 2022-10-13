package com.computablefacts.asterix;

import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

@CheckReturnValue
public interface Result<T> {

  Empty<?> EMPTY = new Empty<>();

  @SuppressWarnings("unchecked")
  static <T> Result<T> empty() {
    return (Result<T>) EMPTY;
  }

  static <T> Result<T> success(T value) {
    return new Success<>(value);
  }

  static <T> Result<T> failure(String message) {
    return new Failure<>(message);
  }

  static <T> Result<T> failure(Exception exception) {
    return new Failure<>(exception);
  }

  static <T> Result<T> of(T value) {
    return of(value, "value is null");
  }

  static <T> Result<T> of(T value, String message) {
    return value == null ? failure(message) : success(value);
  }

  static <T> Result<T> of(T value, Exception exception) {
    return value == null ? failure(exception) : success(value);
  }

  default boolean isEmpty() {
    return this == EMPTY;
  }

  default boolean isSuccess() {
    return this instanceof Success;
  }

  default boolean isFailure() {
    return this instanceof Failure;
  }

  default Result<T> filter(Predicate<T> pred) {
    return isEmpty() || isFailure() || pred.test(successValue()) ? this : empty();
  }

  default <U> Result<U> mapIfSuccess(Function<T, U> fn) {
    if (isSuccess()) {
      return of(fn.apply(successValue()));
    }
    if (isEmpty()) {
      return empty();
    }
    return failure(errorValue());
  }

  default Result<T> mapIfFailure(Function<String, T> fn) {
    if (isFailure()) {
      return of(fn.apply(errorValue()));
    }
    return this;
  }

  default Result<T> mapIfEmpty(Supplier<T> fn) {
    if (isEmpty()) {
      return of(fn.get());
    }
    return this;
  }

  default T get(T defaultValue) {
    if (isEmpty() || isFailure()) {
      return defaultValue;
    }
    return successValue();
  }

  default T get(Supplier<T> fn) {
    if (isEmpty() || isFailure()) {
      return fn.get();
    }
    return successValue();
  }

  default T getOrThrow() {
    Preconditions.checkState(!isEmpty(), "It is forbidden to call Empty.getOrThrow()");
    Preconditions.checkState(!isFailure(), "It is forbidden to call Failure.getOrThrow()");
    return successValue();
  }

  default View<T> view() {
    if (isEmpty() || isFailure()) {
      return View.of();
    }
    return View.of(successValue());
  }

  T successValue();

  String errorValue();

  @CheckReturnValue
  final class Empty<T> implements Result<T> {

    private Empty() {
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof Empty;
    }

    @Override
    public int hashCode() {
      return 0;
    }

    @Override
    public T successValue() {
      throw new RuntimeException("It is forbidden to call successValue() on an empty Result.");
    }

    @Override
    public String errorValue() {
      throw new RuntimeException("It is forbidden to call errorValue() on an empty Result.");
    }
  }

  @CheckReturnValue
  final class Success<T> implements Result<T> {

    private final T value_;

    private Success(T value) {
      value_ = Preconditions.checkNotNull(value, "value should not be null");
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      Success<?> other = (Success<?>) obj;
      return Objects.equals(value_, other.value_);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(value_);
    }

    @Override
    public T successValue() {
      return value_;
    }

    @Override
    public String errorValue() {
      throw new RuntimeException("It is forbidden to call Success.errorValue()");
    }
  }

  @CheckReturnValue
  final class Failure<T> implements Result<T> {

    private final String message_;

    private Failure(String message) {
      message_ = Preconditions.checkNotNull(message, "message should not be null");
    }

    private Failure(Exception exception) {
      message_ = Throwables.getStackTraceAsString(
          Throwables.getRootCause(Preconditions.checkNotNull(exception, "exception should not be null")));
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      Failure<?> other = (Failure<?>) obj;
      return Objects.equals(message_, other.message_);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(message_);
    }

    @Override
    public T successValue() {
      throw new RuntimeException("It is forbidden to call Failure.errorValue()");
    }

    @Override
    public String errorValue() {
      return message_;
    }
  }
}
