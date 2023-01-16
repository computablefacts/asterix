package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://github.com/ML-KULeuven/problog/blob/master/test/01_queries.pl
 */
public class Queries01Test extends AbstractTest {

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("0.3::p(1).");
    facts.add("0.2::p(2).");
    facts.add("0.1::p(3).");

    return facts;
  }

  @Override
  protected List<String> rules() {

    List<String> rules = new ArrayList<>();
    rules.add("0.5::p(X) :- X > 0, fn_sub(X2, X, 1), p(X2).");

    return rules;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();
    queries.put("p(1)?", "0.3::p(1).");
    queries.put("p(2)?", "0.32::p(2).");
    queries.put("p(3)?", "0.244::p(3).");

    return queries;
  }
}