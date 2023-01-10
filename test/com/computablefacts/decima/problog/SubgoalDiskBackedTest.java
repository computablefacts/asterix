package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.Parser.parseFact;
import static com.computablefacts.decima.problog.Parser.parseQuery;
import static com.computablefacts.decima.problog.Parser.parseRule;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class SubgoalDiskBackedTest {

  /**
   * See https://github.com/ML-KULeuven/problog/blob/master/test/swap.pl
   */
  @Test
  public void testLiteralsSwapping1() throws Exception {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseFact("0.5::f(1,2)."));
    kb.azzert(parseFact("0.5::f(2,1)."));
    kb.azzert(parseFact("0.5::f(1,3)."));
    kb.azzert(parseFact("0.5::f(2,3)."));
    kb.azzert(parseFact("0.5::b(1)."));
    kb.azzert(parseFact("0.5::b(2)."));
    kb.azzert(parseFact("0.5::b(3)."));

    // Init kb with rules
    kb.azzert(parseRule("s1(X) :- b(X)."));
    kb.azzert(parseRule("s1(X) :- f(X,Y),s1(Y)."));
    kb.azzert(parseRule("s2(X) :- f(X,Y),s2(Y)."));
    kb.azzert(parseRule("s2(X) :- b(X)."));

    // Query kb
    // s1(1)?
    Path dir = Files.createTempDirectory("solver");
    Solver solver = new Solver(kb, fact -> new SubgoalDiskBacked(fact, dir.toFile().getAbsolutePath()));
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
  public void testLiteralsSwapping2() throws Exception {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseFact("0.5::f(1,2)."));
    kb.azzert(parseFact("0.5::f(2,1)."));
    kb.azzert(parseFact("0.5::f(1,3)."));
    kb.azzert(parseFact("0.5::f(2,3)."));
    kb.azzert(parseFact("0.5::b(1)."));
    kb.azzert(parseFact("0.5::b(2)."));
    kb.azzert(parseFact("0.5::b(3)."));

    // Init kb with rules
    kb.azzert(parseRule("s1(X) :- b(X)."));
    kb.azzert(parseRule("s1(X) :- f(X,Y),s1(Y)."));
    kb.azzert(parseRule("s2(X) :- f(X,Y),s2(Y)."));
    kb.azzert(parseRule("s2(X) :- b(X)."));

    // Query kb
    // s2(1)?
    Multiset<Fact> facts = HashMultiset.create();
    Path dir = Files.createTempDirectory("solver");
    Solver solver = new Solver(kb, fact -> new SubgoalDiskBacked(fact, dir.toFile().getAbsolutePath(), facts::add));
    Literal query = parseQuery("s2(1)?");
    List<AbstractClause> proofs = Lists.newArrayList(solver.proofs(query));

    // Check subgoals' facts
    Assert.assertEquals(10, facts.size());

    Assert.assertTrue(facts.contains(parseFact("0.5::b(1).")));
    Assert.assertTrue(facts.contains(parseFact("0.5::b(2).")));
    Assert.assertTrue(facts.contains(parseFact("0.5::b(3).")));
    Assert.assertTrue(facts.contains(parseFact("0.5::f(1, 3).")));
    Assert.assertTrue(facts.contains(parseFact("0.5::f(1, 2).")));
    Assert.assertTrue(facts.contains(parseFact("0.5::f(2, 3).")));
    Assert.assertTrue(facts.contains(parseFact("0.5::f(2, 1).")));
    Assert.assertTrue(facts.contains(parseFact("s2(3).")));
    Assert.assertTrue(facts.contains(parseFact("s2(2).")));
    Assert.assertTrue(facts.contains(parseFact("s2(1).")));

    // Verify BDD answer
    // 0.734375::s2(1).
    ProbabilityEstimator estimator = new ProbabilityEstimator(Sets.newHashSet(proofs));
    BigDecimal probability = estimator.probability(new Fact(query), 6);

    Assert.assertEquals(0, BigDecimal.valueOf(0.734375).compareTo(probability));
  }
}