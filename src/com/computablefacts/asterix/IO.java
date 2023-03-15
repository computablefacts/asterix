package com.computablefacts.asterix;

import static com.computablefacts.asterix.IO.eCompressionAlgorithm.BZIP2;
import static com.computablefacts.asterix.IO.eCompressionAlgorithm.GZIP;
import static com.computablefacts.asterix.IO.eCompressionAlgorithm.NONE;

import com.computablefacts.logfmt.LogFormatter;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CheckReturnValue
final public class IO {

  private static final Logger logger_ = LoggerFactory.getLogger(IO.class);

  private IO() {
  }

  public static String readText(File file) {
    return readText(file.toPath());
  }

  public static String readText(File file, eCompressionAlgorithm algorithm) {
    return readText(file.toPath(), algorithm);
  }

  public static String readText(Path file) {
    return readText(file, NONE);
  }

  public static String readText(Path file, eCompressionAlgorithm algorithm) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(Files.exists(file), "file does not exist : %s", file);
    Preconditions.checkNotNull(algorithm, "algorithm should not be null");

    try {
      return View.of(newLineIterator(file, algorithm)).join(x -> x, "\n");
    } catch (IOException e) {
      logger_.error(
          LogFormatter.create().add("file", file).add("compression_algorithm", algorithm).message(e).formatError());
    }
    return "";
  }

  public static boolean writeText(File file, String text, boolean append) {
    return writeText(file.toPath(), text, append);
  }

  public static boolean writeText(File file, String text, boolean append, eCompressionAlgorithm algorithm) {
    return writeText(file.toPath(), text, append, algorithm);
  }

  public static boolean writeText(Path file, String text, boolean append) {
    return writeText(file, text, append, NONE);
  }

  public static boolean writeText(Path file, String text, boolean append, eCompressionAlgorithm algorithm) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(!append || Files.exists(file), "file does not exist : %s", file);
    Preconditions.checkNotNull(text, "text should not be null");
    Preconditions.checkNotNull(algorithm, "algorithm should not be null");

    try (BufferedWriter writer = newFileWriter(file, append, algorithm)) {
      writer.write(text);
      return true;
    } catch (IOException e) {
      logger_.error(LogFormatter.create().add("file", file).add("text", text.substring(0, Math.min(80, text.length())))
          .add("compression_algorithm", algorithm).add("append", append).message(e).formatError());
    }
    return false;
  }

  public static List<String> readLines(File file) {
    return readLines(file.toPath());
  }

  public static List<String> readLines(Path file) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(Files.exists(file), "file does not exist : %s", file);

    try {
      return Files.readAllLines(file, StandardCharsets.UTF_8);
    } catch (IOException e) {
      logger_.error(LogFormatter.create().add("file", file).message(e).formatError());
    }
    return Lists.newArrayList();
  }

  public static boolean writeLines(File file, Iterable<String> lines, boolean append) {
    return writeLines(file.toPath(), lines, append);
  }

  public static boolean writeLines(Path file, Iterable<String> lines, boolean append) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(!append || Files.exists(file), "file does not exist : %s", file);
    Preconditions.checkNotNull(lines, "lines should not be null");

    try (BufferedWriter writer = newFileWriter(file, append)) {
      for (String line : lines) {
        writer.write(line);
        writer.newLine();
      }
      return true;
    } catch (IOException e) {
      logger_.error(LogFormatter.create().add("file", file).add("append", append).message(e).formatError());
    }
    return false;
  }

  public static LineIterator newLineIterator(File file) throws IOException {
    return newLineIterator(file.toPath());
  }

  public static LineIterator newLineIterator(File file, eCompressionAlgorithm algorithm) throws IOException {
    return newLineIterator(file.toPath(), algorithm);
  }

  public static LineIterator newLineIterator(Path file) throws IOException {
    return newLineIterator(file, NONE);
  }

  public static LineIterator newLineIterator(Path file, eCompressionAlgorithm algorithm) throws IOException {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(Files.exists(file), "file does not exist : %s", file);
    Preconditions.checkNotNull(algorithm, "algorithm should not be null");

    return new LineIterator(newFileReader(file, algorithm));
  }

  public static BufferedReader newFileReader(File file) throws IOException {
    return newFileReader(file.toPath());
  }

  public static BufferedReader newFileReader(File file, eCompressionAlgorithm algorithm) throws IOException {
    return newFileReader(file.toPath(), algorithm);
  }

  public static BufferedReader newFileReader(Path file) throws IOException {
    return newFileReader(file, NONE);
  }

  public static BufferedReader newFileReader(Path file, eCompressionAlgorithm algorithm) throws IOException {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(Files.exists(file), "file does not exist : %s", file);
    Preconditions.checkNotNull(algorithm, "algorithm should not be null");

    if (NONE.equals(algorithm)) {
      return Files.newBufferedReader(file, StandardCharsets.UTF_8);
    }
    if (GZIP.equals(algorithm)) {
      return new BufferedReader(
          new InputStreamReader(new GZIPInputStream(Files.newInputStream(file, StandardOpenOption.READ)),
              StandardCharsets.UTF_8));
    }
    return new BufferedReader(
        new InputStreamReader(new BZip2CompressorInputStream(Files.newInputStream(file, StandardOpenOption.READ)),
            StandardCharsets.UTF_8));
  }

  public static BufferedWriter newFileWriter(File file, boolean append) throws IOException {
    return newFileWriter(file.toPath(), append);
  }

  public static BufferedWriter newFileWriter(File file, boolean append, eCompressionAlgorithm algorithm)
      throws IOException {
    return newFileWriter(file.toPath(), append, algorithm);
  }

  public static BufferedWriter newFileWriter(Path file, boolean append) throws IOException {
    return newFileWriter(file, append, NONE);
  }

  public static BufferedWriter newFileWriter(Path file, boolean append, eCompressionAlgorithm algorithm)
      throws IOException {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(!append || Files.exists(file), "file does not exist : %s", file);
    Preconditions.checkNotNull(algorithm, "algorithm should not be null");

    if (NONE.equals(algorithm)) {
      return append ? Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.WRITE,
          StandardOpenOption.CREATE, StandardOpenOption.APPEND)
          : Files.newBufferedWriter(file, StandardCharsets.UTF_8, StandardOpenOption.WRITE,
              StandardOpenOption.CREATE_NEW);
    }
    if (GZIP.equals(algorithm)) {
      return append ? new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(
          Files.newOutputStream(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND)),
          StandardCharsets.UTF_8)) : new BufferedWriter(new OutputStreamWriter(
          new GZIPOutputStream(Files.newOutputStream(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)),
          StandardCharsets.UTF_8));
    }
    return append ? new BufferedWriter(new OutputStreamWriter(new BZip2CompressorOutputStream(
        Files.newOutputStream(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND)),
        StandardCharsets.UTF_8)) : new BufferedWriter(new OutputStreamWriter(new BZip2CompressorOutputStream(
        Files.newOutputStream(file, StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)), StandardCharsets.UTF_8));
  }

  public static boolean bzip2(File input, File output) {
    return bzip2(input.toPath(), output.toPath());
  }

  public static boolean bzip2(Path input, Path output) {

    Preconditions.checkNotNull(input, "input must not be null");
    Preconditions.checkArgument(Files.exists(input), "input does not exist : %s", input);
    Preconditions.checkNotNull(output, "output must not be null");
    Preconditions.checkArgument(!Files.exists(output), "output already exists : %s", output);

    try (BufferedReader reader = newFileReader(input)) {
      try (BufferedWriter writer = newFileWriter(output, false, BZIP2)) {

        char[] buffer = new char[4096];
        @Var int len;

        while ((len = reader.read(buffer)) != -1) {
          writer.write(buffer, 0, len);
        }
        return true;
      }
    } catch (IOException e) {
      logger_.error(LogFormatter.create().add("input", input).add("output", output).message(e).formatError());
    }
    return false;
  }

  public static boolean bunzip2(File input, File output) {
    return bunzip2(input.toPath(), output.toPath());
  }

  public static boolean bunzip2(Path input, Path output) {

    Preconditions.checkNotNull(input, "input must not be null");
    Preconditions.checkArgument(Files.exists(input), "input does not exist : %s", input);
    Preconditions.checkNotNull(output, "output must not be null");
    Preconditions.checkArgument(!Files.exists(output), "output already exists : %s", output);

    try (BufferedReader reader = newFileReader(input, BZIP2)) {
      try (BufferedWriter writer = newFileWriter(output, false)) {

        char[] buffer = new char[4096];
        @Var int len;

        while ((len = reader.read(buffer)) != -1) {
          writer.write(buffer, 0, len);
        }
        return true;
      }
    } catch (IOException e) {
      logger_.error(LogFormatter.create().add("input", input).add("output", output).message(e).formatError());
    }
    return false;
  }

  public static boolean gzip(File input, File output) {
    return gzip(input.toPath(), output.toPath());
  }

  public static boolean gzip(Path input, Path output) {

    Preconditions.checkNotNull(input, "input must not be null");
    Preconditions.checkArgument(Files.exists(input), "input does not exist : %s", input);
    Preconditions.checkNotNull(output, "output must not be null");
    Preconditions.checkArgument(!Files.exists(output), "output already exists : %s", output);

    try (BufferedReader reader = newFileReader(input)) {
      try (BufferedWriter writer = newFileWriter(output, false, GZIP)) {

        char[] buffer = new char[4096];
        @Var int len;

        while ((len = reader.read(buffer)) != -1) {
          writer.write(buffer, 0, len);
        }
        return true;
      }
    } catch (IOException e) {
      logger_.error(LogFormatter.create().add("input", input).add("output", output).message(e).formatError());
    }
    return false;
  }

  public static boolean gunzip(File input, File output) {
    return gunzip(input.toPath(), output.toPath());
  }

  public static boolean gunzip(Path input, Path output) {

    Preconditions.checkNotNull(input, "input must not be null");
    Preconditions.checkArgument(Files.exists(input), "input does not exist : %s", input);
    Preconditions.checkNotNull(output, "output must not be null");
    Preconditions.checkArgument(!Files.exists(output), "output already exists : %s", output);

    try (BufferedReader reader = newFileReader(input, GZIP)) {
      try (BufferedWriter writer = newFileWriter(output, false)) {

        char[] buffer = new char[4096];
        @Var int len;

        while ((len = reader.read(buffer)) != -1) {
          writer.write(buffer, 0, len);
        }
        return true;
      }
    } catch (IOException e) {
      logger_.error(LogFormatter.create().add("input", input).add("output", output).message(e).formatError());
    }
    return false;
  }

  public static Result<Path> newTmpFile(String extension) {
    try {
      return Result.of(Files.createTempFile("", Strings.nullToEmpty(extension)));
    } catch (IOException e) {
      logger_.error(LogFormatter.create().message(e).formatError());
    }
    return Result.failure("file cannot be created");
  }

  public static Result<Path> newTmpDirectory() {
    try {
      return Result.of(Files.createTempDirectory(""));
    } catch (IOException e) {
      logger_.error(LogFormatter.create().message(e).formatError());
    }
    return Result.failure("directory cannot be created");
  }

  @CanIgnoreReturnValue
  public static boolean ensureFileExists(File file) {
    return ensureFileExists(file.toPath());
  }

  @CanIgnoreReturnValue
  public static boolean ensureFileExists(Path path) {

    Preconditions.checkNotNull(path, "path should not be null");

    if (Files.exists(path)) {
      return true;
    }
    try {
      Files.createFile(path);
      return true;
    } catch (IOException e) {
      logger_.error(LogFormatter.create().add("path", path).message(e).formatError());
    }
    return false;
  }

  @CanIgnoreReturnValue
  public static boolean ensureDirectoryExists(File file) {
    return ensureDirectoryExists(file.toPath());
  }

  @CanIgnoreReturnValue
  public static boolean ensureDirectoryExists(Path path) {

    Preconditions.checkNotNull(path, "path should not be null");

    if (Files.exists(path)) {
      return true;
    }
    try {
      Files.createDirectories(path);
      return true;
    } catch (IOException e) {
      logger_.error(LogFormatter.create().add("path", path).message(e).formatError());
    }
    return false;
  }

  @CanIgnoreReturnValue
  public static boolean replace(File source, File destination) {
    return replace(source.toPath(), destination.toPath());
  }

  @CanIgnoreReturnValue
  public static boolean replace(Path source, Path destination) {

    Preconditions.checkNotNull(source, "source should not be null");
    Preconditions.checkNotNull(destination, "destination should not be null");

    if (!Files.exists(source)) {
      return false;
    }
    try {
      Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
      return true;
    } catch (IOException e) {
      logger_.error(
          LogFormatter.create().add("source", source).add("destination", destination).message(e).formatError());
    }
    return false;
  }

  @CanIgnoreReturnValue
  public static boolean move(File source, File destination) {
    return move(source.toPath(), destination.toPath());
  }

  @CanIgnoreReturnValue
  public static boolean move(Path source, Path destination) {

    Preconditions.checkNotNull(source, "source should not be null");
    Preconditions.checkNotNull(destination, "destination should not be null");

    if (!Files.exists(source)) {
      return false;
    }
    if (Files.exists(destination)) {
      return false;
    }
    try {
      Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
      return true;
    } catch (IOException e) {
      logger_.error(
          LogFormatter.create().add("source", source).add("destination", destination).message(e).formatError());
    }
    return false;
  }

  @CanIgnoreReturnValue
  public static boolean copy(File source, File destination) {
    return copy(source.toPath(), destination.toPath());
  }

  @CanIgnoreReturnValue
  public static boolean copy(Path source, Path destination) {

    Preconditions.checkNotNull(source, "source should not be null");
    Preconditions.checkNotNull(destination, "destination should not be null");

    if (!Files.exists(source)) {
      return false;
    }
    if (Files.exists(destination)) {
      return false;
    }
    try {
      Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
      return true;
    } catch (IOException e) {
      logger_.error(
          LogFormatter.create().add("source", source).add("destination", destination).message(e).formatError());
    }
    return false;
  }

  @CanIgnoreReturnValue
  public static boolean delete(File file) {

    Preconditions.checkNotNull(file, "file should not be null");

    if (!file.exists()) {
      return false;
    }
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      for (int i = 0; files != null && i < files.length; ++i) {
        delete(files[i]);
      }
    }
    return file.delete();
  }

  final public static class LineIterator extends AbstractIterator<String> implements AutoCloseable {

    private BufferedReader reader_;

    public LineIterator(BufferedReader reader) {
      reader_ = Preconditions.checkNotNull(reader, "reader should not be null");
    }

    @Override
    public void close() {
      if (reader_ != null) {
        try {
          reader_.close();
        } catch (IOException e) {
          logger_.error(LogFormatter.create().message(e).formatError());
        }
        reader_ = null;
      }
    }

    @Override
    protected void finalize() {
      close();
    }

    @Override
    protected String computeNext() {
      try {
        if (reader_ != null) {
          String line = reader_.readLine();
          if (line != null) {
            return line;
          }
        }
      } catch (IOException e) {
        logger_.error(LogFormatter.create().message(e).formatError());
      }
      close();
      return endOfData();
    }
  }

  public enum eCompressionAlgorithm {
    NONE, GZIP, BZIP2
  }
}