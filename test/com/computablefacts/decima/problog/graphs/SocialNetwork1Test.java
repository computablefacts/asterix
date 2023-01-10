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
 * See http://csci431.artifice.cc/notes/problog.html
 */
public class SocialNetwork1Test {

  @Test
  public void test1() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseFact("person(angelika)."));
    kb.azzert(parseFact("person(joris)."));
    kb.azzert(parseFact("person(jonas)."));
    kb.azzert(parseFact("person(dimitar)."));
    kb.azzert(parseFact("friend(joris, jonas)."));
    kb.azzert(parseFact("friend(joris, angelika)."));
    kb.azzert(parseFact("friend(joris, dimitar)."));
    kb.azzert(parseFact("friend(angelika, jonas)."));

    // Init kb with rules
    kb.azzert(parseRule("0.3::stress(X) :- person(X)."));
    kb.azzert(parseRule("0.2::influences(X,Y) :- person(X), person(Y)."));
    kb.azzert(parseRule("smokes(X) :- stress(X)."));
    kb.azzert(parseRule("smokes(X) :- friend(X,Y), influences(Y,X), smokes(Y)."));
    kb.azzert(parseRule("0.4::asthma(X) :- smokes(X)."));

    // Query kb
    // smokes(angelika)?
    Solver solver = new Solver(kb);
    Literal query = new Literal("smokes", newConst("angelika"));
    Set<AbstractClause> proofs = solver.proofs(query);
    Set<AbstractClause> answers = Sets.newHashSet(solver.solve(query));

    // Verify subgoals
    Assert.assertEquals(11, solver.nbSubgoals());

    // Verify answers
    Assert.assertEquals(2, proofs.size());
    Assert.assertEquals(1, answers.size());

    Rule answer1 = parseRule(
        "smokes(angelika) :- friend(angelika, jonas), person(jonas), person(angelika), person(jonas).");
    Rule answer2 = parseRule("smokes(angelika) :- person(angelika).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2)));

    // Verify BDD answer
    // 0.342::smokes(angelika).
    BigDecimal probability = new ProbabilityEstimator(proofs).probability(new Fact(query), 3);

    Assert.assertEquals(0, BigDecimal.valueOf(0.342).compareTo(probability));
  }

  @Test
  public void test2() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseFact("person(angelika)."));
    kb.azzert(parseFact("person(joris)."));
    kb.azzert(parseFact("person(jonas)."));
    kb.azzert(parseFact("person(dimitar)."));
    kb.azzert(parseFact("friend(joris, jonas)."));
    kb.azzert(parseFact("friend(joris, angelika)."));
    kb.azzert(parseFact("friend(joris, dimitar)."));
    kb.azzert(parseFact("friend(angelika, jonas)."));

    // Init kb with rules
    kb.azzert(parseRule("0.3::stress(X) :- person(X)."));
    kb.azzert(parseRule("0.2::influences(X,Y) :- person(X), person(Y)."));
    kb.azzert(parseRule("smokes(X) :- stress(X)."));
    kb.azzert(parseRule("smokes(X) :- friend(X,Y), influences(Y,X), smokes(Y)."));
    kb.azzert(parseRule("0.4::asthma(X) :- smokes(X)."));

    // Query kb
    // smokes(joris)?
    Solver solver = new Solver(kb);
    Literal query = new Literal("smokes", newConst("joris"));
    Set<AbstractClause> proofs = solver.proofs(query);
    Set<AbstractClause> answers = Sets.newHashSet(solver.solve(query));

    // Verify subgoals
    Assert.assertEquals(22, solver.nbSubgoals());

    // Verify answers
    Assert.assertEquals(5, proofs.size());
    Assert.assertEquals(1, answers.size());

    Rule answer1 = parseRule(
        "smokes(joris) :- friend(joris, dimitar), person(dimitar), person(joris), person(dimitar).");
    Rule answer2 = parseRule("smokes(joris) :- person(joris).");
    Rule answer3 = parseRule(
        "smokes(joris) :- friend(joris, angelika), person(angelika), person(joris), person(angelika).");
    Rule answer4 = parseRule("smokes(joris) :- friend(joris, jonas), person(jonas), person(joris), person(jonas).");
    Rule answer5 = parseRule(
        "smokes(joris) :- friend(joris, angelika), person(angelika), person(joris), friend(angelika, jonas), person(jonas), person(angelika), person(jonas).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2, answer3, answer4, answer5)));

    // Verify BDD answer
    // 0.42301296::smokes(joris).
    ProbabilityEstimator estimator = new ProbabilityEstimator(proofs);
    BigDecimal probability = estimator.probability(new Fact(query), 8);

    Assert.assertEquals(0, BigDecimal.valueOf(0.42556811).compareTo(probability));
  }
}