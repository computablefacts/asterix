package com.computablefacts.decima.problog;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@CheckReturnValue
final public class Rule extends AbstractClause {

  private final List<Literal> body_;
  private Boolean isGrounded_ = null;

  /**
   * Initialize a rule.
   *
   * @param head literal.
   * @param body body literals.
   */
  public Rule(Literal head, Literal... body) {
    this(head, Lists.newArrayList(body));
  }

  /**
   * Initialize a fact or a rule.
   *
   * @param head literal.
   * @param body list of literals.
   */
  public Rule(Literal head, List<Literal> body) {

    super(head);

    Preconditions.checkNotNull(body, "body should not be null");
    Preconditions.checkState(body.stream().noneMatch(Objects::isNull), "body literals should not be null");

    body_ = new ArrayList<>(body);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Rule)) {
      return false;
    }
    Rule rule = (Rule) obj;
    return Objects.equals(head(), rule.head()) && Objects.equals(body_, rule.body_);
  }

  @Override
  public int hashCode() {
    return Objects.hash(head(), body_);
  }

  @Override
  public String toString() {

    StringBuilder builder = new StringBuilder();
    builder.append(head().toString());
    builder.append(" :- ");

    for (int i = 0; i < body_.size(); i++) {
      if (i > 0) {
        builder.append(", ");
      }
      Literal literal = body_.get(i);
      builder.append(literal.toString());
    }
    return builder.toString();
  }

  @Override
  public boolean isFact() {
    return false;
  }

  @Override
  public boolean isRule() {
    return true;
  }

  /**
   * Get the current clause body.
   *
   * @return the clause body.
   */
  public List<Literal> body() {
    return body_;
  }

  /**
   * Check if the current clause is grounded.
   *
   * @return true iif the current clause is grounded.
   */
  public boolean isGrounded() {
    if (isGrounded_ == null) {
      if (!head().isGrounded()) {
        isGrounded_ = false;
        return isGrounded_;
      }
      for (Literal literal : body_) {
        if (!literal.isGrounded()) {
          isGrounded_ = false;
          return isGrounded_;
        }
      }
      isGrounded_ = true;
    }
    return isGrounded_;
  }

  /**
   * Check if two clauses can be unified.
   *
   * @param rule rule.
   * @return true iif the two clauses can be unified.
   */
  public boolean isRelevant(Rule rule) {

    Preconditions.checkNotNull(rule, "rule should not be null");

    if (!head().isRelevant(rule.head())) {
      return false;
    }
    if (body_.size() != rule.body_.size()) {
      return false;
    }
    for (int i = 0; i < body_.size(); i++) {
      if (!body_.get(i).isRelevant(rule.body_.get(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * A clause is safe if every variable in its head is in its body.
   *
   * @return true iif the current clause is safe.
   */
  public boolean isSafe() {
    for (AbstractTerm term : head().terms()) {
      if (!term.isConst()) {
        if (!bodyHasTerm(term)) {
          return false;
        }
      }
    }
    return true;
  }

  /**
   * Rename the variables in a clause. Every variable in the head is in the body, so the head can be ignored while
   * generating an environment.
   *
   * @return a new clause.
   */
  public Rule rename() {

    @com.google.errorprone.annotations.Var Map<Var, AbstractTerm> env = new HashMap<>();

    for (Literal literal : body_) {
      env = literal.shuffle(env);
    }
    return subst(env);
  }

  /**
   * Clause substitution in which the substitution is applied to each each literal that makes up the clause.
   *
   * @param env environment.
   * @return a new clause.
   */
  public Rule subst(Map<Var, AbstractTerm> env) {

    if (env == null || env.isEmpty()) {
      return this;
    }

    Literal head = head().subst(env);
    List<Literal> body = new ArrayList<>(body_.size());

    for (Literal literal : body_) {
      body.add(literal.subst(env));
    }
    return new Rule(head, body);
  }

  /**
   * Resolve the first literal in a rule with a given literal. If the two literals unify, a new clause is generated that
   * has the same number of literals in the body.
   *
   * @param literal literal.
   * @return a new clause or null on error.
   */
  public Rule resolve(Literal literal) {

    Preconditions.checkNotNull(literal, "literal should not be null");
    Preconditions.checkArgument(literal.isGrounded(), "literal should be grounded : %s", literal);

    Literal first = body_.get(0);
    Map<Var, AbstractTerm> env = first.unify(literal.rename());

    if (env == null) {
      return null;
    }

    Literal head = head().subst(env);
    List<Literal> body = new ArrayList<>(body_.size());
    body.add(literal);

    for (int i = 1; i < body_.size(); i++) {
      body.add(body_.get(i).subst(env));
    }
    return new Rule(head, body);
  }

  /**
   * Check if the current clause body contains a given term.
   *
   * @param term term.
   * @return true iif the current clause body contains the given term.
   */
  private boolean bodyHasTerm(AbstractTerm term) {

    Preconditions.checkNotNull(term, "term should not be null");

    for (Literal literal : body_) {
      if (literal.hasTerm(term)) {
        return true;
      }
    }
    return false;
  }
}
