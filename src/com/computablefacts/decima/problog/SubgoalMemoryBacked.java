package com.computablefacts.decima.problog;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@CheckReturnValue
final public class SubgoalMemoryBacked extends AbstractSubgoal {

  private final Set<Fact> facts_ = ConcurrentHashMap.newKeySet();
  private final Set<Rule> proofs_ = ConcurrentHashMap.newKeySet();

  private final Consumer<Fact> peek_;

  public SubgoalMemoryBacked(Literal literal) {
    this(literal, null);
  }

  public SubgoalMemoryBacked(Literal literal, Consumer<Fact> peek) {
    super(literal);
    peek_ = peek;
  }

  @Override
  public void fact(Fact fact) {

    Preconditions.checkNotNull(fact, "fact should not be null");

    facts_.add(fact);

    if (peek_ != null) {
      peek_.accept(fact);
    }
  }

  @Override
  public Iterator<Fact> facts() {
    return facts_.iterator();
  }

  @Override
  public boolean contains(Fact fact) {

    Preconditions.checkNotNull(fact, "fact should not be null");

    return facts_.contains(fact);
  }

  @Override
  public int nbFacts() {
    return facts_.size();
  }

  @Override
  public void proof(Rule proof) {

    Preconditions.checkNotNull(proof, "proof should not be null");
    Preconditions.checkArgument(proof.isGrounded(), "proof should be grounded : %s", proof);

    proofs_.add(proof);
  }

  @Override
  public Iterator<Rule> proofs() {
    return proofs_.iterator();
  }

  @Override
  public boolean contains(Rule proof) {

    Preconditions.checkNotNull(proof, "proof should not be null");

    return proofs_.contains(proof);
  }

  @Override
  public int nbProofs() {
    return proofs_.size();
  }
}