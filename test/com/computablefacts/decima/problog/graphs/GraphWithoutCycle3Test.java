package com.computablefacts.decima.problog.graphs;

import static com.computablefacts.decima.problog.Parser.parseFact;
import static com.computablefacts.decima.problog.Parser.parseQuery;
import static com.computablefacts.decima.problog.Parser.parseRule;
import static com.computablefacts.decima.problog.TestUtils.checkAnswers;

import com.computablefacts.decima.problog.AbstractClause;
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
 * Extracted from https://dtai.cs.kuleuven.be/problog/tutorial/basic/04_pgraph.html
 */
public class GraphWithoutCycle3Test {

  @Test
  public void testGraph() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseFact("0.6::edge(1, 2)."));
    kb.azzert(parseFact("0.1::edge(1, 3)."));
    kb.azzert(parseFact("0.4::edge(2, 5)."));
    kb.azzert(parseFact("0.3::edge(2, 6)."));
    kb.azzert(parseFact("0.3::edge(3, 4)."));
    kb.azzert(parseFact("0.8::edge(4, 5)."));
    kb.azzert(parseFact("0.2::edge(5, 6)."));

    // Init kb with rules
    kb.azzert(parseRule("path(X, Y) :- edge(X, Y)."));
    kb.azzert(parseRule("path(X, Y) :- path(X, Z), edge(Z, Y)."));

    // Query kb
    // path(1, 6)?
    Solver solver = new Solver(kb);
    Literal query = parseQuery("path(1, _)?");
    Set<AbstractClause> proofs = solver.proofs(query);
    Set<AbstractClause> answers = Sets.newHashSet(solver.solve(query));

    // Verify subgoals
    Assert.assertEquals(7, solver.nbSubgoals());

    // Verify answers
    // path(1, 6) :- 0.6::edge(1, 2), 0.3::edge(2, 6).
    // path(1, 6) :- 0.6::edge(1, 2), 0.4::edge(2, 5), 0.2::edge(5, 6).
    // path(1, 6) :- 0.1::edge(1, 3), 0.3::edge(3, 4), 0.8::edge(4, 5), 0.2::edge(5, 6).
    Assert.assertEquals(8, proofs.size());
    Assert.assertEquals(5, answers.size());

    Rule answer1 = parseRule("path(1, 6) :- 0.6::edge(1, 2), 0.3::edge(2, 6).");
    Rule answer2 = parseRule("path(1, 6) :- 0.6::edge(1, 2), 0.4::edge(2, 5), 0.2::edge(5, 6).");
    Rule answer3 = parseRule("path(1, 6) :- 0.1::edge(1, 3), 0.3::edge(3, 4), 0.8::edge(4, 5), 0.2::edge(5, 6).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2, answer3)));

    // Verify BDD answer
    // 0.2167296::path(1, 6).
    // 0.25824::path(1, 5).
    ProbabilityEstimator estimator = new ProbabilityEstimator(proofs);
    BigDecimal probability1 = estimator.probability(parseFact("path(1, 6)."), 7);
    BigDecimal probability2 = estimator.probability(parseFact("path(1, 5)."), 7);

    Assert.assertEquals(0, BigDecimal.valueOf(0.2167296).compareTo(probability1));
    Assert.assertEquals(0, BigDecimal.valueOf(0.25824).compareTo(probability2));
  }
}