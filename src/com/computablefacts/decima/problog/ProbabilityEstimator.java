package com.computablefacts.decima.problog;

import com.computablefacts.asterix.RandomString;
import com.computablefacts.asterix.View;
import com.computablefacts.decima.robdd.BddManager;
import com.computablefacts.decima.robdd.BddNode;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * See Theofrastos Mantadelis and Gerda Janssens (2010). "Nesting Probabilistic Inference" for details.
 */
@CheckReturnValue
final public class ProbabilityEstimator {

  private final RandomString randomString_ = new RandomString(7);
  private final Set<AbstractClause> proofs_;

  public ProbabilityEstimator(Set<AbstractClause> proofs) {

    Preconditions.checkNotNull(proofs, "proofs should not be null");
    Preconditions.checkArgument(proofs.stream().allMatch(p -> p.isFact() || ((Rule) p).isGrounded()),
        "All proofs should be grounded");

    proofs_ = proofs;
  }

  @Beta
  public Map<Fact, BigDecimal> probabilities() {
    return probabilities(5);
  }

  /**
   * Compute the probability associated with each fact.
   *
   * @param nbSignificantDigits number of significant digits.
   * @return map between facts and probabilities.
   */
  public Map<Fact, BigDecimal> probabilities(int nbSignificantDigits) {

    if (proofs_.isEmpty()) {
      return new HashMap<>();
    }

    Map<Fact, BigDecimal> probabilities = new HashMap<>();

    for (AbstractClause rule : proofs_) {

      Fact fact = new Fact(rule.head());

      if (!probabilities.containsKey(fact)) {
        probabilities.put(fact, probability(fact, nbSignificantDigits));
      }
    }
    return probabilities;
  }

  /**
   * Compute the probability associated with a given fact.
   *
   * @param fact fact.
   * @return probability.
   */
  public BigDecimal probability(Fact fact) {

    Preconditions.checkNotNull(fact, "fact should not be null");

    return probability(fact, 5);
  }

  /**
   * Compute the probability associated with a given fact.
   *
   * @param fact                fact.
   * @param nbSignificantDigits number of significant digits.
   * @return probability.
   */
  public BigDecimal probability(Fact fact, int nbSignificantDigits) {

    Preconditions.checkNotNull(fact, "clause should not be null");
    Preconditions.checkArgument(nbSignificantDigits > 0, "nbSignificantDigits should be > 0");

    if (proofs_.isEmpty()) {
      return BigDecimal.ZERO;
    }

    ProbabilityEstimator estimator = new ProbabilityEstimator(
        proofs_.stream().filter(p -> p.head().tag().equals(fact.head().tag())).collect(Collectors.toSet()));
    BigDecimal probability = estimator.probability();
    int newScale = nbSignificantDigits - probability.precision() + probability.scale();

    return probability.setScale(newScale, RoundingMode.HALF_UP);
  }

  private BigDecimal probability() {

    if (proofs_.isEmpty()) {
      return BigDecimal.ZERO;
    }

    Preconditions.checkArgument(proofs_.stream().map(p -> p.head().tag()).collect(Collectors.toSet()).size() == 1,
        "All proofs should be about the same fact");

    BddManager mgr = new BddManager(10);
    BiMap<BddNode, Literal> bddVars = HashBiMap.create();

    Set<AbstractClause> proofs = proofs_.stream().map(this::rewriteRuleBody)
        .filter(proof -> proof.isFact() || !((Rule) proof).body().isEmpty()).collect(Collectors.toSet());

    proofs.stream().flatMap(p -> p.isFact() ? ImmutableList.of(p.head()).stream() : ((Rule) p).body().stream()).filter(
            literal -> BigDecimal.ONE.compareTo(literal.probability())
                != 0 /* Literals with probability of 1 do not contribute to the final score */).distinct()
        .forEach(literal -> bddVars.put(mgr.create(mgr.createVariable(), mgr.One, mgr.Zero), literal));

    List<BddNode> trees = new ArrayList<>();

    for (AbstractClause proof : proofs) {

      List<Literal> body = proof.isFact() ? ImmutableList.of(proof.head()) : ((Rule) proof).body();
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

    BddNode node = or(mgr, trees);
    // String str = mgr.toDot(node, n -> newBddVars.get(n.index()).toString().replace("\"", ""), true);
    return probability(newBddVars, node);
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

      // Literals with probability of 1 do not contribute to the final score
      if (BigDecimal.ONE.compareTo(literal.probability()) != 0) {
        if (bdd == null) {
          bdd = bddVars.get(literal);
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
   * @return rewritten clause.
   */
  private AbstractClause rewriteRuleBody(AbstractClause clause) {

    Preconditions.checkNotNull(clause, "clause should not be null");

    if (clause.isFact()) {
      return clause;
    }

    Rule rule = (Rule) clause;
    Literal head = rule.head();
    List<Literal> body = new ArrayList<>(rule.body().size());

    for (Literal literal : rule.body()) {
      if (literal.predicate().baseName().equals("proba")) {

        Preconditions.checkState(literal.terms().size() == 1, "'proba' facts must be of arity 1");

        String predicate = ((Const) literal.terms().get(0)).value().toString();
        body.add(new Literal(literal.probability(), (literal.predicate().isNegated() ? "~" : "") + predicate,
            literal.terms()));

      } else if (BigDecimal.ONE.compareTo(literal.probability()) != 0) {
        body.add(literal);
      }
    }
    return new Rule(head, body);
  }
}