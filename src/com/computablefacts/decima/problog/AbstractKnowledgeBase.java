package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;
import static com.computablefacts.decima.problog.Parser.reorderBodyLiterals;

import com.computablefacts.asterix.RandomString;
import com.computablefacts.asterix.View;
import com.computablefacts.decima.robdd.Pair;
import com.google.common.annotations.Beta;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.errorprone.annotations.CheckReturnValue;
import com.google.errorprone.annotations.Var;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

/**
 * This class allows us to be agnostic from the storage layer. It is used to assert facts and rules.
 */
@CheckReturnValue
public abstract class AbstractKnowledgeBase {

  private final RandomString randomString_ = new RandomString(7);

  public AbstractKnowledgeBase() {
  }

  @Override
  public String toString() {

    StringBuilder builder = new StringBuilder();
    Iterator<Fact> facts = facts();
    Iterator<Rule> rules = rules();

    while (facts.hasNext()) {
      builder.append(facts.next().toString());
      builder.append("\n");
    }
    while (rules.hasNext()) {
      builder.append(rules.next().toString());
      builder.append("\n");
    }
    return builder.toString();
  }

  /**
   * Add a new fact or rule to the database. There are two assumptions here :
   *
   * <ul>
   * <li>facts and rules predicates must not overlap ;</li>
   * <li>a rule body literals should not have probabilities attached.</li>
   * </ul>
   *
   * @param clause fact or rule.
   */
  public void azzert(AbstractClause clause) {

    Preconditions.checkNotNull(clause, "clause should not be null");

    if (clause.head().predicate().isPrimitive()) {
      return; // Ignore assertions for primitives
    }

    BigDecimal probability = clause.head().probability();

    Preconditions.checkState(!BigDecimal.ZERO.equals(probability), "head probability must be != 0.0 : %s", clause);

    if (clause.isFact()) {
      azzertFact((Fact) clause);
    } else {

      Rule rule = (Rule) clause;

      Preconditions.checkArgument(rule.isSafe(), "clause should be safe : %s", rule);

      // Remove probability from the clause head (otherwise it is a no-op)
      Pair<Rule, Fact> clauses = rewriteProbabilisticRule(rule);

      if (clauses.u != null) {
        azzertFact(clauses.u); // Assert created fact (if any)
      }

      Rule newRule = clauses.t; // Assert rewritten clause

      for (int i = 0; i < newRule.body().size(); i++) {

        Literal literal = newRule.body().get(i);

        Preconditions.checkState(BigDecimal.ONE.equals(literal.probability()),
            "body literals should not have probabilities attached : %s", newRule);
      }

      azzertRule(newRule);
    }
  }

  /**
   * Adds new facts or rules to the database.
   *
   * @param clauses facts or rules.
   */
  public void azzert(Set<? extends AbstractClause> clauses) {

    Preconditions.checkNotNull(clauses, "clauses should not be null");

    clauses.forEach(this::azzert);
  }

  protected abstract void azzertFact(@NotNull Fact fact);

  protected abstract void azzertRule(@NotNull Rule rule);

  protected abstract Iterator<Fact> facts(@NotNull Literal literal);

  protected abstract Iterator<Rule> rules(@NotNull Literal literal);

  public abstract Iterator<Fact> facts();

  public abstract Iterator<Rule> rules();

  public long nbFacts(@NotNull Literal literal) {
    return Iterators.size(facts(literal));
  }

  public long nbRules(@NotNull Literal literal) {
    return Iterators.size(rules(literal));
  }

  public long nbFacts() {
    return Iterators.size(facts());
  }

  public long nbRules() {
    return Iterators.size(rules());
  }

  @Beta
  public List<Rule> compact() {

    // Find all rules that do not reference another rule (if any)
    Map.Entry<List<Rule>, List<Rule>> rules = View.of(rules()).divide(rule -> rule.body().stream()
        .noneMatch(literal -> View.of(rules()).map(Rule::head).anyMatch(head -> head.isRelevant(literal))));
    List<Rule> dontReferenceOtherRules = rules.getKey();
    List<Rule> referenceOtherRules = rules.getValue();

    // Inline all rules that are not referenced by another rule
    List<Rule> newRules = referenceOtherRules.stream().flatMap(rule -> {

      @Var List<Rule> list = Lists.newArrayList(rule);

      while (true) {

        List<Rule> newList = new ArrayList<>();

        for (int i = 0; newList.isEmpty() && i < list.size(); i++) {

          Rule clause = list.get(i);

          for (int j = 0; newList.isEmpty() && j < clause.body().size(); j++) {

            int pos = j;
            Literal literal = clause.body().get(pos);

            newList.addAll(dontReferenceOtherRules.stream().filter(r -> r.head().isRelevant(literal)).map(or -> {

              Rule referencingRule = clause.rename();
              Rule referencedRule = or.rename();
              Rule newRule = mergeRules(referencingRule, referencedRule, pos);

              return (Rule) reorderBodyLiterals(newRule);
            }).collect(Collectors.toList()));
          }
        }
        if (newList.isEmpty()) {
          break;
        }
        list = newList;
      }
      return list.stream();
    }).collect(Collectors.toList());

    newRules.addAll(dontReferenceOtherRules);
    return newRules;
  }

  /**
   * Perform the following actions :
   *
   * <ul>
   * <li>Remove probability from the clause head ;</li>
   * <li>Create a random fact name with the clause head probability ;</li>
   * <li>Add a reference to the newly created fact in the clause body.</li>
   * </ul>
   *
   * @param rule clause.
   * @return a {@link Pair} with {@link Pair#t} containing the rewritten clause and {@link Pair#u} the newly created
   * fact.
   */
  protected Pair<Rule, Fact> rewriteProbabilisticRule(Rule rule) {

    Preconditions.checkNotNull(rule, "clause should not be null");

    Literal head = rule.head();

    if (BigDecimal.ONE.compareTo(head.probability()) == 0) {
      return new Pair<>(rule, null);
    }

    Preconditions.checkState(!head.predicate().isNegated(), "the rule head should not be negated : %s", rule);

    String predicate = head.predicate().name();
    BigDecimal probability = head.probability();

    // Create fact
    String uuid = randomString_.nextString().toLowerCase();
    String newPredicate = "proba";
    Literal newLiteral = new Literal(probability, newPredicate, newConst(uuid));
    Fact newFact = new Fact(newLiteral);

    // Rewrite clause
    Literal newHead = new Literal(predicate, rule.head().terms());
    List<Literal> newBody = new ArrayList<>(rule.body());
    newBody.add(new Literal(newPredicate, newConst(uuid)));
    Rule newRule = new Rule(newHead, newBody);

    return new Pair<>(newRule, newFact);
  }

  @Beta
  protected Rule mergeRules(Rule referencingRule, Rule referencedRule, int pos) {

    Preconditions.checkNotNull(referencingRule, "referencingRule should not be null");
    Preconditions.checkNotNull(referencedRule, "referencedRule should not be null");
    Preconditions.checkArgument(pos >= 0 && pos < referencingRule.body().size(), "pos must be such as 0 <= pos < %s",
        referencingRule.body().size());

    Map<com.computablefacts.decima.problog.Var, AbstractTerm> env = referencedRule.head()
        .unify(referencingRule.body().get(pos));

    Preconditions.checkState(env != null, "env should not be null");

    Rule newReferencedRule = referencedRule.subst(env);
    Rule newReferencingRule = referencingRule.subst(env);

    Literal newHead = newReferencingRule.head();
    List<Literal> newBody = new ArrayList<>();

    for (int k = 0; k < newReferencingRule.body().size(); k++) {
      if (k != pos) {
        newBody.add(newReferencingRule.body().get(k));
      } else {
        newBody.addAll(newReferencedRule.body());
      }
    }
    return new Rule(newHead, newBody);
  }
}