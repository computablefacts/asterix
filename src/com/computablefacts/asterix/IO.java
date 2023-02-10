package com.computablefacts.asterix;

import com.computablefacts.logfmt.LogFormatter;
import com.google.common.base.Preconditions;
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

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file does not exist : %s", file);

    return String.join("\n", readLines(file));
  }

  public static boolean writeText(File file, String text, boolean append) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(!append || file.exists(), "file does not exist : %s", file);
    Preconditions.checkNotNull(text, "text should not be null");

    try (BufferedWriter writer = newFileWriter(file, append)) {
      writer.write(text);
      return true;
    } catch (IOException e) {
      logger_.error(LogFormatter.create().add("file", file).add("text", text.substring(0, Math.min(80, text.length())))
          .add("append", append).message(e).formatError());
    }
    return false;
  }

  public static boolean writeCompressedTextBZip2(File file, String text, boolean append) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(!append || file.exists(), "file does not exist : %s", file);
    Preconditions.checkNotNull(text, "text should not be null");

    try (BufferedWriter writer = newCompressedFileWriterBZip2(file, append)) {
      writer.write(text);
      return true;
    } catch (IOException e) {
      logger_.error(LogFormatter.create().add("file", file).add("text", text.substring(0, Math.min(80, text.length())))
          .add("append", append).message(e).formatError());
    }
    return false;
  }

  public static boolean writeCompressedText(File file, String text, boolean append) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(!append || file.exists(), "file does not exist : %s", file);
    Preconditions.checkNotNull(text, "text should not be null");

    try (BufferedWriter writer = newCompressedFileWriter(file, append)) {
      writer.write(text);
      return true;
    } catch (IOException e) {
      logger_.error(LogFormatter.create().add("file", file).add("text", text.substring(0, Math.min(80, text.length())))
          .add("append", append).message(e).formatError());
    }
    return false;
  }

  public static List<String> readLines(File file) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file does not exist : %s", file);

    try {
      return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      logger_.error(LogFormatter.create().add("file", file).message(e).formatError());
    }
    return Lists.newArrayList();
  }

  public static boolean writeLines(File file, Iterable<String> lines, boolean append) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(!append || file.exists(), "file does not exist : %s", file);
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

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file does not exist : %s", file);

    return new LineIterator(newFileReader(file));
  }

  public static LineIterator newCompressedLineIteratorBZip2(File file) throws IOException {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file does not exist : %s", file);

    return new LineIterator(newCompressedFileReaderBZip2(file));
  }

  public static LineIterator newCompressedLineIterator(File file) throws IOException {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file does not exist : %s", file);

    return new LineIterator(newCompressedFileReader(file));
  }

  public static BufferedReader newFileReader(File file) throws IOException {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file does not exist : %s", file);

    return Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
  }

  public static BufferedWriter newFileWriter(File file, boolean append) throws IOException {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(!append || file.exists(), "file does not exist : %s", file);

    if (!append) {
      return Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8, StandardOpenOption.WRITE,
          StandardOpenOption.CREATE_NEW);
    }
    return Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8, StandardOpenOption.WRITE,
        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
  }

  public static BufferedReader newCompressedFileReaderBZip2(File file) throws IOException {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file does not exist : %s", file);

    return new BufferedReader(new InputStreamReader(
        new BZip2CompressorInputStream(Files.newInputStream(file.toPath(), StandardOpenOption.READ)),
        StandardCharsets.UTF_8));
  }

  public static BufferedWriter newCompressedFileWriterBZip2(File file, boolean append) throws IOException {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(!append || file.exists(), "file does not exist : %s", file);

    if (!append) {
      return new BufferedWriter(new OutputStreamWriter(new BZip2CompressorOutputStream(
          Files.newOutputStream(file.toPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)),
          StandardCharsets.UTF_8));
    }
    return new BufferedWriter(new OutputStreamWriter(new BZip2CompressorOutputStream(
        Files.newOutputStream(file.toPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE,
            StandardOpenOption.APPEND)), StandardCharsets.UTF_8));
  }

  public static BufferedReader newCompressedFileReader(File file) throws IOException {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file does not exist : %s", file);

    return new BufferedReader(
        new InputStreamReader(new GZIPInputStream(Files.newInputStream(file.toPath(), StandardOpenOption.READ)),
            StandardCharsets.UTF_8));
  }

  public static BufferedWriter newCompressedFileWriter(File file, boolean append) throws IOException {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(!append || file.exists(), "file does not exist : %s", file);

    if (!append) {
      return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(
          Files.newOutputStream(file.toPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)),
          StandardCharsets.UTF_8));
    }
    return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(
        Files.newOutputStream(file.toPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE,
            StandardOpenOption.APPEND)), StandardCharsets.UTF_8));
  }

  public static boolean bzip2(File input, File output) {

    Preconditions.checkNotNull(input, "input must not be null");
    Preconditions.checkArgument(input.exists(), "input does not exist : %s", input);
    Preconditions.checkNotNull(output, "output must not be null");
    Preconditions.checkArgument(!output.exists(), "output already exists : %s", output);

    try (BufferedReader reader = newFileReader(input)) {
      try (BufferedWriter writer = newCompressedFileWriterBZip2(output, false)) {

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

    Preconditions.checkNotNull(input, "input must not be null");
    Preconditions.checkArgument(input.exists(), "input does not exist : %s", input);
    Preconditions.checkNotNull(output, "output must not be null");
    Preconditions.checkArgument(!output.exists(), "output already exists : %s", output);

    try (BufferedReader reader = newCompressedFileReaderBZip2(input)) {
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

    Preconditions.checkNotNull(input, "input must not be null");
    Preconditions.checkArgument(input.exists(), "input does not exist : %s", input);
    Preconditions.checkNotNull(output, "output must not be null");
    Preconditions.checkArgument(!output.exists(), "output already exists : %s", output);

    try (BufferedReader reader = newFileReader(input)) {
      try (BufferedWriter writer = newCompressedFileWriter(output, false)) {

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

    Preconditions.checkNotNull(input, "input must not be null");
    Preconditions.checkArgument(input.exists(), "input does not exist : %s", input);
    Preconditions.checkNotNull(output, "output must not be null");
    Preconditions.checkArgument(!output.exists(), "output already exists : %s", output);

    try (BufferedReader reader = newCompressedFileReader(input)) {
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

  @CanIgnoreReturnValue
  public static boolean move(Path source, Path destination) {

    Preconditions.checkNotNull(source, "source should not be null");
    Preconditions.checkNotNull(destination, "destination should not be null");

    if (!source.toFile().exists()) {
      return false;
    }
    if (destination.toFile().exists()) {
      return false;
    }
    try {
      Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
      return true;
    } catch (IOException e) {
      logger_.error(LogFormatter.create().message(e).formatError());
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
}