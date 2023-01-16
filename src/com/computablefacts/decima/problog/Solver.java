package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;

import com.computablefacts.Generated;
import com.computablefacts.asterix.RandomString;
import com.computablefacts.asterix.Result;
import com.computablefacts.asterix.View;
import com.computablefacts.decima.problog.AbstractSubgoal.Waiter;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
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

  private static final Logger logger_ = LoggerFactory.getLogger(Solver.class);
  private static final BiFunction<String, Object, Object> rewriteProbabilisticFact_ = (idx, l) -> {

    if (l instanceof Node || !((Literal) l).predicate().name().equals("proba")) {
      return l;
    }

    Literal fact = (Literal) l;

    Preconditions.checkState(fact.terms().size() == 1, "'proba' facts must be of arity 1");

    return new Literal(fact.probability(), fact.predicate().name(),
        newConst(((Const) fact.terms().get(0)).value() + "_" + idx));
  };

  private final AbstractKnowledgeBase kb_;
  private final Map<String, AbstractSubgoal> subgoals_;
  private final Function<Literal, AbstractSubgoal> newSubgoal_;
  private final AbstractFunctions functions_;
  private final boolean trackProofs_;
  private final List<Node> trees_ = new ArrayList<>(); // proofs

  private AbstractSubgoal root_ = null;
  private int maxSampleSize_ = -1;

  @Deprecated
  public Solver(AbstractKnowledgeBase kb) {
    this(kb, new Functions(kb), true, SubgoalMemoryBacked::new);
  }

  public Solver(AbstractKnowledgeBase kb, AbstractFunctions functions) {
    this(kb, functions, true, SubgoalMemoryBacked::new);
  }

  public Solver(AbstractKnowledgeBase kb, AbstractFunctions functions, boolean trackProofs,
      Function<Literal, AbstractSubgoal> newSubgoal) {

    Preconditions.checkNotNull(kb, "kb should not be null");
    Preconditions.checkNotNull(functions, "functions should not be null");
    Preconditions.checkNotNull(newSubgoal, "newSubgoal should not be null");

    kb_ = kb;
    functions_ = functions;
    trackProofs_ = trackProofs;
    subgoals_ = new ConcurrentHashMap<>();
    newSubgoal_ = newSubgoal;
  }

  private static List<List<Literal>> unfold(Node tree) {

    RandomString randomString = new RandomString(5);
    List<List<Literal>> unfolded = new ArrayList<>();
    List<Proof> unfolding = View.of(tree.bodies_).map(body -> new Proof(View.of(body)
        .map(l -> tree.rules_.size() == 1 ? l : rewriteProbabilisticFact_.apply(randomString.nextString(), l)).toList(),
        Sets.newHashSet(tree))).toList(); // (proof, visited trees)

    while (!unfolding.isEmpty()) {

      List<Proof> copyOfUnfolding = new ArrayList<>(unfolding);
      unfolding.clear();

      for (Proof proof : copyOfUnfolding) {

        List<Proof> expanded = new ArrayList<>();
        expanded.add(proof);

        @Var Set<Rule> visited = null;

        for (int i = proof.proof_.size() - 1; i >= 0; i--) {

          Object obj = proof.proof_.get(i);

          if (obj instanceof Node) {

            List<Proof> copyOfExpanded = new ArrayList<>(expanded);
            expanded.clear();

            if (visited == null) {
              visited = new HashSet<>(((Node) obj).rules_);
            } else {
              visited = Sets.intersection(visited, ((Node) obj).rules_);
            }

            for (int k = 0; k < copyOfExpanded.size(); k++) { // TODO : foreach

              Proof p = copyOfExpanded.get(k);
              List<Proof> tmp = p.expand(i, (Node) obj, visited.isEmpty() ? null : randomString.nextString());

              expanded.addAll(tmp);
            }
          }
        }

        View.of(expanded).forEachRemaining(p -> {
          if (p.isUnfolded()) {
            unfolded.add(p.unfoldedProof());
          } else {
            unfolding.add(p);
          }
        });
      }
    }
    return unfolded;
  }

  /**
   * Return the number of subgoals.
   *
   * @return the number of subgoals.
   */
  public int nbSubgoals() {
    return subgoals_.size();
  }

  /**
   * First, sets up and calls the subgoal search procedure. Then, extracts the answers and unfold the proofs. In order
   * to work, subgoals must track proofs i.e. {@code trackProofs_ = true}.
   *
   * @param query goal.
   * @return proofs.
   */
  public Set<AbstractClause> proofs(Literal query) {

    Preconditions.checkNotNull(query, "query should not be null");
    Preconditions.checkState(trackProofs_, "trackProofs must be true on probabilistic settings");

    root_ = newSubgoal_.apply(query);
    subgoals_.put(query.tag(), root_);

    search(root_, 0);

    return View.of(trees_).filter(t -> t.head_.isRelevant(root_.literal()))
        .flatten(tree -> View.of(unfold(tree)).map(proof -> (AbstractClause) new Rule(tree.head_, proof)))
        .concat(View.of(root_.facts()).map(fact -> (AbstractClause) fact)).toSet();
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

    search(root_, 0);
    return root_.facts();
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
  private void search(AbstractSubgoal subgoal, int idx) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");

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

            Preconditions.checkState(trackProofs_, "trackProofs must be true on probabilistic settings");

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
      if (trackProofs_) {

        Literal head = newRule.head();
        List<Object> body = View.of(newRule.body()).map(literal -> {
          Result<Node> fold = View.of(trees_).findFirst(f -> f.head_.isRelevant(literal));
          return fold.mapIfSuccess(f -> (Object) f).mapIfEmpty(() -> literal).getOrThrow();
        }).toList();

        List<Node> nodes = View.of(trees_).filter(node -> node.head_.isRelevant(head)).toList();
        List<Rule> rules = View.of(kb_.rules(head)).filter(r -> r.isRelevant(newRule)).toList();

        Preconditions.checkState(nodes.size() == 0 || nodes.size() == 1);
        Preconditions.checkState(rules.size() == 0 || rules.size() == 1);

        if (nodes.isEmpty()) {
          trees_.add(new Node(rules.isEmpty() ? null : rules.get(0), head, body));
        } else {

          nodes.get(0).bodies_.add(body);

          if (!rules.isEmpty()) {
            nodes.get(0).rules_.add(rules.get(0));
          }
        }

        subgoal.proof(newRule);
      }

      fact(subgoal, new Fact(newRule.head()));
    } else {
      rule(subgoal, newRule, idx + 1);
    }
  }

  final static class Node {

    public final Literal head_;
    public final Set<List<Object>> bodies_ = new HashSet<>();
    public final Set<Rule> rules_ = new HashSet<>();

    public Node(Rule rule, Literal head, List<Object> body) {

      Preconditions.checkNotNull(head, "head should not be null");
      Preconditions.checkNotNull(body, "body should not be null");

      head_ = head;
      bodies_.add(body);

      if (rule != null) {
        rules_.add(rule);
      }
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Node)) {
        return false;
      }
      Node node = (Node) obj;
      return Objects.equals(head_, node.head_) /* && Objects.equals(bodies_, node.bodies_) */;
    }

    @Override
    public int hashCode() {
      return Objects.hash(head_ /* , bodies_ */);
    }

    @Generated
    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("head", head_)/* .add("bodies", bodies_) */.toString();
    }
  }

  final static class Proof {

    public final List<Object> proof_ = new ArrayList<>();
    public final Set<Node> visited_ = new HashSet<>();

    public Proof(List<Object> proof, Set<Node> visited) {

      Preconditions.checkNotNull(proof, "proof should not be null");
      Preconditions.checkNotNull(visited, "visited should not be null");

      proof_.addAll(proof);
      visited_.addAll(visited);
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Proof)) {
        return false;
      }
      Proof proof = (Proof) obj;
      return Objects.equals(proof_, proof.proof_) && Objects.equals(visited_, proof.visited_);
    }

    @Override
    public int hashCode() {
      return Objects.hash(proof_, visited_);
    }

    @Generated
    @Override
    public String toString() {
      return MoreObjects.toStringHelper(this).add("proof", proof_).add("visited2", visited_).toString();
    }

    public boolean isUnfolded() {
      return View.of(proof_).allMatch(l -> l instanceof Literal);
    }

    public List<Literal> unfoldedProof() {

      Preconditions.checkState(isUnfolded());

      return View.of(proof_).map(l -> (Literal) l).toList();
    }

    // Replace tree at position pos by its subtrees
    public List<Proof> expand(int pos, Node tree, String uuid) {

      Preconditions.checkNotNull(tree, "tree should not be null");
      Preconditions.checkArgument(0 <= pos && pos < proof_.size(), "pos must be such as 0 <= pos <= %s", proof_.size());
      Preconditions.checkArgument(proof_.get(pos) instanceof Node, "proof.get(pos) must be a Node");
      Preconditions.checkNotNull(rewriteProbabilisticFact_, "rewriteProbabilisticFact should not be null");

      return View.of(tree.bodies_)
          .filter(proof -> !visited_.contains(tree) || View.of(proof).allMatch(l -> l instanceof Literal))
          .map(proof -> {

            List<Object> prefix = proof_.subList(0, pos);
            List<Object> suffix = proof_.subList(pos + 1, proof_.size());
            List<Object> newProof = View.of(prefix)
                .concat(View.of(proof).map(l -> rewriteProbabilisticFact_.apply((uuid == null ? "" : uuid), l)))
                .concat(suffix).toList();
            Set<Node> newVisited = View.of(visited_).append(tree).toSet();

            return new Proof(newProof, newVisited);
          }).toList();
    }
  }
}