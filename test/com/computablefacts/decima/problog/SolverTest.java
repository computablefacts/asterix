package com.computablefacts.decima.problog;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;
import static com.computablefacts.decima.problog.Parser.parseClause;
import static com.computablefacts.decima.problog.Parser.parseQuery;
import static com.computablefacts.decima.problog.Parser.parseRule;
import static com.computablefacts.decima.problog.TestUtils.checkAnswers;

import com.google.common.collect.Sets;
import java.util.Set;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.Assert;
import org.junit.Test;

public class SolverTest {
/*
  @Test
  public void testSimpleQuery() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("girl(alice)."));
    kb.azzert(parseClause("boy(alex)."));

    // Init kb with rules
    kb.azzert(parseClause("child(X) :- boy(X)."));
    kb.azzert(parseClause("child(Y) :- girl(Y)."));

    // Query kb
    // child(Z)?
    Solver solver = new Solver(kb);
    Literal query = parseQuery("child(_)?");
    Set<Fact> answers = Sets.newHashSet(solver.solve(query));

    // Verify answers
    Assert.assertEquals(2, answers.size());

    Rule answer1 = parseRule("child(alice) :- girl(alice).");
    Rule answer2 = parseRule("child(alex) :- boy(alex).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2)));
  }

  @Test
  public void testComplexQuery() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("boy(bill)."));
    kb.azzert(parseClause("mother(alice, bill)."));

    // Init kb with rules
    kb.azzert(parseClause("child(X,Y) :- mother(Y,X)."));
    kb.azzert(parseClause("child(X,Y) :- father(Y,X)."));
    kb.azzert(parseClause("son(X,Y) :- child(X,Y),boy(X)."));

    // Query kb
    // son(Z, alice)?
    Solver solver = new Solver(kb);
    Literal query = parseQuery("son(_, alice)?");
    Set<Fact> answers = Sets.newHashSet(solver.solve(query));

    // Verify answers
    Assert.assertEquals(1, answers.size());

    Rule answer = parseRule("son(bill, alice) :- mother(alice, bill), boy(bill).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer)));
  }

  @Test
  public void testNegation() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("girl(alice)."));
    kb.azzert(parseClause("boy(alex)."));
    kb.azzert(parseClause("girl(nikka)."));
    kb.azzert(parseClause("boy(nikka)."));

    // Init kb with rules
    kb.azzert(parseClause("human(X) :- girl(X), ~boy(X)."));
    kb.azzert(parseClause("human(X) :- boy(X), ~girl(X)."));

    // Query kb
    // human(Z)?
    Solver solver = new Solver(kb);
    Literal query = parseQuery("human(_)?");
    Set<Fact> answers = Sets.newHashSet(solver.solve(query));

    // Verify answers
    Assert.assertEquals(2, answers.size());

    Rule answer1 = parseRule("human(alice) :- girl(alice), ~boy(alice).");
    Rule answer2 = parseRule("human(alex) :- boy(alex), ~girl(alex).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2)));
  }

  @Test
  public void testRecursion() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("edge(a,b)."));
    kb.azzert(parseClause("edge(b,c)."));
    kb.azzert(parseClause("edge(a,d)."));

    // Init kb with rules
    kb.azzert(parseClause("path(X, Y) :- edge(X, Y)."));
    kb.azzert(parseClause("path(X, Y) :- path(X, Z), edge(Z, Y)."));

    // Query kb
    // path(a, V)?
    Solver solver = new Solver(kb);
    Literal query = parseQuery("path(a, _)?");
    Set<Fact> answers = Sets.newHashSet(solver.solve(query));

    // Verify answers
    Assert.assertEquals(3, answers.size());

    Rule answer1 = parseRule("path(a, b) :- edge(a, b).");
    Rule answer2 = parseRule("path(a, c) :- edge(a, b), edge(b, c).");
    Rule answer3 = parseRule("path(a, d) :- edge(a, d).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2, answer3)));
  }
*/
  @Test
  public void testSimplePrimitive() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("one(1)."));
    kb.azzert(parseClause("two(2)."));

    // Init kb with rules
    kb.azzert(parseClause("three(Z) :- one(X), two(Y), fn_add(W, X, Y), fn_int(Z, W)."));
    kb.azzert(parseClause("four(Z) :- three(X), fn_add(W, X, 1), fn_int(Z, W)."));

    // Query kb
    // three(Z)?
    Solver solver = new Solver(kb);
    Literal query1 = parseQuery("three(_)?");
    Set<Fact> answers1 = Sets.newHashSet(solver.solve(query1));

    // Verify answers
    Assert.assertEquals(1, answers1.size());

    Rule answer1 = parseRule("three(3) :- one(1), two(2), fn_add(3, 1, 2), fn_int(3, 3).");

    Assert.assertTrue(checkAnswers(answers1, Sets.newHashSet(answer1)));

    // Query kb
    // four(Z)?
    Literal query2 = parseQuery("four(_)?");
    Set<Fact> answers2 = Sets.newHashSet(solver.solve(query2));

    // Verify answers
    Assert.assertEquals(1, answers2.size());

    Rule answer2 = parseRule(
        "four(4) :- one(1), two(2), fn_add(3, 1, 2), fn_int(3, 3), fn_add(4, 3, 1), fn_int(4, 4).");

    Assert.assertTrue(checkAnswers(answers2, Sets.newHashSet(answer2)));
  }

  @Test
  public void testIsTrue() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("bagItems(\"red_bag\", 1)."));
    kb.azzert(parseClause("bagItems(\"green_bag\", 2)."));
    kb.azzert(parseClause("bagItems(\"blue_bag\", 3)."));

    // Init kb with rules
    kb.azzert(parseClause("hasMoreItems(X, Y) :- bagItems(X, A), bagItems(Y, B), fn_gt(U, A, B), fn_is_true(U)."));

    // Query kb
    // hasMoreItems(X, Y)?
    Solver solver = new Solver(kb);
    Literal query = parseQuery("hasMoreItems(_, _)?");
    Set<Fact> answers = Sets.newHashSet(solver.solve(query));

    // Verify answers
    Assert.assertEquals(3, answers.size());

    Rule answer1 = parseRule(
        "hasMoreItems(green_bag, red_bag) :- bagItems(green_bag, 2), bagItems(red_bag, 1), fn_gt(true, 2, 1), fn_is_true(true).");
    Rule answer2 = parseRule(
        "hasMoreItems(blue_bag, red_bag) :- bagItems(blue_bag, 3), bagItems(red_bag, 1), fn_gt(true, 3, 1), fn_is_true(true).");
    Rule answer3 = parseRule(
        "hasMoreItems(blue_bag, green_bag) :- bagItems(blue_bag, 3), bagItems(green_bag, 2), fn_gt(true, 3, 2), fn_is_true(true).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2, answer3)));
  }

  @Test
  public void testIsFalse() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("bagItems(\"red_bag\", 1)."));
    kb.azzert(parseClause("bagItems(\"green_bag\", 2)."));
    kb.azzert(parseClause("bagItems(\"blue_bag\", 2)."));

    // Init kb with rules
    kb.azzert(parseClause(
        "hasDifferentNumberOfItems(X, Y) :- bagItems(X, A), bagItems(Y, B), fn_eq(U, A, B), fn_is_false(U)."));

    // Query kb
    // hasDifferentNumberOfItems(X, Y)?
    Solver solver = new Solver(kb);
    Literal query = parseQuery("hasDifferentNumberOfItems(_, _)?");
    Set<Fact> answers = Sets.newHashSet(solver.solve(query));

    // Verify answers
    Assert.assertEquals(4, answers.size());

    Rule answer1 = parseRule(
        "hasDifferentNumberOfItems(red_bag, green_bag) :- bagItems(red_bag, 1), bagItems(green_bag, 2), fn_eq(false, 1, 2), fn_is_false(false).");
    Rule answer2 = parseRule(
        "hasDifferentNumberOfItems(red_bag, blue_bag) :- bagItems(red_bag, 1),bagItems(blue_bag, 2), fn_eq(false, 1, 2), fn_is_false(false).");
    Rule answer3 = parseRule(
        "hasDifferentNumberOfItems(green_bag, red_bag) :- bagItems(green_bag, 2), bagItems(red_bag, 1), fn_eq(false, 2, 1), fn_is_false(false).");
    Rule answer4 = parseRule(
        "hasDifferentNumberOfItems(blue_bag, red_bag) :- bagItems(blue_bag, 2), bagItems(red_bag, 1), fn_eq(false, 2, 1), fn_is_false(false).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2, answer3, answer4)));
  }

  @Test
  public void testSampleOfSizeMinus1() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("bagItems(\"red_bag\", 1)."));
    kb.azzert(parseClause("bagItems(\"green_bag\", 2)."));
    kb.azzert(parseClause("bagItems(\"blue_bag\", 2)."));

    // Init kb with rules
    kb.azzert(parseClause(
        "hasDifferentNumberOfItems(X, Y) :- bagItems(X, A), bagItems(Y, B), fn_eq(U, A, B), fn_is_false(U)."));

    // Query kb
    // hasDifferentNumberOfItems(X, Y)?
    Solver solver = new Solver(kb);
    Literal query = parseQuery("hasDifferentNumberOfItems(_, _)?");
    Set<Fact> answers = Sets.newHashSet(solver.solve(query, -1));

    // Verify answers
    Assert.assertEquals(4, answers.size());

    Rule answer1 = parseRule(
        "hasDifferentNumberOfItems(\"green_bag\", \"red_bag\") :- bagItems(\"green_bag\", \"2\"), bagItems(\"red_bag\", \"1\"), fn_eq(\"false\", \"2\", \"1\"), fn_is_false(\"false\").");
    Rule answer2 = parseRule(
        "hasDifferentNumberOfItems(\"red_bag\", \"blue_bag\") :- bagItems(\"red_bag\", \"1\"), bagItems(\"blue_bag\", \"2\"), fn_eq(\"false\", \"1\", \"2\"), fn_is_false(\"false\").");
    Rule answer3 = parseRule(
        "hasDifferentNumberOfItems(\"red_bag\", \"green_bag\") :- bagItems(\"red_bag\", \"1\"), bagItems(\"green_bag\", \"2\"), fn_eq(\"false\", \"1\", \"2\"), fn_is_false(\"false\").");
    Rule answer4 = parseRule(
        "hasDifferentNumberOfItems(\"blue_bag\", \"red_bag\") :- bagItems(\"blue_bag\", \"2\"), bagItems(\"red_bag\", \"1\"), fn_eq(\"false\", \"2\", \"1\"), fn_is_false(\"false\").");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2, answer3, answer4)));
  }

  @Test
  public void testSampleOfSize1() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("bagItems(\"red_bag\", 1)."));
    kb.azzert(parseClause("bagItems(\"green_bag\", 2)."));
    kb.azzert(parseClause("bagItems(\"blue_bag\", 2)."));

    // Init kb with rules
    kb.azzert(parseClause(
        "hasDifferentNumberOfItems(X, Y) :- bagItems(X, A), bagItems(Y, B), fn_eq(U, A, B), fn_is_false(U)."));

    // Query kb
    // hasDifferentNumberOfItems(X, Y)?
    Solver solver = new Solver(kb);
    Literal query = parseQuery("hasDifferentNumberOfItems(_, _)?");
    Set<Fact> answers = Sets.newHashSet(solver.solve(query, 1));

    // Verify answers
    Assert.assertEquals(1, answers.size());
  }

  @Test
  public void testSampleOfSize2() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("bagItems(\"red_bag\", 1)."));
    kb.azzert(parseClause("bagItems(\"green_bag\", 2)."));
    kb.azzert(parseClause("bagItems(\"blue_bag\", 2)."));

    // Init kb with rules
    kb.azzert(parseClause(
        "hasDifferentNumberOfItems(X, Y) :- bagItems(X, A), bagItems(Y, B), fn_eq(U, A, B), fn_is_false(U)."));

    // Query kb
    // hasDifferentNumberOfItems(X, Y)?
    Solver solver = new Solver(kb);
    Literal query = parseQuery("hasDifferentNumberOfItems(_, _)?");
    Set<Fact> answers = Sets.newHashSet(solver.solve(query, 2));

    // Verify answers
    Assert.assertEquals(2, answers.size());
  }

  @Test
  public void testSampleOfSize3() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("bagItems(\"red_bag\", 1)."));
    kb.azzert(parseClause("bagItems(\"green_bag\", 2)."));
    kb.azzert(parseClause("bagItems(\"blue_bag\", 2)."));

    // Init kb with rules
    kb.azzert(parseClause(
        "hasDifferentNumberOfItems(X, Y) :- bagItems(X, A), bagItems(Y, B), fn_eq(U, A, B), fn_is_false(U)."));

    // Query kb
    // hasDifferentNumberOfItems(X, Y)?
    Solver solver = new Solver(kb);
    Literal query = parseQuery("hasDifferentNumberOfItems(_, _)?");
    Set<Fact> answers = Sets.newHashSet(solver.solve(query, 3));

    // Verify answers
    Assert.assertEquals(3, answers.size());
  }

  @Test
  public void testSampleOfSize4() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("bagItems(\"red_bag\", 1)."));
    kb.azzert(parseClause("bagItems(\"green_bag\", 2)."));
    kb.azzert(parseClause("bagItems(\"blue_bag\", 2)."));

    // Init kb with rules
    kb.azzert(parseClause(
        "hasDifferentNumberOfItems(X, Y) :- bagItems(X, A), bagItems(Y, B), fn_eq(U, A, B), fn_is_false(U)."));

    // Query kb
    // hasDifferentNumberOfItems(X, Y)?
    Solver solver = new Solver(kb);
    Literal query = parseQuery("hasDifferentNumberOfItems(_, _)?");
    Set<Fact> answers = Sets.newHashSet(solver.solve(query, 4));

    // Verify answers
    Assert.assertEquals(4, answers.size());
  }

  @Test
  public void testSampleOfSize5() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseClause("bagItems(\"red_bag\", 1)."));
    kb.azzert(parseClause("bagItems(\"green_bag\", 2)."));
    kb.azzert(parseClause("bagItems(\"blue_bag\", 2)."));

    // Init kb with rules
    kb.azzert(parseClause(
        "hasDifferentNumberOfItems(X, Y) :- bagItems(X, A), bagItems(Y, B), fn_eq(U, A, B), fn_is_false(U)."));

    // Query kb
    // hasDifferentNumberOfItems(X, Y)?
    Solver solver = new Solver(kb);
    Literal query = parseQuery("hasDifferentNumberOfItems(_, _)?");
    Set<Fact> answers = Sets.newHashSet(solver.solve(query, 5));

    // Verify answers
    Assert.assertEquals(4, answers.size());
  }

  @Test
  public void testProofHashcodeAndEquals() {
    Literal blue = new Literal("red", newConst("abc"));
    Literal red = new Literal("blue", newConst(123));
    EqualsVerifier.forClass(Proofer.Proof.class).withPrefabValues(Literal.class, red, blue).verify();
  }

  @Test
  public void testNodeHashcodeAndEquals() {
    Literal blue = new Literal("red", newConst("abc"));
    Literal red = new Literal("blue", newConst(123));
    EqualsVerifier.forClass(Proofer.Node.class).withIgnoredFields("bodies_").withIgnoredFields("rules_")
        .withPrefabValues(Literal.class, red, blue).verify();
  }
}