package com.computablefacts.decima.problog.graphs;

import static com.computablefacts.decima.problog.Parser.parseFact;
import static com.computablefacts.decima.problog.Parser.parseQuery;
import static com.computablefacts.decima.problog.Parser.parseRule;

import com.computablefacts.decima.problog.AbstractClause;
import com.computablefacts.decima.problog.KnowledgeBaseMemoryBacked;
import com.computablefacts.decima.problog.Literal;
import com.computablefacts.decima.problog.ProbabilityEstimator;
import com.computablefacts.decima.problog.Solver;
import java.math.BigDecimal;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

/**
 * See https://dtai.cs.kuleuven.be/problog/tutorial/basic/10_inhibitioneffects.html
 */
public class SocialNetwork2Test {

  @Test
  public void test() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseFact("person(angelika)."));
    kb.azzert(parseFact("person(boris)."));

    // Init kb with rules
    kb.azzert(parseRule("0.1::initialInf(X) :- person(X)."));
    kb.azzert(parseRule("0.1::contact(X,Y) :- person(X), person(Y)."));
    kb.azzert(parseRule("inf(X) :- initialInf(X)."));
    kb.azzert(parseRule("0.6::inf(X) :- contact(X, Y), inf(Y)."));

    // Query kb
    // inf(_)?
    Solver solver = new Solver(kb);
    Literal query = parseQuery("inf(_)?");
    Set<AbstractClause> proofs = solver.proofs(query);

    // Verify BDD answer
    // 0.1054 ::inf(angelika).
    // 0.1054 ::inf(boris).
    BigDecimal probability1 = new ProbabilityEstimator(proofs).probability(parseFact("inf(angelika)."), 4);
    BigDecimal probability2 = new ProbabilityEstimator(proofs).probability(parseFact("inf(boris)."), 5);

    Assert.assertEquals(0, BigDecimal.valueOf(0.1054).compareTo(probability1));
    Assert.assertEquals(0, BigDecimal.valueOf(0.1054).compareTo(probability2));
  }
}