package com.computablefacts.decima.problog;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;

/**
 * A clause has a head literal, and a sequence of literals that form its body. If there are no literals in its body, the
 * clause is called a fact. If there is at least one literal in its body, it is called a rule.
 * <p>
 * A clause asserts that its head is true if every literal in its body is true.
 */
@CheckReturnValue
public abstract class AbstractClause {

  private final Literal head_;

  protected AbstractClause(Literal head) {
    head_ = Preconditions.checkNotNull(head, "head should not be null");
  }

  /**
   * Get the current clause head.
   *
   * @return the clause head.
   */
  public Literal head() {
    return head_;
  }
  
  /**
   * Check if the current clause is a fact.
   *
   * @return true iif the current clause is a fact.
   */
  public abstract boolean isFact();

  /**
   * Check if the current clause is a rule.
   *
   * @return true iif the current clause is a rule.
   */
  public abstract boolean isRule();
}
