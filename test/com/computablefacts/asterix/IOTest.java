package com.computablefacts.asterix;

import static com.computablefacts.asterix.IO.eCompressionAlgorithm.BZIP2;
import static com.computablefacts.asterix.IO.eCompressionAlgorithm.GZIP;

import com.google.common.base.Splitter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class IOTest {

  @Test
  public void testAppendTextThenReadText() {

    File file = IO.newTmpFile(".txt").getOrThrow().toFile();
    String text = "a\nb\nc\nd";

    Assert.assertTrue(file.exists());
    Assert.assertTrue(IO.writeText(file, text, true));
    Assert.assertEquals(text, IO.readText(file));
    Assert.assertTrue(file.delete());
  }

  @Test
  public void testAppendLinesThenReadLines() {

    File file = IO.newTmpFile(".txt").getOrThrow().toFile();
    String text = "a\nb\nc\nd";
    List<String> lines = Splitter.on('\n').splitToList(text);

    Assert.assertTrue(file.exists());
    Assert.assertTrue(IO.writeLines(file, lines, true));
    Assert.assertEquals(lines, IO.readLines(file));
    Assert.assertTrue(file.delete());
  }

  @Test
  public void testAppendTextThenReadLines() {

    File file = IO.newTmpFile(".txt").getOrThrow().toFile();
    String text = "a\nb\nc\nd";
    List<String> lines = Splitter.on('\n').splitToList(text);

    Assert.assertTrue(file.exists());
    Assert.assertTrue(IO.writeText(file, text, true));
    Assert.assertEquals(lines, IO.readLines(file));
    Assert.assertTrue(file.delete());
  }

  @Test
  public void testAppendLinesThenReadText() {

    File file = IO.newTmpFile(".txt").getOrThrow().toFile();
    String text = "a\nb\nc\nd";
    List<String> lines = Splitter.on('\n').splitToList(text);

    Assert.assertTrue(file.exists());
    Assert.assertTrue(IO.writeLines(file, lines, true));
    Assert.assertEquals(text, IO.readText(file));
    Assert.assertTrue(file.delete());
  }

  @Test
  public void testCompressFileUsingBZip2ThenReadCompressedFileUsingBZip2() throws IOException {

    File input = IO.newTmpFile(".txt").getOrThrow().toFile();
    String text = "a\nb\nc\nd";

    Assert.assertTrue(input.exists());
    Assert.assertTrue(IO.writeText(input, text, true));

    File output = new File(input.getAbsolutePath() + ".bz2");

    Assert.assertFalse(output.exists());
    Assert.assertTrue(IO.bzip2(input, output));
    Assert.assertTrue(input.exists());

    try (IO.LineIterator iterator = IO.newLineIterator(output, BZIP2)) {

      List<String> lines = new ArrayList<>();
      iterator.forEachRemaining(lines::add);

      Assert.assertEquals(text, String.join("\n", lines));
    }

    Assert.assertTrue(input.delete());
    Assert.assertTrue(output.delete());
  }

  @Test
  public void testCompressFileThenReadCompressedFile() throws IOException {

    File input = IO.newTmpFile(".txt").getOrThrow().toFile();
    String text = "a\nb\nc\nd";

    Assert.assertTrue(input.exists());
    Assert.assertTrue(IO.writeText(input, text, true));

    File output = new File(input.getAbsolutePath() + ".bz2");

    Assert.assertFalse(output.exists());
    Assert.assertTrue(IO.gzip(input, output));
    Assert.assertTrue(input.exists());

    try (IO.LineIterator iterator = IO.newLineIterator(output, GZIP)) {

      List<String> lines = new ArrayList<>();
      iterator.forEachRemaining(lines::add);

      Assert.assertEquals(text, String.join("\n", lines));
    }

    Assert.assertTrue(input.delete());
    Assert.assertTrue(output.delete());
  }

  @Test
  public void testWriteCompressedFileUsingBZip2ThenReadDecompressedFileUsingBZip2() throws IOException {

    File input = IO.newTmpFile(".txt.bz2").getOrThrow().toFile();
    String text = "a\nb\nc\nd";

    Assert.assertTrue(input.exists());
    Assert.assertTrue(IO.writeText(input, text, true, BZIP2));

    File output = new File(input.getAbsolutePath().substring(0, input.getAbsolutePath().lastIndexOf('.')));

    Assert.assertFalse(output.exists());
    Assert.assertTrue(IO.bunzip2(input, output));
    Assert.assertTrue(output.exists());

    try (IO.LineIterator iterator = IO.newLineIterator(output)) {

      List<String> lines = new ArrayList<>();
      iterator.forEachRemaining(lines::add);

      Assert.assertEquals(text, String.join("\n", lines));
    }

    Assert.assertTrue(input.delete());
    Assert.assertTrue(output.delete());
  }

  @Test
  public void testWriteCompressedFileThenReadDecompressedFile() throws IOException {

    File input = IO.newTmpFile(".txt.gz").getOrThrow().toFile();
    String text = "a\nb\nc\nd";

    Assert.assertTrue(input.exists());
    Assert.assertTrue(IO.writeText(input, text, true, GZIP));

    File output = new File(input.getAbsolutePath().substring(0, input.getAbsolutePath().lastIndexOf('.')));

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

  @Test
  public void testReplaceFile() {

    String path = IO.newTmpDirectory().getOrThrow().toString();
    File src = new File(path + File.separator + "src.txt");
    File dest = new File(path + File.separator + "dest.txt");

    Assert.assertFalse(src.exists());
    Assert.assertFalse(dest.exists());

    Assert.assertTrue(IO.writeText(src, "Dummy text in first file!", false));
    Assert.assertTrue(IO.writeText(dest, "Dummy text in second file!", false));

    Assert.assertTrue(src.exists());
    Assert.assertTrue(dest.exists());

    Assert.assertTrue(IO.replace(src.toPath(), dest.toPath()));

    Assert.assertFalse(src.exists());
    Assert.assertTrue(dest.exists());
  }

  @Test
  public void testReplaceFileFailsOnMissingSourceFile() {

    String path = IO.newTmpDirectory().getOrThrow().toString();
    File src = new File(path + File.separator + "src.txt");
    File dest = new File(path + File.separator + "dest.txt");

    Assert.assertFalse(src.exists());
    Assert.assertFalse(dest.exists());

    Assert.assertTrue(IO.writeText(dest, "Dummy text in dest file!", false));

    Assert.assertFalse(src.exists());
    Assert.assertTrue(dest.exists());

    Assert.assertFalse(IO.replace(src, dest));

    Assert.assertFalse(src.exists());
    Assert.assertTrue(dest.exists());
  }

  @Test
  public void testMoveFile() {

    String path = IO.newTmpDirectory().getOrThrow().toString();
    File src = new File(path + File.separator + "src.txt");
    File dest = new File(path + File.separator + "dest.txt");

    Assert.assertFalse(src.exists());
    Assert.assertFalse(dest.exists());

    Assert.assertTrue(IO.writeText(src, "Dummy text!", false));

    Assert.assertTrue(src.exists());
    Assert.assertFalse(dest.exists());

    Assert.assertTrue(IO.move(src.toPath(), dest.toPath()));

    Assert.assertFalse(src.exists());
    Assert.assertTrue(dest.exists());
  }

  @Test
  public void testMoveFileFailsOnMissingSourceFile() {

    String path = IO.newTmpDirectory().getOrThrow().toString();
    File src = new File(path + File.separator + "src.txt");
    File dest = new File(path + File.separator + "dest.txt");

    Assert.assertFalse(src.exists());
    Assert.assertFalse(dest.exists());

    Assert.assertFalse(IO.move(src, dest));

    Assert.assertFalse(src.exists());
    Assert.assertFalse(dest.exists());
  }

  @Test
  public void testMoveFileFailsOnOverwrite() {

    String path = IO.newTmpDirectory().getOrThrow().toString();
    File src = new File(path + File.separator + "src.txt");
    File dest = new File(path + File.separator + "dest.txt");

    Assert.assertFalse(src.exists());
    Assert.assertFalse(dest.exists());

    Assert.assertTrue(IO.writeText(src, "Dummy text in src file!", false));
    Assert.assertTrue(IO.writeText(dest, "Dummy text in dest file!", false));

    Assert.assertTrue(src.exists());
    Assert.assertTrue(dest.exists());

    Assert.assertFalse(IO.move(src.toPath(), dest.toPath()));

    Assert.assertTrue(src.exists());
    Assert.assertTrue(dest.exists());
  }

  @Test
  public void testCopyFile() {

    String path = IO.newTmpDirectory().getOrThrow().toString();
    File src = new File(path + File.separator + "src.txt");
    File dest = new File(path + File.separator + "dest.txt");

    Assert.assertFalse(src.exists());
    Assert.assertFalse(dest.exists());

    Assert.assertTrue(IO.writeText(src, "Dummy text in first file!", false));

    Assert.assertTrue(src.exists());
    Assert.assertFalse(dest.exists());

    Assert.assertTrue(IO.copy(src.toPath(), dest.toPath()));

    Assert.assertTrue(src.exists());
    Assert.assertTrue(dest.exists());
  }

  @Test
  public void testCopyFileFailsOnMissingSourceFile() {

    String path = IO.newTmpDirectory().getOrThrow().toString();
    File src = new File(path + File.separator + "src.txt");
    File dest = new File(path + File.separator + "dest.txt");

    Assert.assertFalse(src.exists());
    Assert.assertFalse(dest.exists());

    Assert.assertFalse(IO.copy(src, dest));

    Assert.assertFalse(src.exists());
    Assert.assertFalse(dest.exists());
  }

  @Test
  public void testCopyFileFailsOnOverwrite() {

    String path = IO.newTmpDirectory().getOrThrow().toString();
    File src = new File(path + File.separator + "src.txt");
    File dest = new File(path + File.separator + "dest.txt");

    Assert.assertFalse(src.exists());
    Assert.assertFalse(dest.exists());

    Assert.assertTrue(IO.writeText(src, "Dummy text in src file!", false));
    Assert.assertTrue(IO.writeText(dest, "Dummy text in dest file!", false));

    Assert.assertTrue(src.exists());
    Assert.assertTrue(dest.exists());

    Assert.assertFalse(IO.copy(src.toPath(), dest.toPath()));

    Assert.assertTrue(src.exists());
    Assert.assertTrue(dest.exists());
  }

  @Test
  public void testDeleteAllFiles() {

    String path = IO.newTmpDirectory().getOrThrow().toString();

    for (int i = 0; i < 10; i++) {
      File file = new File(path + File.separator + i + ".txt");
      Assert.assertTrue(IO.writeText(file, "Dummy text!", false));
    }

    Assert.assertEquals(10, new File(path).listFiles().length);
    Assert.assertTrue(IO.delete(new File(path)));
    Assert.assertFalse(new File(path).exists());
  }

  @Test
  public void testDeleteFileFailsOnMissingFile() {

    String path = IO.newTmpDirectory().getOrThrow().toString();
    File file = new File(path + File.separator + "missing.txt");

    Assert.assertFalse(file.exists());
    Assert.assertFalse(IO.delete(file));
    Assert.assertFalse(file.exists());
  }

  @Test
  public void testEnsureFileExists() {

    String path = IO.newTmpDirectory().getOrThrow().toString();
    File file = new File(path + File.separator + "missing.txt");

    Assert.assertFalse(file.exists());
    Assert.assertTrue(IO.ensureFileExists(file));
    Assert.assertTrue(file.exists());
    Assert.assertTrue(IO.ensureFileExists(file));
  }

  @Test
  public void testEnsureDirectoryExists() {

    String path = IO.newTmpDirectory().getOrThrow().toString();
    File file = new File(path + File.separator + "missing");

    Assert.assertFalse(file.exists());
    Assert.assertTrue(IO.ensureDirectoryExists(file));
    Assert.assertTrue(file.exists());
  }
}