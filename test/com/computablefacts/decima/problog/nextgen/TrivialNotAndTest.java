package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://github.com/ML-KULeuven/problog/blob/master/test/00_trivial_not_and.pl
 */
public class TrivialNotAndTest extends AbstractTest {

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("0.5::f(a).");
    facts.add("0.3::f(b).");

    return facts;
  }

  @Override
  protected List<String> rules() {

    List<String> rules = new ArrayList<>();
    rules.add("q(X, Y) :- f(X), f(Y).");
    rules.add("p(X, Y) :- ~q(X, Y).");

    return rules;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();
    queries.put("p(a, b)?", "0.85::p(a, b).");

    return queries;
  }
}