package com.computablefacts.decima.problog;

import com.google.errorprone.annotations.CheckReturnValue;
import java.util.Iterator;

@CheckReturnValue
public abstract class AbstractSubgoalFacts {

  public abstract boolean contains(Fact fact);

  public abstract Iterator<Fact> facts();

  public abstract int size();

  public abstract void add(Fact fact);
}
