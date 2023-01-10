package com.computablefacts.decima.problog.graphs;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;
import static com.computablefacts.decima.problog.Parser.parseFact;
import static com.computablefacts.decima.problog.Parser.parseRule;
import static com.computablefacts.decima.problog.TestUtils.checkAnswers;

import com.computablefacts.decima.problog.AbstractClause;
import com.computablefacts.decima.problog.KnowledgeBaseMemoryBacked;
import com.computablefacts.decima.problog.Literal;
import com.computablefacts.decima.problog.ProbabilityEstimator;
import com.computablefacts.decima.problog.Rule;
import com.computablefacts.decima.problog.Solver;
import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

/**
 * Extracted from Theofrastos Mantadelis and Gerda Janssens (2010). "Nesting Probabilistic Inference"
 */
public class GraphWithoutCycle1Test {

  @Test
  public void testGraph() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseFact("0.4::edge(a, b)."));
    kb.azzert(parseFact("0.55::edge(a, c)."));
    kb.azzert(parseFact("0.8::edge(b, e)."));
    kb.azzert(parseFact("0.2::edge(b, d)."));
    kb.azzert(parseFact("0.4::edge(c, d)."));
    kb.azzert(parseFact("0.3::edge(e, f)."));
    kb.azzert(parseFact("0.5::edge(d, f)."));
    kb.azzert(parseFact("0.6::edge(d, g)."));
    kb.azzert(parseFact("0.7::edge(f, h)."));
    kb.azzert(parseFact("0.7::edge(g, h)."));

    // Init kb with rules
    kb.azzert(parseRule("path(X, Y) :- edge(X, Y)."));
    kb.azzert(parseRule("path(X, Y) :- path(X, Z), edge(Z, Y)."));

    // Query kb
    // path(b, f)?
    Solver solver = new Solver(kb);
    Literal query = new Literal("path", newConst("b"), newConst("f"));
    Set<AbstractClause> proofs = solver.proofs(query);
    Set<AbstractClause> answers = Sets.newHashSet(solver.solve(query));

    // Verify subgoals
    Assert.assertEquals(14, solver.nbSubgoals());

    // Verify proofs
    Assert.assertEquals(2, proofs.size());
    Assert.assertEquals(1, answers.size());

    Rule answer1 = parseRule("path(b, f) :- 0.2::edge(b, d), 0.5::edge(d, f).");
    Rule answer2 = parseRule("path(b, f) :- 0.8::edge(b, e), 0.3::edge(e, f).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2)));

    // Verify BDD answer
    // 0.316::path(b, f).
    ProbabilityEstimator estimator = new ProbabilityEstimator(proofs);
    BigDecimal probability = estimator.probability(query, 3);

    Assert.assertTrue(BigDecimal.valueOf(0.316).compareTo(probability) == 0);
  }

  @Test
  public void testExtractClausesInProofs() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseFact("0.4::edge(a, b)."));
    kb.azzert(parseFact("0.55::edge(a, c)."));
    kb.azzert(parseFact("0.8::edge(b, e)."));
    kb.azzert(parseFact("0.2::edge(b, d)."));
    kb.azzert(parseFact("0.4::edge(c, d)."));
    kb.azzert(parseFact("0.3::edge(e, f)."));
    kb.azzert(parseFact("0.5::edge(d, f)."));
    kb.azzert(parseFact("0.6::edge(d, g)."));
    kb.azzert(parseFact("0.7::edge(f, h)."));
    kb.azzert(parseFact("0.7::edge(g, h)."));

    // Init kb with rules
    kb.azzert(parseRule("path(X, Y) :- edge(X, Y)."));
    kb.azzert(parseRule("path(X, Y) :- path(X, Z), edge(Z, Y)."));

    // Query kb
    // path(b, f)?
    Solver solver = new Solver(kb);
    Literal query = new Literal("path", newConst("b"), newConst("f"));
    List<String> table = solver.tableOfProofs(query);

    Assert.assertEquals(8, table.size());
    Assert.assertEquals("[fact] depth=0, 0.3::edge(\"e\", \"f\").\n" + "[fact] depth=0, 0.5::edge(\"d\", \"f\").\n"
        + "[fact] depth=1, 0.2::edge(\"b\", \"d\").\n" + "[fact] depth=1, 0.8::edge(\"b\", \"e\").\n"
        + "[rule] depth=0, path(\"b\", \"f\") :- path(\"b\", \"d\"), 0.5::edge(\"d\", \"f\").\n"
        + "[rule] depth=0, path(\"b\", \"f\") :- path(\"b\", \"e\"), 0.3::edge(\"e\", \"f\").\n"
        + "[rule] depth=1, path(\"b\", \"d\") :- 0.2::edge(\"b\", \"d\").\n"
        + "[rule] depth=1, path(\"b\", \"e\") :- 0.8::edge(\"b\", \"e\").", Joiner.on("\n").join(table));
  }
}
