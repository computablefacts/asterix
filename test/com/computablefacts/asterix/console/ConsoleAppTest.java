package com.computablefacts.asterix.console;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class ConsoleAppTest {

  @Test(expected = RuntimeException.class)
  public void testMissingArgumentValue() {

    String[] args = new String[] {"-cmd"};

    String value = ConsoleApp.getArg(args, "cmd");
  }

  @Test(expected = RuntimeException.class)
  public void testGetFileCommandWrongArgWithDefault() {

    String[] args = new String[] {"-cmd", "c:\\test.txt"};

    File f = ConsoleApp.getFileCommand(args, "nop", "");
  }

  @Test(expected = NullPointerException.class)
  public void testGetFileCommandWrongArgWithoutDefault() {

    String[] args = new String[] {"-cmd", "c:\\test.txt"};

    File f = ConsoleApp.getFileCommand(args, "nop", null);
  }

  @Test
  public void testGetFileCommand() throws IOException {

    File file = Files.createTempFile("tmp", "-1").toFile();

    String[] args = new String[] {"-cmd", file.getAbsolutePath()};

    Assert.assertEquals(file.getAbsolutePath(),
        ConsoleApp.getFileCommand(args, "cmd", "").getAbsolutePath());
    Assert.assertEquals(file.getAbsolutePath(),
        ConsoleApp.getFileCommand(args, "cmd", null).getAbsolutePath());
  }

  @Test
  public void testGetStringCommand() {

    String[] args = new String[] {"-cmd", "test"};

    Assert.assertEquals("test", ConsoleApp.getStringCommand(args, "cmd", ""));
    Assert.assertEquals("test", ConsoleApp.getStringCommand(args, "cmd", null));
    Assert.assertEquals("", ConsoleApp.getStringCommand(args, "nop", ""));
    Assert.assertNull(ConsoleApp.getStringCommand(args, "nop", null));
  }

  @Test
  public void testGetIntCommand() {

    String[] args = new String[] {"-cmd", "123"};

    Assert.assertEquals(Integer.valueOf(123), ConsoleApp.getIntCommand(args, "cmd", 0));
    Assert.assertEquals(Integer.valueOf(123), ConsoleApp.getIntCommand(args, "cmd", null));
    Assert.assertEquals(Integer.valueOf(0), ConsoleApp.getIntCommand(args, "nop", 0));
    Assert.assertNull(ConsoleApp.getIntCommand(args, "nop", null));
  }

  @Test
  public void testGetDoubleCommand() {

    String[] args = new String[] {"-cmd", "123.456"};

    Assert.assertEquals(Double.valueOf(123.456), ConsoleApp.getDoubleCommand(args, "cmd", 0.0));
    Assert.assertEquals(Double.valueOf(123.456), ConsoleApp.getDoubleCommand(args, "cmd", null));
    Assert.assertEquals(Double.valueOf(0.0), ConsoleApp.getDoubleCommand(args, "nop", 0.0));
    Assert.assertNull(ConsoleApp.getDoubleCommand(args, "nop", null));
  }

  @Test
  public void testGetBooleanCommand() {

    String[] args = new String[] {"-cmd1", "TRUE", "-cmd2", "false"};

    Assert.assertEquals(true, ConsoleApp.getBooleanCommand(args, "cmd1", false));
    Assert.assertEquals(true, ConsoleApp.getBooleanCommand(args, "cmd1", null));

    Assert.assertEquals(false, ConsoleApp.getBooleanCommand(args, "cmd2", false));
    Assert.assertEquals(false, ConsoleApp.getBooleanCommand(args, "cmd2", null));

    Assert.assertEquals(false, ConsoleApp.getBooleanCommand(args, "nop", false));
    Assert.assertNull(ConsoleApp.getBooleanCommand(args, "nop", null));
  }

  @Test(expected = RuntimeException.class)
  public void testGetAttributesCommandWithMissingValue() {

    String[] args = new String[] {"-cmd", "k1=v1&k2&k3=v3"};

    Map<String, String> attributes = ConsoleApp.getAttributesCommand(args, "cmd", "");
  }

  @Test(expected = RuntimeException.class)
  public void testGetAttributesCommandWithDuplicateKey() {

    String[] args = new String[] {"-cmd", "k1=v1&k2=v2&k1=v3"};

    Map<String, String> attributes = ConsoleApp.getAttributesCommand(args, "cmd", "");
  }

  @Test
  public void testGetAttributesCommand() {

    Map<String, String> attributes = new HashMap<>();
    attributes.put("k1", "v1");
    attributes.put("k2", "v2.1 v2.2");
    attributes.put("k3", "v3");

    String[] args = new String[] {"-cmd", "k1=v1&k2=v2.1 v2.2&k3=v3"};

    Assert.assertEquals(attributes, ConsoleApp.getAttributesCommand(args, "cmd", ""));
    Assert.assertEquals(attributes, ConsoleApp.getAttributesCommand(args, "cmd", null));
    Assert.assertEquals(new HashMap<>(), ConsoleApp.getAttributesCommand(args, "nop", ""));
    Assert.assertNull(ConsoleApp.getAttributesCommand(args, "nop", null));
  }

  @Test
  public void testGetArg() {

    String[] args = new String[] {"-cmd", "test"};

    Assert.assertEquals("test", ConsoleApp.getArg(args, "cmd"));
    Assert.assertNull(ConsoleApp.getArg(args, "nop"));
  }
}
