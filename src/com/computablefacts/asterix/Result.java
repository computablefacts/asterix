package com.computablefacts.asterix;

import com.google.errorprone.annotations.CheckReturnValue;
import java.util.function.Supplier;

@Generated
@CheckReturnValue
public abstract class Result<T> {

  public static <T> Result<T> failure(Exception e) {
    return new Failure<>(e);
  }

  public static <T> Result<T> success(T value) {
    return new Success<>(value);
  }

  public static <T> Result<T> empty() {
    return new Empty<>();
  }

  public static <T> Result<T> of(T value) {
    return value != null ? new Success<>(value) : new Failure<>(new IllegalStateException("value must not be null"));
  }

  public abstract boolean isSuccess();

  public abstract boolean isFailure();

  public abstract boolean isEmpty();

  public abstract T getOrElse(T defaultValue);

  public abstract T getOrElse(Supplier<T> defaultValue);

  public abstract T successValue();

  public abstract Exception failureValue();

  private static class Empty<T> extends Result<T> {

    public Empty() {
    }

    @Override
    public boolean isSuccess() {
      return false;
    }

    @Override
    public boolean isFailure() {
      return false;
    }

    @Override
    public boolean isEmpty() {
      return true;
    }

    @Override
    public T getOrElse(T defaultValue) {
      return defaultValue;
    }

    @Override
    public T getOrElse(Supplier<T> defaultValue) {
      return defaultValue.get();
    }

    @Override
    public T successValue() {
      throw new IllegalStateException("Method successValue() called on an Empty result");
    }

    @Override
    public Exception failureValue() {
      throw new IllegalStateException("Method failureValue() called on an Empty result");
    }
  }

  private static final class Failure<T> extends Empty<T> {

    private final Exception exception_;

    public Failure(Exception exception) {
      exception_ = exception;
    }

    @Override
    public boolean isSuccess() {
      return false;
    }

    @Override
    public boolean isFailure() {
      return true;
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public T getOrElse(T defaultValue) {
      return defaultValue;
    }

    @Override
    public T getOrElse(Supplier<T> defaultValue) {
      return defaultValue.get();
    }

    @Override
    public T successValue() {
      throw new IllegalStateException("Method successValue() called on a Failure result");
    }

    @Override
    public Exception failureValue() {
      return exception_;
    }
  }

  private static final class Success<T> extends Result<T> {

    private final T t_;

    public Success(T t) {
      t_ = t;
    }

    @Override
    public boolean isSuccess() {
      return true;
    }

    @Override
    public boolean isFailure() {
      return false;
    }

    @Override
    public boolean isEmpty() {
      return false;
    }

    @Override
    public T getOrElse(T defaultValue) {
      return t_;
    }

    @Override
    public T getOrElse(Supplier<T> defaultValue) {
      return t_;
    }

    @Override
    public T successValue() {
      return t_;
    }

    @Override
    public Exception failureValue() {
      throw new IllegalStateException("Method failureValue() called on a Success result");
    }
  }
}
