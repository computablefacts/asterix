package com.computablefacts.decima.problog.nextgen;

import com.computablefacts.asterix.View;
import com.computablefacts.decima.problog.AbstractFunctions;
import com.computablefacts.decima.problog.AbstractKnowledgeBase;
import com.computablefacts.decima.problog.Fact;
import com.computablefacts.decima.problog.KnowledgeBaseMemoryBacked;
import com.computablefacts.decima.problog.Literal;
import com.computablefacts.decima.problog.Parser;
import com.computablefacts.decima.problog.Proofer;
import com.computablefacts.decima.problog.SubgoalDiskBacked;
import com.computablefacts.decima.problog.SubgoalMemoryBacked;
import com.computablefacts.nona.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CheckReturnValue;
import java.io.IOException;
import java.math.BigDecimal;
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

    for (Proofer solver : solvers(kb, functions)) {
      for (Map.Entry<String, String> entry : queries().entrySet()) {

        Literal query = Parser.parseQuery(entry.getKey());
        Set<Fact> answers = Sets.newHashSet(solver.solve(query));
        BigDecimal probability = solver.probability(query, 5).getOrThrow();

        if (entry.getValue() == null) {
          Assert.assertTrue(String.format("no answer should be returned for query %s", query), answers.isEmpty());
        } else {

          Fact fact = Parser.parseFact(entry.getValue());
          Fact expected = new Fact(new Literal(fact.head().predicate().name(), fact.head().terms()));

          Assert.assertTrue(String.format("missing answer for query %s : %s", query, expected),
              factBelongsToAnswers(fact, answers));

          Assert.assertEquals(String.format(
              "mismatch between the fact expected probability (%s) and the fact actual probability (%s) for query %s",
              fact.head().probability(), probability, query), 0, probability.compareTo(fact.head().probability()));
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

  private List<Proofer> solvers(AbstractKnowledgeBase kb, AbstractFunctions functions) {
    try {
      String path = Files.createTempDirectory("problog").toFile().getAbsolutePath();
      return Lists.newArrayList(new Proofer(kb, functions, SubgoalMemoryBacked::new),
          new Proofer(kb, functions, literal -> new SubgoalDiskBacked(literal, path)));
    } catch (IOException e) {
      return Lists.newArrayList(new Proofer(kb, functions, SubgoalMemoryBacked::new));
    }
  }

  private boolean factBelongsToAnswers(Fact fact, Set<Fact> answers) {

    Fact expected = new Fact(new Literal(fact.head().predicate().name(), fact.head().terms()));

    return View.of(answers).anyMatch(f -> {
      Fact actual = new Fact(new Literal(f.head().predicate().name(), f.head().terms()));
      return expected.equals(actual);
    });
  }

  protected abstract Map<String, String> queries();
}