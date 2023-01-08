package com.computablefacts.decima.problog;

import com.google.errorprone.annotations.CheckReturnValue;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@CheckReturnValue
final public class InMemorySubgoalFacts extends AbstractSubgoalFacts {

  private final Set<Fact> facts_ = ConcurrentHashMap.newKeySet();

  public InMemorySubgoalFacts() {
  }

  @Override
  public boolean contains(Fact rule) {
    return facts_.contains(rule);
  }

  @Override
  public Iterator<Fact> facts() {
    return facts_.iterator();
  }

  @Override
  public int size() {
    return facts_.size();
  }

  @Override
  public void add(Fact rule) {
    facts_.add(rule);
  }
}
