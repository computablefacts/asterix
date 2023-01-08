package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Test;

public class RuleTest {

  @Test
  public void testHashcodeAndEquals() {
    Literal blue = new Literal("red", newConst("abc"));
    Literal red = new Literal("blue", newConst(123));
    EqualsVerifier.forClass(Rule.class).withIgnoredFields("isGrounded_").withPrefabValues(Literal.class, red, blue)
        .verify();
  }
}
