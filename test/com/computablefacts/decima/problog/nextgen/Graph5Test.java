package com.computablefacts.decima.problog.nextgen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * https://github.com/ML-KULeuven/problog/blob/master/test/7_probabilistic_graph.pl
 */
public class Graph5Test extends AbstractTest {

  @Override
  protected List<String> facts() {

    List<String> facts = new ArrayList<>();
    facts.add("0.6::edge(1,2).");
    facts.add("0.1::edge(1,3).");
    facts.add("0.4::edge(2,5).");
    facts.add("0.3::edge(2,6).");
    facts.add("0.3::edge(3,4).");
    facts.add("0.8::edge(4,5).");
    facts.add("0.2::edge(5,6).");

    return facts;
  }

  @Override
  protected List<String> rules() {

    List<String> rules = new ArrayList<>();
    rules.add("path(X, Y) :- edge(X, Y).");
    rules.add("path(X, Y) :- path(X, Z), fn_eq(U, X, Z), fn_is_false(U), edge(Z, Y).");

    return rules;
  }

  @Override
  protected Map<String, String> queries() {

    Map<String, String> queries = new HashMap<>();
    queries.put("path(1, 2)?", "0.6::path(1, 2).");
    queries.put("path(1, 3)?", "0.1::path(1, 3).");
    queries.put("path(1, 4)?", "0.03::path(1, 4).");
    queries.put("path(1, 5)?", "0.25824::path(1, 5).");
    queries.put("path(1, 6)?", "0.21673::path(1, 6).");
    queries.put("path(2, 5)?", "0.4::path(2, 5).");
    queries.put("path(2, 6)?", "0.356::path(2, 6).");
    queries.put("path(3, 4)?", "0.3::path(3, 4).");
    queries.put("path(3, 5)?", "0.24::path(3, 5).");
    queries.put("path(3, 6)?", "0.048::path(3, 6).");
    queries.put("path(4, 5)?", "0.8::path(4, 5).");
    queries.put("path(4, 6)?", "0.16::path(4, 6).");
    queries.put("path(5, 6)?", "0.2::path(5, 6).");

    return queries;
  }
}