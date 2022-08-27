package com.computablefacts.asterix.queries;

import com.computablefacts.asterix.Generated;
import com.computablefacts.asterix.View;
import com.computablefacts.logfmt.LogFormatter;
import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Internal (non-leaf) expression node class.
 * <p>
 * See http://www.blackbeltcoder.com/Articles/data/easy-full-text-search-queries for details.
 */
@CheckReturnValue
final public class InternalNode<T extends AbstractQueryEngine> extends AbstractNode<T> {

  private static final Logger logger_ = LoggerFactory.getLogger(InternalNode.class);

  private eConjunctionTypes conjunction_;
  private AbstractNode<T> child1_;
  private AbstractNode<T> child2_;

  public InternalNode(eConjunctionTypes conjunction, AbstractNode<T> child1, AbstractNode<T> child2) {

    conjunction_ = Preconditions.checkNotNull(conjunction, "conjunction should not be null");

    child1_ = child1;
    child2_ = child2;
  }

  @Override
  public String toString() {
    return (exclude() ? "Not(" : "(") + (child1_ == null ? "" : child1_.toString()) + " " + conjunction_.toString()
        + " " + (child2_ == null ? "" : child2_.toString()) + ")";
  }

  @Override
  public long cardinality(T engine) {

    Preconditions.checkNotNull(engine, "engine should not be null");

    if (logger_.isDebugEnabled()) {
      logger_.debug(LogFormatter.create().add("conjunction", conjunction_).add("child1", child1_).add("child2", child2_)
          .formatDebug());
    }

    long cardChild1;
    long cardChild2;
    long cardinality;

    if (child1_ == null) {
      cardChild1 = 0;
    } else {
      cardChild1 = child1_.cardinality(engine);
    }

    if (child2_ == null) {
      cardChild2 = 0;
    } else {
      cardChild2 = child2_.cardinality(engine);
    }

    if (child1_ != null && child2_ != null) {

      // Here, the query is in {A OR B, A AND B, NOT A AND B, A AND NOT B, NOT A OR B, A OR NOT B,
      // NOT A AND NOT B, NOT A OR NOT B}
      if (child1_.exclude() && child2_.exclude()) {
        return 0; // (NOT A AND NOT B) or (NOT A OR NOT B)
      }

      // Here, the query is in {A OR B, A AND B, NOT A AND B, A AND NOT B, NOT A OR B, A OR NOT B}
      if (eConjunctionTypes.Or.equals(conjunction_) && (child1_.exclude() || child2_.exclude())) {
        if (child1_.exclude()) {
          return cardChild2; // NOT A OR B
        }
        return cardChild1; // A OR NOT B
      }

      // Here, the query is in {A OR B, A AND B, NOT A AND B, A AND NOT B}
      if (eConjunctionTypes.And.equals(conjunction_) && (child1_.exclude() || child2_.exclude())) {
        if (child1_.exclude()) {
          return cardChild2; // NOT A AND B -> should be Math.min(cardChild2, #entries - cardChild1)
        }
        return cardChild1; // A AND NOT B -> should be Math.min(cardChild1, #entries - cardChild2)
      }
    }

    // Here, the query is in {A OR B, A AND B}
    if (eConjunctionTypes.Or.equals(conjunction_)) {
      cardinality = cardChild1 + cardChild2;
    } else {
      cardinality = Math.min(cardChild1, cardChild2);
    }
    return cardinality;
  }

  @Override
  public View<String> execute(T engine) {

    Preconditions.checkNotNull(engine, "engine should not be null");

    if (logger_.isDebugEnabled()) {
      logger_.debug(LogFormatter.create().add("conjunction", conjunction_).add("child1", child1_).add("child2", child2_)
          .formatDebug());
    }

    if (child1_ == null) {
      if (child2_ == null) {
        return View.of();
      }
      if (child2_.exclude()) { // (NULL AND/OR NOT B) is not a valid construct
        if (logger_.isErrorEnabled()) {
          logger_.error(LogFormatter.create().add("query", toString()).message("ill-formed query : (NULL AND/OR NOT B)")
              .formatError());
        }
        return View.of();
      }
      return eConjunctionTypes.Or.equals(conjunction_) ? child2_.execute(engine) : View.of();
    }
    if (child2_ == null) {
      if (child1_.exclude()) { // (NOT A AND/OR NULL) is not a valid construct
        if (logger_.isErrorEnabled()) {
          logger_.error(LogFormatter.create().add("query", toString()).message("ill-formed query : (NOT A AND/OR NULL)")
              .formatError());
        }
        return View.of();
      }
      return eConjunctionTypes.Or.equals(conjunction_) ? child1_.execute(engine) : View.of();
    }

    // Here, the query is in {A OR B, A AND B, NOT A AND B, A AND NOT B, NOT A OR B, A OR NOT B, NOT
    // A AND NOT B, NOT A OR NOT B}
    if (child1_.exclude() && child2_.exclude()) {
      if (logger_.isErrorEnabled()) {
        logger_.error(LogFormatter.create().add("query", toString()).message("ill-formed query : (NOT A AND/OR NOT B)")
            .formatError());
      }
      return View.of(); // (NOT A AND NOT B) or (NOT A OR NOT B)
    }

    // Here, the query is in {A OR B, A AND B, NOT A AND B, A AND NOT B, NOT A OR B, A OR NOT B}
    if (eConjunctionTypes.Or.equals(conjunction_) && (child1_.exclude() || child2_.exclude())) {
      if (logger_.isErrorEnabled()) {
        logger_.error(
            LogFormatter.create().add("query", toString()).message("ill-formed query : (A OR NOT B) or (NOT A OR B)")
                .formatError());
      }
      return View.of();
    }

    View<String> ids1 = child1_.execute(engine);
    View<String> ids2 = child2_.execute(engine);

    // Here, the query is in {A OR B, A AND B, NOT A AND B, A AND NOT B}
    if (eConjunctionTypes.And.equals(conjunction_) && (child1_.exclude() || child2_.exclude())) {
      if (child1_.exclude()) {
        return ids2.diffSorted(ids1); // NOT A AND B
      }
      return ids1.diffSorted(ids2); // A AND NOT B
    }

    // Here, the query is in {A OR B, A AND B}
    if (eConjunctionTypes.Or.equals(conjunction_)) {

      List<View<String>> list = new ArrayList<>();
      list.add(ids2);

      // Advance both iterators synchronously. The assumption is that both iterators are sorted.
      return ids1.mergeSorted(list, String::compareTo).dedupSorted();
    }

    // Advance both iterators synchronously. The assumption is that both iterators are sorted.
    return ids1.intersectSorted(ids2);
  }

  @Generated
  public eConjunctionTypes conjunction() {
    return conjunction_;
  }

  @Generated
  public void conjunction(eConjunctionTypes conjunction) {
    conjunction_ = conjunction;
  }

  @Generated
  public AbstractNode<T> child1() {
    return child1_;
  }

  @Generated
  public void child1(AbstractNode<T> child) {
    child1_ = child;
  }

  @Generated
  public AbstractNode<T> child2() {
    return child2_;
  }

  @Generated
  public void child2(AbstractNode<T> child) {
    child2_ = child;
  }

  public enum eConjunctionTypes {
    And, Or
  }
}
