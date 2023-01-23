package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DoubleNegationTest extends AbstractTest {

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("boy(a).");
    facts.add("boy(b).");
    facts.add("girl(a).");
    facts.add("girl(c).");

    return facts;
  }

  @Override
  protected List<String> rules() {

    List<String> rules = new ArrayList<>();
    rules.add("isBoy(X) :- boy(X), ~girl(X).");
    rules.add("isGirl(X) :- girl(X), ~boy(X).");
    rules.add("isBoyNotGirl(X) :- isBoy(X), ~isGirl(X).");
    rules.add("isGirlNotBoy(X) :- isGirl(X), ~isBoy(X).");
    rules.add("match(X, Y) :- match(Y, X).");
    rules.add("match(X, Y) :- isBoyNotGirl(X), ~isGirlNotBoy(X), isGirlNotBoy(Y), ~isBoyNotGirl(Y).");

    return rules;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();
    queries.put("match(a, a)?", null);
    queries.put("match(a, b)?", null);
    queries.put("match(a, c)?", null);
    queries.put("match(b, a)?", null);
    queries.put("match(b, b)?", null);
    queries.put("match(b, c)?", "match(b, c).");
    queries.put("match(c, a)?", null);
    queries.put("match(c, b)?", "match(c, b).");
    queries.put("match(c, c)?", null);

    return queries;
  }
}