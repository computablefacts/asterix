package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://github.com/ML-KULeuven/problog/blob/master/test/non_ground_query.pl
 */
public class NonGroundQueryTest extends AbstractTest {

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("0.1::b(1).");
    facts.add("0.2::b(2).");
    facts.add("0.3::e(1).");
    facts.add("0.4::e(3).");
    facts.add("d(1).");
    facts.add("d(2).");
    facts.add("d(3).");

    return facts;
  }

  @Override
  protected List<String> rules() {

    List<String> rules = new ArrayList<>();
    rules.add("a(X) :- b(2), c(X,Y).");
    rules.add("c(X,Y) :- c(X,Z), c(Z,Y).");
    rules.add("c(X,Y) :- d(X), d(Y).");

    return rules;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();
    queries.put("a(1)?", "0.2::a(1).");
    queries.put("a(2)?", "0.2::a(2).");
    queries.put("a(3)?", "0.2::a(3).");

    return queries;
  }
}