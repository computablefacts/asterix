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
 * Extracted from Angelika Kimmig, Bart Demoen and Luc De Raedt (2010). "On the Implementation of the Probabilistic
 * Logic Programming Language ProbLog"
 */
public class GraphWithoutCycle2Test {

  @Test
  public void testGraph() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseFact("0.8::edge(a, c)."));
    kb.azzert(parseFact("0.6::edge(b, c)."));
    kb.azzert(parseFact("0.7::edge(a, b)."));
    kb.azzert(parseFact("0.9::edge(c, d)."));
    kb.azzert(parseFact("0.8::edge(c, e)."));
    kb.azzert(parseFact("0.5::edge(e, d)."));

    // Init kb with rules
    kb.azzert(parseRule("path(X, Y) :- edge(X, Y)."));
    kb.azzert(parseRule("path(X, Y) :- path(X, Z), edge(Z, Y)."));

    // Query kb
    // path(a, d)?
    Solver solver = new Solver(kb);
    Literal query1 = new Literal("path", newConst("a"), newConst("d"));
    Set<AbstractClause> proofs1 = solver.proofs(query1);
    Set<AbstractClause> answers1 = Sets.newHashSet(solver.solve(query1));

    // Verify subgoals
    Assert.assertEquals(12, solver.nbSubgoals());

    // Verify answers
    Assert.assertEquals(4, proofs1.size());
    Assert.assertEquals(1, answers1.size());

    Rule answer1 = parseRule("path(a, d) :- 0.8::edge(a, c), 0.9::edge(c, d).");
    Rule answer2 = parseRule("path(a, d) :- 0.7::edge(a, b), 0.6::edge(b, c), 0.9::edge(c, d).");
    Rule answer3 = parseRule("path(a, d) :- 0.8::edge(a, c), 0.8::edge(c, e), 0.5::edge(e, d).");
    Rule answer4 = parseRule("path(a, d) :- 0.7::edge(a, b), 0.6::edge(b, c), 0.8::edge(c, e), 0.5::edge(e, d).");

    Assert.assertTrue(checkAnswers(answers1, Sets.newHashSet(answer1, answer2, answer3, answer4)));

    // Verify BDD answer
    // 0.83096::path(a, d).
    ProbabilityEstimator estimator1 = new ProbabilityEstimator(proofs1);
    BigDecimal probability1 = estimator1.probability(new Fact(query1), 5);

    Assert.assertTrue(BigDecimal.valueOf(0.83096).compareTo(probability1) == 0);

    // Query kb
    // path(c, d)?
    Literal query2 = new Literal("path", newConst("c"), newConst("d"));
    Set<AbstractClause> proofs2 = solver.proofs(query2);
    Set<AbstractClause> answers2 = Sets.newHashSet(solver.solve(query2));

    // Verify proofs
    Assert.assertEquals(2, proofs2.size());
    Assert.assertEquals(1, answers2.size());

    Rule answer5 = parseRule("path(c, d) :- 0.8::edge(c, e), 0.5::edge(e, d).");
    Rule answer6 = parseRule("path(c, d) :- 0.9::edge(c, d).");

    Assert.assertTrue(checkAnswers(answers2, Sets.newHashSet(answer5, answer6)));

    // Verify BDD answer
    // 0.94::path(c, d).
    ProbabilityEstimator estimator2 = new ProbabilityEstimator(proofs2);
    BigDecimal probability2 = estimator2.probability(new Fact(query2), 2);

    Assert.assertTrue(BigDecimal.valueOf(0.94).compareTo(probability2) == 0);
  }
}