package com.computablefacts.asterix;

import static com.computablefacts.asterix.IO.eCompressionAlgorithm.GZIP;
import static com.computablefacts.asterix.IO.eCompressionAlgorithm.NONE;

import com.computablefacts.Generated;
import com.computablefacts.asterix.IO.eCompressionAlgorithm;
import com.computablefacts.asterix.console.AsciiProgressBar;
import com.computablefacts.logfmt.LogFormatter;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.AbstractSequentialIterator;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.PeekingIterator;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CheckReturnValue
public class View<T> extends AbstractIterator<T> implements AutoCloseable {

  private static final Logger logger_ = LoggerFactory.getLogger(View.class);
  private static final View<?> EMPTY_VIEW = new View<>(Collections.emptyIterator());
  protected final Iterator<T> iterator_;

  protected View(Iterator<T> iterator) {
    iterator_ = Preconditions.checkNotNull(iterator, "iterator should not be null");
  }

  public static <T> View<T> of() {
    return (View<T>) EMPTY_VIEW;
  }

  @SafeVarargs
  public static <E> View<E> of(E... elements) {

    Preconditions.checkNotNull(elements, "elements should not be null");

    return of(new AbstractIterator<E>() {

      private int i = 0;

      @Override
      protected E computeNext() {
        return elements.length <= i ? endOfData() : elements[i++];
      }
    });
  }

  public static <T> View<T> of(Stream<T> stream) {

    Preconditions.checkNotNull(stream, "stream should not be null");

    return new View<>(new StreamIterator<>(stream));
  }

  public static View<String> of(BufferedReader reader) {

    Preconditions.checkNotNull(reader, "reader should not be null");

    return of(reader.lines().onClose(() -> {
      try {
        reader.close();
      } catch (Exception e) {
        logger_.error(LogFormatter.create().message(e).formatError());
      }
    }));
  }

  public static View<String> of(Process process) {

    Preconditions.checkNotNull(process, "process should not be null");

    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

    return of(reader.lines().onClose(() -> {
      try {
        reader.close();
        process.destroyForcibly();
      } catch (Exception e) {
        logger_.error(LogFormatter.create().message(e).formatError());
      }
    }));
  }

  public static <T> View<T> of(Iterator<T> iterator) {

    Preconditions.checkNotNull(iterator, "iterator should not be null");

    return new View<>(iterator);
  }

  public static <T> View<T> of(Iterable<T> iterable) {

    Preconditions.checkNotNull(iterable, "iterable should not be null");

    return new View<>(iterable.iterator());
  }

  @Generated
  public static <T> View<T> of(Enumeration<T> enumeration) {
    return new View<>(Iterators.forEnumeration(enumeration));
  }

  public static <K, V> View<Map.Entry<K, V>> of(Map<K, V> map) {

    Preconditions.checkNotNull(map, "map should not be null");

    return of(map.entrySet());
  }

  @Generated
  public static <T> View<T> of(ResultSet rs, Function<ResultSet, T> fn) {

    Preconditions.checkNotNull(rs, "rs should not be null");
    Preconditions.checkNotNull(fn, "fn should not be null");

    return of(new AbstractIterator<T>() {

      @Override
      protected T computeNext() {
        try {
          if (rs.next()) {
            return fn.apply(rs);
          }
        } catch (SQLException e) {
          logger_.error(LogFormatter.create().message(e).formatError());
        }
        return endOfData();
      }
    });
  }

  public static View<String> of(File file) {
    return of(file, false);
  }

  @Deprecated
  public static View<String> of(File file, boolean isCompressed) {
    return of(file, isCompressed ? GZIP : NONE);
  }

  public static View<String> of(File file, eCompressionAlgorithm algorithm) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkNotNull(algorithm, "algorithm should not be null");

    try {
      return new View<>(IO.newLineIterator(file, algorithm));
    } catch (IOException e) {
      logger_.error(LogFormatter.create().message(e).formatError());
    }
    return of();
  }

  public static <T> View<List<T>> stitch(List<? extends Iterable<T>> views) {
    return of(new StitchingIterator<>(View.of(views).map(Iterable::iterator).toList()));
  }

  public static View<String> split(String string, char separator) {
    return of(Splitter.on(separator).trimResults().split(string));
  }

  public static View<String> split(String string, String separator) {
    return of(Splitter.on(separator).trimResults().split(string));
  }

  @Beta
  public static View<String> executeBashCommand(String command) {

    Preconditions.checkArgument(!Strings.isNullOrEmpty(command), "command should neither be null nor empty");

    try {

      ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
      processBuilder.redirectErrorStream(true);

      return of(processBuilder.start());
    } catch (IOException e) {
      logger_.error(LogFormatter.create().add("command", command).message(e).formatError());
      return View.of();
    }
  }

  /**
   * Returns an infinite sequence of the same object.
   *
   * @param object the object to return.
   * @param <T>
   * @return an infinite sequence of the same object.
   */
  public static <T> View<T> repeat(T object) {
    return iterate(object, s -> s);
  }

  /**
   * Returns a sequence of integers in {@code begin} included and {@code end} excluded.
   *
   * @param begin the beginning of the sequence (included).
   * @param end   the end of the sequence (excluded).
   * @return a sequence of consecutive integers.
   */
  public static View<Integer> range(int begin, int end) {

    Preconditions.checkArgument(begin >= 0, "begin must be >= 0");
    Preconditions.checkArgument(end >= begin, "end must be >= begin");

    return iterate(begin, x -> x + 1).take(end - begin);
  }

  /**
   * Returns a sequence of objects.
   *
   * @param seed the first value.
   * @param next a function that compute the next value from the previous one.
   * @param <T>
   * @return a sequence of objects.
   */
  public static <T> View<T> iterate(T seed, Function<T, T> next) {
    return of(new AbstractSequentialIterator<T>(seed) {

      @Nullable
      @Override
      protected T computeNext(T previous) {
        return next.apply(previous);
      }
    });
  }

  @Override
  public T computeNext() {
    if (iterator_.hasNext()) {
      return iterator_.next();
    }
    close();
    return endOfData();
  }

  @Override
  public void close() {
    if (iterator_ instanceof AutoCloseable) {
      try {
        ((AutoCloseable) iterator_).close();
      } catch (Exception e) {
        logger_.error(LogFormatter.create().message(e).formatError());
      }
    }
  }

  @Override
  protected void finalize() {
    close();
  }

  /**
   * Returns the first element of the view, leaving the current view with one less element.
   *
   * @return the first element of the view.
   */
  public Result<T> first() {
    return hasNext() ? Result.of(next()) : Result.empty();
  }

  /**
   * Returns the last element of the view, leaving the current view exhausted.
   *
   * @return the last element of the view.
   */
  public Result<T> last() {
    return hasNext() ? Result.of(Iterators.getLast(this)) : Result.empty();
  }

  /**
   * Accumulates the view elements into a new {@link List}.
   *
   * @return a {@link List}.
   */
  public List<T> toList() {
    return Lists.newArrayList(this);
  }

  /**
   * Accumulates the view elements into a new sorted {@link List}.
   *
   * @return a {@link List}.
   */
  public List<T> toSortedList(Comparator<T> comparator) {

    Preconditions.checkNotNull(comparator, "comparator should not be null");

    List<T> list = toList();
    list.sort(comparator);
    return list;
  }

  /**
   * Accumulates the view elements into a new {@link Set}.
   *
   * @return a {@link Set}.
   */
  public Set<T> toSet() {
    return Sets.newHashSet(this);
  }

  @Deprecated
  public String toString(String separator) {
    return join(separator, null, null);
  }

  @Deprecated
  public String toString(String separator, String prefix, String suffix) {
    return join(separator, prefix, suffix);
  }

  /**
   * Accumulates the view elements into a new {@link String}.
   *
   * @param separator join mapped elements together using the specified separator.
   * @return a {@link String} whose format is {@code <el1><separator><el2><separator><el3>...}.
   */
  public String join(String separator) {
    return join(separator, null, null);
  }

  /**
   * Accumulates the view elements into a new {@link String}.
   * <p>
   * Note that an empty string is returned if the view is empty. Even if the {@code <prefix>} and/or {@code <suffix>}
   * are either null or empty.
   *
   * @param separator join mapped elements together using the specified separator.
   * @param prefix    string to add at the beginning of the buffer (optional).
   * @param suffix    string to add at the end of the buffer (optional).
   * @return a {@link String} whose format is {@code <prefix><el1><separator><el2><separator><el3>...<suffix>}.
   */
  public String join(String separator, String prefix, String suffix) {

    Preconditions.checkNotNull(separator, "separator should not be null");

    PeekingIterator<T> iterator = Iterators.peekingIterator(this);
    T t = iterator.hasNext() ? iterator.peek() : null;
    View<String> view;

    if (t instanceof String) {
      view = new View<>((PeekingIterator<String>) iterator);
    } else if (t instanceof Integer) {
      view = new View<>((PeekingIterator<Integer>) iterator).map(i -> Integer.toString(i, 10));
    } else if (t instanceof Long) {
      view = new View<>((PeekingIterator<Long>) iterator).map(i -> Long.toString(i, 10));
    } else if (t instanceof Double) {
      view = new View<>((PeekingIterator<Double>) iterator).map(i -> Double.toString(i));
    } else if (t instanceof Float) {
      view = new View<>((PeekingIterator<Float>) iterator).map(i -> Float.toString(i));
    } else {
      view = View.of();
    }

    StringBuilder builder = new StringBuilder();

    while (view.hasNext()) {
      if (builder.length() == 0 && prefix != null) {
        builder.append(prefix);
      } else if (builder.length() > 0) {
        builder.append(separator);
      }
      builder.append(view.next());
    }
    if (builder.length() > 0 && suffix != null) {
      builder.append(suffix);
    }
    return builder.toString();
  }

  /**
   * Write view elements to a file.
   *
   * @param file   where the view elements must be written.
   * @param append false iif a new file must be created. Otherwise, view elements are appended at the end of an existing
   *               file.
   */
  public void toFile(File file, boolean append) {
    toFile(file, append, NONE);
  }

  /**
   * Write view elements to a file.
   *
   * @param file     where the view elements must be written.
   * @param append   false iif a new file must be created. Otherwise, view elements are appended at the end of an
   *                 existing file.
   * @param compress true iif the output must be compressed (gzip), false otherwise.
   */
  @Deprecated
  public void toFile(File file, boolean append, boolean compress) {
    toFile(file, append, compress ? GZIP : NONE);
  }

  /**
   * Write view elements to a file.
   *
   * @param file      where the view elements must be written.
   * @param append    false iif a new file must be created. Otherwise, view elements are appended at the end of an
   *                  existing file.
   * @param algorithm the compression algorithm to use.
   */
  public void toFile(File file, boolean append, eCompressionAlgorithm algorithm) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkNotNull(algorithm, "algorithm should not be null");

    PeekingIterator<T> iterator = Iterators.peekingIterator(this);
    T t = iterator.hasNext() ? iterator.peek() : null;
    View<String> view;

    if (t instanceof String) {
      view = new View<>((PeekingIterator<String>) iterator);
    } else if (t instanceof Integer) {
      view = new View<>((PeekingIterator<Integer>) iterator).map(i -> Integer.toString(i, 10));
    } else if (t instanceof Long) {
      view = new View<>((PeekingIterator<Long>) iterator).map(i -> Long.toString(i, 10));
    } else if (t instanceof Double) {
      view = new View<>((PeekingIterator<Double>) iterator).map(i -> Double.toString(i));
    } else if (t instanceof Float) {
      view = new View<>((PeekingIterator<Float>) iterator).map(i -> Float.toString(i));
    } else {
      view = View.of();
    }
    try (BufferedWriter writer = IO.newFileWriter(file, append, algorithm)) {
      view.forEachRemaining(el -> {
        try {
          writer.write(el);
          writer.newLine();
        } catch (IOException e) {
          logger_.error(
              LogFormatter.create().add("file", file).add("append", append).add("compression_algorithm", algorithm)
                  .message(e).formatError());
        }
      });
    } catch (IOException e) {
      logger_.error(
          LogFormatter.create().add("file", file).add("append", append).add("compression_algorithm", algorithm)
              .message(e).formatError());
    }
  }

  /**
   * Returns a sample of values using Algorithm L.
   *
   * @param size the sample size.
   * @return the sample.
   */
  public List<T> sample(int size) {
    AlgorithmL<T> algorithmL = new AlgorithmL<>(size);
    this.forEachRemaining(algorithmL::add);
    return algorithmL.sample();
  }

  /**
   * Split the view elements into two sub-lists : one matching the given predicate and another one not matching the
   * given predicate.
   *
   * @param predicate the predicate to match.
   * @return the two sub-lists. {@code Map.Entry.getKey()} returns the elements matching the given predicate.
   * {@code Map.Entry.getValue()} returns the elements not matching the given predicate.
   */
  public Map.Entry<List<T>, List<T>> divide(Predicate<T> predicate) {

    Preconditions.checkNotNull(predicate, "predicate should not be null");

    Map.Entry<List<T>, List<T>> entry = new AbstractMap.SimpleImmutableEntry<>(new ArrayList<>(), new ArrayList<>());

    while (hasNext()) {

      T element = next();

      if (predicate.test(element)) {
        entry.getKey().add(element);
      } else {
        entry.getValue().add(element);
      }
    }
    return entry;
  }

  /**
   * Split the view elements into two sub-lists : one with the elements in odd positions and another one with the
   * elements in even positions.
   *
   * @return the two sub-lists. {@code Map.Entry.getKey()} returns the elements in odd positions.
   * {@code Map.Entry.getValue()} returns the elements in even positions.
   */
  public Map.Entry<List<T>, List<T>> divide() {

    Map.Entry<List<T>, List<T>> entry = new AbstractMap.SimpleImmutableEntry<>(new ArrayList<>(), new ArrayList<>());

    View<Map.Entry<Integer, T>> view = index();

    while (view.hasNext()) {

      Map.Entry<Integer, T> element = view.next();

      if (element.getKey() % 2 != 0) {
        entry.getKey().add(element.getValue());
      } else {
        entry.getValue().add(element.getValue());
      }
    }
    return entry;
  }

  /**
   * Returns a {@link Map} where keys are the result of a function applied to each element of the view and values are
   * lists of elements corresponding to each key.
   *
   * @param fn  the function to apply.
   * @param <U>
   * @return a {@link Map}.
   */
  public <U> Map<U, List<T>> groupAll(Function<T, U> fn) {

    Preconditions.checkNotNull(fn, "fn should not be null");

    Map<U, List<T>> groups = new HashMap<>();

    while (hasNext()) {

      T value = next();
      U key = fn.apply(value);

      if (!groups.containsKey(key)) {
        groups.put(key, new ArrayList<>());
      }
      groups.get(key).add(value);
    }
    return groups;
  }

  /**
   * Returns a {@link Map} where keys are the result of a function applied to each element of the view and values are
   * sets of elements corresponding to each key.
   *
   * @param fn  the function to apply.
   * @param <U>
   * @return a {@link Map}.
   */
  public <U> Map<U, Set<T>> groupDistinct(Function<T, U> fn) {

    Preconditions.checkNotNull(fn, "fn should not be null");

    Map<U, Set<T>> groups = new HashMap<>();

    while (hasNext()) {

      T value = next();
      U key = fn.apply(value);

      if (!groups.containsKey(key)) {
        groups.put(key, new HashSet<>());
      }
      groups.get(key).add(value);
    }
    return groups;
  }

  /**
   * Assemble two lists into one by combining the elements of the same index.
   *
   * @param elements the other elements.
   * @param <U>
   * @return a new {@link View} of the combined views.
   */
  @Generated
  public <U> View<Map.Entry<T, U>> zip(U... elements) {
    return zip(View.of(elements));
  }

  /**
   * Assemble two lists into one by combining the elements of the same index.
   *
   * @param stream the other elements.
   * @param <U>
   * @return a new {@link View} of the combined views.
   */
  @Generated
  public <U> View<Map.Entry<T, U>> zip(Stream<U> stream) {
    return zip(View.of(stream));
  }

  /**
   * Assemble two lists into one by combining the elements of the same index.
   *
   * @param iterable the other elements.
   * @param <U>
   * @return a new {@link View} of the combined views.
   */
  @Generated
  public <U> View<Map.Entry<T, U>> zip(Iterable<U> iterable) {
    return zip(View.of(iterable));
  }

  /**
   * Assemble two lists into one by combining the elements of the same index.
   *
   * @param enumeration the other elements.
   * @param <U>
   * @return a new {@link View} of the combined views.
   */
  @Generated
  public <U> View<Map.Entry<T, U>> zip(Enumeration<U> enumeration) {
    return zip(View.of(enumeration));
  }

  /**
   * Assemble two lists into one by combining the elements of the same index.
   *
   * @param view the other view.
   * @param <U>
   * @return a new {@link View} of the combined views.
   */
  public <U> View<Map.Entry<T, U>> zip(View<U> view) {

    Preconditions.checkNotNull(view, "view should not be null");

    View<T> self = this;
    return new View<>(new AbstractIterator<Map.Entry<T, U>>() {

      @Override
      protected Map.Entry<T, U> computeNext() {
        if (self.hasNext() && view.hasNext()) {
          return new AbstractMap.SimpleImmutableEntry<>(self.next(), view.next());
        }
        return endOfData();
      }
    });
  }

  /**
   * Makes two lists out of one by "deconstructing" the elements.
   *
   * @param fn  a function that maps a single element to a tuple.
   * @param <U>
   * @param <V>
   * @return a {@link Map.Entry}.
   */
  public <U, V> Map.Entry<List<U>, List<V>> unzip(Function<T, Map.Entry<U, V>> fn) {

    Preconditions.checkNotNull(fn, "fn should not be null");

    return map(fn).reduce(new AbstractMap.SimpleImmutableEntry<>(new ArrayList<>(), new ArrayList<>()), (carry, e) -> {
      carry.getKey().add(e.getKey());
      carry.getValue().add(e.getValue());
      return carry;
    });
  }

  /**
   * Adds an element to the end of the view.
   *
   * @param element the element to add.
   * @return a new {@link View}.
   */
  public View<T> append(T element) {
    return new View<>(Iterators.concat(this, Lists.newArrayList(element).iterator()));
  }

  /**
   * Adds an element to the beginning of the view.
   *
   * @param element the element to add.
   * @return a new {@link View}.
   */
  public View<T> prepend(T element) {
    return new View<>(Iterators.concat(Lists.newArrayList(element).iterator(), this));
  }

  /**
   * Returns a view where all elements are indexed by there position in the underlying stream of values.
   *
   * @return a new {@link View}.
   */
  public View<Map.Entry<Integer, T>> index() {

    View<T> self = this;
    return new View<>(new AbstractIterator<Map.Entry<Integer, T>>() {

      private int index_ = 0;

      @Override
      protected Map.Entry<Integer, T> computeNext() {
        return self.hasNext() ? new AbstractMap.SimpleImmutableEntry<>(++index_, self.next()) : endOfData();
      }
    });
  }

  /**
   * Returns whether any elements of this view match the provided predicate.
   *
   * @param predicate the predicate to satisfy.
   * @return true if one or more elements returned by this view satisfy the given predicate.
   */
  public boolean contains(Predicate<? super T> predicate) {
    return anyMatch(predicate);
  }

  /**
   * Returns whether any elements of this view match the provided predicate.
   *
   * @param predicate the predicate to satisfy.
   * @return true if one or more elements returned by this view satisfy the given predicate.
   */
  public boolean anyMatch(Predicate<? super T> predicate) {
    return Iterators.any(this, predicate::test);
  }

  /**
   * Returns whether all elements of this view match the provided predicate.
   *
   * @param predicate the predicate to satisfy.
   * @return true if every element returned by this view satisfies the given predicate. If the view is empty, true is
   * returned.
   */
  public boolean allMatch(Predicate<? super T> predicate) {
    return Iterators.all(this, predicate::test);
  }

  /**
   * Returns whether none of the elements of this view match the provided predicate.
   *
   * @param predicate the predicate to satisfy.
   * @return true if every element returned by this view doesn't satisfy the given predicate. If the view is empty, true
   * is returned.
   */
  public boolean noneMatch(Predicate<? super T> predicate) {
    return !anyMatch(predicate);
  }

  /**
   * Returns an {@link Result} containing the first element of this view that satisfies the provided predicate.
   *
   * @param predicate the predicate to satisfy.
   * @return an {@link Result}.
   */
  public Result<T> findFirst(Predicate<? super T> predicate) {
    return Iterators.tryFind(this, predicate::test).toJavaUtil().map(Result::of).orElse(Result.empty());
  }

  /**
   * Returns a {@link View} containing all the elements of this view that satisfy the provided predicate.
   *
   * @param predicate the predicate to satisfy.
   * @return a new {@link View}.
   */
  public View<T> findAll(Predicate<? super T> predicate) {
    return filter(predicate);
  }

  /**
   * Performs the given action for each remaining element of the view until all elements have been processed or the
   * caller stopped the enumeration.
   *
   * @param consumer the action to be performed for each element.
   */
  public void forEachRemaining(BiConsumer<? super T, Breaker> consumer) {

    Preconditions.checkNotNull(consumer, "consumer should not be null");

    Breaker breaker = new Breaker();

    while (!breaker.shouldBreak() && hasNext()) {
      consumer.accept(next(), breaker);
    }
  }

  /**
   * Performs the given action for each remaining element of the view in parallel until all elements have been
   * processed.
   *
   * @param consumer the action to be performed for each element.
   */
  public void forEachRemainingInParallel(Consumer<? super T> consumer) {
    forEachRemainingInParallel(consumer, Long.MAX_VALUE, TimeUnit.NANOSECONDS);
  }

  /**
   * Performs the given action for each remaining element of the view in parallel until all elements have been
   * processed.
   *
   * @param consumer the action to be performed for each element.
   * @param timeout  the maximum time to wait.
   * @param unit     the time unit of the timeout argument.
   */
  public void forEachRemainingInParallel(Consumer<? super T> consumer, long timeout, TimeUnit unit) {

    Preconditions.checkNotNull(consumer, "consumer should not be null");

    int cores = Runtime.getRuntime().availableProcessors();
    ExecutorService executorService = Executors.newFixedThreadPool(cores * 8);

    while (hasNext()) {
      T element = next();
      executorService.execute(() -> consumer.accept(element));
    }
    try {
      executorService.shutdown();
      if (!executorService.awaitTermination(timeout, unit)) {
        logger_.error(
            LogFormatter.create().message("forEachRemainingInParallel(...) - The timeout elapsed before termination.")
                .formatError());
      }
    } catch (InterruptedException e) {
      logger_.error(LogFormatter.create().message(e).formatError());
    }
  }

  /**
   * Reduce the view to a single value using a given operation.
   *
   * @param carry     the neutral element.
   * @param operation the operation to apply.
   * @param <U>
   * @return a single value.
   */
  public <U> U reduce(@Var U carry, BiFunction<U, T, U> operation) {

    Preconditions.checkNotNull(carry, "carry should not be null");
    Preconditions.checkNotNull(operation, "operation should not be null");

    while (hasNext()) {
      carry = operation.apply(carry, next());
    }
    return carry;
  }

  /**
   * Returns the first {@code n} elements of a view.
   *
   * @param n the number of elements to keep.
   * @return the first {@code n} elements of the {@link View}.
   */
  public View<T> take(long n) {

    Preconditions.checkArgument(n >= 0, "n must be >= 0");

    View<T> self = this;
    return new View<>(new AbstractIterator<T>() {

      private long taken_ = 0;

      @Override
      protected T computeNext() {
        if (self.hasNext() && taken_ < n) {
          taken_++;
          return self.next();
        }
        return endOfData();
      }
    });
  }

  /**
   * Returns a view containing all starting elements as long as a condition is matched.
   *
   * @param predicate the condition to match.
   * @return a {@link View} of the matching elements.
   */
  public View<T> takeWhile(Predicate<? super T> predicate) {

    Preconditions.checkNotNull(predicate, "predicate should not be null");

    View<T> self = this;
    return new View<>(new AbstractIterator<T>() {

      @Override
      protected T computeNext() {
        if (self.hasNext()) {
          T e = self.next();
          if (predicate.test(e)) {
            return e;
          }
        }
        return endOfData();
      }
    });
  }

  /**
   * Returns a view with the first {@code n} elements removed.
   *
   * @param n the number of elements to remove.
   * @return a {@link View} with the first {@code n} elements removed.
   */
  public View<T> skip(long n) {
    return drop(n);
  }

  /**
   * Returns a view with the first {@code n} elements removed.
   *
   * @param n the number of elements to remove.
   * @return a {@link View} with the first {@code n} elements removed.
   */
  public View<T> drop(long n) {

    Preconditions.checkArgument(n >= 0, "n must be >= 0");

    View<T> self = this;
    return new View<>(new AbstractIterator<T>() {

      private long dropped_ = 0;

      @Override
      protected T computeNext() {
        while (self.hasNext() && dropped_ < n) {
          dropped_++;
          self.next();
        }
        return self.hasNext() ? self.next() : endOfData();
      }
    });
  }

  /**
   * Returns a view with the front elements removed as long as they satisfy a condition.
   *
   * @param predicate the condition to satisfy.
   * @return a {@link View} with the front elements removed.
   */
  public View<T> skipWhile(Predicate<? super T> predicate) {
    return dropWhile(predicate);
  }

  /**
   * Returns a view with the front elements removed as long as they satisfy a condition.
   *
   * @param predicate the condition to satisfy.
   * @return a {@link View} with the front elements removed.
   */
  public View<T> dropWhile(Predicate<? super T> predicate) {

    Preconditions.checkNotNull(predicate, "predicate should not be null");

    View<T> self = this;
    return new View<>(new AbstractIterator<T>() {

      private boolean dropped_ = false;

      @Override
      protected T computeNext() {
        if (!dropped_) {
          while (self.hasNext()) {
            T e = self.next();
            if (!predicate.test(e)) {
              dropped_ = true;
              return e;
            }
          }
          dropped_ = true;
        }
        return self.hasNext() ? self.next() : endOfData();
      }
    });
  }

  /**
   * Load all the view elements in memory then sort them.
   *
   * @return a sorted {@link View}.
   */
  public View<T> sort(Comparator<T> comparator) {
    return of(toSortedList(comparator));
  }

  /**
   * Compress (using the BZIP2 algorithm) then encrypt (using the Open SSL library) the view elements.
   * <p>
   * Note that unless properly configured, the password will be visible through `ps`.
   *
   * @param password the password to use.
   * @return a stream of encrypted rows.
   */
  @Beta
  public View<String> encrypt(String password) {

    Preconditions.checkArgument(!Strings.isNullOrEmpty(password), "password should neither be null nor empty");

    return mapUsingBashCommand(String.format("bzip2 | openssl enc -aes-256-cbc -a -pbkdf2 -pass pass:%s", password));
  }

  /**
   * Decrypt (using the Open SSL library) then decompress (using the BZIP2 algorithm) the view elements.
   * <p>
   * Note that unless properly configured, the password will be visible through `ps`.
   *
   * @param password the password to use.
   * @return a stream of encrypted rows.
   */
  @Beta
  public View<String> decrypt(String password) {

    Preconditions.checkArgument(!Strings.isNullOrEmpty(password), "password should neither be null nor empty");

    return mapUsingBashCommand(
        String.format("openssl enc -aes-256-cbc -a -d -pbkdf2 -pass pass:%s | bunzip2", password));
  }

  @Beta
  public View<String> mapUsingBashCommand(String command) {

    Preconditions.checkArgument(!Strings.isNullOrEmpty(command), "command should neither be null nor empty");

    PeekingIterator<T> iterator = Iterators.peekingIterator(this);
    T t = iterator.hasNext() ? iterator.peek() : null;
    View<String> view;

    if (t instanceof String) {
      view = new View<>((PeekingIterator<String>) iterator);
    } else if (t instanceof Integer) {
      view = new View<>((PeekingIterator<Integer>) iterator).map(i -> Integer.toString(i, 10));
    } else if (t instanceof Long) {
      view = new View<>((PeekingIterator<Long>) iterator).map(i -> Long.toString(i, 10));
    } else if (t instanceof Double) {
      view = new View<>((PeekingIterator<Double>) iterator).map(i -> Double.toString(i));
    } else if (t instanceof Float) {
      view = new View<>((PeekingIterator<Float>) iterator).map(i -> Float.toString(i));
    } else {
      view = null;
    }

    Preconditions.checkState(view != null, "view elements cannot be automatically mapped to strings");

    try {

      ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
      processBuilder.redirectErrorStream(true);

      Process process = processBuilder.start();

      new Thread(() -> {
        try (BufferedWriter writer = new BufferedWriter(
            new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {

          view.forEachRemaining(str -> {
            try {
              writer.write(str);
              writer.newLine();
            } catch (IOException e) {
              logger_.error(LogFormatter.create().add("command", command).add("str", str).message(e).formatError());
            }
          });
        } catch (IOException e) {
          logger_.error(LogFormatter.create().add("command", command).message(e).formatError());
        }
      }).start();

      return of(process);
    } catch (IOException e) {
      logger_.error(LogFormatter.create().add("command", command).message(e).formatError());
      return View.of();
    }
  }

  /**
   * Returns a view consisting of the results of applying the given function to the elements of this view.
   *
   * @param fn  the function to apply.
   * @param <U>
   * @return a new {@link View}.
   */
  public <U> View<U> map(Function<? super T, ? extends U> fn) {
    return of(Iterators.transform(this, fn::apply));
  }

  /**
   * Returns a view consisting of the elements of this view matching the given predicate.
   *
   * @param predicate the predicate to satisfy.
   * @return a new {@link View}.
   */
  public View<T> filter(Predicate<? super T> predicate) {
    return new View<>(Iterators.filter(this, predicate::test));
  }

  /**
   * Returns a view consisting of the elements of this view that match the given predicate.
   *
   * @param predicate the predicate to satisfy.
   * @return a new {@link View}.
   */
  public View<T> retain(Predicate<? super T> predicate) {
    return filter(predicate);
  }

  /**
   * Returns a view consisting of the elements of this view that do not match the given predicate.
   *
   * @param predicate the predicate to satisfy.
   * @return a new {@link View}.
   */
  public View<T> discard(Predicate<? super T> predicate) {
    return filter(predicate.negate());
  }

  /**
   * Peek each element.
   *
   * @param consumer the action to perform on each element.
   * @return a new {@link View}.
   */
  public View<T> peek(Consumer<T> consumer) {

    Preconditions.checkNotNull(consumer, "consumer should not be null");

    return map(x -> {
      consumer.accept(x);
      return x;
    });
  }

  /**
   * Peek each element and process them according to the predicate.
   *
   * @param predicate the predicate to satisfy.
   * @param ifTrue    the function to apply if the predicate is satisfied.
   * @return a new {@link View}.
   */
  public View<T> peekIfTrue(Predicate<? super T> predicate, Consumer<? super T> ifTrue) {
    return peek(predicate, ifTrue, null);
  }

  /**
   * Peek each element and process them according to the predicate.
   *
   * @param predicate the predicate to satisfy.
   * @param ifFalse   the function to apply if the predicate is not satisfied.
   * @return a new {@link View}.
   */
  public View<T> peekIfFalse(Predicate<? super T> predicate, Consumer<? super T> ifFalse) {
    return peek(predicate, null, ifFalse);
  }

  /**
   * Peek each element and process them according to the predicate.
   *
   * @param predicate the predicate to satisfy.
   * @param ifTrue    the function to apply if the predicate is satisfied.
   * @param ifFalse   the function to apply if the predicate is not satisfied.
   * @return a new {@link View}.
   */
  public View<T> peek(Predicate<? super T> predicate, Consumer<? super T> ifTrue, Consumer<? super T> ifFalse) {

    Preconditions.checkNotNull(predicate, "predicate should not be null");

    return peek(element -> {
      if (predicate.test(element)) {
        if (ifTrue != null) {
          ifTrue.accept(element);
        }
      } else {
        if (ifFalse != null) {
          ifFalse.accept(element);
        }
      }
    });
  }

  /**
   * Iterate and process each element according to the predicate.
   *
   * @param predicate the predicate to satisfy.
   * @param ifTrue    the function to apply if the predicate is satisfied.
   */
  public void forEachRemainingIfTrue(Predicate<? super T> predicate, Consumer<? super T> ifTrue) {
    forEachRemaining(predicate, ifTrue, null);
  }

  /**
   * Iterate and process each element according to the predicate.
   *
   * @param predicate the predicate to satisfy.
   * @param ifFalse   the function to apply if the predicate is not satisfied.
   */
  public void forEachRemainingIfFalse(Predicate<? super T> predicate, Consumer<? super T> ifFalse) {
    forEachRemaining(predicate, null, ifFalse);
  }

  /**
   * Iterate and process each element according to the predicate.
   *
   * @param predicate the predicate to satisfy.
   * @param ifTrue    the function to apply if the predicate is satisfied.
   * @param ifFalse   the function to apply if the predicate is not satisfied.
   */
  public void forEachRemaining(Predicate<? super T> predicate, Consumer<? super T> ifTrue,
      Consumer<? super T> ifFalse) {

    Preconditions.checkNotNull(predicate, "predicate should not be null");

    forEachRemaining(element -> {
      if (predicate.test(element)) {
        if (ifTrue != null) {
          ifTrue.accept(element);
        }
      } else {
        if (ifFalse != null) {
          ifFalse.accept(element);
        }
      }
    });
  }

  /**
   * Combines two views into a single view. The returned view iterates across the elements of the current view, followed
   * by the elements of the other view.
   *
   * @param elements the other elements.
   * @return a new {@link View}.
   */
  @Generated
  public View<T> concat(T... elements) {
    return concat(View.of(elements));
  }

  /**
   * Combines two views into a single view. The returned view iterates across the elements of the current view, followed
   * by the elements of the other view.
   *
   * @param stream the other elements.
   * @return a new {@link View}.
   */
  @Generated
  public View<T> concat(Stream<T> stream) {
    return concat(View.of(stream));
  }

  /**
   * Combines two views into a single view. The returned view iterates across the elements of the current view, followed
   * by the elements of the other view.
   *
   * @param iterable the other elements.
   * @return a new {@link View}.
   */
  @Generated
  public View<T> concat(Iterable<T> iterable) {
    return concat(View.of(iterable));
  }

  /**
   * Combines two views into a single view. The returned view iterates across the elements of the current view, followed
   * by the elements of the other view.
   *
   * @param enumeration the other elements.
   * @return a new {@link View}.
   */
  @Generated
  public View<T> concat(Enumeration<T> enumeration) {
    return concat(View.of(enumeration));
  }

  /**
   * Combines two views into a single view. The returned view iterates across the elements of the current view, followed
   * by the elements of the other view.
   *
   * @param view the other {@link View}.
   * @return a new {@link View}.
   */
  public View<T> concat(View<? extends T> view) {

    Preconditions.checkNotNull(view, "view should not be null");

    return new View<>(Iterators.concat(this, view));
  }

  /**
   * Divides a view into unmodifiable sublists of the given size (the final list may be smaller).
   *
   * @param size the size of each list.
   * @return a new {@link View}.
   */
  public View<List<T>> partition(int size) {
    return new View<>(Iterators.partition(this, size));
  }

  /**
   * Display progress in slices of {@code sliceSize} length.
   *
   * @param sliceSize the length of each slice.
   * @return a new {@link View}.
   */
  @Generated
  public View<T> displayProgress(int sliceSize) {

    AtomicInteger slice = new AtomicInteger(0);
    AtomicInteger count = new AtomicInteger(0);
    String msg = "slice_id=%d, slice_count=%d, total_count=%d";
    AsciiProgressBar.ProgressBar progress = AsciiProgressBar.create();

    return peek(t -> {

      int sliceId = slice.get() + 1;
      int cnt = count.incrementAndGet();
      int done = cnt % sliceSize == 0 ? sliceSize : cnt % sliceSize;

      if (hasNext()) {
        progress.update(done, sliceSize, String.format(msg, sliceId, done, cnt));
      } else {
        progress.update(done, done, String.format(msg, sliceId, done, cnt));
      }
      if (done == sliceSize) {
        slice.incrementAndGet();
      }
    });
  }

  /**
   * Remove consecutive duplicates from the current view.
   * <p>
   * The view must be sorted.
   *
   * @return a new {@link View}.
   */
  public View<T> dedupSorted() {
    PeekingIterator<T> stream = Iterators.peekingIterator(this);
    return new View<>(new AbstractIterator<T>() {

      @Override
      protected T computeNext() {

        if (!stream.hasNext()) {
          return endOfData();
        }

        @Var T curr = stream.next();

        while (stream.hasNext() && curr.equals(stream.peek())) {
          curr = stream.next();
        }
        return curr;
      }
    });
  }

  /**
   * Returns elements that are presents in the first view but not in the second one. Duplicate elements are returned
   * only once.
   * <p>
   * Both views must be sorted.
   *
   * @param view the other {@link View}.
   * @return a new {@link View}.
   */
  public View<T> diffSorted(View<? extends Comparable<T>> view) {

    Preconditions.checkNotNull(view, "view should not be null");

    PeekingIterator<T> thisStream = Iterators.peekingIterator(this);
    PeekingIterator<? extends Comparable<T>> thatStream = Iterators.peekingIterator(view);

    return new View<>(new AbstractIterator<T>() {

      private Comparable<T> cur2_;

      @Override
      protected T computeNext() {

        while (thisStream.hasNext()) {

          T cur1 = thisStream.next();

          while (thatStream.hasNext() && (cur2_ == null || cur2_.compareTo(cur1) < 0)) {
            cur2_ = thatStream.next();
          }

          if (cur2_ != null && cur2_.compareTo(cur1) < 0) {
            return cur1;
          }
          if (cur2_ == null || cur2_.compareTo(cur1) > 0) {
            return cur1;
          }
        }
        return endOfData();
      }
    });
  }

  /**
   * Returns elements that are presents in both views. Duplicate elements are returned only once.
   * <p>
   * Both views must be sorted.
   *
   * @param view the other {@link View}.
   * @return a new {@link View}.
   */
  public View<T> intersectSorted(View<? extends Comparable<T>> view) {

    Preconditions.checkNotNull(view, "view should not be null");

    PeekingIterator<T> thisStream = Iterators.peekingIterator(this);
    PeekingIterator<? extends Comparable<T>> thatStream = Iterators.peekingIterator(view);

    return new View<>(new AbstractIterator<T>() {

      @Override
      protected T computeNext() {

        @Var T cur = null;

        while (thisStream.hasNext() && thatStream.hasNext()) {

          cur = thisStream.next();

          while (thatStream.hasNext() && thatStream.peek().compareTo(cur) < 0) {
            thatStream.next();
          }

          if (!thatStream.hasNext()) {
            return endOfData();
          }

          int cmp = thatStream.peek().compareTo(cur);

          if (cmp == 0) {
            while (thisStream.hasNext() && thisStream.peek().equals(cur)) {
              cur = thisStream.next();
            }
            while (thatStream.hasNext() && thatStream.peek().equals(cur)) {
              cur = (T) thatStream.next();
            }
            return cur;
          }
        }
        return endOfData();
      }
    });
  }

  /**
   * Merge the output of one or more sorted views with the output of the current view. The assumption is that all views
   * (including this one) are sorted in non-descending order.
   *
   * @param views      the views to merge with the output of the current view.
   * @param comparator the comparator used to merge the output of each view.
   * @return a new {@link View}.
   */
  public View<T> mergeSorted(Iterable<? extends View<? extends T>> views, Comparator<? super T> comparator) {

    Preconditions.checkNotNull(views, "views should not be null");
    Preconditions.checkNotNull(comparator, "comparator should not be null");

    List<View<? extends T>> list = new ArrayList<>();
    list.add(this);
    views.forEach(list::add);

    return new View<>(Iterators.mergeSorted(list, comparator));
  }

  /**
   * Group consecutive values matching a given predicate together. Return the group as a whole.
   *
   * @param predicate the predicate to match.
   * @return a new {@link View}.
   */
  public View<View<T>> groupSorted(BiPredicate<? super T, ? super T> predicate) {

    Preconditions.checkNotNull(predicate, "predicate should not be null");

    PeekingIterator<T> self = Iterators.peekingIterator(this);
    return new View<>(new AbstractIterator<View<T>>() {

      @Override
      protected View<T> computeNext() {

        List<T> list = new ArrayList<>();

        while (self.hasNext()) {

          T t = self.peek();

          if (list.isEmpty() || predicate.test(list.get(list.size() - 1), t)) {
            list.add(self.next());
          } else {
            return new View<>(list.iterator());
          }
        }
        return list.isEmpty() ? endOfData() : new View<>(list.iterator());
      }
    });
  }

  /**
   * Flatten a view. Optionally map the view entries at the same time.
   *
   * @param fn  the mapping function.
   * @param <U>
   * @return a new {@link View}.
   */
  public <U> View<U> flatten(Function<T, View<U>> fn) {

    Preconditions.checkNotNull(fn, "fn should not be null");

    PeekingIterator<T> stream = Iterators.peekingIterator(this);

    return new View<>(new AbstractIterator<U>() {

      private View<U> view_;

      @Override
      protected U computeNext() {

        if (view_ == null || !view_.hasNext()) {

          view_ = null;

          while (stream.hasNext() && (view_ == null || !view_.hasNext())) {

            View<U> view = fn.apply(stream.next());

            if (view != null && view.hasNext()) {
              view_ = view;
            }
          }

          if (view_ == null || !view_.hasNext()) {
            return endOfData();
          }
        }
        return view_.next();
      }
    });
  }

  /**
   * Split the view into windows of fixed size {@code length} (the final list may be smaller). At each step, the first
   * {@code length - 1} elements of the window are a suffix of the previous window.
   *
   * @param length the window size.
   * @return a {@link ImmutableList}.
   */
  public View<List<T>> overlappingWindow(int length) {
    return new View<>(new SlidingWindowIterator<>(this, length, true, false));
  }

  /**
   * Split the view into windows of fixed size {@code length} (the final list may be smaller). The returned windows do
   * not intersect.
   *
   * @param length the window size.
   * @return a {@link ImmutableList}.
   */
  public View<List<T>> nonOverlappingWindow(int length) {
    return new View<>(new SlidingWindowIterator<>(this, length, false, false));
  }

  /**
   * Split the view into windows of fixed size {@code length} (the final list will never be smaller). At each step, the
   * first {@code length - 1} elements of the window are a suffix of the previous window.
   *
   * @param length the window size.
   * @return a {@link ImmutableList}.
   */
  public View<List<T>> overlappingWindowWithStrictLength(int length) {
    return new View<>(new SlidingWindowIterator<>(this, length, true, true));
  }

  /**
   * Split the view into windows of fixed size {@code length} (the final list will never be smaller). The returned
   * windows do not intersect.
   *
   * @param length the window size.
   * @return a {@link ImmutableList}.
   */
  public View<List<T>> nonOverlappingWindowWithStrictLength(int length) {
    return new View<>(new SlidingWindowIterator<>(this, length, false, true));
  }

  public static class Breaker {

    private boolean shouldBreak_ = false;

    public void stop() {
      shouldBreak_ = true;
    }

    public boolean shouldBreak() {
      return shouldBreak_;
    }
  }

  private static class StitchingIterator<T> extends AbstractIterator<List<T>> {

    private final List<? extends Iterator<T>> views_;

    public StitchingIterator(List<? extends Iterator<T>> views) {
      views_ = Preconditions.checkNotNull(views, "views should not be null");
    }

    @Override
    protected List<T> computeNext() {

      List<T> row = new ArrayList<>();

      for (int i = 0; i < views_.size(); i++) {
        if (!views_.get(i).hasNext()) {
          return endOfData();
        }
        row.add(views_.get(i).next());
      }
      return row.isEmpty() ? endOfData() : row;
    }
  }

  private static class StreamIterator<T> extends AbstractIterator<T> implements AutoCloseable {

    private final Iterator<T> iterator_;
    private Stream<T> stream_;

    public StreamIterator(Stream<T> stream) {

      Preconditions.checkNotNull(stream, "stream should not be null");

      iterator_ = stream.iterator();
      stream_ = stream;
    }

    @Override
    public void close() {
      if (stream_ != null) {
        stream_.close();
        stream_ = null;
      }
    }

    @Override
    protected void finalize() {
      close();
    }

    @Override
    protected T computeNext() {
      if (iterator_.hasNext()) {
        return iterator_.next();
      }
      close();
      return endOfData();
    }
  }

  private static class SlidingWindowIterator<T> extends AbstractIterator<List<T>> {

    private final Iterator<T> iterator_;
    private final int length_;
    private final boolean overlaps_;
    private final List<T> list_;
    private final boolean strictWindowsLength_;

    public SlidingWindowIterator(Iterator<T> iterator, int length, boolean overlaps, boolean strictWindowsLength) {

      Preconditions.checkNotNull(iterator, "iterator should not be null");
      Preconditions.checkArgument(length > 0, "length must be > 0");

      iterator_ = iterator;
      length_ = length;
      overlaps_ = overlaps;
      list_ = new ArrayList<>(length);
      strictWindowsLength_ = strictWindowsLength;
    }

    @Override
    protected List<T> computeNext() {
      if (!overlaps_) {
        list_.clear();
      } else if (!list_.isEmpty()) {
        list_.remove(0);
      }
      while (iterator_.hasNext() && list_.size() < length_) {
        list_.add(iterator_.next());
      }
      return list_.isEmpty() || (strictWindowsLength_ && list_.size() < length_) ? endOfData()
          : ImmutableList.copyOf(list_);
    }
  }

  /**
   * Extracted from https://richardstartin.github.io/posts/reservoir-sampling
   */
  private static class AlgorithmL<T> {

    private final int capacity_;
    private final List<T> reservoir_;
    private long counter_;
    private long next_;
    private double w_;

    public AlgorithmL(int capacity) {
      capacity_ = capacity;
      reservoir_ = new ArrayList<>(capacity);
      counter_ = 0;
      next_ = capacity_;
      w_ = Math.exp(Math.log(ThreadLocalRandom.current().nextDouble()) / capacity_);
      skip();
    }

    public List<T> sample() {
      return reservoir_;
    }

    public void add(T value) {
      if (counter_ < capacity_) {
        reservoir_.add(value);
      } else {
        if (counter_ == next_) {
          reservoir_.set(ThreadLocalRandom.current().nextInt(capacity_), value);
          skip();
        }
      }
      ++counter_;
    }

    private void skip() {
      next_ += (long) (Math.log(ThreadLocalRandom.current().nextDouble()) / Math.log(1 - w_)) + 1;
      w_ *= Math.exp(Math.log(ThreadLocalRandom.current().nextDouble()) / capacity_);
    }
  }
}