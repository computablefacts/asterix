package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;

import com.computablefacts.Generated;
import com.computablefacts.asterix.RandomString;
import com.computablefacts.asterix.Result;
import com.computablefacts.asterix.View;
import com.computablefacts.decima.robdd.BddManager;
import com.computablefacts.decima.robdd.BddNode;
import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Beta
@CheckReturnValue
final public class Proofer extends Solver {

  private static final Logger logger_ = LoggerFactory.getLogger(Proofer.class);
  private static final BiFunction<String, Object, Object> rewriteProbabilisticFact_ = (idx, l) -> {

    if (l instanceof Node || !((Literal) l).predicate().name().equals("proba")) {
      return l;
    }

    Literal fact = (Literal) l;

    Preconditions.checkState(fact.terms().size() == 1, "'proba' facts must be of arity 1");

    return new Literal(fact.probability(), fact.predicate().name(),
        newConst(((Const) fact.terms().get(0)).value() + "_" + idx));
  };

  private final List<Node> trees_ = new ArrayList<>(); // proofs

  @Deprecated
  public Proofer(AbstractKnowledgeBase kb) {
    super(kb);
  }

  public Proofer(AbstractKnowledgeBase kb, AbstractFunctions functions) {
    super(kb, functions);
  }

  public Proofer(AbstractKnowledgeBase kb, AbstractFunctions functions, Function<Literal, AbstractSubgoal> newSubgoal) {
    super(kb, functions, newSubgoal);
  }

  /**
   * Calls the subgoal search procedure.
   *
   * @param query         goal.
   * @param maxSampleSize must be -1.
   * @return facts answering the query.
   */
  @Override
  public Iterator<Fact> solve(Literal query, int maxSampleSize) {
    return super.solve(query, -1 /* override parameter otherwise proofs will be missing */);
  }

  @Override
  protected void trackProofs(AbstractSubgoal subgoal, Rule rule) {

    Preconditions.checkNotNull(subgoal, "subgoal should not be null");
    Preconditions.checkNotNull(rule, "rule should not be null");

    Literal head = rule.head();
    List<Object> body = View.of(rule.body()).map(literal -> {
      Result<Node> fold = View.of(trees_).findFirst(f -> f.head_.isRelevant(literal));
      return fold.map(f -> (Object) f).mapIfEmpty(() -> literal).getOrThrow();
    }).toList();

    List<Node> nodes = View.of(trees_).filter(node -> node.head_.isRelevant(head)).toList();
    List<Rule> rules = View.of(kb_.rules(head)).filter(r -> r.isRelevant(rule)).toList();

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

    subgoal.proof(rule);
  }

  /**
   * First, we remark that the DNF formula describing sets of proofs is monotone, meaning that adding more proofs will
   * never decrease the probability of the formula being true. Thus, formulae describing subsets of the full set of
   * proofs of a query will always give a lower bound on the query’s success probability.
   * <p>
   * Then, the probability of a proof `b1 ∧ . . . ∧ bn` will always be at most the probability of an arbitrary prefix
   * `b1 ∧ . . . ∧ bi`, `i ≤ n`. As disjoining sets of proofs, i.e. including information on facts that are not elements
   * of the subprograms described by a certain proof, can only decrease the contribution of single proofs, this upper
   * bound carries over to a set of proofs or partial proofs, as long as prefixes for all possible proofs are included.
   *
   * @param query               goal.
   * @param nbSignificantDigits number of significant digits.
   * @return a probability estimation.
   */
  public Result<BigDecimal> probability(Literal query, int nbSignificantDigits) {

    Preconditions.checkNotNull(query, "query should not be null");
    Preconditions.checkArgument(query.isGrounded(), "query should be grounded");
    Preconditions.checkArgument(nbSignificantDigits > 0, "nbSignificantDigits should be > 0");

    List<List<Literal>> facts = View.of(solve(query, -1))
        .filter(fact -> BigDecimal.ZERO.compareTo(fact.head().probability()) != 0)
        .map(fact -> (List<Literal>) Lists.newArrayList(fact.head())).toList();

    List<List<Literal>> proofs = View.of(trees_).findFirst(tree -> tree.head_.isRelevant(root_.literal()))
        .map(proof -> View.of(unfold(proof)).concat(facts).toList()).mapIfFailure(t -> facts).mapIfEmpty(() -> facts)
        .getOrThrow();

    BigDecimal upperBound = probability(query, proofs);

    int newScale = nbSignificantDigits - upperBound.precision() + upperBound.scale();
    return Result.success(upperBound.setScale(newScale, RoundingMode.HALF_UP));
  }

  private List<List<Literal>> unfold(Node tree) {

    Preconditions.checkNotNull(tree, "tree should not be null");

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

            for (Proof p : copyOfExpanded) {
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

  private BigDecimal probability(Literal query, List<List<Literal>> proofs) {

    Preconditions.checkNotNull(query, "query should not be null");
    Preconditions.checkNotNull(proofs, "proofs should not be null");
    Preconditions.checkArgument(query.isGrounded(), "query should be grounded");

    if (proofs.isEmpty()) {
      return BigDecimal.ZERO;
    }

    BddManager mgr = new BddManager(10);
    BiMap<BddNode, Literal> bddVars = HashBiMap.create();
    Set<AbstractClause> clauses = View.of(proofs)
        .map(proof -> proof.size() == 1 ? new Fact(proof.get(0)) : new Rule(query, proof)).map(this::rewriteClause)
        .filter(clause -> (clause.isFact() && BigDecimal.ONE.compareTo(clause.head().probability())
            != 0 /* Literals with probability of 1 do not contribute to the final score */) || (clause.isRule()
            && ((Rule) clause).body().size() > 0)).peek(clause -> {

          List<Literal> literals = clause.isFact() ? ImmutableList.of(clause.head()) : ((Rule) clause).body();
          View.of(literals).filter(literal -> literal.predicate().isNegated() ? !bddVars.containsValue(literal.negate())
              : !bddVars.containsValue(literal)).forEachRemaining(literal -> {
            if (literal.predicate().isNegated()) {
              bddVars.put(mgr.create(mgr.createVariable(), mgr.One, mgr.Zero), literal.negate());
            } else {
              bddVars.put(mgr.create(mgr.createVariable(), mgr.One, mgr.Zero), literal);
            }
          });

        }).toSet();

    List<BddNode> trees = new ArrayList<>();

    for (AbstractClause clause : clauses) {

      List<Literal> body = clause.isFact() ? ImmutableList.of(clause.head()) : ((Rule) clause).body();
      BddNode bddNode = and(mgr, bddVars.inverse(), body);

      if (bddNode != null) {
        trees.add(bddNode);
      }
    }

    if (trees.isEmpty()) {
      return BigDecimal.ONE;
    }

    BiMap<Integer, Literal> newBddVars = HashBiMap.create();
    View.of(bddVars).forEachRemaining(var -> newBddVars.put(var.getKey().index(), var.getValue()));

    BddNode node = mgr.reduce(or(mgr, trees));
    // String str = mgr.toDot(mgr.reduce(node), n -> newBddVars.get(n.index()).toString().replace("\"", ""), true);
    return probability(newBddVars, mgr.reduce(node));
  }

  private BigDecimal probability(BiMap<Integer, Literal> bddVars, BddNode node) {

    Preconditions.checkNotNull(bddVars, "bddVars should not be null");
    Preconditions.checkNotNull(node, "node should not be null");

    if (node.isOne()) {
      return BigDecimal.ONE;
    }
    if (node.isZero()) {
      return BigDecimal.ZERO;
    }

    BigDecimal probH = probability(bddVars, node.high());
    BigDecimal probL = probability(bddVars, node.low());

    BigDecimal probability = bddVars.get(node.index()).probability();
    return probability.multiply(probH).add(BigDecimal.ONE.subtract(probability).multiply(probL));
  }

  private BddNode and(BddManager mgr, BiMap<Literal, BddNode> bddVars, List<Literal> body) {

    Preconditions.checkNotNull(mgr, "mgr should not be null");
    Preconditions.checkNotNull(bddVars, "bddVars should not be null");
    Preconditions.checkNotNull(body, "body should not be null");
    Preconditions.checkArgument(!body.isEmpty(), "body should not be empty");

    @Var BddNode bdd = null;

    for (int i = 0; i < body.size(); i++) {

      Literal literal = body.get(i);

      if (bdd == null) {
        if (literal.predicate().isNegated()) {
          bdd = mgr.negate(bddVars.get(literal.negate()));
        } else {
          bdd = bddVars.get(literal);
        }
      } else {
        if (literal.predicate().isNegated()) {
          bdd = mgr.and(bdd, mgr.negate(bddVars.get(literal.negate())));
        } else {
          bdd = mgr.and(bdd, bddVars.get(literal));
        }
      }
    }
    return bdd;
  }

  private BddNode or(BddManager mgr, List<BddNode> trees) {

    Preconditions.checkNotNull(mgr, "mgr should not be null");
    Preconditions.checkNotNull(trees, "trees should not be null");
    Preconditions.checkArgument(!trees.isEmpty(), "trees should not be empty");

    @Var BddNode bdd = null;

    for (int i = 0; i < trees.size(); i++) {
      if (bdd == null) {
        bdd = trees.get(i);
      } else {
        bdd = mgr.or(bdd, trees.get(i));
      }
    }
    return bdd;
  }

  /**
   * Replace all probabilistic literals created by {@link AbstractKnowledgeBase#rewriteProbabilisticRule(Rule)} with a
   * unique literal with the same probability.
   *
   * @param clause a fact or a rule.
   * @return a rewritten clause.
   */
  private AbstractClause rewriteClause(AbstractClause clause) {

    Preconditions.checkNotNull(clause, "clause should not be null");

    if (clause.isFact()) {
      return clause;
    }

    Rule rule = (Rule) clause;
    Literal head = rule.head();
    List<Literal> body = new ArrayList<>(rule.body().size());

    for (Literal literal : rule.body()) {
      if (BigDecimal.ONE.compareTo(literal.probability()) == 0) {
        continue; // Literals with probability of 1 do not contribute to the final score
      }

      Preconditions.checkState(BigDecimal.ZERO.compareTo(literal.probability()) != 0,
          "0 is an invalid fact's probability : %s", rule);

      if (!literal.predicate().baseName().equals("proba")) {
        body.add(literal);
      } else {
        Preconditions.checkState(!literal.predicate().isNegated(), "'proba' facts must not be negated");
        Preconditions.checkState(literal.terms().size() == 1, "'proba' facts must be of arity 1");

        String predicate = ((Const) literal.terms().get(0)).value().toString();
        body.add(new Literal(literal.probability(), predicate, literal.terms()));
      }
    }
    return new Rule(head, body);
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
