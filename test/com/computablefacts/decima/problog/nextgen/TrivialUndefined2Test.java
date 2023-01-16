package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://github.com/ML-KULeuven/problog/blob/master/test/00_trivial_undefined2.pl
 */
public class TrivialUndefined2Test extends AbstractTest {

  @Override
  protected List<String> rules() {

    List<String> rules = new ArrayList<>();
    rules.add("q(X) :- p(X).");

    return rules;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();
    queries.put("q(_)?", null);

    return queries;
  }
}