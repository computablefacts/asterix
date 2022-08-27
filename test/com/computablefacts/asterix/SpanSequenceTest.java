package com.computablefacts.asterix;

import com.google.common.collect.Lists;
import java.util.Iterator;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Assert;
import org.junit.Test;

public class SpanSequenceTest {

  @Test
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(SpanSequence.class).verify();
  }

  @Test(expected = NullPointerException.class)
  public void testAddNullSpan() {
    SpanSequence sequence = new SpanSequence();
    sequence.add((Span) null);
  }

  @Test(expected = NullPointerException.class)
  public void testAddNullSpanSequence() {
    SpanSequence sequence = new SpanSequence();
    sequence.add((SpanSequence) null);
  }

  @Test
  public void testEqualsWithNull() {

    SpanSequence sequence = new SpanSequence();

    Assert.assertFalse(sequence.equals(null));
  }

  @Test
  public void testEqualsWithWrongObjectType() {

    SpanSequence sequence = new SpanSequence();

    Assert.assertFalse(sequence.equals("string"));
  }

  @Test
  public void testEquals() {

    SpanSequence sequence1 = new SpanSequence();
    SpanSequence sequence2 = new SpanSequence();
    SpanSequence sequence3 = new SpanSequence();

    for (int i = 0; i < 100; i++) {

      Span span1 = new Span("span-" + Integer.toString(i, 10));
      Span span2 = new Span("span-" + Integer.toString(i, 10));
      Span span3 = new Span("span_" + Integer.toString(i, 10));

      sequence1.add(span1);
      sequence2.add(span2);
      sequence3.add(span3);
    }

    Assert.assertEquals(sequence1, sequence1);
    Assert.assertEquals(sequence2, sequence2);
    Assert.assertEquals(sequence3, sequence3);

    Assert.assertEquals(sequence1, sequence2);

    Assert.assertNotEquals(sequence3, sequence1);
    Assert.assertNotEquals(sequence3, sequence2);
  }

  @Test
  public void testHashcode() {

    SpanSequence sequence1 = new SpanSequence();
    SpanSequence sequence2 = new SpanSequence();
    SpanSequence sequence3 = new SpanSequence();

    for (int i = 0; i < 100; i++) {

      Span span1 = new Span("span-" + Integer.toString(i, 10));
      Span span2 = new Span("span-" + Integer.toString(i, 10));
      Span span3 = new Span("span_" + Integer.toString(i, 10));

      sequence1.add(span1);
      sequence2.add(span2);
      sequence3.add(span3);
    }

    Assert.assertEquals(sequence1.hashCode(), sequence1.hashCode());
    Assert.assertEquals(sequence2.hashCode(), sequence2.hashCode());
    Assert.assertEquals(sequence3.hashCode(), sequence3.hashCode());

    Assert.assertEquals(sequence1.hashCode(), sequence2.hashCode());

    Assert.assertNotEquals(sequence3.hashCode(), sequence1.hashCode());
    Assert.assertNotEquals(sequence3.hashCode(), sequence2.hashCode());
  }

  @Test
  public void testCompareToSequenceWithSameLength() {

    SpanSequence sequence1 = new SpanSequence();
    SpanSequence sequence2 = new SpanSequence();

    for (int i = 0; i < 100; i++) {

      Span span1 = new Span("span-" + Integer.toString(i, 10));
      Span span2 = new Span("span-" + Integer.toString(i, 10));

      sequence1.add(span1);
      sequence2.add(span2);
    }

    Assert.assertEquals(0, sequence1.compareTo(sequence2));

    sequence2.sort(Span::compareTo);

    Assert.assertEquals(-1, sequence2.compareTo(sequence1));
    Assert.assertEquals(1, sequence1.compareTo(sequence2));
  }

  @Test
  public void testCompareToSequenceWithDifferentLength() {

    SpanSequence sequence1 = new SpanSequence();
    SpanSequence sequence2 = new SpanSequence();

    for (int i = 0; i < 50; i++) {
      sequence1.add(new Span("span-" + Integer.toString(i, 10)));
    }
    for (int i = 0; i < 100; i++) {
      sequence2.add(new Span("span-" + Integer.toString(i, 10)));
    }

    Assert.assertEquals(1, sequence2.compareTo(sequence1));
    Assert.assertEquals(0, sequence1.compareTo(sequence2));
  }

  @Test
  public void testExtractSpans() {

    String text = "GUATEMALA CITY, 4 FEB 90 (ACAN-EFE)";

    Span day = new Span(text, 16, 17);
    Span month = new Span(text, 18, 21);
    Span year = new Span(text, 22, 24);

    SpanSequence sequence = new SpanSequence(Lists.newArrayList(day, month, year));

    Assert.assertEquals(3, sequence.size());
    Assert.assertEquals(day, sequence.span(0));
    Assert.assertEquals(month, sequence.span(1));
    Assert.assertEquals(year, sequence.span(2));

    Assert.assertEquals(new SpanSequence(Lists.newArrayList(day, month)), sequence.sequence(0, 2));
    Assert.assertEquals(new SpanSequence(Lists.newArrayList(month, year)), sequence.sequence(1, 3));
    Assert.assertEquals(new SpanSequence(Lists.newArrayList(day, month, year)), sequence.sequence(0, 3));
    Assert.assertEquals(new SpanSequence(Lists.newArrayList(month)), sequence.sequence(1, 2));
  }

  @Test
  public void testAddRemoveSpan() {

    String text = "GUATEMALA CITY, 4 FEB 90 (ACAN-EFE)";

    Span day = new Span(text, 16, 17);
    Span month = new Span(text, 18, 21);
    Span year = new Span(text, 22, 24);

    SpanSequence sequence = new SpanSequence();
    sequence.add(new SpanSequence(Lists.newArrayList(day, month)));

    Assert.assertEquals(2, sequence.size());
    Assert.assertEquals(day, sequence.span(0));
    Assert.assertEquals(month, sequence.span(1));

    sequence.add(year);

    Assert.assertEquals(3, sequence.size());
    Assert.assertEquals(day, sequence.span(0));
    Assert.assertEquals(month, sequence.span(1));
    Assert.assertEquals(year, sequence.span(2));

    sequence.remove(0);

    Assert.assertEquals(2, sequence.size());
    Assert.assertEquals(month, sequence.span(0));
    Assert.assertEquals(year, sequence.span(1));
  }

  @Test
  public void testIterator() {

    SpanSequence sequence = new SpanSequence();

    for (int i = 0; i < 50; i++) {
      sequence.add(new Span("span-" + Integer.toString(i, 10)));
    }

    Iterator<Span> iterator = sequence.iterator();

    for (int i = 0; i < 50; i++) {
      Span span = iterator.next();
      Assert.assertEquals("span-" + Integer.toString(i, 10), span.text());
    }

    Assert.assertFalse(iterator.hasNext());
  }
}
