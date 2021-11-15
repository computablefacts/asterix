package com.computablefacts.asterix;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Splitter;

public class IOTest {

  @Test
  public void testAppendTextThenReadText() throws IOException {

    File file = java.nio.file.Files.createTempFile("test-", ".txt").toFile();
    String text = "a\nb\nc\nd";

    Assert.assertTrue(file.exists());
    Assert.assertTrue(IO.writeText(file, text, true));
    Assert.assertEquals(text, IO.readText(file));
    Assert.assertTrue(file.delete());
  }

  @Test
  public void testAppendLinesThenReadLines() throws IOException {

    File file = java.nio.file.Files.createTempFile("test-", ".txt").toFile();
    String text = "a\nb\nc\nd";
    List<String> lines = Splitter.on('\n').splitToList(text);

    Assert.assertTrue(file.exists());
    Assert.assertTrue(IO.writeLines(file, lines, true));
    Assert.assertEquals(lines, IO.readLines(file));
    Assert.assertTrue(file.delete());
  }

  @Test
  public void testAppendTextThenReadLines() throws IOException {

    File file = java.nio.file.Files.createTempFile("test-", ".txt").toFile();
    String text = "a\nb\nc\nd";
    List<String> lines = Splitter.on('\n').splitToList(text);

    Assert.assertTrue(file.exists());
    Assert.assertTrue(IO.writeText(file, text, true));
    Assert.assertEquals(lines, IO.readLines(file));
    Assert.assertTrue(file.delete());
  }

  @Test
  public void testAppendLinesThenReadText() throws IOException {

    File file = java.nio.file.Files.createTempFile("test-", ".txt").toFile();
    String text = "a\nb\nc\nd";
    List<String> lines = Splitter.on('\n').splitToList(text);

    Assert.assertTrue(file.exists());
    Assert.assertTrue(IO.writeLines(file, lines, true));
    Assert.assertEquals(text, IO.readText(file));
    Assert.assertTrue(file.delete());
  }

  @Test
  public void testCompressFileThenReadCompressedFile() throws IOException {

    File input = java.nio.file.Files.createTempFile("test-", ".txt").toFile();
    String text = "a\nb\nc\nd";

    Assert.assertTrue(input.exists());
    Assert.assertTrue(IO.writeText(input, text, true));

    File output = new File(input.getAbsolutePath() + ".gz");

    Assert.assertFalse(output.exists());
    Assert.assertTrue(IO.gzip(input, output));
    Assert.assertTrue(input.exists());

    try (IO.LineIterator iterator = IO.newCompressedLineIterator(output)) {

      List<String> lines = new ArrayList<>();
      iterator.forEachRemaining(lines::add);

      Assert.assertEquals(text, String.join("\n", lines));
    }

    Assert.assertTrue(input.delete());
    Assert.assertTrue(output.delete());
  }

  @Test
  public void testWriteCompressedFileThenReadDecompressedFile() throws IOException {

    File input = java.nio.file.Files.createTempFile("test-", ".txt.gz").toFile();
    String text = "a\nb\nc\nd";

    Assert.assertTrue(input.exists());
    Assert.assertTrue(IO.writeCompressedText(input, text, true));

    File output =
        new File(input.getAbsolutePath().substring(0, input.getAbsolutePath().lastIndexOf('.')));

    Assert.assertFalse(output.exists());
    Assert.assertTrue(IO.gunzip(input, output));
    Assert.assertTrue(output.exists());

    try (IO.LineIterator iterator = IO.newLineIterator(output)) {

      List<String> lines = new ArrayList<>();
      iterator.forEachRemaining(lines::add);

      Assert.assertEquals(text, String.join("\n", lines));
    }

    Assert.assertTrue(input.delete());
    Assert.assertTrue(output.delete());
  }
}
