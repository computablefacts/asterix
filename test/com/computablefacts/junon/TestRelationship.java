package com.computablefacts.junon;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;
import org.junit.Test;

public class TestRelationship {

  @Test
  public void testHashcodeAndEquals() {
    EqualsVerifier.forClass(Relationship.class).suppress(Warning.NONFINAL_FIELDS)
        .withIgnoredFields("id_", "externalId_", "fromExternalId_", "toExternalId_").verify();
  }
}
