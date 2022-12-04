package com.computablefacts.junon;

import java.util.Date;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Assert;
import org.junit.Test;

public class TestProvenance {

  @Test
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(Provenance.class).verify();
  }

  @Test
  public void testConstructorWith5Params() {

    Provenance provenance = new Provenance("STORAGE/ROOT/DATASET/DOC_ID",
        "ACCUMULO/client_prod/example/gcqcl|2021-01-27T23:23:45.006Z", "High", new Date(), new Date());

    Assert.assertEquals("ACCUMULO/client_prod/example/gcqcl|2021-01-27T23:23:45.006Z", provenance.sourceStore());
    Assert.assertEquals("STORAGE/ROOT/DATASET/DOC_ID", provenance.sourceType());
    Assert.assertEquals("High", provenance.sourceReliability());
    Assert.assertNull(provenance.string());
    Assert.assertNull(provenance.span());
    Assert.assertNull(provenance.spanHash());
    Assert.assertEquals(-1, provenance.startIndex());
    Assert.assertEquals(-1, provenance.endIndex());
    Assert.assertTrue(provenance.extractionDate().matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z$"));
    Assert.assertTrue(provenance.modificationDate().matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z$"));
    Assert.assertEquals(-1, provenance.page());
  }

  @Test
  public void testConstructorWith7Params() {

    Provenance provenance = new Provenance("STORAGE/ROOT/DATASET/DOC_ID",
        "ACCUMULO/client_prod/example/gcqcl|2021-01-27T23:23:45.006Z", "High", new Date(), new Date(), 1,
        "A single page.");

    Assert.assertEquals("ACCUMULO/client_prod/example/gcqcl|2021-01-27T23:23:45.006Z", provenance.sourceStore());
    Assert.assertEquals("STORAGE/ROOT/DATASET/DOC_ID", provenance.sourceType());
    Assert.assertEquals("High", provenance.sourceReliability());
    Assert.assertEquals("A single page.", provenance.string());
    Assert.assertNull(provenance.span());
    Assert.assertNull(provenance.spanHash());
    Assert.assertEquals(-1, provenance.startIndex());
    Assert.assertEquals(-1, provenance.endIndex());
    Assert.assertTrue(provenance.extractionDate().matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z$"));
    Assert.assertTrue(provenance.modificationDate().matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z$"));
    Assert.assertEquals(1, provenance.page());
  }

  @Test
  public void testConstructorWith10Params() {

    Provenance provenance = new Provenance("STORAGE/ROOT/DATASET/DOC_ID",
        "ACCUMULO/client_prod/example/gcqcl|2021-01-27T23:23:45.006Z", "High", new Date(), new Date(), 1,
        "A single page.", "single", 0, "single".length());

    Assert.assertEquals("ACCUMULO/client_prod/example/gcqcl|2021-01-27T23:23:45.006Z", provenance.sourceStore());
    Assert.assertEquals("STORAGE/ROOT/DATASET/DOC_ID", provenance.sourceType());
    Assert.assertEquals("High", provenance.sourceReliability());
    Assert.assertEquals("A single page.", provenance.string());
    Assert.assertEquals("single", provenance.span());
    Assert.assertEquals("c9e449e2873d5b8796c2794f198d6cef", provenance.spanHash());
    Assert.assertEquals(0, provenance.startIndex());
    Assert.assertEquals("single".length(), provenance.endIndex());
    Assert.assertTrue(provenance.extractionDate().matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z$"));
    Assert.assertTrue(provenance.modificationDate().matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*Z$"));
    Assert.assertEquals(1, provenance.page());
  }

  @Test
  public void testAccessors() {

    Provenance provenance = new Provenance("STORAGE/ROOT/DATASET/DOC_ID",
        "ACCUMULO/client_prod/example/gcqcl|2021-01-27T23:23:45.006Z", "High", new Date(), new Date(), 1,
        "A single page.", "single", 0, "single".length());

    Assert.assertEquals("ACCUMULO", provenance.storage());
    Assert.assertEquals("client_prod", provenance.root());
    Assert.assertEquals("example", provenance.dataset());
    Assert.assertEquals("gcqcl|2021-01-27T23:23:45.006Z", provenance.docId());
  }
}
