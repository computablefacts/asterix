package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://github.com/ML-KULeuven/problog/blob/master/test/tc_3.pl
 */
public class TC3Test extends AbstractTest {

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("athlet(1).");
    facts.add("athlet(2).");
    facts.add("student(2).");
    facts.add("student(3).");

    return facts;
  }

  @Override
  protected List<String> rules() {

    List<String> rules = new ArrayList<>();
    rules.add("0.5::stressed(X) :- student(X).");
    rules.add("0.2::stressed(X) :- athlet(X).");

    return rules;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();
    queries.put("stressed(1)?", "0.2::stressed(1).");
    queries.put("stressed(2)?", "0.6::stressed(2).");
    queries.put("stressed(3)?", "0.5::stressed(3).");

    return queries;
  }
}