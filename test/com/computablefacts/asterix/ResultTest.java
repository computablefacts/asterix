package com.computablefacts.asterix;

import com.computablefacts.asterix.Result.Empty;
import com.computablefacts.asterix.Result.Failure;
import com.computablefacts.asterix.Result.Success;
import com.google.common.collect.Lists;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Assert;
import org.junit.Test;

public class ResultTest {

  @Test
  public void testEmptyEqualsAndHashCode() {
    EqualsVerifier.forClass(Empty.class).verify();
  }

  @Test
  public void testSuccessEqualsAndHashCode() {
    EqualsVerifier.forClass(Success.class).verify();
  }

  @Test
  public void testFailureEqualsAndHashCode() {
    EqualsVerifier.forClass(Failure.class).verify();
  }

  @Test
  public void testOf() {

    Assert.assertFalse(Result.of(null) instanceof Empty);
    Assert.assertFalse(Result.of(null) instanceof Success);
    Assert.assertTrue(Result.of(null) instanceof Failure);

    Assert.assertFalse(Result.of(null).isEmpty());
    Assert.assertFalse(Result.of(null).isSuccess());
    Assert.assertTrue(Result.of(null).isFailure());

    Assert.assertFalse(Result.of(null, "null value") instanceof Empty);
    Assert.assertFalse(Result.of(null, "null value") instanceof Success);
    Assert.assertTrue(Result.of(null, "null value") instanceof Failure);

    Assert.assertFalse(Result.of(null, "null value").isEmpty());
    Assert.assertFalse(Result.of(null, "null value").isSuccess());
    Assert.assertTrue(Result.of(null, "null value").isFailure());

    Assert.assertFalse(Result.of(null, new RuntimeException("null value")) instanceof Empty);
    Assert.assertFalse(Result.of(null, new RuntimeException("null value")) instanceof Success);
    Assert.assertTrue(Result.of(null, new RuntimeException("null value")) instanceof Failure);

    Assert.assertFalse(Result.of(null, new RuntimeException("null value")).isEmpty());
    Assert.assertFalse(Result.of(null, new RuntimeException("null value")).isSuccess());
    Assert.assertTrue(Result.of(null, new RuntimeException("null value")).isFailure());

    Assert.assertFalse(Result.of("Hello world!") instanceof Empty);
    Assert.assertTrue(Result.of("Hello world!") instanceof Success);
    Assert.assertFalse(Result.of("Hello world!") instanceof Failure);

    Assert.assertFalse(Result.of("Hello world!").isEmpty());
    Assert.assertTrue(Result.of("Hello world!").isSuccess());
    Assert.assertFalse(Result.of("Hello world!").isFailure());
  }

  @Test
  public void testEmpty() {

    Result<?> result = Result.empty();

    Assert.assertFalse(result.isFailure());
    Assert.assertFalse(result.isSuccess());
    Assert.assertTrue(result.isEmpty());
  }

  @Test(expected = RuntimeException.class)
  public void testSuccessValueOnEmptyThrowsAnException() {

    Result<?> result = Result.empty();
    Assert.assertEquals("Hello world!", result.successValue());
  }

  @Test(expected = RuntimeException.class)
  public void testErrorValueOnEmptyThrowsAnException() {

    Result<?> result = Result.empty();
    Assert.assertEquals("Hello world!", result.errorValue());
  }

  @Test
  public void testSuccess() {

    Result<?> result = Result.success("Hello world!");

    Assert.assertFalse(result.isFailure());
    Assert.assertTrue(result.isSuccess());
    Assert.assertFalse(result.isEmpty());
  }

  @Test
  public void testSuccessValueOnSuccessDoesNotThrowAnException() {

    Result<?> result = Result.success("Hello world!");
    Assert.assertEquals("Hello world!", result.successValue());
  }

  @Test(expected = RuntimeException.class)
  public void testErrorValueOnSuccessThrowsAnException() {

    Result<?> result = Result.success("Hello world!");
    Assert.assertEquals("Hello world!", result.errorValue());
  }

  @Test
  public void testFailure() {

    Result<?> result1 = Result.failure("null value");

    Assert.assertTrue(result1.isFailure());
    Assert.assertFalse(result1.isSuccess());
    Assert.assertFalse(result1.isEmpty());

    Result<?> result2 = Result.failure(new RuntimeException("null value"));

    Assert.assertTrue(result2.isFailure());
    Assert.assertFalse(result2.isSuccess());
    Assert.assertFalse(result2.isEmpty());
  }

  @Test(expected = RuntimeException.class)
  public void testErrorValueOnFailureThrowsAnException() {

    Result<?> result = Result.failure("null value");
    Assert.assertEquals("null value", result.successValue());
  }

  @Test
  public void testErrorValueOnFailureDoesNotThrowAnException() {

    Result<?> result1 = Result.failure("null value");
    Assert.assertEquals("null value", result1.errorValue());

    Result<?> result2 = Result.failure(new RuntimeException("null value"));
    Assert.assertTrue(result2.errorValue().startsWith("java.lang.RuntimeException: null value\n"));
  }

  @Test
  public void testFilter() {

    Result<String> result1 = Result.success("Hello world!").filter(str -> str.length() == 12);

    Assert.assertTrue(result1 instanceof Success);
    Assert.assertFalse(result1.isEmpty());
    Assert.assertTrue(result1.isSuccess());
    Assert.assertFalse(result1.isFailure());
    Assert.assertEquals("Hello world!", result1.successValue());
    Assert.assertEquals("Hello world!", result1.get(""));
    Assert.assertEquals("Hello world!", result1.get(() -> ""));

    Result<String> result2 = Result.success("Hello world!").filter(str -> str.length() != 12);

    Assert.assertTrue(result2 instanceof Empty);
    Assert.assertTrue(result2.isEmpty());
    Assert.assertFalse(result2.isSuccess());
    Assert.assertFalse(result2.isFailure());
    Assert.assertEquals("", result2.get(""));
    Assert.assertEquals("", result2.get(() -> ""));

    Result<String> result3 = Result.<String>failure("null value").filter(str -> str.length() == 10);

    Assert.assertTrue(result3 instanceof Failure);
    Assert.assertFalse(result3.isEmpty());
    Assert.assertFalse(result3.isSuccess());
    Assert.assertTrue(result3.isFailure());
    Assert.assertEquals("", result3.get(""));
    Assert.assertEquals("", result3.get(() -> ""));
    Assert.assertEquals("null value", result3.errorValue());

    Result<String> result4 = Result.<String>empty().filter(str -> str.length() == 0);

    Assert.assertTrue(result4 instanceof Empty);
    Assert.assertTrue(result4.isEmpty());
    Assert.assertFalse(result4.isSuccess());
    Assert.assertFalse(result4.isFailure());
    Assert.assertEquals("", result4.get(""));
    Assert.assertEquals("", result4.get(() -> ""));
  }

  @Test
  public void testMapIfSuccess() {

    Result<String> result1 = Result.success("Hello world!").mapIfSuccess(String::toUpperCase);

    Assert.assertTrue(result1 instanceof Success);
    Assert.assertFalse(result1.isEmpty());
    Assert.assertTrue(result1.isSuccess());
    Assert.assertFalse(result1.isFailure());
    Assert.assertEquals("HELLO WORLD!", result1.successValue());
    Assert.assertEquals("HELLO WORLD!", result1.get(""));
    Assert.assertEquals("HELLO WORLD!", result1.get(() -> ""));

    Result<String> result2 = Result.<String>failure("null value").mapIfSuccess(String::toUpperCase);

    Assert.assertTrue(result2 instanceof Failure);
    Assert.assertFalse(result2.isEmpty());
    Assert.assertFalse(result2.isSuccess());
    Assert.assertTrue(result2.isFailure());
    Assert.assertEquals("", result2.get(""));
    Assert.assertEquals("", result2.get(() -> ""));
    Assert.assertEquals("null value", result2.errorValue());

    Result<String> result3 = Result.<String>empty().mapIfSuccess(String::toUpperCase);

    Assert.assertTrue(result3 instanceof Empty);
    Assert.assertTrue(result3.isEmpty());
    Assert.assertFalse(result3.isSuccess());
    Assert.assertFalse(result3.isFailure());
    Assert.assertEquals("", result3.get(""));
    Assert.assertEquals("", result3.get(() -> ""));
  }

  @Test
  public void testMapIfFailure() {

    Result<String> result1 = Result.success("Hello world!").mapIfFailure(String::toUpperCase);

    Assert.assertTrue(result1 instanceof Success);
    Assert.assertFalse(result1.isEmpty());
    Assert.assertTrue(result1.isSuccess());
    Assert.assertFalse(result1.isFailure());
    Assert.assertEquals("Hello world!", result1.successValue());
    Assert.assertEquals("Hello world!", result1.get(""));
    Assert.assertEquals("Hello world!", result1.get(() -> ""));

    Result<String> result2 = Result.<String>failure("null value").mapIfFailure(String::toUpperCase);

    Assert.assertTrue(result2 instanceof Success);
    Assert.assertFalse(result2.isEmpty());
    Assert.assertTrue(result2.isSuccess());
    Assert.assertFalse(result2.isFailure());
    Assert.assertEquals("NULL VALUE", result2.get(""));
    Assert.assertEquals("NULL VALUE", result2.get(() -> ""));
    Assert.assertEquals("NULL VALUE", result2.successValue());

    Result<String> result3 = Result.<String>empty().mapIfFailure(String::toUpperCase);

    Assert.assertTrue(result3 instanceof Empty);
    Assert.assertTrue(result3.isEmpty());
    Assert.assertFalse(result3.isSuccess());
    Assert.assertFalse(result3.isFailure());
    Assert.assertEquals("", result3.get(""));
    Assert.assertEquals("", result3.get(() -> ""));
  }

  @Test
  public void testMapIfEmpty() {

    Result<String> result1 = Result.success("Hello world!").mapIfEmpty(() -> "empty value");

    Assert.assertTrue(result1 instanceof Success);
    Assert.assertFalse(result1.isEmpty());
    Assert.assertTrue(result1.isSuccess());
    Assert.assertFalse(result1.isFailure());
    Assert.assertEquals("Hello world!", result1.successValue());
    Assert.assertEquals("Hello world!", result1.get(""));
    Assert.assertEquals("Hello world!", result1.get(() -> ""));

    Result<String> result2 = Result.<String>failure("null value").mapIfEmpty(() -> "empty value");

    Assert.assertTrue(result2 instanceof Failure);
    Assert.assertFalse(result2.isEmpty());
    Assert.assertFalse(result2.isSuccess());
    Assert.assertTrue(result2.isFailure());
    Assert.assertEquals("", result2.get(""));
    Assert.assertEquals("", result2.get(() -> ""));
    Assert.assertEquals("null value", result2.errorValue());

    Result<String> result3 = Result.<String>empty().mapIfEmpty(() -> "empty value");

    Assert.assertTrue(result3 instanceof Success);
    Assert.assertFalse(result3.isEmpty());
    Assert.assertTrue(result3.isSuccess());
    Assert.assertFalse(result3.isFailure());
    Assert.assertEquals("empty value", result3.get(""));
    Assert.assertEquals("empty value", result3.get(() -> ""));
  }

  @Test(expected = RuntimeException.class)
  public void testGetOrThrowThrowsAnExceptionOnEmptyResult() {

    Result<?> result = Result.empty();
    Assert.assertEquals("", result.getOrThrow());
  }

  @Test(expected = RuntimeException.class)
  public void testGetOrThrowThrowsAnExceptionOnOnFailureResult() {

    Result<?> result = Result.failure("null value");
    Assert.assertEquals("null value", result.getOrThrow());
  }

  @Test
  public void testGetOrThrow() {

    Result<?> result = Result.success("Hello world!");
    Assert.assertEquals("Hello world!", result.getOrThrow());
  }

  @Test
  public void testView() {

    View<String> result1 = Result.success("Hello world!").view();
    Assert.assertEquals(Lists.newArrayList("Hello world!"), result1.toList());

    View<String> result2 = Result.<String>failure("null value").view();
    Assert.assertEquals(Lists.newArrayList(), result2.toList());

    View<String> result3 = Result.<String>empty().view();
    Assert.assertEquals(Lists.newArrayList(), result3.toList());
  }
}
