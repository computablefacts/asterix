package com.computablefacts.decima.problog.graphs;

import static com.computablefacts.decima.problog.AbstractTerm.newConst;
import static com.computablefacts.decima.problog.Parser.parseFact;
import static com.computablefacts.decima.problog.Parser.parseRule;
import static com.computablefacts.decima.problog.TestUtils.checkAnswers;

import com.computablefacts.asterix.nlp.WildcardMatcher;
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

public class ToothacheTest {

  @Test
  public void testToothache() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseFact("0.10::cavity(a)."));
    kb.azzert(parseFact("0.05::gum_disease(a)."));

    // Init kb with rules
    kb.azzert(parseRule("1.00::toothache(X) :- cavity(X), gum_disease(X)."));
    kb.azzert(parseRule("0.60::toothache(X) :- cavity(X), ~gum_disease(X)."));
    kb.azzert(parseRule("0.30::toothache(X) :- ~cavity(X), gum_disease(X)."));
    kb.azzert(parseRule("0.05::toothache(X) :- ~cavity(X), ~gum_disease(X)."));

    // Query kb
    // path(1, 6)?
    Solver solver = new Solver(kb);
    Literal query = new Literal("toothache", newConst("a"));
    Set<AbstractClause> proofs = solver.proofs(query);
    Set<AbstractClause> answers = Sets.newHashSet(solver.solve(query));

    // Verify subgoals
    Assert.assertEquals(8, solver.nbSubgoals());

    // Verify answers
    Assert.assertEquals(4, proofs.size());
    Assert.assertEquals(1, answers.size());

    Rule answer1 = parseRule("toothache(a) :- 0.9::~cavity(a), 0.95::~gum_disease(a).");
    Rule answer2 = parseRule("toothache(a) :- 0.1::cavity(a), 0.05::gum_disease(a).");
    Rule answer3 = parseRule("toothache(a) :- 0.9::~cavity(a), 0.05::gum_disease(a).");
    Rule answer4 = parseRule("toothache(a) :- 0.1::cavity(a), 0.95::~gum_disease(a).");

    Assert.assertTrue(checkAnswers(answers, Sets.newHashSet(answer1, answer2, answer3, answer4)));

    // Verify BDD answer
    // 0.11825::toothache(a).
    ProbabilityEstimator estimator = new ProbabilityEstimator(proofs);
    BigDecimal probability = estimator.probability(query, 5);

    Assert.assertEquals(0, BigDecimal.valueOf(0.11082).compareTo(probability));
  }

  @Test
  public void testExtractClausesInProofs() {

    // Create kb
    KnowledgeBaseMemoryBacked kb = new KnowledgeBaseMemoryBacked();

    // Init kb with facts
    kb.azzert(parseFact("0.10::cavity(a)."));
    kb.azzert(parseFact("0.05::gum_disease(a)."));

    // Init kb with rules
    kb.azzert(parseRule("1.00::toothache(X) :- cavity(X), gum_disease(X)."));
    kb.azzert(parseRule("0.60::toothache(X) :- cavity(X), ~gum_disease(X)."));
    kb.azzert(parseRule("0.30::toothache(X) :- ~cavity(X), gum_disease(X)."));
    kb.azzert(parseRule("0.05::toothache(X) :- ~cavity(X), ~gum_disease(X)."));

    // Query kb
    // path(1, 6)?
    Solver solver = new Solver(kb);
    Literal query = new Literal("toothache", newConst("a"));
    List<String> table = solver.tableOfProofs(query);

    Assert.assertEquals(11, table.size());
    Assert.assertTrue(WildcardMatcher.match(Joiner.on("\n").join(table),
        "[fact] depth=0, 0.05::gum_disease(\"a\").\n" + "[fact] depth=0, 0.05::proba(\"???????\").\n"
            + "[fact] depth=0, 0.1::cavity(\"a\").\n" + "[fact] depth=0, 0.3::proba(\"???????\").\n"
            + "[fact] depth=0, 0.6::proba(\"???????\").\n" + "[fact] depth=0, 0.95::~gum_disease(\"a\").\n"
            + "[fact] depth=0, 0.9::~cavity(\"a\").\n"
            + "[rule] depth=0, toothache(\"a\") :- 0.05::gum_disease(\"a\"), 0.9::~cavity(\"a\"), 0.3::proba(\"???????\").\n"
            + "[rule] depth=0, toothache(\"a\") :- 0.1::cavity(\"a\"), 0.05::gum_disease(\"a\").\n"
            + "[rule] depth=0, toothache(\"a\") :- 0.1::cavity(\"a\"), 0.95::~gum_disease(\"a\"), 0.6::proba(\"???????\").\n"
            + "[rule] depth=0, toothache(\"a\") :- 0.9::~cavity(\"a\"), 0.95::~gum_disease(\"a\"), 0.05::proba(\"???????\")."));
  }
}
