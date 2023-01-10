package com.computablefacts.decima.problog;

import com.computablefacts.Generated;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A subgoal is the item that is tabled by this algorithm.
 * <p>
 * A subgoal has a literal, a set of facts, and an array of waiters. A waiter is a pair containing a subgoal and a
 * clause.
 * <p>
 * All maps and lists should support concurrency because they will be updated and enumerated at the same time by the
 * tabling algorithm
 */
@CheckReturnValue
final public class Subgoal {

  private final Literal literal_;

  // Parent rules benefiting from this sub-goal resolution
  private final Set<Map.Entry<Subgoal, Map.Entry<Rule, Integer>>> waiters_ = ConcurrentHashMap.newKeySet();

  // Facts derived for this subgoal
  private final AbstractSubgoalFacts facts_;
  private final Set<Rule> proofs_ = new HashSet<>();

  public Subgoal(Literal literal, AbstractSubgoalFacts facts) {

    Preconditions.checkNotNull(literal, "literal should not be null");
    Preconditions.checkNotNull(facts, "facts should not be null");

    literal_ = literal;
    facts_ = facts;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Subgoal)) {
      return false;
    }
    Subgoal subgoal = (Subgoal) obj;
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

  boolean contains(Fact fact) {
    return facts_.contains(fact);
  }

  Iterator<Fact> facts() {
    return facts_.facts();
  }

  int nbFacts() {
    return facts_.size();
  }

  Set<Map.Entry<Subgoal, Map.Entry<Rule, Integer>>> waiters() {
    return waiters_;
  }

  /**
   * Add a new waiter i.e. a subgoal that should be resumed when this subgoal has been evaluated.
   *
   * @param subgoal the subgoal to resume.
   * @param rule    the rule being evaluated.
   * @param idx     the position of the rule body literal that should be evaluated next.
   */
  void waiter(Subgoal subgoal, Rule rule, int idx) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");
    Preconditions.checkNotNull(rule, "clause should not be null");
    Preconditions.checkArgument(0 <= idx && idx < rule.body().size(), "idx must be such as 0 <= idx < %s",
        rule.body().size());

    waiters_.add(new AbstractMap.SimpleEntry<>(subgoal, new AbstractMap.SimpleEntry<>(rule, idx)));
  }

  /**
   * Get all the profs that entails the subgoal.
   *
   * @return a set of proofs.
   */
  Collection<Rule> proofs() {
    return ImmutableSet.copyOf(proofs_);
  }

  /**
   * Add a proof to the subgoal.
   *
   * @param proof the proof to add.
   */
  void proof(Rule proof) {

    Preconditions.checkNotNull(proof, "proof should not be null");
    Preconditions.checkArgument(proof.isGrounded(), "proof should be grounded : %s", proof);

    proofs_.add(proof);
  }

  /**
   * Add a fact to the subgoal.
   *
   * @param fact the fact to add.
   */
  void fact(Fact fact) {

    Preconditions.checkNotNull(fact, "fact should not be null");

    facts_.add(fact);
  }
}