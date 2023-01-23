package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/**
 * https://github.com/ML-KULeuven/problog/blob/master/test/nonground.pl
 */
public class NonGroundTest extends AbstractTest {

  @Override
  @Test(expected = IllegalStateException.class)
  public void test() {
    execute();
  }

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("0.4::b(1).");
    facts.add("0.4::b(2).");
    facts.add("0.4::c(1).");
    facts.add("0.4::c(2).");

    return facts;
  }

  @Override
  protected List<String> rules() {

    List<String> rules = new ArrayList<>();
    rules.add("0.4::a(X,Y) :- \\+b(X), \\+c(Y).");

    return rules;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();
    queries.put("a(_, _)?", null); // Negation on non-ground probabilistic facts are forbidden

    return queries;
  }
}