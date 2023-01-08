package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;

import com.computablefacts.Generated;
import com.computablefacts.asterix.BloomFilter;
import com.computablefacts.asterix.trie.Trie;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
final public class Solver {

  private static final double FALSE_POSITIVE_PROBABILITY = 0.05;
  private static final int EXPECTED_NUMBER_OF_ELEMENTS = 10000000;
  private static final Logger logger_ = LoggerFactory.getLogger(Solver.class);

  private final AbstractKnowledgeBase kb_;
  private final Map<String, Subgoal> subgoals_;
  private final Function<Literal, Subgoal> newSubgoal_;
  private final BloomFilter<String> bf_;

  private Subgoal root_ = null;
  private int maxSampleSize_ = -1;

  public Solver(AbstractKnowledgeBase kb, boolean computeProofs) {
    this(kb, literal -> new Subgoal(literal, new InMemorySubgoalFacts(), computeProofs));
  }

  public Solver(AbstractKnowledgeBase kb, Function<Literal, Subgoal> newSubgoal) {
    this(kb, newSubgoal, FALSE_POSITIVE_PROBABILITY, EXPECTED_NUMBER_OF_ELEMENTS); // BF : ~72MB
  }

  public Solver(AbstractKnowledgeBase kb, Function<Literal, Subgoal> newSubgoal, double falsePositiveProbability,
      int expectedNumberOfElements) {

    Preconditions.checkNotNull(kb, "kb should not be null");
    Preconditions.checkNotNull(newSubgoal, "newSubgoal should not be null");

    kb_ = kb;
    subgoals_ = new ConcurrentHashMap<>();
    newSubgoal_ = newSubgoal;
    bf_ = new BloomFilter<>(falsePositiveProbability, expectedNumberOfElements);
  }

  /**
   * Return the number of subgoals.
   *
   * @return the number of subgoals.
   */
  @Generated
  public int nbSubgoals() {
    return subgoals_.size();
  }

  /**
   * First, sets up and calls the subgoal search procedure. Then, extracts the answers and unfold the proofs. In order
   * to work, subgoals must track rules i.e. {@code computeProofs = true}.
   *
   * @param query goal.
   * @return proofs.
   */
  public Set<AbstractClause> proofs(Literal query) {

    Preconditions.checkNotNull(query, "query should not be null");

    root_ = newSubgoal_.apply(query);
    subgoals_.put(query.tag(), root_);

    search(root_);

    ProofAssistant assistant = new ProofAssistant(subgoals_.values());
    return assistant.proofs(root_.literal());
  }

  @Beta
  public List<String> tableOfProofs(Literal query) {

    Preconditions.checkNotNull(query, "query should not be null");

    root_ = newSubgoal_.apply(query);
    subgoals_.put(query.tag(), root_);

    search(root_);

    ProofAssistant assistant = new ProofAssistant(subgoals_.values());
    Set<AbstractClause> proofs = assistant.proofs(root_.literal());
    return assistant.tableOfProofs();
  }

  /**
   * Map each proof to a trie.
   *
   * @param query goal.
   * @return proofs.
   */
  public Map<Literal, Trie<Literal>> tries(Literal query) {

    Preconditions.checkNotNull(query, "query should not be null");

    Set<AbstractClause> proofs = proofs(query);
    Map<Literal, Trie<Literal>> tries = new HashMap<>();

    for (AbstractClause proof : proofs) {
      if (!tries.containsKey(proof.head())) {
        tries.put(proof.head(), new Trie<>());
      }
      if (proof.isRule()) {
        tries.get(proof.head()).insert(((Rule) proof).body());
      } else {
        tries.get(proof.head()).insert(Lists.newArrayList());
      }
    }
    return tries;
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
   * First, sets up and calls the subgoal search procedure. Then, extracts the answers but do not unfold the proofs.
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

    search(root_);
    return root_.facts();
  }

  /**
   * Dump the subgoals rules. This method will yield no result if {@code computeProofs} is set to {@code false}.
   *
   * @return the generated rules.
   */
  @Generated
  public String dumpSubgoals() {
    return subgoals_.values().stream().map(
            subgoal -> subgoal.literal().toString() + " : " + subgoal.nbFacts() + "\n" + (subgoal.rules().isEmpty()
                ? "  -> nil"
                : subgoal.rules().stream().map(rule -> "  -> " + rule.toString()).collect(Collectors.joining("\n"))))
        .collect(Collectors.joining("\n"));
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
   */
  private void search(Subgoal subgoal) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");

    Literal literal = subgoal.literal();
    Predicate predicate = literal.predicate();

    Preconditions.checkState(!predicate.isPrimitive(), "predicate should not be a primitive : %s", literal);

    if (predicate.isNegated()) {

      Preconditions.checkState(literal.isSemiGrounded(), "negated clauses should be grounded : %s", literal);

      // Evaluate the positive version of the rule (i.e. negation as failure)
      Literal base = new Literal(predicate.baseName(), literal.terms());
      Subgoal sub = newSubgoal_.apply(base);

      subgoals_.put(sub.literal().tag(), sub);

      search(sub);

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
            if (sub.rules().isEmpty()) {

              // Negate a probabilistic fact
              Fact newFact = new Fact(
                  new Literal(BigDecimal.ONE.subtract(fact.head().probability()), newPredicate, newTerms));

              if (!BigDecimal.ZERO.equals(newFact.head().probability())) {
                fact(subgoal, newFact);
              } else {
                subgoal.pop(new Rule(literal));
              }
            } else {

              // Negate a probabilistic rule
              // i.e. if (q :- a, b) then ~q is rewritten as (~q :- ~a) or (~q :- ~b)
              for (Rule rule : sub.rules()) {
                for (Literal lit : rule.body()) {
                  if (!lit.predicate().isPrimitive()) {

                    Rule newRule = new Rule(new Literal(newPredicate, newTerms), Lists.newArrayList(lit.negate()));

                    subgoal.add(newRule);
                  }
                }
              }

              List<Rule> rules = sub.proofs().stream().filter(Rule::isGrounded).collect(Collectors.toList());

              for (Rule rule : rules) {
                for (Literal lit : rule.body()) {
                  if (!lit.predicate().isPrimitive()) {

                    Rule newLiteral = new Rule(new Literal(newPredicate, newTerms), Lists.newArrayList(lit.negate()));

                    rule(subgoal, newLiteral, false);
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

      @Var boolean match = false;
      Iterator<Fact> facts = kb_.facts(literal);

      while (facts.hasNext()) {

        Fact fact = facts.next();
        Literal renamed = fact.head();
        Map<com.computablefacts.decima.problog.Var, AbstractTerm> env = literal.unify(renamed);

        if (env != null) {
          fact(subgoal, new Fact(renamed.subst(env)));
          match = true;
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
          rule(subgoal, renamed.subst(env), true);
          match = true;
        }
        if (maxSampleSizeReached()) {
          break;
        }
      }

      if (!match) {
        subgoal.pop(new Rule(literal));
      }
    }
  }

  /**
   * Store a fact, and inform all waiters of the fact too.
   *
   * @param subgoal subgoal.
   * @param fact    the fact to add to the subgoal.
   */
  private void fact(Subgoal subgoal, Fact fact) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");
    Preconditions.checkNotNull(fact, "fact should not be null");

    String hash = subgoal.literal().id() + fact.head().id();

    if (!bf_.contains(hash)) {
      bf_.add(hash);
    } else {
      if (subgoal.contains(fact)) { // Potentially expensive call...
        return;
      }
    }

    subgoal.add(fact);

    for (Map.Entry<Subgoal, Rule> entry : subgoal.waiters()) {

      ground(entry.getKey(), entry.getValue(), fact);

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
   * @param isInKb  true iif the rule has been loaded from the KB, false otherwise.
   */
  private void rule(Subgoal subgoal, Rule rule, boolean isInKb) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");
    Preconditions.checkNotNull(rule, "rule should not be null");
    Preconditions.checkArgument(rule.isRule(), "rule should be a rule : %s", rule);

    subgoal.add(isInKb ? rule : null);
    Literal first = rule.body().get(0);

    if (first.predicate().isPrimitive()) {

      Iterator<Literal> facts = first.execute(kb_.definitions());

      if (facts != null) {
        while (facts.hasNext()) {

          ground(subgoal, rule, new Fact(facts.next()));

          if (maxSampleSizeReached()) {
            break;
          }
        }
        return;
      }
      subgoal.pop(rule);
      return;
    }

    @Var Subgoal sub = subgoals_.get(first.tag());

    if (sub != null) {
      sub.waiter(subgoal, rule);
    } else {

      sub = newSubgoal_.apply(first);
      sub.waiter(subgoal, rule);

      subgoals_.put(sub.literal().tag(), sub);

      search(sub);
    }

    Iterator<Fact> facts = sub.facts();

    if (!facts.hasNext()) {
      subgoal.pop(rule);
    } else {
      while (facts.hasNext()) {

        ground(subgoal, rule, facts.next());

        if (maxSampleSizeReached()) {
          return;
        }
      }
    }
  }

  /**
   * Start grounding a rule.
   *
   * @param subgoal subgoal.
   * @param rule    the rule to ground.
   * @param fact    the fact associated with the rule's first body literal.
   */
  private void ground(Subgoal subgoal, Rule rule, Fact fact) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");
    Preconditions.checkNotNull(rule, "rule should not be null");
    Preconditions.checkNotNull(fact, "fact should not be null");

    // Rule with first body literal
    Rule prevRule = rule.resolve(fact.head());

    Preconditions.checkState(prevRule != null, "resolution failed : rule = %s / head = %s", rule, fact);

    // Rule minus first body literal
    Literal head = prevRule.head();
    List<Literal> body = Collections.unmodifiableList(prevRule.body().subList(1, prevRule.body().size()));

    subgoal.push(prevRule);

    if (body.isEmpty()) {
      fact(subgoal, new Fact(head));
    } else {
      rule(subgoal, new Rule(head, body), false);
    }
  }
}