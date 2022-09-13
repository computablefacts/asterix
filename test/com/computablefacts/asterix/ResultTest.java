package com.computablefacts.asterix;

import com.computablefacts.asterix.Result.Empty;
import com.computablefacts.asterix.Result.Failure;
import com.computablefacts.asterix.Result.Success;
import java.util.NoSuchElementException;
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

    Assert.assertFalse(Result.of(null).successValue().isPresent());
    Assert.assertTrue(Result.of(null).errorValue().isPresent());

    Assert.assertFalse(Result.of(null, "null value") instanceof Empty);
    Assert.assertFalse(Result.of(null, "null value") instanceof Success);
    Assert.assertTrue(Result.of(null, "null value") instanceof Failure);

    Assert.assertFalse(Result.of(null, "null value").successValue().isPresent());
    Assert.assertTrue(Result.of(null, "null value").errorValue().isPresent());

    Assert.assertFalse(Result.of(null, new RuntimeException("null value")) instanceof Empty);
    Assert.assertFalse(Result.of(null, new RuntimeException("null value")) instanceof Success);
    Assert.assertTrue(Result.of(null, new RuntimeException("null value")) instanceof Failure);

    Assert.assertFalse(Result.of(null, new RuntimeException("null value")).successValue().isPresent());
    Assert.assertTrue(Result.of(null, new RuntimeException("null value")).errorValue().isPresent());

    Assert.assertFalse(Result.of("Hello world!") instanceof Empty);
    Assert.assertTrue(Result.of("Hello world!") instanceof Success);
    Assert.assertFalse(Result.of("Hello world!") instanceof Failure);

    Assert.assertTrue(Result.of("Hello world!").successValue().isPresent());
    Assert.assertFalse(Result.of("Hello world!").errorValue().isPresent());
  }

  @Test
  public void testEmpty() {

    Result<?> result = Result.empty();

    Assert.assertFalse(result.isFailure());
    Assert.assertFalse(result.isSuccess());
    Assert.assertTrue(result.isEmpty());

    Assert.assertFalse(result.errorValue().isPresent());
    Assert.assertFalse(result.successValue().isPresent());
  }

  @Test(expected = NoSuchElementException.class)
  public void testSuccessValueOnEmptyThrowsAnException() {

    Result<?> result = Result.empty();
    Assert.assertEquals("Hello world!", result.successValue().get());
  }

  @Test(expected = NoSuchElementException.class)
  public void testErrorValueOnEmptyThrowsAnException() {

    Result<?> result = Result.empty();
    Assert.assertEquals("Hello world!", result.errorValue().get());
  }

  @Test
  public void testSuccess() {

    Result<?> result = Result.success("Hello world!");

    Assert.assertFalse(result.isFailure());
    Assert.assertTrue(result.isSuccess());
    Assert.assertFalse(result.isEmpty());

    Assert.assertFalse(result.errorValue().isPresent());
    Assert.assertTrue(result.successValue().isPresent());
  }

  @Test
  public void testSuccessValueOnSuccessDoesNotThrowAnException() {

    Result<?> result = Result.success("Hello world!");
    Assert.assertEquals("Hello world!", result.successValue().get());
  }

  @Test(expected = NoSuchElementException.class)
  public void testErrorValueOnSuccessThrowsAnException() {

    Result<?> result = Result.success("Hello world!");
    Assert.assertEquals("Hello world!", result.errorValue().get());
  }

  @Test
  public void testFailure() {

    Result<?> result1 = Result.failure("null value");

    Assert.assertTrue(result1.isFailure());
    Assert.assertFalse(result1.isSuccess());
    Assert.assertFalse(result1.isEmpty());

    Assert.assertTrue(result1.errorValue().isPresent());
    Assert.assertFalse(result1.successValue().isPresent());

    Result<?> result2 = Result.failure(new RuntimeException("null value"));

    Assert.assertTrue(result2.isFailure());
    Assert.assertFalse(result2.isSuccess());
    Assert.assertFalse(result2.isEmpty());

    Assert.assertTrue(result2.errorValue().isPresent());
    Assert.assertFalse(result2.successValue().isPresent());
  }

  @Test(expected = NoSuchElementException.class)
  public void testErrorValueOnFailureThrowsAnException() {

    Result<?> result = Result.failure("null value");
    Assert.assertEquals("null value", result.successValue().get());
  }

  @Test
  public void testErrorValueOnFailureDoesNotThrowAnException() {

    Result<?> result1 = Result.failure("null value");
    Assert.assertEquals("null value", result1.errorValue().get());

    Result<?> result2 = Result.failure(new RuntimeException("null value"));
    Assert.assertTrue(result2.errorValue().get().startsWith("java.lang.RuntimeException: null value\n"));
  }

  @Test
  public void testFilter() {

    Result<String> result1 = Result.success("Hello world!").filter(str -> str.length() == 12);

    Assert.assertTrue(result1 instanceof Success);
    Assert.assertTrue(result1.successValue().isPresent());
    Assert.assertFalse(result1.errorValue().isPresent());
    Assert.assertEquals("Hello world!", result1.successValue().get());
    Assert.assertEquals("", result1.errorValue().orElse(""));

    Result<String> result2 = Result.success("Hello world!").filter(str -> str.length() != 12);

    Assert.assertTrue(result2 instanceof Empty);
    Assert.assertFalse(result2.successValue().isPresent());
    Assert.assertFalse(result2.errorValue().isPresent());
    Assert.assertEquals("", result2.successValue().orElse(""));
    Assert.assertEquals("", result2.errorValue().orElse(""));

    Result<String> result3 = Result.<String>failure("null value").filter(str -> str.length() == 10);

    Assert.assertTrue(result3 instanceof Failure);
    Assert.assertFalse(result3.successValue().isPresent());
    Assert.assertTrue(result3.errorValue().isPresent());
    Assert.assertEquals("", result3.successValue().orElse(""));
    Assert.assertEquals("null value", result3.errorValue().get());

    Result<String> result4 = Result.<String>empty().filter(str -> str.length() == 0);

    Assert.assertTrue(result4 instanceof Empty);
    Assert.assertFalse(result4.successValue().isPresent());
    Assert.assertFalse(result4.errorValue().isPresent());
    Assert.assertEquals("", result4.successValue().orElse(""));
    Assert.assertEquals("", result4.errorValue().orElse(""));
  }

  @Test
  public void testMap() {

    Result<String> result1 = Result.success("Hello world!").map(String::toUpperCase);

    Assert.assertTrue(result1 instanceof Success);
    Assert.assertTrue(result1.successValue().isPresent());
    Assert.assertFalse(result1.errorValue().isPresent());
    Assert.assertEquals("HELLO WORLD!", result1.successValue().get());
    Assert.assertEquals("", result1.errorValue().orElse(""));

    Result<String> result2 = Result.<String>failure("null value").map(String::toUpperCase);

    Assert.assertTrue(result2 instanceof Failure);
    Assert.assertFalse(result2.successValue().isPresent());
    Assert.assertTrue(result2.errorValue().isPresent());
    Assert.assertEquals("", result2.successValue().orElse(""));
    Assert.assertEquals("null value", result2.errorValue().get());

    Result<String> result3 = Result.<String>empty().map(String::toUpperCase);

    Assert.assertTrue(result3 instanceof Empty);
    Assert.assertFalse(result3.successValue().isPresent());
    Assert.assertFalse(result3.errorValue().isPresent());
    Assert.assertEquals("", result3.successValue().orElse(""));
    Assert.assertEquals("", result3.errorValue().orElse(""));
  }
}
