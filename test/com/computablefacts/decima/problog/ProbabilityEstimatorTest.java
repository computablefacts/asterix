package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.Parser.parseClause;
import static com.computablefacts.decima.problog.Parser.parseFact;
import static com.computablefacts.decima.problog.Parser.parseQuery;
import static com.computablefacts.decima.problog.Parser.parseRule;
import static com.computablefacts.decima.problog.TestUtils.checkAnswers;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

public class ProbabilityEstimatorTest {

  @Test
  public void testComputeProbabilityWithoutProofs() {
    ProbabilityEstimator estimator = new ProbabilityEstimator(new HashSet<>());
    Assert.assertEquals(BigDecimal.ZERO, estimator.probability(parseFact("fake(1).")));
  }

  /**
   * See https://github.com/ML-KULeuven/problog/blob/master/test/swap.pl
   */
  @Test
  public void testLiteralsSwapping1() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("0.5::f(1,2)."));
    kb.azzert(parseClause("0.5::f(2,1)."));
    kb.azzert(parseClause("0.5::f(1,3)."));
    kb.azzert(parseClause("0.5::f(2,3)."));
    kb.azzert(parseClause("0.5::b(1)."));
    kb.azzert(parseClause("0.5::b(2)."));
    kb.azzert(parseClause("0.5::b(3)."));

    // Init kb with rules
    kb.azzert(parseClause("s1(X) :- b(X)."));
    kb.azzert(parseClause("s1(X) :- f(X,Y),s1(Y)."));
    kb.azzert(parseClause("s2(X) :- f(X,Y),s2(Y)."));
    kb.azzert(parseClause("s2(X) :- b(X)."));

    // Query kb
    // s1(1)?
    Solver solver = new Solver(kb);
    Literal query = parseQuery("s1(1)?");
    List<AbstractClause> proofs = Lists.newArrayList(solver.proofs(query));

    // Verify BDD answer
    // 0.734375::s1(1).
    ProbabilityEstimator estimator = new ProbabilityEstimator(Sets.newHashSet(proofs));
    BigDecimal probability = estimator.probability(new Fact(query), 6);

    Assert.assertEquals(0, BigDecimal.valueOf(0.734375).compareTo(probability));
  }

  /**
   * See https://github.com/ML-KULeuven/problog/blob/master/test/swap.pl
   */
  @Test
  public void testLiteralsSwapping2() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("0.5::f(1,2)."));
    kb.azzert(parseClause("0.5::f(2,1)."));
    kb.azzert(parseClause("0.5::f(1,3)."));
    kb.azzert(parseClause("0.5::f(2,3)."));
    kb.azzert(parseClause("0.5::b(1)."));
    kb.azzert(parseClause("0.5::b(2)."));
    kb.azzert(parseClause("0.5::b(3)."));

    // Init kb with rules
    kb.azzert(parseClause("s1(X) :- b(X)."));
    kb.azzert(parseClause("s1(X) :- f(X,Y),s1(Y)."));
    kb.azzert(parseClause("s2(X) :- f(X,Y),s2(Y)."));
    kb.azzert(parseClause("s2(X) :- b(X)."));

    // Query kb
    // s2(1)?
    Solver solver = new Solver(kb);
    Literal query = parseQuery("s2(1)?");
    List<AbstractClause> proofs = Lists.newArrayList(solver.proofs(query));

    // Verify BDD answer
    // 0.734375::s2(1).
    ProbabilityEstimator estimator = new ProbabilityEstimator(Sets.newHashSet(proofs));
    BigDecimal probability = estimator.probability(new Fact(query), 6);

    Assert.assertEquals(0, BigDecimal.valueOf(0.734375).compareTo(probability));
  }

  /**
   * Non-ground query
   * <p>
   * See https://github.com/ML-KULeuven/problog/blob/master/test/non_ground_query.pl
   */
  @Test
  public void testNonGroundQuery() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("0.1::b(1)."));
    kb.azzert(parseClause("0.2::b(2)."));
    kb.azzert(parseClause("0.3::e(1)."));
    kb.azzert(parseClause("0.4::e(3)."));
    kb.azzert(parseClause("d(1)."));
    kb.azzert(parseClause("d(2)."));
    kb.azzert(parseClause("d(3)."));

    // Init kb with rules
    kb.azzert(parseClause("a(X) :- b(2), c(X,Y)."));
    kb.azzert(parseClause("c(X,Y) :- c(X,Z), c(Z,Y)."));
    kb.azzert(parseClause("c(X,Y) :- d(X), d(Y)."));

    // Query kb
    // a(X)?
    Solver solver = new Solver(kb);
    Literal query = parseQuery("a(_)?");
    Set<AbstractClause> proofs = solver.proofs(query);

    // Verify BDD answer
    // 0.2::a(1).
    // 0.2::a(2).
    // 0.2::a(3).
    ProbabilityEstimator estimator = new ProbabilityEstimator(proofs);
    Map<Fact, BigDecimal> probabilities = estimator.probabilities();

    Fact a1 = parseFact("a(1).");
    Fact a2 = parseFact("a(2).");
    Fact a3 = parseFact("a(3).");

    Assert.assertEquals(0, BigDecimal.valueOf(0.2).compareTo(probabilities.get(a1)));
    Assert.assertEquals(0, BigDecimal.valueOf(0.2).compareTo(probabilities.get(a2)));
    Assert.assertEquals(0, BigDecimal.valueOf(0.2).compareTo(probabilities.get(a3)));
  }

  /**
   * Ground, non-ground query
   * <p>
   * See https://github.com/ML-KULeuven/problog/blob/master/test/ground_nonground_bug_v4.pl
   */
  @Test
  public void testGroundNonGroundQuery4() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("0.1::p(1)."));

    // Init kb with rules
    kb.azzert(parseClause("p(X) :- p(1), fn_is(X, unk)."));
    kb.azzert(parseClause("fill(X) :- fn_is(X, unk)."));
    kb.azzert(parseClause("fill(X) :- p(X), fill(X)."));
    kb.azzert(parseClause("q(X) :- fill(X)."));

    // Query kb
    // q(X)?
    Solver solver = new Solver(kb);
    Literal query = parseQuery("q(_)?");
    Set<AbstractClause> proofs = solver.proofs(query);

    // Verify BDD answer
    // 0.1::q(unk).
    ProbabilityEstimator estimator = new ProbabilityEstimator(proofs);
    Map<Fact, BigDecimal> probabilities = estimator.probabilities();

    Fact answer = parseFact("q(unk).");

    Assert.assertEquals(0, BigDecimal.valueOf(0.1).compareTo(probabilities.get(answer)));
  }

  /**
   * Negative query
   * <p>
   * See https://github.com/ML-KULeuven/problog/blob/master/test/negative_query.pl
   */
  @Test
  public void testNegativeQuery() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("0.3::p(1)."));

    // Query kb
    Solver solver = new Solver(kb);
    Literal query = parseQuery("~p(1)?");
    Set<AbstractClause> proofs = solver.proofs(query);
    Set<Fact> answers = Sets.newHashSet(solver.solve(query));

    // Verify answers
    Assert.assertEquals(1, proofs.size());
    Assert.assertEquals(1, answers.size());

    Fact answer = parseFact("0.7::~p(1).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer)));

    // Verify BDD answer
    // 0.7::~p(1).
    ProbabilityEstimator estimator = new ProbabilityEstimator(Sets.newHashSet(proofs));
    BigDecimal probability = estimator.probability(new Fact(query));

    Assert.assertEquals(0, BigDecimal.valueOf(0.7).compareTo(probability));
  }

  /**
   * Tossing coins
   * <p>
   * Description: two coins - one biased and one not.
   * <p>
   * Query: what is the probability of throwing some heads.
   * <p>
   * See https://github.com/ML-KULeuven/problog/blob/master/test/00_trivial_or.pl
   */
  @Test
  public void testTrivialOr() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("0.5::heads1(a)."));
    kb.azzert(parseClause("0.6::heads2(a)."));

    // Init kb with rules
    kb.azzert(parseClause("someHeads(X) :- heads1(X)."));
    kb.azzert(parseClause("someHeads(X) :- heads2(X)."));

    // Query kb
    // someHeads(X)?
    Solver solver = new Solver(kb);
    Literal query = parseQuery("someHeads(_)?");
    Set<AbstractClause> proofs = solver.proofs(query);
    Set<Fact> answers = Sets.newHashSet(solver.solve(query));

    // Verify answers
    Assert.assertEquals(2, proofs.size());
    Assert.assertEquals(1, answers.size());

    Rule answer1 = parseRule("someHeads(a) :- 0.5::heads1(a).");
    Rule answer2 = parseRule("someHeads(a) :- 0.6::heads2(a).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2)));

    // Verify BDD answer
    // 0.8::someHeads(a).
    ProbabilityEstimator estimator = new ProbabilityEstimator(Sets.newHashSet(proofs));
    BigDecimal probability = estimator.probability(parseFact("someHeads(a)."));

    Assert.assertEquals(0, BigDecimal.valueOf(0.8).compareTo(probability));
  }

  /**
   * Tossing coins
   * <p>
   * Description: two coins - one biased and one not.
   * <p>
   * Query: what is the probability of throwing two heads.
   * <p>
   * See https://github.com/ML-KULeuven/problog/blob/master/test/00_trivial_and.pl
   */
  @Test
  public void testTrivialAnd() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("0.5::heads1(a)."));
    kb.azzert(parseClause("0.6::heads2(a)."));

    // Init kb with rules
    kb.azzert(parseClause("twoHeads(X) :- heads1(X), heads2(X)."));

    // Query kb
    // twoHeads(X)?
    Solver solver = new Solver(kb);
    Literal query = parseQuery("twoHeads(_)?");
    List<AbstractClause> proofs = Lists.newArrayList(solver.proofs(query));
    Set<Fact> answers = Sets.newHashSet(solver.solve(query));

    // Verify answers
    Assert.assertEquals(1, proofs.size());
    Assert.assertEquals(1, answers.size());

    Rule answer = parseRule("twoHeads(a) :- 0.5::heads1(a), 0.6::heads2(a).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer)));

    // Verify BDD answer
    // 0.3::twoHeads(a).
    ProbabilityEstimator estimator = new ProbabilityEstimator(Sets.newHashSet(proofs));
    BigDecimal probability = estimator.probability(parseFact("twoHeads(a)."));

    Assert.assertEquals(0, BigDecimal.valueOf(0.3).compareTo(probability));
  }

  /**
   * Duplicate fact
   * <p>
   * Description: Interpret as two separate facts.
   * <p>
   * See https://github.com/ML-KULeuven/problog/blob/master/test/00_trivial_duplicate.pl
   */
  @Test
  public void testTrivialDuplicate() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("0.3::p(1)."));
    kb.azzert(parseClause("0.2::p(2)."));
    kb.azzert(parseClause("0.6::p(1)."));

    // Query kb
    // p(1)?
    // p(2)?
    Solver solver = new Solver(kb);
    Literal query1 = parseQuery("p(1)?");
    Set<AbstractClause> proofs1 = solver.proofs(query1);
    Set<Fact> answers1 = Sets.newHashSet(solver.solve(query1));

    Literal query2 = parseQuery("p(2)?");
    Set<AbstractClause> proofs2 = solver.proofs(query2);
    Set<Fact> answers2 = Sets.newHashSet(solver.solve(query2));

    // Verify answers
    Assert.assertEquals(2, proofs1.size());
    Assert.assertEquals(2, answers1.size());
    Assert.assertEquals(1, proofs2.size());
    Assert.assertEquals(1, answers2.size());

    Fact answer1 = parseFact("0.3::p(1).");
    Fact answer2 = parseFact("0.6::p(1).");

    Assert.assertTrue(checkAnswers(answers1, Sets.newHashSet(answer1, answer2)));

    Fact answer3 = parseFact("0.2::p(2).");

    Assert.assertTrue(checkAnswers(answers2, Sets.newHashSet(answer3)));

    // Verify BDD answer
    // 0.72::p(1).
    // 0.2::p(2).
    ProbabilityEstimator estimator1 = new ProbabilityEstimator(Sets.newHashSet(proofs1));
    BigDecimal probability1 = estimator1.probability(new Fact(query1));

    Assert.assertEquals(0, BigDecimal.valueOf(0.72).compareTo(probability1));

    ProbabilityEstimator estimator2 = new ProbabilityEstimator(Sets.newHashSet(proofs2));
    BigDecimal probability2 = estimator2.probability(new Fact(query2));

    Assert.assertEquals(0, BigDecimal.valueOf(0.2).compareTo(probability2));
  }

  /**
   * Probabilistic negation
   * <p>
   * Description: Compute probability of a negated fact.
   * <p>
   * See https://github.com/ML-KULeuven/problog/blob/master/test/00_trivial_not.pl
   */
  @Test
  public void testTrivialNot() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("0.4::p(1)."));

    // Query kb
    // ~p(1)?
    Solver solver = new Solver(kb);
    Literal query = parseQuery("~p(1)?");
    Set<AbstractClause> proofs = solver.proofs(query);
    Set<Fact> answers = Sets.newHashSet(solver.solve(query));

    // Verify answers
    Assert.assertEquals(1, proofs.size());
    Assert.assertEquals(1, answers.size());

    Fact answer = parseFact("0.6::~p(1).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer)));

    // Verify BDD answer
    // 0.6::~p(1).
    ProbabilityEstimator estimator = new ProbabilityEstimator(Sets.newHashSet(proofs));
    BigDecimal probability = estimator.probability(new Fact(query));

    Assert.assertEquals(0, BigDecimal.valueOf(0.6).compareTo(probability));
  }

  /**
   * Probabilistic negation of a rule
   * <p>
   * Description: Compute probability of a negated rule.
   * <p>
   * See https://github.com/ML-KULeuven/problog/blob/master/test/00_trivial_not_and.pl
   */
  @Test
  public void testTrivialNotAnd() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("0.5::t(1)."));
    kb.azzert(parseClause("0.3::t(2)."));

    // Init kb with rules
    kb.azzert(parseClause("q(X) :- t(X), fn_add(U, X, 1), fn_int(V, U), t(V)."));
    kb.azzert(parseClause("p(X) :- ~q(X)."));

    // Query kb
    // p(1)?
    Solver solver = new Solver(kb);
    Literal query = parseQuery("p(1)?");
    Set<AbstractClause> proofs = solver.proofs(query);
    Set<Fact> answers = Sets.newHashSet(solver.solve(query));

    // Verify answers
    Assert.assertEquals(2, proofs.size());
    Assert.assertEquals(1, answers.size());

    Rule answer1 = parseRule("p(1) :- 0.5::~t(1).");
    Rule answer2 = parseRule("p(1) :- 0.7::~t(2).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2)));

    // Verify BDD answer
    // 0.85::p(1).
    ProbabilityEstimator estimator = new ProbabilityEstimator(Sets.newHashSet(proofs));
    BigDecimal probability = estimator.probability(new Fact(query));

    Assert.assertEquals(0, BigDecimal.valueOf(0.85).compareTo(probability));
  }

  /**
   * See https://github.com/ML-KULeuven/problog/blob/master/test/tc_3.pl
   */
  @Test
  public void testRuleWithProbabilityInHead() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("athlet(1)."));
    kb.azzert(parseClause("athlet(2)."));
    kb.azzert(parseClause("student(2)."));
    kb.azzert(parseClause("student(3)."));

    // Init kb with rules
    kb.azzert(parseClause("0.5::stressed(X) :- student(X)."));
    kb.azzert(parseClause("0.2::stressed(X) :- athlet(X)."));

    // Query kb
    // stressed(1)?
    // stressed(2)?
    // stressed(3)?
    Solver solver = new Solver(kb);

    Literal query1 = parseQuery("stressed(1)?");
    Set<AbstractClause> proofs1 = solver.proofs(query1);
    Set<Fact> answers1 = Sets.newHashSet(solver.solve(query1));

    Literal query2 = parseQuery("stressed(2)?");
    Set<AbstractClause> proofs2 = solver.proofs(query2);
    Set<Fact> answers2 = Sets.newHashSet(solver.solve(query2));

    Literal query3 = parseQuery("stressed(3)?");
    Set<AbstractClause> proofs3 = solver.proofs(query3);
    Set<Fact> answers3 = Sets.newHashSet(solver.solve(query3));

    // Verify answers
    // stressed("1") :- athlet("1"), 0.2::proba("0sr9pjn").
    // stressed("2") :- student("2"), 0.5::proba("8jyexcv").
    // stressed("2") :- athlet("2"), 0.2::proba("0sr9pjn").
    // stressed("3") :- student("3"), 0.5::proba("8jyexcv").
    Assert.assertEquals(1, proofs1.size());
    Assert.assertEquals(1, answers1.size());
    Assert.assertEquals(2, proofs2.size());
    Assert.assertEquals(1, answers2.size());
    Assert.assertEquals(1, proofs3.size());
    Assert.assertEquals(1, answers3.size());

    Rule answer1 = parseRule("stressed(1) :- athlet(1).");

    Assert.assertTrue(checkAnswers(answers1, Sets.newHashSet(answer1)));

    Rule answer2 = parseRule("stressed(2) :- student(2).");
    Rule answer3 = parseRule("stressed(2) :- athlet(2).");

    Assert.assertTrue(checkAnswers(answers2, Sets.newHashSet(answer2, answer3)));

    Rule answer4 = parseRule("stressed(3) :- student(3).");

    Assert.assertTrue(checkAnswers(answers3, Sets.newHashSet(answer4)));

    // Verify BDD answer
    // 0.2::stressed(1).
    // 0.6::stressed(2).
    // 0.5::stressed(3).
    ProbabilityEstimator estimator1 = new ProbabilityEstimator(Sets.newHashSet(proofs1));
    BigDecimal probability1 = estimator1.probability(new Fact(query1));

    Assert.assertEquals(0, BigDecimal.valueOf(0.2).compareTo(probability1));

    ProbabilityEstimator estimator2 = new ProbabilityEstimator(Sets.newHashSet(proofs2));
    BigDecimal probability2 = estimator2.probability(new Fact(query2));

    Assert.assertEquals(0, BigDecimal.valueOf(0.6).compareTo(probability2));

    ProbabilityEstimator estimator3 = new ProbabilityEstimator(Sets.newHashSet(proofs3));
    BigDecimal probability3 = estimator3.probability(new Fact(query3));

    Assert.assertEquals(0, BigDecimal.valueOf(0.5).compareTo(probability3));
  }
}
