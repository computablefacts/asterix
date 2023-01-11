package com.computablefacts.decima.problog;

import com.computablefacts.Generated;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A subgoal is the item that is tabled by this algorithm.
 * <p>
 * A subgoal has a literal, a set of facts, and an array of waiters. A waiter is a pair containing a subgoal and a
 * clause.
 * </p>
 * All maps and lists should support concurrency because they will be updated and enumerated at the same time by the
 * tabling algorithm
 */
@CheckReturnValue
public abstract class AbstractSubgoal {

  private final Literal literal_;

  // Parent rules benefiting from this sub-goal resolution
  private final Set<Waiter> waiters_ = ConcurrentHashMap.newKeySet();

  public AbstractSubgoal(Literal literal) {
    literal_ = Preconditions.checkNotNull(literal, "literal should not be null");
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof AbstractSubgoal)) {
      return false;
    }
    AbstractSubgoal subgoal = (AbstractSubgoal) obj;
    return literal_.equals(subgoal.literal_);
  }

  @Override
  public int hashCode() {
    return literal_.hashCode();
  }

  @Generated
  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("literal", literal_).toString();
  }

  public Literal literal() {
    return literal_;
  }

  Set<Waiter> waiters() {
    return waiters_;
  }

  /**
   * Add a new waiter i.e. a subgoal that should be resumed when this subgoal has been evaluated.
   *
   * @param subgoal the subgoal to resume.
   * @param rule    the rule being evaluated.
   * @param idx     the position of the rule body literal that should be evaluated next.
   */
  void waiter(AbstractSubgoal subgoal, Rule rule, int idx) {
    waiters_.add(new Waiter(subgoal, rule, idx));
  }

  public abstract void fact(Fact fact);

  public abstract Iterator<Fact> facts();

  public abstract boolean contains(Fact fact);

  public abstract int nbFacts();

  public abstract void proof(Rule proof);

  public abstract Iterator<Rule> proofs();

  public abstract boolean contains(Rule proof);

  public abstract int nbProofs();

  static final class Waiter {

    public final AbstractSubgoal subgoal_;
    public final Rule rule_;
    public final int idx_;

    public Waiter(AbstractSubgoal subgoal, Rule rule, int idx) {

      Preconditions.checkNotNull(subgoal, "subgoal should not be null");
      Preconditions.checkNotNull(rule, "clause should not be null");
      Preconditions.checkArgument(0 <= idx && idx < rule.body().size(), "idx must be such as 0 <= idx < %s",
          rule.body().size());

      subgoal_ = subgoal;
      rule_ = rule;
      idx_ = idx;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Waiter)) {
        return false;
      }
      Waiter waiter = (Waiter) obj;
      return Objects.equals(subgoal_, waiter.subgoal_) && Objects.equals(rule_, waiter.rule_) && Objects.equals(idx_,
          waiter.idx_);
    }

    @Override
    public int hashCode() {
      return Objects.hash(subgoal_, rule_, idx_);
    }

    @Generated
    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("literal", subgoal_.literal_).add("rule", rule_).add("idx", idx_)
          .toString();
    }
  }
}