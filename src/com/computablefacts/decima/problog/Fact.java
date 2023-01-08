package com.computablefacts.decima.problog;

import com.computablefacts.Generated;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.Objects;

@CheckReturnValue
final public class Fact extends AbstractClause {

  /**
   * Initialize a fact.
   *
   * @param head literal.
   */
  @Generated
  public Fact(Literal head) {
    super(head);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Fact)) {
      return false;
    }
    Fact fact = (Fact) obj;
    return Objects.equals(head(), fact.head());
  }

  @Override
  public int hashCode() {
    return Objects.hash(head());
  }

  @Override
  public String toString() {
    return head().toString();
  }


  @Override
  public boolean isFact() {
    return true;
  }

  @Override
  public boolean isRule() {
    return false;
  }
}