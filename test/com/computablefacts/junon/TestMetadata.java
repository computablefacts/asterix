package com.computablefacts.junon;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class TestMetadata {

  @Test
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(Metadata.class).verify();
  }
}
