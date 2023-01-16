package com.computablefacts.decima.problog.nextgen;

import com.computablefacts.decima.problog.AbstractClause;
import com.computablefacts.decima.problog.AbstractFunctions;
import com.computablefacts.decima.problog.AbstractKnowledgeBase;
import com.computablefacts.decima.problog.Fact;
import com.computablefacts.decima.problog.KnowledgeBaseMemoryBacked;
import com.computablefacts.decima.problog.Literal;
import com.computablefacts.decima.problog.Parser;
import com.computablefacts.decima.problog.ProbabilityEstimator;
import com.computablefacts.decima.problog.Solver;
import com.computablefacts.decima.problog.SubgoalDiskBacked;
import com.computablefacts.decima.problog.SubgoalMemoryBacked;
import com.computablefacts.nona.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CheckReturnValue;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;

@CheckReturnValue
public abstract class AbstractTest {

  public AbstractTest() {
  }

  @Test
  public void test() {
    execute();
  }

  public void execute() {

    AbstractKnowledgeBase kb = new KnowledgeBaseMemoryBacked();
    facts().forEach(fact -> kb.azzert(Parser.parseFact(fact)));
    rules().forEach(rule -> kb.azzert(Parser.parseRule(rule)));

    AbstractFunctions functions = new AbstractFunctions();
    functions().forEach(fn -> functions.register(fn.name(), fn));

    for (Solver solver : datalog(kb, functions)) {
      for (Map.Entry<String, String> entry : queries().entrySet()) {

        Literal query = Parser.parseQuery(entry.getKey());
        Set<Fact> answers = Sets.newHashSet(solver.solve(query));

        if (entry.getValue() == null) {
          Assert.assertTrue(String.format("no answer should be returned for query %s", query), answers.isEmpty());
        } else {

          Fact fact = Parser.parseFact(entry.getValue());
          Fact expected = new Fact(new Literal(fact.head().predicate().name(), fact.head().terms()));

          Assert.assertTrue(String.format("missing fact %s from answers for query %s", fact, query),
              answers.contains(expected));
        }
      }
    }
    for (Solver solver : probLog(kb, functions)) {
      for (Map.Entry<String, String> entry : queries().entrySet()) {

        Literal query = Parser.parseQuery(entry.getKey());
        Set<Fact> answers = Sets.newHashSet(solver.solve(query));
        Set<AbstractClause> proofs = solver.proofs(query);
        ProbabilityEstimator estimator = new ProbabilityEstimator(proofs);

        if (entry.getValue() == null) {
          Assert.assertTrue(String.format("no answer should be returned for query %s", query), answers.isEmpty());
        } else {

          Fact fact = Parser.parseFact(entry.getValue());
          Fact expected = new Fact(new Literal(fact.head().predicate().name(), fact.head().terms()));

          Assert.assertTrue(String.format("missing fact %s from answers for query %s", fact, query),
              answers.contains(expected));
          Assert.assertEquals(String.format(
                  "mismatch between the expected probability (%s) and the actual probability (%s) of fact %s for query %s",
                  expected.head().probability(), estimator.probability(expected), fact, query), 0,
              expected.head().probability().compareTo(estimator.probability(expected)));
        }
      }
    }
  }

  protected List<String> facts() {
    return Lists.newArrayList();
  }

  protected List<String> rules() {
    return Lists.newArrayList();
  }

  protected List<Function> functions() {
    return Lists.newArrayList();
  }

  private List<Solver> probLog(AbstractKnowledgeBase kb, AbstractFunctions functions) {
    try {
      String path = Files.createTempDirectory("problog").toFile().getAbsolutePath();
      return Lists.newArrayList(new Solver(kb, functions, true, SubgoalMemoryBacked::new),
          new Solver(kb, functions, true, literal -> new SubgoalDiskBacked(literal, path)));
    } catch (IOException e) {
      return Lists.newArrayList(new Solver(kb, functions, true, SubgoalMemoryBacked::new));
    }
  }

  private List<Solver> datalog(AbstractKnowledgeBase kb, AbstractFunctions functions) {
    try {
      String path = Files.createTempDirectory("datalog").toFile().getAbsolutePath();
      return Lists.newArrayList(new Solver(kb, functions, false, SubgoalMemoryBacked::new),
          new Solver(kb, functions, false, literal -> new SubgoalDiskBacked(literal, path)));
    } catch (IOException e) {
      return Lists.newArrayList(new Solver(kb, functions, false, SubgoalMemoryBacked::new));
    }
  }

  protected abstract Map<String, String> queries();
}