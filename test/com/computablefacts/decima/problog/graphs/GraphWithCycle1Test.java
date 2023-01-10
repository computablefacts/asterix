package com.computablefacts.decima.problog.graphs;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;
import static com.computablefacts.decima.problog.Parser.parseFact;
import static com.computablefacts.decima.problog.Parser.parseRule;
import static com.computablefacts.decima.problog.TestUtils.checkAnswers;

import com.computablefacts.decima.problog.AbstractClause;
import com.computablefacts.decima.problog.Fact;
import com.computablefacts.decima.problog.KnowledgeBaseMemoryBacked;
import com.computablefacts.decima.problog.Literal;
import com.computablefacts.decima.problog.ProbabilityEstimator;
import com.computablefacts.decima.problog.Rule;
import com.computablefacts.decima.problog.Solver;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

/**
 * Extracted from Mantadelis, Theofrastos &amp; Janssens, Gerda. (2010). "Dedicated Tabling for a Probabilistic
 * Setting". Technical Communications of ICLP. 7. 124-133. 10.4230/LIPIcs.ICLP.2010.124.
 */
public class GraphWithCycle1Test {

  @Test
  public void testGraph() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseFact("0.9::edge(1, 2)."));//
    // kb.azzert(parseFact("0.9::edge(2, 1)."));
    kb.azzert(parseFact("0.2::edge(5, 4)."));//
    kb.azzert(parseFact("0.4::edge(6, 5)."));//
    // kb.azzert(parseFact("0.4::edge(5, 6)."));
    // kb.azzert(parseFact("0.2::edge(4, 5)."));
    kb.azzert(parseFact("0.8::edge(2, 3)."));//
    // kb.azzert(parseFact("0.8::edge(3, 2)."));
    kb.azzert(parseFact("0.7::edge(1, 6)."));//
    kb.azzert(parseFact("0.5::edge(2, 6)."));//
    kb.azzert(parseFact("0.5::edge(6, 2)."));//
    // kb.azzert(parseFact("0.7::edge(6, 1)."));
    kb.azzert(parseFact("0.7::edge(5, 3)."));//
    kb.azzert(parseFact("0.7::edge(3, 5)."));//
    kb.azzert(parseFact("0.6::edge(3, 4)."));//
    // kb.azzert(parseFact("0.6::edge(4, 3)."));

    // Init kb with rules
    kb.azzert(parseRule("path(X, Y) :- edge(X, Y)."));
    kb.azzert(parseRule("path(X, Y) :- edge(X, Z), fn_is_false(fn_eq(Z, Y)),  path(Z, Y)."));

    // Query kb
    // path(1, 4)?
    Solver solver = new Solver(kb);
    Literal query = new Literal("path", newConst("1"), newConst("4"));
    Set<AbstractClause> proofs = solver.proofs(query);
    Set<AbstractClause> answers = Sets.newHashSet(solver.solve(query));

    // Verify subgoals
    Assert.assertEquals(18, solver.nbSubgoals());

    // Verify proofs
    // Assert.assertEquals(13, proofs.size());
    Assert.assertEquals(1, answers.size());

    Rule answer1 = parseRule(
        "path(1, 4) :- 0.7::edge(1, 6), 0.5::edge(6, 2), 0.8::edge(2, 3), 0.7::edge(3, 5), 0.2::edge(5, 4), fn_eq(false, 5, 4), fn_is_false(false), fn_eq(false, 3, 4), fn_is_false(false), fn_eq(false, 2, 4), fn_is_false(false), fn_eq(false, 6, 4), fn_is_false(false).");
    Rule answer2 = parseRule(
        "path(1, 4) :- 0.9::edge(1, 2), 0.5::edge(2, 6), 0.4::edge(6, 5), 0.7::edge(5, 3), 0.6::edge(3, 4), fn_eq(false, 3, 4), fn_is_false(false), fn_eq(false, 5, 4), fn_is_false(false), fn_eq(false, 6, 4), fn_is_false(false), fn_eq(false, 2, 4), fn_is_false(false).");
    Rule answer3 = parseRule(
        "path(1, 4) :- 0.7::edge(1, 6), 0.4::edge(6, 5), 0.7::edge(5, 3), 0.7::edge(3, 5), 0.2::edge(5, 4), fn_eq(false, 5, 4), fn_is_false(false), fn_eq(false, 3, 4), fn_is_false(false), fn_eq(false, 5, 4), fn_is_false(false), fn_eq(false, 6, 4), fn_is_false(false).");
    Rule answer4 = parseRule(
        "path(1, 4) :- 0.9::edge(1, 2), 0.5::edge(2, 6), 0.4::edge(6, 5), 0.7::edge(5, 3), 0.7::edge(3, 5), 0.2::edge(5, 4), fn_eq(false, 5, 4), fn_is_false(false), fn_eq(false, 3, 4), fn_is_false(false), fn_eq(false, 5, 4), fn_is_false(false), fn_eq(false, 6, 4), fn_is_false(false), fn_eq(false, 2, 4), fn_is_false(false).");
    Rule answer5 = parseRule(
        "path(1, 4) :- 0.7::edge(1, 6), 0.4::edge(6, 5), 0.7::edge(5, 3), 0.6::edge(3, 4), fn_eq(false, 3, 4), fn_is_false(false), fn_eq(false, 5, 4), fn_is_false(false), fn_eq(false, 6, 4), fn_is_false(false).");
    Rule answer6 = parseRule(
        "path(1, 4) :- 0.9::edge(1, 2), 0.8::edge(2, 3), 0.7::edge(3, 5), 0.2::edge(5, 4), fn_eq(false, 5, 4), fn_is_false(false), fn_eq(false, 3, 4), fn_is_false(false), fn_eq(false, 2, 4), fn_is_false(false).");
    Rule answer7 = parseRule(
        "path(1, 4) :- 0.9::edge(1, 2), 0.5::edge(2, 6), 0.4::edge(6, 5), 0.2::edge(5, 4), fn_eq(false, 5, 4), fn_is_false(false), fn_eq(false, 6, 4), fn_is_false(false), fn_eq(false, 2, 4), fn_is_false(false).");
    Rule answer8 = parseRule(
        "path(1, 4) :- 0.7::edge(1, 6), 0.5::edge(6, 2), 0.5::edge(2, 6), 0.4::edge(6, 5), 0.7::edge(5, 3), 0.6::edge(3, 4), fn_eq(false, 3, 4), fn_is_false(false), fn_eq(false, 5, 4), fn_is_false(false), fn_eq(false, 6, 4), fn_is_false(false), fn_eq(false, 2, 4), fn_is_false(false), fn_eq(false, 6, 4), fn_is_false(false).");
    Rule answer9 = parseRule(
        "path(1, 4) :- 0.7::edge(1, 6), 0.5::edge(6, 2), 0.5::edge(2, 6), 0.4::edge(6, 5), 0.7::edge(5, 3), 0.7::edge(3, 5), 0.2::edge(5, 4), fn_eq(false, 5, 4), fn_is_false(false), fn_eq(false, 3, 4), fn_is_false(false), fn_eq(false, 5, 4), fn_is_false(false), fn_eq(false, 6, 4), fn_is_false(false), fn_eq(false, 2, 4), fn_is_false(false), fn_eq(false, 6, 4), fn_is_false(false).");
    Rule answer10 = parseRule(
        "path(1, 4) :- 0.9::edge(1, 2), 0.8::edge(2, 3), 0.6::edge(3, 4), fn_eq(false, 3, 4), fn_is_false(false), fn_eq(false, 2, 4), fn_is_false(false).");
    Rule answer11 = parseRule(
        "path(1, 4) :- 0.7::edge(1, 6), 0.5::edge(6, 2), 0.5::edge(2, 6), 0.4::edge(6, 5), 0.2::edge(5, 4), fn_eq(false, 5, 4), fn_is_false(false), fn_eq(false, 6, 4), fn_is_false(false), fn_eq(false, 2, 4), fn_is_false(false), fn_eq(false, 6, 4), fn_is_false(false).");
    Rule answer12 = parseRule(
        "path(1, 4) :- 0.7::edge(1, 6), 0.5::edge(6, 2), 0.8::edge(2, 3), 0.6::edge(3, 4), fn_eq(false, 3, 4), fn_is_false(false), fn_eq(false, 2, 4), fn_is_false(false), fn_eq(false, 6, 4), fn_is_false(false).");
    Rule answer13 = parseRule(
        "path(1, 4) :- 0.7::edge(1, 6), 0.4::edge(6, 5), 0.2::edge(5, 4), fn_eq(false, 5, 4), fn_is_false(false), fn_eq(false, 6, 4), fn_is_false(false).");

    Assert.assertTrue(checkAnswers(answers,
        Sets.newHashSet(answer1, answer2, answer3, answer4, answer5, answer6, answer7, answer8, answer9, answer10,
            answer11, answer12, answer13)));
    // Assert
    // .assertTrue(checkProofs(tries, Sets.newHashSet(answer1, answer2, answer3, answer4, answer5,
    // answer6, answer7, answer8, answer9, answer10, answer11, answer12, answer13), true));

    // Verify BDD answer
    // 0.53864::path(1, 4).
    ProbabilityEstimator estimator = new ProbabilityEstimator(proofs);
    BigDecimal probability = estimator.probability(new Fact(query), 5);

    Assert.assertEquals(0, BigDecimal.valueOf(0.53864).compareTo(probability));
  }
}