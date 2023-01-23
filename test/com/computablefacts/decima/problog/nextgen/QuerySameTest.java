package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://github.com/ML-KULeuven/problog/blob/master/test/query_same.pl
 */
public class QuerySameTest extends AbstractTest {

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("a(2, 3).");
    facts.add("a(1, 1).");

    return facts;
  }

  @Override
  protected List<String> rules() {

    List<String> rules = new ArrayList<>();
    rules.add("p(X) :- a(X, X).");

    return rules;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();
    queries.put("a(1, 1)?", "a(1, 1).");
    queries.put("p(1)?", "p(1).");

    return queries;
  }
}