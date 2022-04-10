package com.computablefacts.asterix.queries;

import com.computablefacts.asterix.Generated;
import com.computablefacts.asterix.View;
import com.google.errorprone.annotations.CheckReturnValue;

/**
 * Common interface for expression nodes.
 *
 * See http://www.blackbeltcoder.com/Articles/data/easy-full-text-search-queries for details.
 */
@CheckReturnValue
public abstract class AbstractNode<T extends AbstractQueryEngine> {

  private boolean exclude_ = false;

  public AbstractNode() {
    super();
  }

  /**
   * Indicates this term (or both child terms) should be excluded from the results.
   */
  @Generated
  final public boolean exclude() {
    return this.exclude_;
  }

  @Generated
  final public void exclude(boolean exclude) {
    this.exclude_ = exclude;
  }

  public abstract long cardinality(T engine);

  public abstract View<String> execute(T engine);
}
