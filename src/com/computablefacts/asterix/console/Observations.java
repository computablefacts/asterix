package com.computablefacts.asterix.console;

import com.computablefacts.asterix.View;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.errorprone.annotations.CheckReturnValue;
import java.io.File;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import javax.annotation.concurrent.NotThreadSafe;

/**
 * Log messages to both the console and a file. Simultaneously, keep track of the elapsed time.
 */
@NotThreadSafe
@CheckReturnValue
final public class Observations implements AutoCloseable {

  private final Stopwatch stopwatch_ = Stopwatch.createStarted();
  private final Queue<String> observations_ = new ConcurrentLinkedQueue<>();
  private final File file_;
  private final int threshold_;

  public Observations(File file) {
    this(file, 50);
  }

  public Observations(File file, int threshold) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkState(threshold > 0, "threshold must be > 0");

    file_ = file;
    threshold_ = threshold;
  }

  @Override
  protected void finalize() {
    flush();
  }

  @Override
  public void close() {
    flush();
  }

  public void flush() {

    stopwatch_.stop();
    observations_.offer(String.format("Elapsed time : %ds", stopwatch_.elapsed(TimeUnit.SECONDS)));

    if (file_ != null) {
      synchronized (this) {
        View.of(observations_).toFile(Function.identity(), file_, file_.exists());
        observations_.clear();
      }
    }

    stopwatch_.start();
  }

  public void add(String message) {
    if (message != null) {

      String msg = message.trim();

      if (file_ != null) {
        observations_.offer(msg);
      }
      System.out.println(msg);
    }
    flushPrivate();
  }

  private void flushPrivate() {
    if (file_ != null) {
      if (observations_.size() >= threshold_) {
        synchronized (this) {
          View.of(observations_).toFile(Function.identity(), file_, file_.exists());
          observations_.clear();
        }
      }
    }
  }
}
