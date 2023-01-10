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
public class GraphWithCycle2Test {

  @Test
  public void testGraph() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseFact("0.1::edge(1, 2)."));
    kb.azzert(parseFact("0.5::edge(1, 3)."));
    kb.azzert(parseFact("0.7::edge(3, 1)."));
    kb.azzert(parseFact("0.3::edge(2, 3)."));
    kb.azzert(parseFact("0.2::edge(3, 2)."));
    kb.azzert(parseFact("0.6::edge(2, 4)."));

    // Init kb with rules
    kb.azzert(parseRule("path(X, Y) :- edge(X, Y)."));
    kb.azzert(parseRule("path(X, Y) :- path(X, Z), fn_eq(U, X, Z), fn_is_false(U), edge(Z, Y)."));

    // Query kb
    // path(1, 4)?
    Solver solver = new Solver(kb);
    Literal query = new Literal("path", newConst("1"), newConst("4"));
    Set<AbstractClause> proofs = solver.proofs(query);
    Set<AbstractClause> answers = Sets.newHashSet(solver.solve(query));

    // Verify subgoals
    Assert.assertEquals(10, solver.nbSubgoals());

    // Verify proofs
    Assert.assertEquals(3, proofs.size());
    Assert.assertEquals(1, answers.size());

    Rule answer1 = parseRule(
        "path(1, 4) :- 0.6::edge(2, 4), 0.2::edge(3, 2), 0.5::edge(1, 3), fn_eq(false, 1, 3), fn_is_false(false), fn_eq(false, 1, 2), fn_is_false(false).");
    Rule answer2 = parseRule("path(1, 4) :- 0.6::edge(2, 4), 0.1::edge(1, 2), fn_eq(false, 1, 2), fn_is_false(false).");
    Rule answer3 = parseRule(
        "path(1, 4) :- 0.6::edge(2, 4), 0.2::edge(3, 2), 0.3::edge(2, 3), 0.1::edge(1, 2), fn_eq(false, 1, 2), fn_is_false(false), fn_eq(false, 1, 3), fn_is_false(false), fn_eq(false, 1, 2), fn_is_false(false).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2, answer3)));

    // Verify BDD answer
    // 0.114::path(1, 4).
    ProbabilityEstimator estimator = new ProbabilityEstimator(proofs);
    BigDecimal probability = estimator.probability(new Fact(query), 3);

    Assert.assertEquals(0, BigDecimal.valueOf(0.114).compareTo(probability));
  }
}