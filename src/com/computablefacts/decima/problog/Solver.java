package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;

import com.computablefacts.asterix.View;
import com.computablefacts.decima.problog.AbstractSubgoal.Waiter;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tabling algorithm in a non probabilistic setting :
 *
 * <ul>
 * <li>Chen, Weidong et al. "Efficient Top-Down Computation of Queries under the Well-Founded
 * Semantics." J. Log. Program. 24 (1995): 161-199.</li>
 * <li>Chen, Weidong &amp; Warren, David. (1996). Tabled evaluation with delaying for general logic
 * programs. J. ACM. 43. 20-74. 10.1145/227595.227597.</li>
 * </ul>
 * <p>
 * Tabling algorithm in a probabilistic setting :
 *
 * <ul>
 * <li>Luc De Raedt, Angelika Kimmig (2015). "Probabilistic (logic) programming concepts"</li>
 * <li>Mantadelis, Theofrastos &amp; Janssens, Gerda. (2010). "Dedicated Tabling for a Probabilistic
 * Setting.". Technical Communications of ICLP. 7. 124-133. 10.4230/LIPIcs.ICLP.2010.124.</li>
 * </ul>
 */
@CheckReturnValue
public class Solver {

  protected final AbstractKnowledgeBase kb_;
  protected final Map<String, AbstractSubgoal> subgoals_;
  protected final Function<Literal, AbstractSubgoal> newSubgoal_;
  protected final AbstractFunctions functions_;

  protected AbstractSubgoal root_ = null;
  protected int maxSampleSize_ = -1;

  @Deprecated
  public Solver(AbstractKnowledgeBase kb) {
    this(kb, new Functions(kb), SubgoalMemoryBacked::new);
  }

  public Solver(AbstractKnowledgeBase kb, AbstractFunctions functions) {
    this(kb, functions, SubgoalMemoryBacked::new);
  }

  public Solver(AbstractKnowledgeBase kb, AbstractFunctions functions, Function<Literal, AbstractSubgoal> newSubgoal) {

    Preconditions.checkNotNull(kb, "kb should not be null");
    Preconditions.checkNotNull(functions, "functions should not be null");
    Preconditions.checkNotNull(newSubgoal, "newSubgoal should not be null");

    kb_ = kb;
    functions_ = functions;
    subgoals_ = new ConcurrentHashMap<>();
    newSubgoal_ = newSubgoal;
  }

  /**
   * First, sets up and calls the subgoal search procedure. Then, extracts the answers but do not unfold the proofs.
   *
   * @param query goal.
   * @return facts answering the query.
   */
  public Iterator<Fact> solve(Literal query) {
    return solve(query, -1);
  }

  /**
   * Calls the subgoal search procedure.
   *
   * @param query         goal.
   * @param maxSampleSize stops the solver after the goal reaches this number of solutions or more. If this number is
   *                      less than or equals to 0, returns all solutions.
   * @return facts answering the query.
   */
  public Iterator<Fact> solve(Literal query, int maxSampleSize) {

    Preconditions.checkNotNull(query, "query should not be null");

    root_ = newSubgoal_.apply(query);
    subgoals_.put(query.tag(), root_);
    maxSampleSize_ = maxSampleSize <= 0 ? -1 : maxSampleSize;

    search(root_, 0);
    return root_.facts();
  }

  /**
   * Called each time a rule has been unfolded.
   *
   * @param subgoal the evaluated subgoal.
   * @param rule    the unfolded rule i.e. all body literals have constants terms.
   */
  protected void trackProofs(AbstractSubgoal subgoal, Rule rule) {
  }

  /**
   * Check if the number of samples asked by the caller has been reached.
   *
   * @return true iif the number of samples has been reached, false otherwise.
   */
  private boolean maxSampleSizeReached() {
    return maxSampleSize_ > 0 && root_ != null && root_.nbFacts() >= maxSampleSize_;
  }

  /**
   * Search for derivations of the literal associated with {@param subgoal}.
   *
   * @param subgoal subgoal.
   * @param idx     the next rule body literal to evaluate.
   */
  private void search(AbstractSubgoal subgoal, int idx) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");
    Preconditions.checkArgument(0 <= idx, "idx must be such as 0 <= idx");

    Literal literal = subgoal.literal();
    Predicate predicate = literal.predicate();

    Preconditions.checkState(!predicate.isPrimitive(), "predicate should not be a primitive : %s", literal);

    if (predicate.isNegated()) {

      Preconditions.checkState(literal.isSemiGrounded(), "negated clauses should be grounded : %s", literal);

      // Evaluate the positive version of the rule (i.e. negation as failure)
      Literal base = new Literal(predicate.baseName(), literal.terms());
      AbstractSubgoal sub = newSubgoal_.apply(base);

      subgoals_.put(sub.literal().tag(), sub);

      search(sub, idx);

      String newPredicate = literal.predicate().name();
      List<AbstractTerm> newTerms = literal.terms().stream().map(t -> t.isConst() ? t : newConst("_"))
          .collect(Collectors.toList());
      Iterator<Fact> facts = sub.facts();

      if (!facts.hasNext()) {

        // The positive version of the rule yielded no fact
        // => resume the current rule evaluation
        fact(subgoal, new Fact(new Literal(newPredicate, newTerms)));
      } else {

        // The positive version of the rule yielded at least one fact
        // => fail the current rule evaluation iif the probability of the produced facts is 0
        while (facts.hasNext()) {

          Fact fact = facts.next();

          if (fact.head().isRelevant(base)) {
            if (sub.nbProofs() == 0) {

              // Negate a probabilistic fact
              Fact negatedFact = new Fact(
                  new Literal(BigDecimal.ONE.subtract(fact.head().probability()), newPredicate, newTerms));

              if (!BigDecimal.ZERO.equals(negatedFact.head().probability())) {
                fact(subgoal, negatedFact);
              }
            } else {

              // Negate a probabilistic rule
              // i.e. if (q :- a, b) then ~q is rewritten as (~q :- ~a) or (~q :- ~b)
              List<Rule> rules = View.of(sub.proofs()).filter(Rule::isGrounded).toList();

              for (Rule rule : rules) {
                for (Literal lit : rule.body()) {
                  if (!lit.predicate().isPrimitive()) {
                    Rule negatedRule = new Rule(new Literal(newPredicate, newTerms), Lists.newArrayList(lit.negate()));
                    rule(subgoal, negatedRule, idx);
                  }
                }
              }
            }
          }
          if (maxSampleSizeReached()) {
            break;
          }
        }
      }
    } else {

      Iterator<Fact> facts = kb_.facts(literal);

      while (facts.hasNext()) {

        Fact fact = facts.next();
        Literal renamed = fact.head();
        Map<com.computablefacts.decima.problog.Var, AbstractTerm> env = literal.unify(renamed);

        if (env != null) {
          fact(subgoal, new Fact(renamed.subst(env)));
        }
        if (maxSampleSizeReached()) {
          break;
        }
      }

      Iterator<Rule> rules = kb_.rules(literal);

      while (rules.hasNext()) {

        Rule rule = rules.next();
        Rule renamed = rule.rename();
        Map<com.computablefacts.decima.problog.Var, AbstractTerm> env = literal.unify(renamed.head());

        if (env != null) {
          rule(subgoal, renamed.subst(env), idx);
        }
        if (maxSampleSizeReached()) {
          break;
        }
      }
    }
  }

  /**
   * Store a fact, and inform all waiters of the fact too.
   *
   * @param subgoal subgoal.
   * @param fact    the fact to add to the subgoal.
   */
  private void fact(AbstractSubgoal subgoal, Fact fact) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");
    Preconditions.checkNotNull(fact, "fact should not be null");

    if (subgoal.contains(fact)) { // Potentially expensive call...
      return;
    }

    subgoal.fact(fact);

    for (Waiter waiter : subgoal.waiters()) {

      ground(waiter.subgoal_, waiter.rule_, fact, waiter.idx_);

      if (maxSampleSizeReached()) {
        return;
      }
    }
  }

  /**
   * Evaluate a newly derived rule.
   *
   * @param subgoal subgoal.
   * @param rule    the rule to add to the subgoal.
   * @param idx     the next rule body literal to evaluate.
   */
  private void rule(AbstractSubgoal subgoal, Rule rule, int idx) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");
    Preconditions.checkNotNull(rule, "rule should not be null");
    Preconditions.checkArgument(0 <= idx && idx < rule.body().size(), "idx must be such as 0 <= idx");

    Literal first = rule.body().get(idx);

    if (first.predicate().isPrimitive()) {

      Iterator<Literal> facts = first.execute(functions_.definitions());

      if (facts != null) {
        while (facts.hasNext()) {

          ground(subgoal, rule, new Fact(facts.next()), idx);

          if (maxSampleSizeReached()) {
            break;
          }
        }
        return;
      }
      return;
    }

    @Var AbstractSubgoal sub = subgoals_.get(first.tag());

    if (sub != null) {
      sub.waiter(subgoal, rule, idx);
    } else {

      sub = newSubgoal_.apply(first);
      sub.waiter(subgoal, rule, idx);

      subgoals_.put(sub.literal().tag(), sub);

      search(sub, 0);
    }

    Iterator<Fact> facts = sub.facts();

    while (facts.hasNext()) {

      ground(subgoal, rule, facts.next(), idx);

      if (maxSampleSizeReached()) {
        return;
      }
    }
  }

  /**
   * Start grounding a rule.
   *
   * @param subgoal subgoal.
   * @param rule    the rule to ground.
   * @param fact    the fact associated with the rule's first body literal.
   * @param idx     the next rule body literal to evaluate.
   */
  private void ground(AbstractSubgoal subgoal, Rule rule, Fact fact, int idx) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");
    Preconditions.checkNotNull(rule, "rule should not be null");
    Preconditions.checkNotNull(fact, "fact should not be null");

    // Rule minus first body literal
    List<Literal> bodyTmp = Collections.unmodifiableList(rule.body().subList(idx, rule.body().size()));
    Rule ruleTmp = new Rule(rule.head(), bodyTmp).resolve(fact.head());

    Preconditions.checkState(ruleTmp != null, "resolution failed : rule = %s / head = %s", rule, fact);

    Rule newRule = new Rule(ruleTmp.head(), View.of(rule.body().subList(0, idx)).concat(ruleTmp.body()).toList());

    if (ruleTmp.body().size() == 1) {
      trackProofs(subgoal, newRule);
      fact(subgoal, new Fact(newRule.head()));
    } else {
      rule(subgoal, newRule, idx + 1);
    }
  }
}