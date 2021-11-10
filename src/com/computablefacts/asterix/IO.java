package com.computablefacts.asterix;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;

@CheckReturnValue
final public class IO {

  private IO() {}

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
      // FALL THROUGH
    }
    return false;
  }

  public static List<String> readLines(File file) {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file does not exist : %s", file);

    try {
      return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      // FALL THROUGH
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
      // FALL THROUGH
    }
    return false;
  }

  public static LineIterator newLineIterator(File file) throws IOException {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file does not exist : %s", file);

    return new LineIterator(newFileReader(file));
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
      return Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8,
          StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW);
    }
    return Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8, StandardOpenOption.WRITE,
        StandardOpenOption.CREATE, StandardOpenOption.APPEND);
  }

  public static BufferedReader newCompressedFileReader(File file) throws IOException {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(file.exists(), "file does not exist : %s", file);

    return new BufferedReader(new InputStreamReader(
        new GZIPInputStream(Files.newInputStream(file.toPath(), StandardOpenOption.READ)),
        StandardCharsets.UTF_8));
  }

  public static BufferedWriter newCompressedFileWriter(File file, boolean append)
      throws IOException {

    Preconditions.checkNotNull(file, "file should not be null");
    Preconditions.checkArgument(!append || file.exists(), "file does not exist : %s", file);

    if (!append) {
      return new BufferedWriter(
          new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(file.toPath(),
              StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW)), StandardCharsets.UTF_8));
    }
    return new BufferedWriter(new OutputStreamWriter(
        new GZIPOutputStream(Files.newOutputStream(file.toPath(), StandardOpenOption.WRITE,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND)),
        StandardCharsets.UTF_8));
  }

  public static boolean gzip(File input, File output) {

    Preconditions.checkNotNull(input, "input must not be null");
    Preconditions.checkArgument(input.exists(), "input does not exist : %s", input);
    Preconditions.checkNotNull(output, "output must not be null");
    Preconditions.checkArgument(!output.exists(), "output already exists : %s", output);

    try (BufferedReader reader = newFileReader(input)) {
      try (BufferedWriter writer = newCompressedFileWriter(output, false)) {

        char[] buffer = new char[4096];
        @Var
        int len;

        while ((len = reader.read(buffer)) != -1) {
          writer.write(buffer, 0, len);
        }
        return true;
      }
    } catch (IOException e) {
      // FALL THROUGH
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
        @Var
        int len;

        while ((len = reader.read(buffer)) != -1) {
          writer.write(buffer, 0, len);
        }
        return true;
      }
    } catch (IOException e) {
      // FALL THROUGH
    }
    return false;
  }

  final public static class LineIterator extends AbstractIterator<String> implements AutoCloseable {

    private BufferedReader reader_;

    public LineIterator(BufferedReader reader) {
      reader_ = Preconditions.checkNotNull(reader, "reader should not be null");
    }

    public void close() {
      if (reader_ != null) {
        try {
          reader_.close();
        } catch (IOException e) {
          // FALL THROUGH
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
        // FALL THROUGH
      }
      close();
      return endOfData();
    }
  }
}
